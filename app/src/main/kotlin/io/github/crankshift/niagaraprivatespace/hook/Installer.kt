package io.github.crankshift.niagaraprivatespace.hook

import io.github.crankshift.niagaraprivatespace.decision.Hook
import io.github.crankshift.niagaraprivatespace.finder.Resolved
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Constructor
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
            OptionsHooks.installSheet(hooks, constructor(Hook.OPTIONS_SHEET))
            PickerHooks.install(
                hooks,
                flags,
                filters = methods(Hook.PICKER_FILTER) + methods(Hook.PICKER_FILTER_OVERRIDES),
                leisureAppsFilter = method(Hook.PICKER_FILTER_LEISURE_APPS),
            )
            ListingHooks.installSuggestions(hooks, flags, method(Hook.SUGGESTION_ELIGIBILITY), field(Hook.SUGGESTION_ITEM))
            ListingHooks.installAddShortcutItems(hooks, flags, constructor(Hook.ADD_SHORTCUT_ITEMS))
        } catch (t: Throwable) {
            hooks.removeAll()
            throw t
        }
        return hooks
    }

    private fun field(hook: Hook): Field = requireNotNull(resolved.fieldOf(hook)).getFieldInstance(classLoader)

    private fun method(hook: Hook): Method = methods(hook).single()

    private fun constructor(hook: Hook): Constructor<*> =
        resolved.methodsOf(hook).single().getConstructorInstance(classLoader)

    private fun methods(hook: Hook): List<Method> = resolved.methodsOf(hook).map { it.getMethodInstance(classLoader) }
}
