package io.github.crankshift.niagaraprivatespace.hook

import android.content.Context
import android.content.pm.ApplicationInfo

/** Niagara's version, read from its APK, for the log line and the unsupported-version notice. */
data class NiagaraVersion(val code: Long, val name: String) {
    override fun toString() = "$name ($code)"

    companion object {
        /**
         * Asks the package manager about Niagara's APK file. This runs before Niagara has an Application, so it
         * borrows the system context from the activity thread. Null when anything about that fails.
         */
        fun read(applicationInfo: ApplicationInfo): NiagaraVersion? =
            try {
                val activityThread = Class.forName("android.app.ActivityThread")
                val thread = activityThread.getMethod("currentActivityThread").invoke(null)
                val context = activityThread.getMethod("getSystemContext").invoke(thread) as Context
                val info = context.packageManager.getPackageArchiveInfo(applicationInfo.sourceDir, 0)
                info?.let { NiagaraVersion(it.longVersionCode, it.versionName ?: "?") }
            } catch (_: Throwable) {
                null
            }
    }
}
