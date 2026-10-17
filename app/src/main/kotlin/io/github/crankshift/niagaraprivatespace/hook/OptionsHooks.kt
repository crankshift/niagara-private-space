package io.github.crankshift.niagaraprivatespace.hook

import io.github.crankshift.niagaraprivatespace.decision.Decisions
import java.lang.reflect.Constructor
import java.lang.reflect.Method

/** Gives a private app the long-press options of a work app, and pinnable shortcuts. */
object OptionsHooks {
    fun install(hooks: InstalledHooks, flags: ItemFlags, optionsBuilder: Method) {
        hooks.hook(optionsBuilder) { chain ->
            val item = chain.getArg(0)
            if (item == null) {
                chain.proceed()
            } else {
                val seen = Decisions.profileForLongPressOptions(flags.kindOf(item))
                flags.whileSeenAs(item, seen) { chain.proceed() }
            }
        }
    }

    /**
     * The options sheet builds the options for an app's shortcuts in its constructor: it asks the options builder
     * and then checks the Private Space flag itself, to decide whether a shortcut can be pinned to the app list.
     * The builder hook has already restored the flag by then, so the constructor holds the change open until it
     * returns.
     */
    fun installSheet(hooks: InstalledHooks, optionsSheet: Constructor<*>) {
        hooks.hook(optionsSheet) { chain -> ItemFlags.FlagChangeScope().around { chain.proceed() } }
    }
}
