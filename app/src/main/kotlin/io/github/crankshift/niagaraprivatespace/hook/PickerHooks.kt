package io.github.crankshift.niagaraprivatespace.hook

import io.github.crankshift.niagaraprivatespace.decision.Decisions
import io.github.crankshift.niagaraprivatespace.decision.PickerRule
import java.lang.reflect.Method

/**
 * Lists private apps in Niagara's app pickers. Each picker filters its candidate apps with a method that rejects
 * a private app by reading its Private Space flag. The hook shows a private app as another profile for that one
 * call, so the rest of the filter (the pop-up editor refusing its own host, for instance) runs as Niagara wrote it.
 * While Private Space is locked a private app keeps its own profile, and the pickers keep refusing it, and the
 * rows of the private apps already chosen are left out of the list ([installRows]).
 */
object PickerHooks {
    /** The app is the third argument of the picker's row step. */
    private const val ITEM_ARGUMENT = 2

    fun install(
        hooks: InstalledHooks,
        flags: ItemFlags,
        filters: List<Method>,
        leisureAppsFilter: Method,
    ) {
        filters.forEach { hook(hooks, flags, it, PickerRule.ANY_PROFILE) }
        hook(hooks, flags, leisureAppsFilter, PickerRule.PERSONAL_APPS_ONLY)
    }

    /**
     * Hides the rows of locked private apps from a picker's list. The pickers draw the apps already chosen straight
     * from Niagara's stored lists, so without this a locked private app would still be named there. The hook skips
     * adding the row, and nothing else. Read from the 1638 bytecode: the step reads the chosen list to mark a row
     * as selected and never writes it, and saving writes that list back as it is, so the placements should survive;
     * the device check (the home page section, "Edit while locked") is where that is seen. The step also adds the
     * rows of widgets, which carry the same flags, so a locked private app's widget is hidden too.
     */
    fun installRows(hooks: InstalledHooks, flags: ItemFlags, rowStep: Method) {
        hooks.hook(rowStep) { chain ->
            val item = checkNotNull(chain.getArg(ITEM_ARGUMENT)) { "the picker row step has no app" }
            val kind = flags.kindOf(item)
            if (Decisions.mayBeListed(kind, PrivateSpaceState.isLockedFor(kind))) chain.proceed() else null
        }
    }

    private fun hook(hooks: InstalledHooks, flags: ItemFlags, filter: Method, rule: PickerRule) {
        hooks.hook(filter) { chain ->
            val item = chain.getArg(0)
            if (item == null) {
                chain.proceed()
            } else {
                val kind = flags.kindOf(item)
                val seen = Decisions.profileSeenByPicker(kind, rule, PrivateSpaceState.isLockedFor(kind))
                flags.whileSeenAs(item, seen) { chain.proceed() }
            }
        }
    }
}
