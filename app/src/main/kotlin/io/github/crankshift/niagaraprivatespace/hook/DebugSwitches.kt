package io.github.crankshift.niagaraprivatespace.hook

import io.github.crankshift.niagaraprivatespace.decision.Hook
import io.github.crankshift.niagaraprivatespace.decision.ResolutionReport

/**
 * Two switches for the device check, to see the unsupported-version notice without waiting for a Niagara update.
 * They are Android system properties, read once when Niagara starts, and everything is off unless a property is
 * set to `1`, so they ship switched off and do nothing on a phone where nobody sets them:
 *
 * - `debug.niagaraps.unsupported`: pretend an essential hook is missing, so the module stays inactive and notifies;
 * - `debug.niagaraps.no_tip_system`: pretend Niagara's tip system is missing, so the notice is a Toast.
 *
 * They are set from the device with `adb shell setprop`; `docs/device-check.md` has the steps. System properties
 * are not public API, so reading one goes through reflection and reads as off if that fails.
 */
class DebugSwitches(val unsupported: Boolean, val noTipSystem: Boolean) {
    val anySet: Boolean get() = unsupported || noTipSystem

    /** What the finder found, less the pieces a switch pretends are missing. */
    fun apply(report: ResolutionReport): ResolutionReport {
        var found = report.found
        if (unsupported) found = found - Hook.APP_LAUNCH
        if (noTipSystem) found = found - Hook.TIP_MANAGER
        return ResolutionReport(found)
    }

    override fun toString() =
        listOfNotNull("unsupported".takeIf { unsupported }, "no_tip_system".takeIf { noTipSystem }).joinToString()

    companion object {
        fun read() = DebugSwitches(isOn("debug.niagaraps.unsupported"), isOn("debug.niagaraps.no_tip_system"))

        private fun isOn(property: String): Boolean =
            try {
                val get = Class.forName("android.os.SystemProperties").getMethod("get", String::class.java)
                get.invoke(null, property) == "1"
            } catch (_: Throwable) {
                false
            }
    }
}
