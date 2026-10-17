package io.github.crankshift.niagaraprivatespace

import android.util.Log
import io.github.crankshift.niagaraprivatespace.decision.Activation
import io.github.crankshift.niagaraprivatespace.decision.Decisions
import io.github.crankshift.niagaraprivatespace.decision.Hook
import io.github.crankshift.niagaraprivatespace.decision.ResolutionReport
import io.github.crankshift.niagaraprivatespace.finder.Finder
import io.github.crankshift.niagaraprivatespace.finder.Resolved
import io.github.crankshift.niagaraprivatespace.hook.DebugSwitches
import io.github.crankshift.niagaraprivatespace.hook.Installer
import io.github.crankshift.niagaraprivatespace.hook.ModuleLog
import io.github.crankshift.niagaraprivatespace.hook.NiagaraVersion
import io.github.crankshift.niagaraprivatespace.hook.Notice
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

        val version = NiagaraVersion.read(param.applicationInfo)
        val niagara = "Niagara ${version ?: "?"}"
        try {
            loadDexKit()
            DexKitBridge.create(param.classLoader, true).use { bridge ->
                val finder = Finder(bridge) { what, error ->
                    log(Log.WARN, TAG, "$niagara: finding the $what failed", error)
                }
                val resolved = finder.resolve()
                activate(resolved, param.classLoader, niagara, version)
            }
        } catch (t: Throwable) {
            log(Log.ERROR, TAG, "$niagara: inactive, the finder could not run", t)
            // The user hears about this as about any Niagara the module cannot recognise, with a Toast.
            val nothingFound = Decisions.activation(ResolutionReport(emptySet()))
            val nothing = Resolved(emptyMap(), emptyMap())
            Notice(this, nothing, param.classLoader, nothingFound, version, ModuleLog(this)).start()
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

    private fun activate(resolved: Resolved, classLoader: ClassLoader, niagara: String, version: NiagaraVersion?) {
        val switches = DebugSwitches.read()
        if (switches.anySet) log(Log.INFO, TAG, "$niagara: debug switches on: $switches")
        val activation = Decisions.activation(switches.apply(resolved.report))
        val installed =
            if (activation.active) {
                install(resolved, classLoader, niagara, activation)
            } else {
                log(Log.WARN, TAG, "$niagara: ${activation.logLine()}")
                false
            }
        // A module that could not install anything is as inactive as one that found nothing.
        val finalActivation = if (installed) activation else activation.afterFailedInstall()
        if (finalActivation.notify) {
            Notice(this, resolved, classLoader, finalActivation, version, ModuleLog(this)).start()
        }
    }

    /** Installs every hook or none. True if the module is now active. */
    private fun install(
        resolved: Resolved,
        classLoader: ClassLoader,
        niagara: String,
        activation: Activation,
    ): Boolean =
        try {
            val hooks = Installer(this, resolved, classLoader, activation.availableParts).install()
            val found = activation.logLine() + targetCounts(resolved)
            val dropped = hooks.droppedParts.joinToString("") { "; $it left out at install" }
            log(Log.INFO, TAG, "$niagara: $found$dropped; ${hooks.size} hooks installed")
            true
        } catch (t: Throwable) {
            log(Log.ERROR, TAG, "$niagara: inactive, installing the hooks failed and none were kept", t)
            false
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
