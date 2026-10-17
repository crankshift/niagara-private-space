package io.github.crankshift.niagaraprivatespace

import android.util.Log
import io.github.crankshift.niagaraprivatespace.decision.Decisions
import io.github.crankshift.niagaraprivatespace.decision.Hook
import io.github.crankshift.niagaraprivatespace.finder.Finder
import io.github.crankshift.niagaraprivatespace.finder.Resolved
import io.github.crankshift.niagaraprivatespace.hook.Installer
import io.github.crankshift.niagaraprivatespace.hook.NiagaraVersion
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import org.luckypray.dexkit.DexKitBridge

/**
 * Entry point Vector loads into Niagara. It finds the code it needs, asks the decision module whether that is
 * enough, and either installs every hook or changes nothing at all. Every start of Niagara leaves one line in
 * Vector's log saying which of the two happened.
 */
class NiagaraPrivateSpaceModule : XposedModule() {
    override fun onPackageReady(param: PackageReadyParam) {
        if (!param.isFirstPackage || param.packageName != NIAGARA_PACKAGE) return

        val niagara = "Niagara ${NiagaraVersion.read(param.applicationInfo) ?: "?"}"
        try {
            loadDexKit()
            DexKitBridge.create(param.classLoader, true).use { bridge ->
                val finder = Finder(bridge) { what, error ->
                    log(Log.WARN, TAG, "$niagara: finding the $what failed", error)
                }
                val resolved = finder.resolve()
                activate(resolved, param.classLoader, niagara)
            }
        } catch (t: Throwable) {
            log(Log.ERROR, TAG, "$niagara: inactive, the finder could not run", t)
        }
    }

    /** DexKit's native library ships in the module's APK. Prefer the class loader's lookup, then the extracted file. */
    private fun loadDexKit() {
        try {
            System.loadLibrary("dexkit")
        } catch (_: UnsatisfiedLinkError) {
            System.load("${moduleApplicationInfo.nativeLibraryDir}/libdexkit.so")
        }
    }

    private fun activate(resolved: Resolved, classLoader: ClassLoader, niagara: String) {
        val activation = Decisions.activation(resolved.report)
        if (!activation.active) {
            log(Log.WARN, TAG, "$niagara: ${activation.logLine()}")
            return
        }
        try {
            val hooks = Installer(this, resolved, classLoader).install()
            val found = activation.logLine() + targetCounts(resolved)
            log(Log.INFO, TAG, "$niagara: $found; ${hooks.size} hooks installed")
        } catch (t: Throwable) {
            log(Log.ERROR, TAG, "$niagara: inactive, installing the hooks failed and none were kept", t)
        }
    }

    /** For hooks that cover several methods, how many were found, so a Niagara update that loses one shows up. */
    private fun targetCounts(resolved: Resolved): String {
        val several = Hook.entries.filter { resolved.methodsOf(it).size > 1 }
        if (several.isEmpty()) return ""
        return several.joinToString(prefix = " (", postfix = ")") { "${it.name}=${resolved.methodsOf(it).size}" }
    }

    companion object {
        const val TAG = "NiagaraPrivateSpace"
        const val NIAGARA_PACKAGE = "bitpit.launcher"
    }
}
