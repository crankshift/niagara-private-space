package io.github.crankshift.niagaraprivatespace.hook

import android.app.Application
import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserHandle
import android.os.UserManager

/**
 * Whether Private Space is locked, asked of Android itself. Niagara's own "hidden everywhere" state is no
 * substitute, because it is also true for suspended apps while Private Space is unlocked. Locked means that
 * Private Space's profile is in quiet mode.
 *
 * Callers ask only once Niagara has shown them a private app, so Private Space must exist. If it cannot be
 * found, or anything else fails, the answer is locked: wrongly hiding a private app is noticed at once,
 * wrongly showing one is not.
 */
object PrivateSpaceState {
    private const val PRIVATE_PROFILE_TYPE = "android.os.usertype.profile.PRIVATE"

    fun isLocked(): Boolean =
        try {
            val context = currentContext()
            val users = context.getSystemService(UserManager::class.java)
            val privateUser = privateProfile(users, context.getSystemService(LauncherApps::class.java))
            privateUser == null || users.isQuietModeEnabled(privateUser)
        } catch (_: Throwable) {
            true
        }

    private fun privateProfile(users: UserManager, launcherApps: LauncherApps): UserHandle? =
        users.userProfiles.firstOrNull { launcherApps.getLauncherUserInfo(it)?.userType == PRIVATE_PROFILE_TYPE }

    /** Niagara's Application, which exists by the time any hooked method runs. */
    private fun currentContext(): Context =
        Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null) as Application
}
