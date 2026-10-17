package io.github.crankshift.niagaraprivatespace.hook

import io.github.crankshift.niagaraprivatespace.decision.Decisions
import java.lang.reflect.Method

/** Gives a private app the long-press options of a work app. */
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
}
