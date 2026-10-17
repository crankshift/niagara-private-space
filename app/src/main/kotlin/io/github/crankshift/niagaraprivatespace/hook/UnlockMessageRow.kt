package io.github.crankshift.niagaraprivatespace.hook

import android.widget.TextView
import java.lang.ref.WeakReference
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * The **unlock message**: a row that says why a folder or pop-up shows fewer apps while Private Space is locked.
 * The module cannot subclass Niagara's obfuscated row base from its own class loader (the research's reading), so
 * it builds an instance of Niagara's row for an empty pop-up and takes over that instance's two jobs: filling in
 * its text, and what a tap on it does. Niagara's own empty-pop-up rows are different instances, which the hooks
 * leave to Niagara's code; that is unverified on the phone.
 *
 * Whether the two-argument constructor of that row gives it the empty-pop-up behaviour was unclear from the
 * decompiled code (the variant it stores looked like 0 for both constructors). In the 1638 bytecode the
 * two-argument constructor stores 1, but the hooks below skip Niagara's own bind and click for our instances
 * anyway, so the variant does not matter.
 */
class UnlockMessageRow(
    private val constructor: Constructor<*>,
    private val host: Field,
    private val notifications: Field,
    private val text: Field,
    private val bind: Method,
    private val click: Method,
    private val requestUnlock: () -> Unit,
    private val log: ModuleLog,
) {
    private val ours = WeakIdentitySet()

    init {
        listOf(host, notifications, text).forEach { it.isAccessible = true }
        constructor.isAccessible = true
    }

    /** A row for the folder or pop-up that [controller] shows. */
    fun newRow(controller: Any): Any =
        constructor.newInstance(host.get(controller), notifications.get(controller)).also { ours.add(it) }

    fun install(hooks: InstalledHooks) {
        hooks.hook(bind) { chain ->
            if (isOurs(chain.thisObject)) {
                (text.get(chain.getArg(0)) as TextView).text = MESSAGE
                null
            } else {
                chain.proceed()
            }
        }
        hooks.hook(click) { chain ->
            if (isOurs(chain.thisObject)) {
                try {
                    requestUnlock()
                } catch (t: Throwable) {
                    log.warn("asking Android to unlock Private Space from the unlock message failed", t)
                }
                null
            } else {
                chain.proceed()
            }
        }
    }

    private fun isOurs(row: Any?) = row != null && ours.contains(row)

    /** Rows are told apart by identity: Niagara's rows compare by id, and all empty-state rows share one. */
    private class WeakIdentitySet {
        private val references = mutableListOf<WeakReference<Any>>()

        @Synchronized
        fun add(row: Any) {
            references.removeAll { it.get() == null }
            references += WeakReference(row)
        }

        @Synchronized
        fun contains(row: Any): Boolean = references.any { it.get() === row }
    }

    companion object {
        const val MESSAGE = "Unlock Private Space to see your apps"
    }
}
