package io.github.crankshift.niagaraprivatespace.hook

import android.app.Application
import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserHandle
import android.os.UserManager
import io.github.crankshift.niagaraprivatespace.decision.ProfileKind

/**
 * What Android says about Private Space. Niagara's own "hidden everywhere" state is no substitute for asking,
 * because it is also true for suspended apps while Private Space is unlocked. Locked means that Private Space's
 * user is in quiet mode.
 *
 * Callers ask only once Niagara has shown them a private app, so Private Space must exist. If it cannot be
 * found, or anything else fails, the answer to "is it locked" is yes: wrongly hiding a private app is noticed at
 * once, wrongly showing one is not. The functions that do something or classify a user ([requestUnlock],
 * [profileKindOf], [isAccessible]) have no such default and throw, for the caller to log.
 */
object PrivateSpaceState {
    private const val PRIVATE_SPACE_USER_TYPE = "android.os.usertype.profile.PRIVATE"

    /** Whether a lock matters for an app of this [kind]: only private apps are hidden by it. */
    fun isLockedFor(kind: ProfileKind): Boolean = kind == ProfileKind.PRIVATE && isLocked()

    fun isLocked(): Boolean =
        try {
            val privateSpaceUser = privateSpaceUser()
            privateSpaceUser == null || users().isQuietModeEnabled(privateSpaceUser)
        } catch (_: Throwable) {
            true
        }

    /**
     * Sends the **unlock request**: asks Android to unlock Private Space, which brings up its credential screen.
     * From AOSP's source, Android allows it because Niagara is the foreground default launcher, so call it from a
     * tap; that is unverified on LineageOS 24. It says nothing about the outcome. Throws when there is no Private
     * Space or Android refuses.
     */
    fun requestUnlock() {
        val privateSpaceUser = checkNotNull(privateSpaceUser()) { "there is no Private Space" }
        users().requestQuietModeEnabled(false, privateSpaceUser)
    }

    /** Whether [user] is Private Space's user. Throws when Android cannot say. */
    private fun isPrivateSpaceUser(user: UserHandle): Boolean =
        launcherApps().getLauncherUserInfo(user)?.userType == PRIVATE_SPACE_USER_TYPE

    /**
     * The kind of app that lives in [profile], one of the current user's profiles: Private Space, or else a work
     * profile, the only other kind Niagara knows. Throws when Android cannot say.
     */
    fun profileKindOf(profile: UserHandle): ProfileKind =
        if (isPrivateSpaceUser(profile)) ProfileKind.PRIVATE else ProfileKind.WORK

    /** Whether [user] is running with its storage unlocked, which is when its apps can start. */
    fun isAccessible(user: UserHandle): Boolean = users().isUserUnlocked(user)

    /** Niagara's Application, which exists by the time any hooked method runs. */
    fun applicationContext(): Context =
        Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null) as Application

    private fun privateSpaceUser(): UserHandle? = users().userProfiles.firstOrNull { isPrivateSpaceUser(it) }

    private fun users(): UserManager = applicationContext().getSystemService(UserManager::class.java)

    fun launcherApps(): LauncherApps = applicationContext().getSystemService(LauncherApps::class.java)
}
