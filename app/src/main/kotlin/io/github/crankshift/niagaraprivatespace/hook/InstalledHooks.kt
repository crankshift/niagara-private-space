package io.github.crankshift.niagaraprivatespace.hook

import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Executable

/**
 * The hooks the module has put into Niagara. Installing is all or nothing: the hooks that lift Niagara's blocks
 * only make sense together with the ones that hide private apps again, so a failure part-way removes every hook.
 */
class InstalledHooks(private val xposed: XposedInterface) {
    private val handles = mutableListOf<XposedInterface.HookHandle>()

    val size get() = handles.size

    fun hook(target: Executable, hooker: XposedInterface.Hooker) {
        handles += xposed.hook(target).intercept(hooker)
    }

    fun removeAll() {
        handles.forEach { runCatching { it.unhook() } }
        handles.clear()
    }
}
