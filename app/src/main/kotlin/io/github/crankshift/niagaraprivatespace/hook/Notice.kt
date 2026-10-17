package io.github.crankshift.niagaraprivatespace.hook

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import io.github.crankshift.niagaraprivatespace.decision.Activation
import io.github.crankshift.niagaraprivatespace.decision.Decisions
import io.github.crankshift.niagaraprivatespace.decision.Hook
import io.github.crankshift.niagaraprivatespace.decision.NoticeChannel
import io.github.crankshift.niagaraprivatespace.finder.BorrowedTip
import io.github.crankshift.niagaraprivatespace.finder.Resolved
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Field
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Tells the user that the module does not support this Niagara version, once per Niagara version. The decision
 * module says whether it is due and how: as a dismissable tip on Niagara's home page, in Niagara's own style, or as
 * a Toast when Niagara's tip system cannot be used. Nothing here changes how Niagara behaves otherwise.
 *
 * The tip borrows Niagara's tip manager: the module registers a tip of a type that only Xiaomi phones use (from
 * reading Niagara's code, nothing else registers or removes it elsewhere), with a provider and two callbacks that
 * are dynamic proxies, because Niagara's method names change with every release. The proxies tell the methods
 * apart by their types. The tip's resources ("button_dismiss", "ic_ip_act_info") are looked up by name at runtime;
 * resources are not obfuscated, and the finder cannot see them.
 *
 * Failing to show the tip must never hurt Niagara: the notice waits until the home page is up and registers the tip
 * then, so that (from reading Niagara's code) Niagara builds it on the spot, inside this class's try block; if that
 * fails, the tip is taken back and a Toast is shown instead. The version last told about is kept in a preferences
 * file of the module's own, inside Niagara's data, and is written (synchronously) before the tip is registered, so
 * at worst a version is told about once. A version that cannot be read counts as version 0, told once.
 */
class Notice(
    private val xposed: XposedInterface,
    private val resolved: Resolved,
    private val classLoader: ClassLoader,
    private val activation: Activation,
    private val version: NiagaraVersion?,
    private val log: ModuleLog,
) {
    private val main = Handler(Looper.getMainLooper())
    private val versionCode = version?.code ?: 0L
    private val text = Decisions.noticeText(version?.name ?: "?")

    /** Starts telling the user, the way the decision module chooses. Never throws. */
    fun start() {
        try {
            when (Decisions.noticeChannel(activation)) {
                NoticeChannel.TIP -> startTip()
                NoticeChannel.TOAST -> showToastWhenReady()
            }
        } catch (t: Throwable) {
            log.warn("telling the user about the unsupported Niagara version failed", t)
            runCatching { showToastWhenReady() }
        }
    }

    private fun startTip() {
        val manager = resolved.methodsOf(Hook.TIP_MANAGER).single().getConstructorInstance(classLoader)
        val parts =
            TipParts(
                register = resolved.methodsOf(Hook.TIP_REGISTER).single().getMethodInstance(classLoader),
                factory = resolved.methodsOf(Hook.TIP_FACTORY).single().getMethodInstance(classLoader),
                homePage = requireNotNull(resolved.fieldOf(Hook.TIP_HOME_PAGE)).getFieldInstance(classLoader),
                paused = requireNotNull(resolved.fieldOf(Hook.TIP_PAUSED)).getFieldInstance(classLoader),
                factoryInstance =
                    requireNotNull(resolved.fieldOf(Hook.TIP_FACTORY_INSTANCE)).getFieldInstance(classLoader),
            )
        xposed.hook(manager).intercept { chain ->
            val result = chain.proceed()
            val created = checkNotNull(chain.thisObject) { "the tip manager is unknown" }
            // Niagara has not finished creating the manager yet: wait for the home page after it returns.
            main.post { showTipWhenHomePageIsUp(created, parts, attempt = 0) }
            result
        }
    }

    private fun showTipWhenHomePageIsUp(manager: Any, parts: TipParts, attempt: Int) {
        try {
            if (!isDue()) return
            if (parts.homePage.get(manager) != null && !parts.paused.getBoolean(manager)) {
                showTip(manager, parts)
            } else if (attempt < MAX_ATTEMPTS) {
                main.postDelayed({ showTipWhenHomePageIsUp(manager, parts, attempt + 1) }, RETRY_MILLIS)
            } else {
                log.info("the home page did not come up in time for the unsupported-version tip; trying next start")
            }
        } catch (t: Throwable) {
            log.warn("showing the unsupported-version tip failed", t)
            showToastOnce()
        }
    }

    private fun showTip(manager: Any, parts: TipParts) {
        // Building the tip's pieces can fail (a missing resource, a renamed tip type). Nothing is told yet, so the
        // caller falls back to the Toast, which still counts as the first notice.
        val factory = checkNotNull(parts.factoryInstance.get(parts.homePage.get(manager))) { "no tip factory" }
        val tip = Tip(parts, factory, text, PrivateSpaceState.applicationContext())
        markNotified()
        try {
            parts.register.invoke(manager, tip.type, tip.provider)
            log.info("showing the unsupported-version tip")
        } catch (t: Throwable) {
            log.warn("the unsupported-version tip failed; taking it back and showing a Toast instead", t)
            tip.dismiss()
            runCatching { parts.register.invoke(manager, tip.type, tip.provider) }
            runCatching { showToast() }.onFailure { log.warn("showing the unsupported-version Toast failed", it) }
        }
    }

    private fun showToastWhenReady() {
        // A message posted now runs after the one that is starting Niagara's Application, so the Application should
        // exist by then (unverified on the phone).
        main.post { showToastOnce() }
    }

    /** Never throws: it runs from the main looper, where an exception would take Niagara down. */
    private fun showToastOnce() {
        try {
            if (isDue()) {
                markNotified()
                showToast()
            }
        } catch (t: Throwable) {
            log.warn("showing the unsupported-version Toast failed", t)
        }
    }

    private fun showToast() {
        Toast.makeText(PrivateSpaceState.applicationContext(), text, Toast.LENGTH_LONG).show()
        log.info("showed the unsupported-version notice as a Toast")
    }

    private fun isDue() = Decisions.noticeDue(activation, versionCode, lastNotifiedVersionCode())

    private fun lastNotifiedVersionCode(): Long? =
        preferences().takeIf { it.contains(LAST_NOTIFIED) }?.getLong(LAST_NOTIFIED, 0L)

    private fun markNotified() = preferences().edit().putLong(LAST_NOTIFIED, versionCode).commit()

    private fun preferences() =
        PrivateSpaceState.applicationContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    /** What the notice needs from Niagara's tip system, as reflection objects. */
    private class TipParts(
        val register: Method,
        val factory: Method,
        val homePage: Field,
        val paused: Field,
        val factoryInstance: Field,
    ) {
        init {
            homePage.isAccessible = true
            paused.isAccessible = true
            factoryInstance.isAccessible = true
        }
    }

    /**
     * One tip: a provider and two callbacks, as dynamic proxies of Niagara's interfaces. The provider says to show
     * the tip until it is dismissed and builds it with Niagara's own factory when asked. Dismissing it, by swipe
     * or by its button, makes the provider say no, and Niagara's manager then takes the tip away.
     */
    private class Tip(
        private val parts: TipParts,
        private val factoryInstance: Any,
        private val text: String,
        context: Context,
    ) {
        @Volatile
        private var dismissed = false
        private val providerInterface = parts.register.parameterTypes[1]
        private val loader = providerInterface.classLoader
        private val holderClass = parts.factory.parameterTypes[1]

        /** Niagara's tip type for Xiaomi phones, looked up by its name because its field is obfuscated. */
        val type: Any = enumConstant(parts.register.parameterTypes[0], BorrowedTip.TYPE_NAME)

        private val buttonResource = resource(context, "button_dismiss", "string").also { check(it != 0) }
        private val iconResource = resource(context, "ic_ip_act_info", "drawable") // 0 means no icon
        private val dismissCallback = callback(parts.factory.parameterTypes[6], result = java.lang.Boolean.TRUE)
        private val clickCallback = callback(parts.factory.parameterTypes[7], result = null)

        val provider: Any =
            Proxy.newProxyInstance(
                loader,
                arrayOf(providerInterface),
                InvocationHandler { proxy, method, args ->
                    when {
                        method.declaringClass == Any::class.java -> objectMethod(proxy, method, args)
                        method.returnType == java.lang.Boolean.TYPE -> !dismissed
                        method.returnType == parts.factory.returnType ->
                            build(args.first { holderClass.isInstance(it) })
                        else -> null
                    }
                },
            )

        fun dismiss() {
            dismissed = true
        }

        private fun build(holder: Any): Any =
            checkNotNull(
                parts.factory.invoke(
                    null, factoryInstance, holder, 0, text, buttonResource, iconResource,
                    dismissCallback, clickCallback, MASK,
                ),
            )

        private fun callback(callbackInterface: Class<*>, result: Any?): Any =
            Proxy.newProxyInstance(
                loader,
                arrayOf(callbackInterface),
                InvocationHandler { proxy, method, args ->
                    if (method.declaringClass == Any::class.java) {
                        objectMethod(proxy, method, args)
                    } else {
                        dismissed = true
                        result
                    }
                },
            )

        private fun objectMethod(proxy: Any, method: Method, args: Array<Any?>?): Any? =
            when (method.name) {
                "equals" -> proxy === args?.firstOrNull()
                "hashCode" -> System.identityHashCode(proxy)
                else -> "Niagara Private Space tip"
            }

        private fun resource(context: Context, name: String, kind: String) =
            context.resources.getIdentifier(name, kind, context.packageName)

        @Suppress("UNCHECKED_CAST")
        private fun enumConstant(type: Class<*>, name: String): Any =
            java.lang.Enum.valueOf(type as Class<out Enum<*>>, name)
    }

    private companion object {
        const val PREFERENCES = "niagara_private_space"
        const val LAST_NOTIFIED = "last_notified_niagara_version_code"
        const val RETRY_MILLIS = 500L
        const val MAX_ATTEMPTS = 120

        /**
         * The factory's last argument, a mask of which defaults to use (Kotlin's default arguments). 210 is the value
         * Niagara's own tip with free text passes (its `NEW_YEAR_STATISTICS` provider): the message is the text given,
         * the button label and the icon come from the resource ids given. Taken from reading the 1638 code; a Niagara
         * update may change what a bit means, which the device check would show as a wrong label or no tip.
         */
        const val MASK = 210
    }
}
