package io.github.crankshift.niagaraprivatespace.hook

import android.util.Log
import io.github.crankshift.niagaraprivatespace.NiagaraPrivateSpaceModule
import io.github.libxposed.api.XposedInterface

/** Writes to Vector's module log under the module's tag, so the device check can read what the module did. */
class ModuleLog(private val xposed: XposedInterface) {
    fun info(message: String) = xposed.log(Log.INFO, NiagaraPrivateSpaceModule.TAG, message)

    fun warn(message: String, error: Throwable) = xposed.log(Log.WARN, NiagaraPrivateSpaceModule.TAG, message, error)
}
