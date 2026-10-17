package io.github.crankshift.niagaraprivatespace.hook

import io.github.crankshift.niagaraprivatespace.decision.OptionalPart
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Executable

/**
 * The hooks the module has put into Niagara. Installing is all or nothing: the hooks that lift Niagara's blocks
 * only make sense together with the ones that hide private apps again, so a failure part-way removes every hook.
 * The one exception is an optional part ([optional]): if it fails, its own hooks are removed, the part is
 * recorded as dropped, and the rest stays.
 */
class InstalledHooks(private val xposed: XposedInterface) {
    private val handles = mutableListOf<XposedInterface.HookHandle>()

    val size get() = handles.size

    fun hook(target: Executable, hooker: XposedInterface.Hooker) {
        handles += xposed.hook(target).intercept(hooker)
    }

    private val dropped = mutableListOf<OptionalPart>()

    /** The optional parts that could not be installed and were left out. */
    val droppedParts: List<OptionalPart> get() = dropped

    /**
     * Installs an optional [part]: if [install] throws, the hooks it added are removed again, the part is recorded
     * as dropped, and the hooks installed before it stay. An optional part never takes the module down with it.
     */
    fun optional(part: OptionalPart, log: ModuleLog, install: () -> Unit) {
        val before = handles.size
        try {
            install()
        } catch (t: Throwable) {
            val added = handles.subList(before, handles.size)
            added.forEach { runCatching { it.unhook() } }
            added.clear()
            dropped += part
            log.warn("$part could not be installed and was left out", t)
        }
    }

    fun removeAll() {
        handles.forEach { runCatching { it.unhook() } }
        handles.clear()
    }
}
