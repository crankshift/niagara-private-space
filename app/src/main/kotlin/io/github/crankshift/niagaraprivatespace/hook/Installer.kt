package io.github.crankshift.niagaraprivatespace.hook

import io.github.crankshift.niagaraprivatespace.decision.Hook
import io.github.crankshift.niagaraprivatespace.finder.Resolved
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Field
import java.lang.reflect.Method

/** Turns what the finder located into installed hooks, or into no hooks at all. */
class Installer(
    private val xposed: XposedInterface,
    private val resolved: Resolved,
    private val classLoader: ClassLoader,
) {
    fun install(): InstalledHooks {
        val hooks = InstalledHooks(xposed)
        try {
            check(ItemFlags.canWriteFinalFields()) { "this runtime does not let reflection write final fields" }
            val flags = ItemFlags(field(Hook.PRIMARY_USER_FLAG), field(Hook.PRIVATE_SPACE_FLAG))

            OptionsHooks.install(hooks, flags, method(Hook.LONG_PRESS_OPTIONS))
        } catch (t: Throwable) {
            hooks.removeAll()
            throw t
        }
        return hooks
    }

    private fun field(hook: Hook): Field = requireNotNull(resolved.fieldOf(hook)).getFieldInstance(classLoader)

    private fun method(hook: Hook): Method = resolved.methodsOf(hook).single().getMethodInstance(classLoader)
}
