package io.github.crankshift.niagaraprivatespace.decision

/** What Niagara should do when the user taps an app. */
enum class TapAction {
    /** Let Niagara open the app. */
    OPEN_NOW,

    /** Ask Android to unlock Private Space first; the module opens the app once it is available. */
    UNLOCK_FIRST,
}

/**
 * Decides what a tap on an app does, and remembers the private app that a tap is waiting on: the **pending app**.
 *
 * Tapping a locked private app asks Android to unlock Private Space, and the module opens the app itself when
 * Private Space becomes available: from AOSP's source, the variant of Android's request that starts an app
 * afterwards needs a permission Niagara does not hold. At most one app is pending. It is forgotten when it
 * opens, when another tap replaces it or opens something else, when Private Space locks again, when the unlock
 * request fails, and after [TIMEOUT_MILLIS]. That last rule stands in for the user backing out of the unlock
 * screen, for which no signal is known: without it, unlocking Private Space minutes later by some other route
 * would open an app the user no longer wants.
 *
 * Time is passed in so the class stays free of clocks. Not thread-safe: callers serialise their calls.
 */
class PendingApp<App : Any> {
    private var pending: App? = null
    private var tappedAtMillis = 0L

    /**
     * What a tap on an app of this [kind] does. [app] says which app it is, and is only asked for when the tap has
     * to wait on it: most taps are on personal and work apps, which the caller need not look into.
     */
    fun tap(kind: ProfileKind, privateSpaceLocked: Boolean, nowMillis: Long, app: () -> App): TapAction {
        // The apps Niagara leaves unlisted while locked are the ones that cannot open until Private Space does.
        if (!Decisions.mayBeListed(kind, privateSpaceLocked)) {
            pending = app()
            tappedAtMillis = nowMillis
            return TapAction.UNLOCK_FIRST
        }
        pending = null
        return TapAction.OPEN_NOW
    }

    /**
     * The app to open now, if a tap is still waiting on it. Only Private Space counts, and it is available once its
     * apps can start, which is not the same moment its quiet mode ends.
     */
    fun profileBecameAvailable(kind: ProfileKind, nowMillis: Long): App? {
        if (kind != ProfileKind.PRIVATE) return null
        val app = pending?.takeIf { nowMillis - tappedAtMillis <= TIMEOUT_MILLIS }
        pending = null
        return app
    }

    fun profileBecameUnavailable(kind: ProfileKind) {
        if (kind == ProfileKind.PRIVATE) pending = null
    }

    /** No unlock is coming for the pending app: the request failed or was refused, or was made for something else. */
    fun unlockCancelled() {
        pending = null
    }

    companion object {
        /** Long enough to type a PIN or use a fingerprint, short enough that a forgotten tap does not linger. */
        const val TIMEOUT_MILLIS = 60_000L
    }
}
