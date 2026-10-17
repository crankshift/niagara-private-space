package io.github.crankshift.niagaraprivatespace.hook

import io.github.crankshift.niagaraprivatespace.decision.Decisions
import io.github.crankshift.niagaraprivatespace.decision.ProfileKind
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Hides locked private apps from folders and pop-ups. Both open through one controller, which builds a row for each
 * app of the folder or pop-up from the contents Niagara loaded. The hook lets Niagara build the rows, then asks the
 * decision module which to keep and whether to add the unlock message below them. Only the rows are changed: the
 * stored contents stay as they are, because the next time the user edits a folder Niagara writes them back, and a
 * private app dropped from them would be gone for good.
 */
object FolderHooks {
    fun install(
        hooks: InstalledHooks,
        flags: ItemFlags,
        rowBuilder: Method,
        rowItem: Field,
        unlockMessageRow: UnlockMessageRow?,
        log: ModuleLog,
    ) {
        rowItem.isAccessible = true
        hooks.hook(rowBuilder) { chain ->
            val built = chain.proceed() as ArrayList<*>
            val rows = built.toList()
            fun kindOf(row: Any?): ProfileKind = flags.kindOf(checkNotNull(rowItem.get(row)) { "a row has no app" })

            // Android is asked only once a private app is among the rows, as PrivateSpaceState expects.
            val privateSpaceLocked = rows.any { kindOf(it) == ProfileKind.PRIVATE } && PrivateSpaceState.isLocked()
            val contents =
                Decisions.folderOrPopUpContents(rows, ::kindOf, privateSpaceLocked, unlockMessageRow != null)
            if (contents.rows.size == rows.size && !contents.showUnlockMessage) {
                built
            } else {
                ArrayList<Any?>(contents.rows).also { shown ->
                    if (contents.showUnlockMessage && unlockMessageRow != null) {
                        addUnlockMessage(shown, unlockMessageRow, chain.thisObject, log)
                    }
                }
            }
        }
    }

    /**
     * The unlock message is cosmetic. If building it fails, the folder still hides its private apps: failing the
     * whole hook would hand Niagara's unfiltered rows back to the user.
     */
    private fun addUnlockMessage(shown: MutableList<Any?>, row: UnlockMessageRow, controller: Any?, log: ModuleLog) {
        try {
            shown += row.newRow(checkNotNull(controller) { "the pop-up controller is unknown" })
        } catch (t: Throwable) {
            log.warn("building the unlock message failed; private apps are hidden without it", t)
        }
    }
}
