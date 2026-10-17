package io.github.crankshift.niagaraprivatespace.hook

import io.github.crankshift.niagaraprivatespace.decision.Hook
import io.github.crankshift.niagaraprivatespace.decision.OptionalPart
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
    private val availableParts: Set<OptionalPart>,
) {
    private val log = ModuleLog(xposed)

    fun install(): InstalledHooks {
        val hooks = InstalledHooks(xposed)
        try {
            check(ItemFlags.canWriteFinalFields()) { "this runtime does not let reflection write final fields" }
            val flags = ItemFlags(field(Hook.PRIMARY_USER_FLAG), field(Hook.PRIVATE_SPACE_FLAG))

            // The hooks that hide private apps again come first; the ones that let them in follow.
            HomeRowHooks.install(hooks, flags, methods(Hook.FAVORITES_ROWS) + methods(Hook.OTHER_HOME_ROWS))
            // It lets nothing in and hides nothing, so its place in this order does not matter.
            val launches = LaunchHooks(flags, log)
            launches.install(
                hooks,
                launch = method(Hook.APP_LAUNCH),
                target = field(Hook.LAUNCH_TARGET),
                component = field(Hook.LAUNCH_COMPONENT),
            )
            // The message row's hooks go in first, and only count if all of them do; the row builder follows.
            var messageRow: UnlockMessageRow? = null
            if (OptionalPart.UNLOCK_MESSAGE_ROW in availableParts) {
                hooks.optional(OptionalPart.UNLOCK_MESSAGE_ROW, log) {
                    messageRow = unlockMessageRow(launches).also { it.install(hooks) }
                }
            }
            FolderHooks.install(
                hooks,
                flags,
                rowBuilder = method(Hook.FOLDER_AND_POP_UP_ROWS),
                rowItem = field(Hook.FOLDER_AND_POP_UP_ROW_ITEM),
                unlockMessageRow = messageRow,
                log = log,
            )

            OptionsHooks.install(hooks, flags, method(Hook.LONG_PRESS_OPTIONS))
            OptionsHooks.installSheet(hooks, constructor(Hook.OPTIONS_SHEET))
            PickerHooks.install(
                hooks,
                flags,
                filters = methods(Hook.PICKER_FILTER) + methods(Hook.PICKER_FILTER_OVERRIDES),
                leisureAppsFilter = method(Hook.PICKER_FILTER_LEISURE_APPS),
            )
            ListingHooks.installSuggestions(
                hooks,
                flags,
                suggestionCheck = method(Hook.SUGGESTION_ELIGIBILITY),
                itemInRecord = field(Hook.SUGGESTION_ITEM),
            )
            ListingHooks.installAddShortcutItems(hooks, flags, constructor(Hook.ADD_SHORTCUT_ITEMS))
        } catch (t: Throwable) {
            hooks.removeAll()
            throw t
        }
        return hooks
    }

    /** Builds the unlock message row from what the finder located. Throws if Niagara's classes do not give it all. */
    private fun unlockMessageRow(launches: LaunchHooks) =
        UnlockMessageRow(
            constructor = constructor(Hook.EMPTY_POP_UP_ROW),
            host = field(Hook.POP_UP_HOST),
            notifications = field(Hook.POP_UP_NOTIFICATIONS),
            text = field(Hook.EMPTY_POP_UP_ROW_TEXT),
            bind = method(Hook.EMPTY_POP_UP_ROW_BIND),
            click = method(Hook.EMPTY_POP_UP_ROW_CLICK),
            requestUnlock = launches::requestUnlockWithoutApp,
            log = log,
        )

    private fun field(hook: Hook): Field = requireNotNull(resolved.fieldOf(hook)).getFieldInstance(classLoader)

    private fun method(hook: Hook): Method = methods(hook).single()

    private fun constructor(hook: Hook): Constructor<*> =
        resolved.methodsOf(hook).single().getConstructorInstance(classLoader)

    private fun methods(hook: Hook): List<Method> = resolved.methodsOf(hook).map { it.getMethodInstance(classLoader) }
}
