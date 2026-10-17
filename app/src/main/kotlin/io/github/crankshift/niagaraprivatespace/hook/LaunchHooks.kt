package io.github.crankshift.niagaraprivatespace.hook

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import android.os.UserHandle
import io.github.crankshift.niagaraprivatespace.decision.PendingApp
import io.github.crankshift.niagaraprivatespace.decision.ProfileKind
import io.github.crankshift.niagaraprivatespace.decision.TapAction
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Makes a tap on a locked private app bring up Android's unlock screen and then open the app. Niagara opens every
 * app through one method, and in the 1638 bytecode that method is the only caller of `startMainActivity`, which
 * is how it starts apps of other profiles. For a locked private app that does nothing useful, so the hook asks the
 * decision module what a tap does. When the answer is to unlock first, the hook sends the unlock request and the
 * decision module keeps the app as the pending app. A receiver opens it when Android announces that Private
 * Space's user is accessible. The module opens the app itself because, from AOSP's source, the variant of
 * Android's request that starts an app afterwards needs a permission Niagara does not hold.
 */
class LaunchHooks(private val flags: ItemFlags, private val log: ModuleLog) {
    private val pendingApp = PendingApp<ComponentName>()
    private val receiverRegistered = AtomicBoolean(false)

    /** Taps and broadcasts are expected on the main thread; the decision module is not thread-safe, so this locks. */
    private fun <T> withPendingApp(block: PendingApp<ComponentName>.() -> T): T =
        synchronized(pendingApp) { pendingApp.block() }

    fun install(hooks: InstalledHooks, launch: Method, target: Field, component: Field) {
        target.isAccessible = true
        component.isAccessible = true
        hooks.hook(launch) { chain ->
            val item = checkNotNull(chain.thisObject) { "the launch method has no item" }
            val kind = flags.kindOf(item)
            val locked = PrivateSpaceState.isLockedFor(kind)
            val app = { component.get(target.get(item)) as ComponentName }
            when (withPendingApp { tap(kind, locked, now(), app) }) {
                TapAction.OPEN_NOW -> chain.proceed()
                // When the request cannot be made there is no unlock to wait for, so Niagara tries as it always did.
                TapAction.UNLOCK_FIRST -> if (requestUnlock()) null else chain.proceed()
            }
        }
    }

    /** Sends the unlock request. False when it could not be made; the pending app is then forgotten. */
    private fun requestUnlock(): Boolean =
        try {
            listenForPrivateSpace(PrivateSpaceState.applicationContext())
            PrivateSpaceState.requestUnlock()
            log.info("asked Android to unlock Private Space")
            true
        } catch (t: Throwable) {
            withPendingApp { unlockCancelled() }
            log.warn("asking Android to unlock Private Space failed", t)
            false
        }

    /** One receiver for the life of Niagara's process, registered with the first tap that needs it. */
    private fun listenForPrivateSpace(context: Context) {
        if (!receiverRegistered.compareAndSet(false, true)) return
        try {
            val filter = IntentFilter(Intent.ACTION_PROFILE_ACCESSIBLE)
            filter.addAction(Intent.ACTION_PROFILE_AVAILABLE)
            filter.addAction(Intent.ACTION_PROFILE_UNAVAILABLE)
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } catch (t: Throwable) {
            receiverRegistered.set(false)
            throw t
        }
    }

    /**
     * Private Space is available when Android says its user is accessible (running, storage unlocked), because that
     * is when its apps can start. Android also announces that Private Space was unlocked, possibly before that, so
     * that broadcast counts only if the user is already accessible. An exception thrown from a receiver would take
     * Niagara down, so every failure is logged and dropped.
     */
    private val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                try {
                    val user = intent.getParcelableExtra(Intent.EXTRA_USER, UserHandle::class.java) ?: return
                    val kind = PrivateSpaceState.profileKindOf(user)
                    // The decision ignores other profiles anyway; returning here keeps the log about Private Space.
                    if (kind != ProfileKind.PRIVATE) return
                    val action = intent.action
                    when (action) {
                        Intent.ACTION_PROFILE_ACCESSIBLE -> openPendingApp(user, kind, action)
                        Intent.ACTION_PROFILE_AVAILABLE ->
                            if (PrivateSpaceState.isAccessible(user)) {
                                openPendingApp(user, kind, action)
                            } else {
                                log.info("Private Space is available but not accessible yet: waiting for $ACCESSIBLE")
                            }
                        Intent.ACTION_PROFILE_UNAVAILABLE -> {
                            log.info("Private Space locked: forgetting any pending app")
                            withPendingApp { profileBecameUnavailable(kind) }
                        }
                    }
                } catch (t: Throwable) {
                    log.warn("handling a profile broadcast failed", t)
                }
            }
        }

    private fun openPendingApp(user: UserHandle, kind: ProfileKind, action: String?) {
        val app = withPendingApp { profileBecameAvailable(kind, now()) }
        if (app == null) {
            log.info("Private Space is available ($action), no app is pending")
            return
        }
        log.info("Private Space is available ($action): opening ${app.flattenToShortString()}")
        PrivateSpaceState.launcherApps().startMainActivity(app, user, null, null)
    }

    private fun now() = SystemClock.elapsedRealtime()

    private companion object {
        const val ACCESSIBLE = "PROFILE_ACCESSIBLE"
    }
}
