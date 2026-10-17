package io.github.crankshift.niagaraprivatespace.decision

/**
 * A piece of the module that Niagara updates may take away without costing the module its purpose. The module
 * runs without it, and the log line says so.
 */
enum class OptionalPart {
    /**
     * The row that tells the user why a folder or pop-up shows fewer apps while Private Space is locked. Without
     * it, private apps hide without a message.
     */
    UNLOCK_MESSAGE_ROW,
}

/**
 * A piece of Niagara the module must locate before it may change anything. A hook is essential unless it belongs to
 * an [OptionalPart]: lifting Niagara's blocks without being able to hide private apps while Private Space is
 * locked would leak them, so one missing essential hook switches the whole module off. A hook of an optional part
 * only costs that part, and one missing hook costs all of it.
 */
enum class Hook(val optionalPart: OptionalPart? = null) {
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

    /**
     * The step that adds the row of one app to a picker's list, for the apps already chosen as well as for the
     * candidates. The pickers draw their current selection straight from the stored lists, so hiding a locked
     * private app there needs this step, not the filter.
     */
    PICKER_ROW,

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

    /** The method that builds the rows of a folder or pop-up, one per app, from the contents Niagara loaded. */
    FOLDER_AND_POP_UP_ROWS,

    /** The field of an app row that holds its app (the item it was built for). */
    FOLDER_AND_POP_UP_ROW_ITEM,

    /** Niagara's row for an empty pop-up: the constructor the module calls to build the unlock message. */
    EMPTY_POP_UP_ROW(OptionalPart.UNLOCK_MESSAGE_ROW),

    /** The empty-pop-up row's bind method, which the unlock message row replaces. */
    EMPTY_POP_UP_ROW_BIND(OptionalPart.UNLOCK_MESSAGE_ROW),

    /** The empty-pop-up row's click method, which the unlock message row replaces. */
    EMPTY_POP_UP_ROW_CLICK(OptionalPart.UNLOCK_MESSAGE_ROW),

    /** The text view in the empty-pop-up row's holder, where the unlock message goes. */
    EMPTY_POP_UP_ROW_TEXT(OptionalPart.UNLOCK_MESSAGE_ROW),

    /** The field of the pop-up controller that holds the host of the folder or pop-up. */
    POP_UP_HOST(OptionalPart.UNLOCK_MESSAGE_ROW),

    /** The field of the pop-up controller that holds the notifications Niagara hands to its rows. */
    POP_UP_NOTIFICATIONS(OptionalPart.UNLOCK_MESSAGE_ROW),
}

/** What the finder located in Niagara: the hooks it found, and nothing about the ones it did not. */
data class ResolutionReport(val found: Set<Hook>)

/** Whether the module changes Niagara, and what it says about that in the log. */
data class Activation(val found: List<Hook>, val missing: List<Hook>) {
    /** The module changes Niagara only when every essential hook was found. */
    val active: Boolean get() = missing.none { it.optionalPart == null }

    /** The optional parts whose hooks were all found. None while the module is inactive. */
    val availableParts: Set<OptionalPart>
        get() = if (active) OptionalPart.entries.filter { missingOf(it).isEmpty() }.toSet() else emptySet()

    private fun missingOf(part: OptionalPart) = missing.filter { it.optionalPart == part }

    /** The line the module writes to Vector's log on every start of Niagara. */
    fun logLine(): String =
        if (active) {
            "active, found: ${found.joinToString()}" +
                OptionalPart.entries.filter { missingOf(it).isNotEmpty() }
                    .joinToString("") { "; without $it (missing: ${missingOf(it).joinToString()})" }
        } else {
            "inactive, missing: ${missing.joinToString()}"
        }
}
