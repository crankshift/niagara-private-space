package io.github.crankshift.niagaraprivatespace.hook

import io.github.crankshift.niagaraprivatespace.decision.ProfileKind
import java.lang.reflect.Field
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Niagara's two per-item profile flags: "is primary user" and "is Private Space". Niagara checks them in many
 * places, and some of them (the Private Space section of the app list, hiding private apps from search) must
 * keep seeing a private app as private, so the module never changes the flags for good: [whileSeenAs] changes
 * them for the duration of one check and restores them. See
 * docs/adr/0001-change-the-profile-flags-for-one-check.md.
 *
 * The flags are final fields, which reflection may write after `setAccessible`. [canWriteFinalFields] checks
 * that the runtime allows it, so the module can stay inactive instead of failing on every long-press. If a
 * write is refused anyway, the hook that asked for it throws, the framework's protective mode runs Niagara's
 * own code, and private apps stay blocked: it fails closed.
 */
class ItemFlags(private val primaryUser: Field, private val privateSpace: Field) {
    init {
        primaryUser.isAccessible = true
        privateSpace.isAccessible = true
    }

    /** The class every item extends, which is where both flags live. */
    val itemClass: Class<*> = privateSpace.declaringClass

    /** The profile Niagara believes [item] is from. An item from no known profile counts as work. */
    fun kindOf(item: Any): ProfileKind =
        when {
            privateSpace.getBoolean(item) -> ProfileKind.PRIVATE
            primaryUser.getBoolean(item) -> ProfileKind.PERSONAL
            else -> ProfileKind.WORK
        }

    /**
     * Runs [block] with [item] looking like an app from [seen] to Niagara, then puts the flags back. Inside a
     * [FlagChangeScope] the flags are put back when the scope ends instead, for checks that run after the hooked method.
     * Calls are serialised: two overlapping calls on one item (a picker filtering on a background thread while the
     * user long-presses) would otherwise restore each other's half-changed values and leave the item looking like
     * a work app for good. Niagara's own reads are not locked, so they can still see the changed flags for the
     * length of the call.
     */
    fun <T> whileSeenAs(item: Any, seen: ProfileKind, block: () -> T): T =
        lock.withLock {
            val wasPrimaryUser = primaryUser.getBoolean(item)
            val wasPrivateSpace = privateSpace.getBoolean(item)
            val nowPrimaryUser = seen == ProfileKind.PERSONAL
            val nowPrivateSpace = seen == ProfileKind.PRIVATE
            if (wasPrimaryUser == nowPrimaryUser && wasPrivateSpace == nowPrivateSpace) return@withLock block()

            val restore = {
                try {
                    primaryUser.setBoolean(item, wasPrimaryUser)
                } finally {
                    privateSpace.setBoolean(item, wasPrivateSpace)
                }
            }
            try {
                primaryUser.setBoolean(item, nowPrimaryUser)
                privateSpace.setBoolean(item, nowPrivateSpace)
            } catch (t: Throwable) {
                restore()
                throw t
            }
            val scope = FlagChangeScope.current.get()
            if (scope != null) {
                // The changed flags outlive this call, so nobody else may touch the item until the scope ends.
                lock.lock()
                scope.locksHeld++
                scope.restoreLater += restore
                return@withLock block()
            }
            try {
                block()
            } finally {
                restore()
            }
        }

    /** Makes [item] look like an app from [seen] for good. Only for items that are built for a single screen. */
    fun showForGoodAs(item: Any, seen: ProfileKind) {
        lock.withLock {
            primaryUser.setBoolean(item, seen == ProfileKind.PERSONAL)
            privateSpace.setBoolean(item, seen == ProfileKind.PRIVATE)
        }
    }

    /**
     * A stretch of Niagara code that checks the flags after the hooked method that changed them has returned,
     * such as the options sheet's constructor, which asks the options builder and then checks a flag itself.
     * Items changed while it is open are restored, last first, when it closes. It takes the lock only once an item
     * is actually changed, and keeps it until it closes, so a scope in which nothing changes costs nothing.
     */
    class FlagChangeScope {
        internal val restoreLater = mutableListOf<() -> Unit>()
        internal var locksHeld = 0

        fun <T> around(block: () -> T): T {
            if (current.get() != null) return block()
            current.set(this)
            try {
                return block()
            } finally {
                current.remove()
                try {
                    runAll(restoreLater.asReversed())
                } finally {
                    repeat(locksHeld) { lock.unlock() }
                }
            }
        }

        /** Runs every action even when one fails, then rethrows the first failure. */
        private fun runAll(actions: List<() -> Unit>) {
            var failure: Throwable? = null
            for (action in actions) {
                try {
                    action()
                } catch (t: Throwable) {
                    failure?.addSuppressed(t) ?: run { failure = t }
                }
            }
            failure?.let { throw it }
        }

        companion object {
            internal val current = ThreadLocal<FlagChangeScope?>()
        }
    }

    companion object {
        internal val lock = ReentrantLock()

        /** A final field of a class of our own, written the way [whileSeenAs] writes Niagara's. */
        fun canWriteFinalFields(): Boolean =
            try {
                val probe = Probe()
                val field = Probe::class.java.getDeclaredField("flag")
                field.isAccessible = true
                field.setBoolean(probe, false)
                !field.getBoolean(probe)
            } catch (_: Throwable) {
                false
            }
    }

    private class Probe {
        @JvmField
        val flag: Boolean = true
    }
}
