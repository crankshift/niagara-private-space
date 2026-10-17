package io.github.crankshift.niagaraprivatespace.decision

/**
 * A piece of Niagara the module must locate before it may change anything. Every hook is required: lifting
 * Niagara's blocks without being able to hide private apps while Private Space is locked would leak them,
 * so one missing hook switches the whole module off.
 */
enum class Hook {
    LONG_PRESS_OPTIONS,
    PRIVATE_SPACE_FLAG,
    PRIMARY_USER_FLAG,

    /** The filter that app pickers apply to every candidate app, in the picker base class. */
    PICKER_FILTER,

    /**
     * Pickers that replace that filter with their own (folders and pop-ups, hide apps, notification summary).
     * This hook is one or more methods.
     */
    PICKER_FILTER_OVERRIDES,

    /** The Leisure Apps picker's filter, which also asks for a personal app. */
    PICKER_FILTER_LEISURE_APPS,

    /** The check that keeps private apps out of the most-used suggestions below favorites. */
    SUGGESTION_ELIGIBILITY,

    /** The field of the suggestion check's usage record that holds the app being asked about. */
    SUGGESTION_ITEM,

    /** The options sheet, whose constructor also decides which shortcuts of an app can be pinned. */
    OPTIONS_SHEET,

    /** The constructor of the items the Add shortcut screen builds for each shortcut an app offers. */
    ADD_SHORTCUT_ITEMS,

    /** The favorites section of the home page: the method that turns a favorite into the row Niagara renders. */
    FAVORITES_ROWS,

    /** The other sections of the home page, built the same way: most-used suggestions and the media section. */
    OTHER_HOME_ROWS,

    /** The method that opens an app of another profile. In the 1638 bytecode every way of opening an app reaches it. */
    APP_LAUNCH,

    /** The field of an app item that holds what to launch: its component and its user. */
    LAUNCH_TARGET,

    /** The component name inside [LAUNCH_TARGET], which the module needs to open the app itself. */
    LAUNCH_COMPONENT,
}

/** What the finder located in Niagara: the hooks it found, and nothing about the ones it did not. */
data class ResolutionReport(val found: Set<Hook>)

/** Whether the module changes Niagara, and what it says about that in the log. */
data class Activation(val found: List<Hook>, val missing: List<Hook>) {
    val active: Boolean get() = missing.isEmpty()

    /** The line the module writes to Vector's log on every start of Niagara. */
    fun logLine(): String =
        if (active) {
            "active, found: ${found.joinToString()}"
        } else {
            "inactive, missing: ${missing.joinToString()}"
        }
}
