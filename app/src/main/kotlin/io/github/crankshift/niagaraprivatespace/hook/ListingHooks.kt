package io.github.crankshift.niagaraprivatespace.hook

import io.github.crankshift.niagaraprivatespace.decision.Decisions
import io.github.crankshift.niagaraprivatespace.decision.ProfileKind
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Lets private apps into the lists that offer apps by themselves: the most-used suggestions below favorites and
 * the Add shortcut screen. Niagara keeps private apps out of both by reading their Private Space flag. While
 * Private Space is locked they stay out.
 */
object ListingHooks {
    /**
     * Suggestions: Niagara asks one method whether an app, wrapped in a usage record, may be suggested. The hook
     * shows an unlocked private app as a work app for that one call.
     */
    fun installSuggestions(hooks: InstalledHooks, flags: ItemFlags, suggestionCheck: Method, itemInRecord: Field) {
        itemInRecord.isAccessible = true
        hooks.hook(suggestionCheck) { chain ->
            val item = itemInRecord.get(chain.getArg(0))
            if (item == null) {
                chain.proceed()
            } else {
                flags.whileSeenAs(item, profileSeen(flags, item)) { chain.proceed() }
            }
        }
    }

    /**
     * Add shortcut screen: it builds one item per shortcut an app offers and checks each item's flag inside a
     * coroutine, where no call can be wrapped. These items exist for that screen alone, so an unlocked private
     * app's items are made to look like work apps as soon as they are built. In the 1638 bytecode that constructor
     * has one caller, the Add shortcut coroutine; what the screen does with an item later was not traced.
     */
    fun installAddShortcutItems(hooks: InstalledHooks, flags: ItemFlags, itemConstructor: Constructor<*>) {
        hooks.hook(itemConstructor) { chain ->
            val result = chain.proceed()
            val item = chain.thisObject
            if (item != null) {
                val seen = profileSeen(flags, item)
                if (seen != flags.kindOf(item)) flags.showForGoodAs(item, seen)
            }
            result
        }
    }

    private fun profileSeen(flags: ItemFlags, item: Any): ProfileKind {
        val kind = flags.kindOf(item)
        return Decisions.profileSeenWhenListing(kind, PrivateSpaceState.isLockedFor(kind))
    }
}
