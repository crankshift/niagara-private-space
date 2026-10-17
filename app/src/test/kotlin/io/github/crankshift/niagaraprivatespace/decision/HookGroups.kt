package io.github.crankshift.niagaraprivatespace.decision

/**
 * Every hook, named by hand and grouped the way the activation rule treats them. [ActivationTest] checks that the
 * groups together are exactly [Hook.entries], so adding a hook forces a line here and a decision about its group.
 */
object HookGroups {
    val essential =
        listOf(
            Hook.LONG_PRESS_OPTIONS,
            Hook.PRIVATE_SPACE_FLAG,
            Hook.PRIMARY_USER_FLAG,
            Hook.PICKER_FILTER,
            Hook.PICKER_FILTER_OVERRIDES,
            Hook.PICKER_FILTER_LEISURE_APPS,
            Hook.PICKER_ROW,
            Hook.SUGGESTION_ELIGIBILITY,
            Hook.SUGGESTION_ITEM,
            Hook.OPTIONS_SHEET,
            Hook.ADD_SHORTCUT_ITEMS,
            Hook.FAVORITES_ROWS,
            Hook.OTHER_HOME_ROWS,
            Hook.APP_LAUNCH,
            Hook.LAUNCH_TARGET,
            Hook.LAUNCH_COMPONENT,
            Hook.FOLDER_AND_POP_UP_ROWS,
            Hook.FOLDER_AND_POP_UP_ROW_ITEM,
        )

    /** The unlock message: borrowed from Niagara's row for an empty pop-up, so a Niagara update may take it away. */
    val unlockMessageRow =
        listOf(
            Hook.EMPTY_POP_UP_ROW,
            Hook.EMPTY_POP_UP_ROW_BIND,
            Hook.EMPTY_POP_UP_ROW_CLICK,
            Hook.EMPTY_POP_UP_ROW_TEXT,
            Hook.POP_UP_HOST,
            Hook.POP_UP_NOTIFICATIONS,
        )

    /** Niagara's tips, which the notice borrows. The module itself does not need them. */
    val tipSystem =
        listOf(
            Hook.TIP_MANAGER,
            Hook.TIP_REGISTER,
            Hook.TIP_FACTORY,
            Hook.TIP_HOME_PAGE,
            Hook.TIP_PAUSED,
            Hook.TIP_FACTORY_INSTANCE,
        )

    val every = essential + unlockMessageRow + tipSystem
}
