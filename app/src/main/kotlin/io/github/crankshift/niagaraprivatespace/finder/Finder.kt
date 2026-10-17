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
 * a method has. A fingerprint must match exactly once. One that matches nothing, or several things, or that
 * throws, leaves its hook missing instead of guessing, and [onError] hears why so the log can say. One failing
 * fingerprint never stops the others from running.
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
        optionsBuilder?.let { methods[Hook.LONG_PRESS_OPTIONS] = listOf(it) }

        val flags =
            if (itemBase != null && optionsBuilder != null) attempt("profile flags") { findProfileFlags(itemBase, optionsBuilder) } else null
        if (itemBase == null || flags == null) return Resolved(methods, fields)
        fields[Hook.PRIMARY_USER_FLAG] = flags.primaryUser
        fields[Hook.PRIVATE_SPACE_FLAG] = flags.privateSpace

        val filters = attempt("picker filters") { findPickerFilters(itemBase, flags) }.orEmpty()
        val pickerBase = attempt("base picker filter") { filters.filter { it.isBaseOf(filters) }.exactlyOne("base picker filter") }
        if (pickerBase != null) {
            methods[Hook.PICKER_FILTER] = listOf(pickerBase)
            val family = filters.filter { it != pickerBase && it.declaredClass.extends(pickerBase.declaredClass) }
            val leisureApps =
                attempt("Leisure Apps picker filter") {
                    family.filter { it in flags.primaryUser.readers }.exactlyOne("Leisure Apps picker filter")
                }
            leisureApps?.let { methods[Hook.PICKER_FILTER_LEISURE_APPS] = listOf(it) }
            val overrides = family.filter { it != leisureApps }
            if (overrides.isEmpty()) onError("picker filter overrides", IllegalStateException("matched nothing"))
            else methods[Hook.PICKER_FILTER_OVERRIDES] = overrides
        }

        return Resolved(methods, fields)
    }

    /** The base class of everything Niagara places: apps, folders and shortcuts. Its `toString` masks the label. */
    private fun findItemBase(): ClassData =
        bridge.findClass { matcher { usingStrings("Ii(l=***, i=") } }.exactlyOne("item base class")

    /**
     * The method that decides which options a long-press shows for an item. It takes the item and the list
     * to fill, and it is the only such method that also reads the "show most used apps" preference.
     */
    private fun findOptionsBuilder(itemBase: ClassData): MethodData =
        bridge.findMethod {
            matcher {
                returnType("void")
                paramTypes(itemBase.name, "java.util.ArrayList")
                usingStrings("bitpit.launcher.key.SHOW_MOST_USED_APPS")
            }
        }.exactlyOne("long-press options builder")

    /**
     * The two final booleans on every item that say which profile it came from. The options builder reads exactly
     * one boolean declared on the item base class, and that one must be the Private Space flag. The item base
     * class's constructor confirms it: it assigns exactly two booleans, the primary-user flag first and the
     * Private Space flag second, and the builder's boolean has to be the second.
     */
    private fun findProfileFlags(itemBase: ClassData, optionsBuilder: MethodData): ProfileFlags {
        val readByBuilder =
            optionsBuilder.usingFields
                .filter { it.usingType.isRead() }
                .map { it.field }
                .filter { it.isBooleanOf(itemBase) }
                .distinct()
                .exactlyOne("boolean the options builder reads")
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
                .exactlyOne("constructor that assigns the profile flags")
        check(assigned.size == 2 && assigned[1] == readByBuilder) {
            "the constructor assigns ${assigned.size} booleans and the options builder's is not the second"
        }
        return ProfileFlags(primaryUser = assigned[0], privateSpace = assigned[1])
    }

    /**
     * Every app picker filters its candidate apps with a method that takes the item and returns a boolean, and
     * rejects private apps by reading the Private Space flag. So the readers of the flag with that signature are
     * the picker filters. They belong to one family of classes:
     *
     * - the **base** is the filter whose class another filter's class extends ([isBaseOf]);
     * - the **Leisure Apps** filter is the one that also reads the primary-user flag, because it asks for a
     *   personal app;
     * - the **overrides** are the remaining filters in classes that extend the base's class.
     *
     * A filter outside that family is not a picker, and is left alone.
     */
    private fun findPickerFilters(itemBase: ClassData, flags: ProfileFlags): List<MethodData> =
        flags.privateSpace.readers.filter {
            it.returnTypeName == "boolean" && it.paramTypeNames == listOf(itemBase.name) && it.declaredClassName != itemBase.name
        }

    private fun MethodData.isBaseOf(filters: List<MethodData>) =
        filters.any { other -> other != this && other.declaredClass.extends(declaredClass) }

    private fun ClassData?.extends(ancestor: ClassData?): Boolean =
        this != null && ancestor != null && generateSequence(superClass) { it.superClass }.any { it == ancestor }

    private fun FieldData.isBooleanOf(itemBase: ClassData) = declaredClassName == itemBase.name && typeName == "boolean"

    private class ProfileFlags(val primaryUser: FieldData, val privateSpace: FieldData)

    /** The only element. A fingerprint must match exactly once, so none or several is an error. */
    private fun <T> List<T>.exactlyOne(what: String): T =
        singleOrNull() ?: error("$what matched ${if (isEmpty()) "nothing" else "$size targets"}")

    private inline fun <T> attempt(what: String, find: () -> T): T? =
        try {
            find()
        } catch (t: Throwable) {
            onError(what, t)
            null
        }
}
