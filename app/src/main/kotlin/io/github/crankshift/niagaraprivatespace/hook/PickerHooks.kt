package io.github.crankshift.niagaraprivatespace.hook

import io.github.crankshift.niagaraprivatespace.decision.Decisions
import io.github.crankshift.niagaraprivatespace.decision.PickerRule
import io.github.crankshift.niagaraprivatespace.decision.ProfileKind
import java.lang.reflect.Method

/**
 * Lists private apps in Niagara's app pickers. Each picker filters its candidate apps with a method that rejects
 * a private app by reading its Private Space flag. The hook shows a private app as another profile for that one
 * call, so the rest of the filter (the pop-up editor refusing its own host, for instance) runs as Niagara wrote it.
 * While Private Space is locked a private app keeps its own profile, and the pickers keep refusing it.
 */
object PickerHooks {
    fun install(
        hooks: InstalledHooks,
        flags: ItemFlags,
        filters: List<Method>,
        leisureAppsFilter: Method,
    ) {
        filters.forEach { hook(hooks, flags, it, PickerRule.ANY_PROFILE) }
        hook(hooks, flags, leisureAppsFilter, PickerRule.PERSONAL_APPS_ONLY)
    }

    private fun hook(hooks: InstalledHooks, flags: ItemFlags, filter: Method, rule: PickerRule) {
        hooks.hook(filter) { chain ->
            val item = chain.getArg(0)
            if (item == null) {
                chain.proceed()
            } else {
                val kind = flags.kindOf(item)
                val locked = kind == ProfileKind.PRIVATE && PrivateSpaceState.isLocked()
                flags.whileSeenAs(item, Decisions.profileSeenByPicker(kind, rule, locked)) { chain.proceed() }
            }
        }
    }
}
