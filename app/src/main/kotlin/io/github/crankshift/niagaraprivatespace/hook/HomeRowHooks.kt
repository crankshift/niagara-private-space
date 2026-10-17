package io.github.crankshift.niagaraprivatespace.hook

import io.github.crankshift.niagaraprivatespace.decision.Decisions
import java.lang.reflect.Method

/**
 * Hides locked private apps from the home page. Niagara builds each section of the home page (favorites,
 * suggestions, the media section) by asking a row provider for the row of every placed app; a provider that
 * answers nothing leaves the app out. In the 1638 bytecode one method asks the providers, for the first build
 * and for incremental updates. Locking swaps in new item instances, so the rows should be asked for again; that
 * is read from the code and left to the device check.
 *
 * Only the rows are filtered. Niagara's stored lists stay as they are, because the next time the user edits
 * favorites Niagara writes those lists back, and a private app removed from them would be gone for good.
 */
object HomeRowHooks {
    fun install(hooks: InstalledHooks, flags: ItemFlags, rowProviders: List<Method>) {
        rowProviders.forEach { provider ->
            hooks.hook(provider) { chain ->
                val row = chain.proceed()
                val item = chain.getArg(0)
                if (row == null || item == null) {
                    row
                } else {
                    val kind = flags.kindOf(item)
                    if (Decisions.mayBeListed(kind, PrivateSpaceState.isLockedFor(kind))) row else null
                }
            }
        }
    }
}
