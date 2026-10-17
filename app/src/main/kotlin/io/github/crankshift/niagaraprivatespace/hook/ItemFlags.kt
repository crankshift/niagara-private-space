package io.github.crankshift.niagaraprivatespace.hook

import io.github.crankshift.niagaraprivatespace.decision.ProfileKind
import java.lang.reflect.Field

/**
 * Niagara's two per-item profile flags: "is primary user" and "is Private Space". Niagara checks them in many
 * places, and some of them (the Private Space section of the app list, hiding private apps from search) must
 * keep seeing a private app as private, so the module never changes the flags for good: [whileSeenAs] changes
 * them for the duration of one check and restores them. See
 * docs/adr/0001-clear-the-private-space-flag-for-one-check.md.
 *
 * The flags are final fields, which reflection may write after `setAccessible`. [canWriteFinalFields] checks
 * that the runtime allows it, so the module can stay inactive instead of failing on every long-press. If a
 * write is refused anyway, the hook that asked for it throws, the framework's protective mode runs Niagara's
 * own code, and private apps stay blocked: it fails closed.
 */
class ItemFlags(private val primaryUser: Field, private val privateSpace: Field) {
    init {
        primaryUser.isAccessible = true
        privateSpace.isAccessible = true
    }

    /** The profile Niagara believes [item] is from. An item from no known profile counts as work. */
    fun kindOf(item: Any): ProfileKind =
        when {
            privateSpace.getBoolean(item) -> ProfileKind.PRIVATE
            primaryUser.getBoolean(item) -> ProfileKind.PERSONAL
            else -> ProfileKind.WORK
        }

    /** Runs [block] with [item] looking like an app from [seen] to Niagara, then puts the flags back. */
    fun <T> whileSeenAs(item: Any, seen: ProfileKind, block: () -> T): T {
        val wasPrimaryUser = primaryUser.getBoolean(item)
        val wasPrivateSpace = privateSpace.getBoolean(item)
        val nowPrimaryUser = seen == ProfileKind.PERSONAL
        val nowPrivateSpace = seen == ProfileKind.PRIVATE
        if (wasPrimaryUser == nowPrimaryUser && wasPrivateSpace == nowPrivateSpace) return block()
        try {
            primaryUser.setBoolean(item, nowPrimaryUser)
            privateSpace.setBoolean(item, nowPrivateSpace)
            return block()
        } finally {
            primaryUser.setBoolean(item, wasPrimaryUser)
            privateSpace.setBoolean(item, wasPrivateSpace)
        }
    }

    companion object {
        /** A final field of a class of our own, written the way [whileSeenAs] writes Niagara's. */
        fun canWriteFinalFields(): Boolean =
            try {
                val probe = Probe()
                val field = Probe::class.java.getDeclaredField("flag")
                field.isAccessible = true
                field.setBoolean(probe, false)
                !field.getBoolean(probe)
            } catch (_: Throwable) {
                false
            }
    }

    private class Probe {
        @JvmField
        val flag: Boolean = true
    }
}
