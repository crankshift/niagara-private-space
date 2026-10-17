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

/** How the user is told that this Niagara is not supported. */
enum class NoticeChannel {
    /** A dismissable tip on Niagara's home page, in Niagara's own style. */
    TIP,

    /** A Toast, when Niagara's tip system cannot be used. */
    TOAST,
}

/** The rows a folder or pop-up shows, and whether the unlock message goes below them. */
data class FolderContents<Row>(val rows: List<Row>, val showUnlockMessage: Boolean)

/**
 * Every stateless decision the module makes, in its own vocabulary. Nothing here knows about Android, Niagara or
 * Xposed, so the hook adapters only translate Niagara's objects into these inputs and apply the answers. The one
 * decision that remembers something between calls, what a tap on a locked private app is waiting for, is the
 * class [PendingApp].
 */
object Decisions {
    /**
     * A private app gets the long-press options of a work app. Personal and work apps are unchanged. Pinning an
     * app's shortcuts to favorites or the app list is one of those options, so the same answer covers it; whether
     * Android accepts a pinned shortcut from a private profile is not decided here.
     */
    fun profileForLongPressOptions(kind: ProfileKind): ProfileKind =
        if (kind == ProfileKind.PRIVATE) ProfileKind.WORK else kind

    /**
     * Whether an app may be listed: shown by a list that offers apps by themselves (suggestions below favorites,
     * the Add shortcut screen, the pickers) or as a row of the home page (favorites, suggestions, media). A
     * private app may while Private Space is unlocked and must not while it is locked, because nobody holding the
     * phone should see which apps live there. Personal and work apps always may.
     */
    fun mayBeListed(kind: ProfileKind, privateSpaceLocked: Boolean): Boolean =
        kind != ProfileKind.PRIVATE || !privateSpaceLocked

    /**
     * The profile a list of suggestions or the Add shortcut screen should see an app as. An unlocked private app
     * looks like a work app, so Niagara lists it as it lists work apps. A locked one stays private, and Niagara
     * leaves it out as it always did.
     */
    fun profileSeenWhenListing(kind: ProfileKind, privateSpaceLocked: Boolean): ProfileKind =
        if (kind == ProfileKind.PRIVATE && mayBeListed(kind, privateSpaceLocked)) ProfileKind.WORK else kind

    /**
     * Whether an app of this profile may be offered by a picker with this [rule]. A private app is offered
     * wherever a work app is, and also in Leisure Apps, whose personal-app requirement the module lifts for
     * private apps alone, but only while it [mayBeListed]. Work apps stay excluded from Leisure Apps.
     */
    fun isEligibleInPicker(kind: ProfileKind, rule: PickerRule, privateSpaceLocked: Boolean): Boolean =
        mayBeListed(kind, privateSpaceLocked) && (kind != ProfileKind.WORK || rule == PickerRule.ANY_PROFILE)

    /**
     * The profile a picker should see an app as, so that Niagara's own check for [rule] passes for exactly the
     * apps that [isEligibleInPicker]. An unlocked private app looks like a work app to a picker that accepts work
     * apps, and like a personal app to Leisure Apps. A locked one stays private, and Niagara refuses it as it
     * always did. Personal and work apps are seen as themselves.
     */
    fun profileSeenByPicker(kind: ProfileKind, rule: PickerRule, privateSpaceLocked: Boolean): ProfileKind {
        val seenWhenListed = profileSeenWhenListing(kind, privateSpaceLocked)
        val liftedForLeisureApps = kind == ProfileKind.PRIVATE && rule == PickerRule.PERSONAL_APPS_ONLY
        return if (seenWhenListed == ProfileKind.WORK && liftedForLeisureApps) ProfileKind.PERSONAL else seenWhenListed
    }

    /**
     * What a folder or pop-up shows, given the rows Niagara built for its apps ([kindOf] says which profile a
     * row's app is from). The folder stays, and while Private Space is locked its private apps leave: the rows of
     * personal and work apps are kept in order. If any row left and there is a row to say so
     * ([unlockMessageRowAvailable]), the **unlock message** is asked for, to go below the rest. A folder with no
     * private apps, or an unlocked one, is untouched. Only the rows are decided here, never the stored contents.
     */
    fun <Row> folderOrPopUpContents(
        rows: List<Row>,
        kindOf: (Row) -> ProfileKind,
        privateSpaceLocked: Boolean,
        unlockMessageRowAvailable: Boolean,
    ): FolderContents<Row> {
        val kept = rows.filter { mayBeListed(kindOf(it), privateSpaceLocked) }
        return FolderContents(kept, showUnlockMessage = kept.size < rows.size && unlockMessageRowAvailable)
    }

    /**
     * The module activates when every essential hook was found. The optional parts it can run with are those whose
     * hooks were all found; the log line says which it runs without.
     */
    fun activation(report: ResolutionReport): Activation {
        val found = Hook.entries.filter { it in report.found }
        val missing = Hook.entries.filter { it !in report.found }
        return Activation(found = found, missing = missing)
    }

    /**
     * Whether to tell the user that this Niagara is not supported: when the module could not activate, and not
     * already for this Niagara version ([lastNotifiedVersionCode], null if never). A new version is told again.
     */
    fun noticeDue(activation: Activation, niagaraVersionCode: Long, lastNotifiedVersionCode: Long?): Boolean =
        activation.notify && lastNotifiedVersionCode != niagaraVersionCode

    /** A tip in Niagara's own style if its tip system was found, otherwise a Toast. */
    fun noticeChannel(activation: Activation): NoticeChannel =
        if (OptionalPart.TIP_SYSTEM in activation.availableParts) NoticeChannel.TIP else NoticeChannel.TOAST

    fun noticeText(niagaraVersionName: String): String =
        "Niagara Private Space doesn't support Niagara $niagaraVersionName yet"
}
