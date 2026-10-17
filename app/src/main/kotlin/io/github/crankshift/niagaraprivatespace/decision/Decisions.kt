package io.github.crankshift.niagaraprivatespace.decision

/** Which profile an app was installed in. Work apps are neither personal nor private. */
enum class ProfileKind { PERSONAL, WORK, PRIVATE }

/**
 * Every decision the module makes, in its own vocabulary. Nothing here knows about Android, Niagara or
 * Xposed, so the hook adapters only translate Niagara's objects into these inputs and apply the answers.
 */
object Decisions {
    /** A private app gets the long-press options of a work app. Personal and work apps are unchanged. */
    fun profileForLongPressOptions(kind: ProfileKind): ProfileKind =
        if (kind == ProfileKind.PRIVATE) ProfileKind.WORK else kind

    /** The module activates only when every hook was found. */
    fun activation(report: ResolutionReport): Activation {
        val found = Hook.entries.filter { it in report.found }
        val missing = Hook.entries.filter { it !in report.found }
        return Activation(found = found, missing = missing)
    }
}
