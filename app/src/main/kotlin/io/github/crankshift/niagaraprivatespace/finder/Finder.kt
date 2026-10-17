package io.github.crankshift.niagaraprivatespace.finder

import io.github.crankshift.niagaraprivatespace.decision.Hook
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.result.ClassData
import org.luckypray.dexkit.result.FieldData
import org.luckypray.dexkit.result.MethodData

/**
 * Locates the Niagara code the hooks need, by fingerprint, because Niagara's names change on every release.
 *
 * Each fingerprint starts from something R8 cannot rename (string constants, JDK and Android types) and
 * reaches the rest through the code's own structure: who reads a field, who extends whom, which signature
 * a method has. A fingerprint that does not match exactly as expected leaves its hook missing instead of
 * guessing, and one failing fingerprint never stops the others from running. [onError] hears about the ones
 * that threw, so the log can say why a hook is missing.
 */
class Finder(
    private val bridge: DexKitBridge,
    private val onError: (what: String, error: Throwable) -> Unit = { _, _ -> },
) {
    fun resolve(): Resolved {
        val methods = linkedMapOf<Hook, List<MethodData>>()
        val fields = linkedMapOf<Hook, FieldData>()

        val itemBase = attempt("item base class") { findItemBase() }

        val optionsBuilder = itemBase?.let { attempt("long-press options builder") { findOptionsBuilder(it) } }
        if (optionsBuilder != null) methods[Hook.LONG_PRESS_OPTIONS] = listOf(optionsBuilder)

        if (itemBase != null && optionsBuilder != null) {
            val flags = attempt("profile flags") { findProfileFlags(itemBase, optionsBuilder) }
            if (flags != null) {
                fields[Hook.PRIMARY_USER_FLAG] = flags.primaryUser
                fields[Hook.PRIVATE_SPACE_FLAG] = flags.privateSpace
            }
        }

        return Resolved(methods, fields)
    }

    /** The base class of everything Niagara places: apps, folders and shortcuts. Its `toString` masks the label. */
    private fun findItemBase(): ClassData? =
        bridge.findClass { matcher { usingStrings("Ii(l=***, i=") } }.onlyOrNull()

    /**
     * The method that decides which options a long-press shows for an item. It takes the item and the list
     * to fill, and it is the only such method that also reads the "show most used apps" preference.
     */
    private fun findOptionsBuilder(itemBase: ClassData): MethodData? =
        bridge.findMethod {
            matcher {
                returnType("void")
                paramTypes(itemBase.name, "java.util.ArrayList")
                usingStrings("bitpit.launcher.key.SHOW_MOST_USED_APPS")
            }
        }.onlyOrNull()

    /**
     * The two final booleans on every item that say which profile it came from. The options builder reads exactly
     * one boolean declared on the item base class, and that one must be the Private Space flag. The item base
     * class's constructor confirms it: it assigns exactly two booleans, the primary-user flag first and the
     * Private Space flag second, and the builder's boolean has to be the second.
     */
    private fun findProfileFlags(itemBase: ClassData, optionsBuilder: MethodData): ProfileFlags? {
        val readByBuilder =
            optionsBuilder.usingFields
                .filter { it.usingType.isRead() }
                .map { it.field }
                .filter { it.isBooleanOf(itemBase) }
                .distinct()
                .onlyOrNull() ?: return null
        val assigned =
            itemBase.methods
                .filter { it.isConstructor }
                .map { constructor ->
                    constructor.usingFields
                        .filter { it.usingType.isWrite() }
                        .map { it.field }
                        .filter { it.isBooleanOf(itemBase) }
                }
                .filter { it.isNotEmpty() }
                .onlyOrNull() ?: return null
        if (assigned.size != 2 || assigned[1] != readByBuilder) return null
        return ProfileFlags(primaryUser = assigned[0], privateSpace = assigned[1])
    }

    private fun FieldData.isBooleanOf(itemBase: ClassData) = declaredClassName == itemBase.name && typeName == "boolean"

    private class ProfileFlags(val primaryUser: FieldData, val privateSpace: FieldData)

    private inline fun <T> attempt(what: String, find: () -> T?): T? =
        try {
            find()
        } catch (t: Throwable) {
            onError(what, t)
            null
        }
}

/** The only element, or null when there are none or several: a fingerprint must match exactly once. */
internal fun <T> List<T>.onlyOrNull(): T? = if (size == 1) this[0] else null
