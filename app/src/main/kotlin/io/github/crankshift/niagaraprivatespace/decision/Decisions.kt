package io.github.crankshift.niagaraprivatespace.decision

/** Which profile an app was installed in. Work apps are neither personal nor private. */
enum class ProfileKind { PERSONAL, WORK, PRIVATE }

/** What a Niagara app picker asks of the profile an app was installed in. */
enum class PickerRule {
    /** Personal and work apps qualify. Most pickers: favorites, folders, pop-ups, Niagara button, music apps. */
    ANY_PROFILE,

    /** Only personal apps qualify, even though work apps are not blocked elsewhere. Leisure Apps asks this. */
    PERSONAL_APPS_ONLY,
}

/**
 * Every decision the module makes, in its own vocabulary. Nothing here knows about Android, Niagara or
 * Xposed, so the hook adapters only translate Niagara's objects into these inputs and apply the answers.
 */
object Decisions {
    /** A private app gets the long-press options of a work app. Personal and work apps are unchanged. */
    fun profileForLongPressOptions(kind: ProfileKind): ProfileKind =
        if (kind == ProfileKind.PRIVATE) ProfileKind.WORK else kind

    /**
     * Whether an app of this profile may be offered by a picker with this [rule]. A private app is offered
     * wherever a work app is, and also in Leisure Apps, whose personal-app requirement the module lifts for
     * private apps alone, but only while Private Space is unlocked: a locked Private Space keeps its apps out
     * of sight, pickers included. Work apps stay excluded from Leisure Apps.
     */
    fun isEligibleInPicker(kind: ProfileKind, rule: PickerRule, privateSpaceLocked: Boolean): Boolean =
        when (kind) {
            ProfileKind.PERSONAL -> true
            ProfileKind.PRIVATE -> !privateSpaceLocked
            ProfileKind.WORK -> rule == PickerRule.ANY_PROFILE
        }

    /**
     * The profile a picker should see an app as, so that Niagara's own check for [rule] passes for exactly the
     * apps that [isEligibleInPicker]. An unlocked private app looks like a work app to a picker that accepts work
     * apps, and like a personal app to Leisure Apps. A locked one stays private, and Niagara refuses it as it
     * always did. Personal and work apps are seen as themselves.
     */
    fun profileSeenByPicker(kind: ProfileKind, rule: PickerRule, privateSpaceLocked: Boolean): ProfileKind =
        when {
            kind != ProfileKind.PRIVATE || !isEligibleInPicker(kind, rule, privateSpaceLocked) -> kind
            rule == PickerRule.PERSONAL_APPS_ONLY -> ProfileKind.PERSONAL
            else -> ProfileKind.WORK
        }

    /** The module activates only when every hook was found. */
    fun activation(report: ResolutionReport): Activation {
        val found = Hook.entries.filter { it in report.found }
        val missing = Hook.entries.filter { it !in report.found }
        return Activation(found = found, missing = missing)
    }
}
