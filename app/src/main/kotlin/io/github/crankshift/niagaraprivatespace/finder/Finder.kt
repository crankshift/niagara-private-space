package io.github.crankshift.niagaraprivatespace.finder

import io.github.crankshift.niagaraprivatespace.decision.Hook
import java.lang.reflect.Modifier
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

        // The tip system serves the notice about an unsupported version, so it is looked for even when nothing else
        // can be found.
        attempt("tip system") { findTipSystem() }?.let {
            methods[Hook.TIP_MANAGER] = listOf(it.managerConstructor)
            methods[Hook.TIP_REGISTER] = listOf(it.register)
            methods[Hook.TIP_FACTORY] = listOf(it.factory)
            fields[Hook.TIP_HOME_PAGE] = it.homePage
            fields[Hook.TIP_PAUSED] = it.paused
            fields[Hook.TIP_FACTORY_INSTANCE] = it.factoryInstance
        }

        val itemBase = attempt("item base class") { findItemBase() }
        val optionsBuilder = itemBase?.let { attempt("long-press options builder") { findOptionsBuilder(it) } }
        optionsBuilder?.let { methods[Hook.LONG_PRESS_OPTIONS] = listOf(it) }

        val flags =
            if (itemBase != null && optionsBuilder != null) {
                attempt("profile flags") { findProfileFlags(itemBase, optionsBuilder) }
            } else {
                null
            }
        if (itemBase == null || flags == null) return Resolved(methods, fields)
        fields[Hook.PRIMARY_USER_FLAG] = flags.primaryUser
        fields[Hook.PRIVATE_SPACE_FLAG] = flags.privateSpace

        val filters = attempt("picker filters") { findPickerFilters(itemBase, flags) }.orEmpty()
        val pickerBase =
            attempt("base picker filter") { filters.filter { it.isBaseOf(filters) }.exactlyOne("base picker filter") }
        if (pickerBase != null) {
            methods[Hook.PICKER_FILTER] = listOf(pickerBase)
            val family = filters.filter { it != pickerBase && it.declaredClass.extends(pickerBase.declaredClass) }
            val leisureApps =
                attempt("Leisure Apps picker filter") {
                    family.filter { it in flags.primaryUser.readers }.exactlyOne("Leisure Apps picker filter")
                }
            leisureApps?.let { methods[Hook.PICKER_FILTER_LEISURE_APPS] = listOf(it) }
            attempt("picker row step") { findPickerRow(itemBase, pickerBase) }
                ?.let { methods[Hook.PICKER_ROW] = listOf(it) }
            val overrides = family.filter { it != leisureApps }
            if (overrides.isEmpty()) onError("picker filter overrides", IllegalStateException("matched nothing"))
            else methods[Hook.PICKER_FILTER_OVERRIDES] = overrides
        }

        val readers =
            attempt("readers of the Private Space flag") {
                flags.privateSpace.readers.filter { it.declaredClassName != itemBase.name }
            }.orEmpty()
        val suggestionCheck = attempt("suggestion eligibility check") { findSuggestionCheck(itemBase, readers) }
        suggestionCheck?.let { methods[Hook.SUGGESTION_ELIGIBILITY] = listOf(it) }
        suggestionCheck?.let { attempt("item in the suggestion usage record") { findSuggestionItem(itemBase, it) } }
            ?.let { fields[Hook.SUGGESTION_ITEM] = it }
        attempt("options sheet") { findOptionsSheet(readers) }?.let { methods[Hook.OPTIONS_SHEET] = listOf(it) }
        attempt("Add shortcut items") { findAddShortcutItems(itemBase) }
            ?.let { methods[Hook.ADD_SHORTCUT_ITEMS] = listOf(it) }

        val favoritesProvider = attempt("favorites row provider") { findFavoritesRowProvider(itemBase) }
        favoritesProvider?.let { methods[Hook.FAVORITES_ROWS] = listOf(it) }
        val otherProviders =
            favoritesProvider?.let { attempt("other home page row providers") { findOtherRowProviders(itemBase, it) } }
        otherProviders?.let { methods[Hook.OTHER_HOME_ROWS] = it }

        val launch = attempt("app launch method") { findAppLaunch() }
        launch?.let { methods[Hook.APP_LAUNCH] = listOf(it) }
        launch?.let { attempt("what the app launch method launches") { findLaunchFields(it) } }?.let {
            fields[Hook.LAUNCH_TARGET] = it.holder
            fields[Hook.LAUNCH_COMPONENT] = it.component
        }

        val rows = attempt("folder and pop-up row builder") { findPopUpRowBuilder() }
        rows?.let { methods[Hook.FOLDER_AND_POP_UP_ROWS] = listOf(it) }
        rows?.let { attempt("app of a folder or pop-up row") { findRowItem(itemBase, it) } }
            ?.let { fields[Hook.FOLDER_AND_POP_UP_ROW_ITEM] = it }
        rows?.let { attempt("unlock message row") { findEmptyPopUpRow(itemBase, it) } }?.let { row ->
            methods[Hook.EMPTY_POP_UP_ROW] = listOf(row.constructor)
            methods[Hook.EMPTY_POP_UP_ROW_BIND] = listOf(row.bind)
            methods[Hook.EMPTY_POP_UP_ROW_CLICK] = listOf(row.click)
            fields[Hook.EMPTY_POP_UP_ROW_TEXT] = row.text
            fields[Hook.POP_UP_HOST] = row.host
            fields[Hook.POP_UP_NOTIFICATIONS] = row.notifications
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
            it.returnTypeName == "boolean" &&
                it.paramTypeNames == listOf(itemBase.name) &&
                it.declaredClassName != itemBase.name
        }

    /**
     * Every picker adds the row of an app to its list with one static method of the picker base class, which takes
     * the picker, the list being built, and the app as its first three parameters. It serves the candidates and the
     * apps already chosen alike. In the 1638 bytecode it is the only static method of that shape in the class.
     */
    private fun findPickerRow(itemBase: ClassData, pickerFilter: MethodData): MethodData {
        val picker = checkNotNull(pickerFilter.declaredClass) { "picker base class is unknown" }
        return picker.methods.filter {
            Modifier.isStatic(it.modifiers) && it.returnTypeName == "void" &&
                it.paramTypeNames.size >= 3 && it.paramTypeNames[0] == picker.name &&
                it.paramTypeNames[2] == itemBase.name
        }.exactlyOne("row step of the picker base class")
    }

    /**
     * Niagara suggests the apps you use most below favorites. The check that admits an app to those suggestions
     * is the only reader of the Private Space flag that takes one argument which is not the item itself and
     * returns a boolean: it takes a usage record that holds the item.
     */
    private fun findSuggestionCheck(itemBase: ClassData, readers: List<MethodData>): MethodData =
        readers.filter {
            it.returnTypeName == "boolean" && it.paramTypeNames.size == 1 && it.paramTypeNames[0] != itemBase.name
        }.exactlyOne("suggestion eligibility check")

    /** The usage record the suggestion check takes has exactly one field that holds the app, typed as the item base. */
    private fun findSuggestionItem(itemBase: ClassData, suggestionCheck: MethodData): FieldData =
        suggestionCheck.paramTypes.exactlyOne("usage record type").fields
            .filter { it.typeName == itemBase.name }
            .exactlyOne("field of the usage record that holds the item")

    /**
     * The long-press options sheet decides in its constructor which shortcuts of an app can be pinned, and checks
     * the Private Space flag there itself. It is the only constructor that reads the flag.
     */
    private fun findOptionsSheet(readers: List<MethodData>): MethodData =
        readers.filter { it.isConstructor }.exactlyOne("options sheet constructor")

    /**
     * The Add shortcut screen builds one item per shortcut an app offers, and checks the Private Space flag inside a
     * coroutine, where it cannot be wrapped. Its items exist for that screen alone. The coroutine is the one method
     * that asks Android for the shortcut configuration activities, and the item it builds from each of them is
     * the item class whose constructor takes a `LauncherActivityInfo`.
     */
    private fun findAddShortcutItems(itemBase: ClassData): MethodData =
        bridge.findMethod {
            matcher { addInvoke(SHORTCUT_CONFIG_ACTIVITIES) }
        }.exactlyOne("Add shortcut screen coroutine").invokes
            .filter {
                it.isConstructor &&
                    it.declaredClass.extends(itemBase) &&
                    it.paramTypeNames.firstOrNull() == "android.content.pm.LauncherActivityInfo"
            }
            .distinct()
            .exactlyOne("constructor of the Add shortcut screen's items")

    /**
     * The home page is built from sections, and each section turns a placed app into the row it renders (or
     * nothing). The favorites section logs `requestHsi: %s index: %s` as it does, which no other code does.
     */
    private fun findFavoritesRowProvider(itemBase: ClassData): MethodData =
        bridge.findMethod {
            matcher {
                paramTypes(itemBase.name)
                usingStrings("requestHsi: %s index: %s")
            }
        }.exactlyOne("favorites row provider")

    /**
     * The other sections (most-used suggestions, the media section) extend the same section base class as the
     * favorites section and have a method with the same signature. The base class's own abstract version belongs
     * to the base, not to a subclass, and is left out.
     */
    private fun findOtherRowProviders(itemBase: ClassData, favoritesProvider: MethodData): List<MethodData> {
        val favoritesSection = checkNotNull(favoritesProvider.declaredClass) { "favorites section class is unknown" }
        val sectionBase = checkNotNull(favoritesSection.superClass) { "favorites section has no base class" }
        return bridge.findMethod {
            matcher {
                paramTypes(itemBase.name)
                returnType(favoritesProvider.returnTypeName)
            }
        }.filter { it != favoritesProvider && it.declaredClass.extends(sectionBase) }
            .ifEmpty { error("other home page row providers matched nothing") }
    }

    /**
     * The method of the app item class that starts apps of other profiles with `LauncherApps.startMainActivity`. In
     * the 1638 bytecode it is the only method that calls it, and the Niagara button, favorites, pop-ups and search
     * all open apps through it.
     */
    private fun findAppLaunch(): MethodData =
        bridge.findMethod { matcher { addInvoke(START_MAIN_ACTIVITY) } }.exactlyOne("app launch method")

    /**
     * What `startMainActivity` is given: the launch method reads a component name from a small holder (component
     * and user) that the app item keeps in a field of its own. So the component is the only `ComponentName` the
     * launch method reads, and the target is the one field of the launch method's class that holds that component's
     * declaring class.
     */
    private fun findLaunchFields(launch: MethodData): LaunchFields {
        val read = launch.fieldsRead()
        val component =
            read.filter { it.typeName == "android.content.ComponentName" }.distinct()
                .exactlyOne("component name the launch method reads")
        val holder =
            read.filter {
                it.declaredClassName == launch.declaredClassName && it.typeName == component.declaredClassName
            }.distinct().exactlyOne("field that holds the component name's owner")
        return LaunchFields(holder, component)
    }

    /**
     * Folders and pop-ups open through one controller, the only class that refuses to show a pop-up outside a
     * `PopupDialogHost`. Its row builder is the only method of it that takes nothing and returns an `ArrayList`.
     */
    private fun findPopUpRowBuilder(): MethodData =
        bridge.findClass { matcher { usingStrings("Popups can only be shown inside PopupDialogHosts") } }
            .exactlyOne("pop-up controller").methods
            .filter { it.paramTypeNames.isEmpty() && it.returnTypeName == "java.util.ArrayList" }
            .exactlyOne("row builder of the pop-up controller")

    /**
     * Each row the builder makes is a subclass of the app row class, whose `toString` is `Iihsi(i=` and which holds
     * the app in its only field typed as the item base. (The subclasses hold the host too, in a second such field.)
     */
    private fun findRowItem(itemBase: ClassData, rowBuilder: MethodData): FieldData {
        val appRow = bridge.findClass { matcher { usingStrings("Iihsi(i=") } }.exactlyOne("app row class")
        check(rowBuilder.invokes.any { it.isConstructor && it.declaredClass.extends(appRow) }) {
            "the row builder builds no subclass of the app row class"
        }
        return appRow.fields.filter { it.typeName == itemBase.name }
            .exactlyOne("field of the app row that holds the app")
    }

    /**
     * The unlock message borrows Niagara's row for an empty pop-up. It is the class whose constructor takes the host
     * item and Niagara's notifications and uses the row's fixed id; the message hub's empty-state row uses the same
     * id but takes one argument. With it come its bind and click methods, the text view of its holder, and the
     * controller's fields for the two constructor arguments: the notifications by their type, and the host as the
     * one `final` field of the item's type that the row builder reads (the controller's other field of that type,
     * the app currently selected, changes, so it is not final).
     */
    private fun findEmptyPopUpRow(itemBase: ClassData, rowBuilder: MethodData): EmptyPopUpRow {
        val constructor =
            bridge.findMethod { matcher { usingNumbers(EMPTY_STATE_ROW_ID) } }
                .filter { it.isConstructor && it.paramTypeNames.size == 2 && it.paramTypeNames[0] == itemBase.name }
                .exactlyOne("constructor of the empty-pop-up row")
        val row = checkNotNull(constructor.declaredClass) { "the empty-pop-up row class is unknown" }
        val bind =
            row.methods.filter { !it.isConstructor && it.returnTypeName == "void" && it.paramTypeNames.size == 1 }
                .exactlyOne("bind method of the empty-pop-up row")
        val click =
            row.methods.filter {
                it.returnTypeName == "void" && it.paramTypeNames.size == 3 &&
                    it.paramTypeNames[2] == "android.view.View"
            }.exactlyOne("click method of the empty-pop-up row")
        val text =
            bind.fieldsRead()
                .filter { it.typeName == "android.widget.TextView" }.distinct()
                .exactlyOne("text view the empty-pop-up row binds")
        val controllerFields =
            rowBuilder.fieldsRead()
                .filter { it.declaredClassName == rowBuilder.declaredClassName }
        val host =
            controllerFields.filter { it.typeName == itemBase.name && Modifier.isFinal(it.modifiers) }.distinct()
                .exactlyOne("final field of the item type the row builder reads")
        val notifications =
            controllerFields.filter { it.typeName == constructor.paramTypeNames[1] }.distinct()
                .exactlyOne("controller field of the empty-pop-up row's second argument")
        return EmptyPopUpRow(constructor, bind, click, text, host, notifications)
    }

    /** The fields a method reads, in the order it reads them. */
    private fun MethodData.fieldsRead(): List<FieldData> = usingFields.filter { it.usingType.isRead() }.map { it.field }

    /**
     * Niagara's tips. A tip is an instance of the class whose `toString` is `Thsi{tt=`. One static method
     * builds it from a message, a button, an icon and two callback interfaces (its dismiss and click). Its constructor
     * takes the tip manager, which is the one parameter type with a void method that takes an enum (the tip type)
     * and an interface (the provider that answers whether to show a tip and builds it). That enum is the one whose
     * constants are Niagara's tip types; it must hold the name [BorrowedTip.TYPE_NAME]. The manager's two other
     * fields are the home page it tells about tips (null until the home page exists) and a paused flag; the
     * notice waits until the first is set and the second is clear. The home page holds the factory in the only field
     * of the factory's type.
     */
    private fun findTipSystem(): TipSystem {
        val tip = bridge.findClass { matcher { usingStrings("Thsi{tt=") } }.exactlyOne("tip class")
        val factory =
            bridge.findMethod { matcher { returnType(tip.name) } }.filter {
                Modifier.isStatic(it.modifiers) && it.paramTypeNames.size == 9 &&
                    it.paramTypeNames[0] == it.declaredClassName &&
                    it.paramTypeNames[3] == "java.lang.CharSequence" && it.paramTypeNames[8] == "int" &&
                    it.paramTypes[6].isInterface() && it.paramTypes[7].isInterface()
            }.exactlyOne("tip factory")
        val constructor = tip.methods.filter { it.isConstructor }.exactlyOne("constructor of the tip class")
        val managers =
            constructor.paramTypes.mapNotNull { type -> findTipRegister(type)?.let { type to it } }
        val (manager, register) = managers.exactlyOne("tip manager")
        val tipTypes = checkNotNull(register.paramTypes[0]) { "tip type unknown" }
        check(tipTypes.methods.any { BorrowedTip.TYPE_NAME in it.usingStrings }) {
            "the tip types lack ${BorrowedTip.TYPE_NAME}"
        }
        val managerConstructor = manager.methods.filter { it.isConstructor }.exactlyOne("tip manager constructor")
        val paused = manager.fields.filter { it.typeName == "boolean" }.exactlyOne("paused flag of the tip manager")
        val homePage =
            manager.fields.filter { it.typeName != "boolean" && !it.typeName.startsWith("java.") }
                .exactlyOne("home page field of the tip manager")
        val factoryInstance =
            checkNotNull(homePage.type) { "home page type unknown" }.fields
                .filter { it.typeName == factory.declaredClassName }
                .exactlyOne("field of the home page that holds the tip factory")
        return TipSystem(managerConstructor, register, factory, homePage, paused, factoryInstance)
    }

    /** The tip manager's registration method: void, taking the tip type (an enum) and the provider (an interface). */
    private fun findTipRegister(candidate: ClassData): MethodData? =
        candidate.methods.filter {
            !it.isConstructor && it.returnTypeName == "void" && it.paramTypeNames.size == 2 &&
                it.paramTypes[0].isEnum() && it.paramTypes[1].isInterface()
        }.singleOrNull()

    private fun ClassData.isInterface() = Modifier.isInterface(modifiers)

    private fun ClassData.isEnum() = modifiers and ACC_ENUM != 0

    private fun MethodData.isBaseOf(filters: List<MethodData>) =
        filters.any { other -> other != this && other.declaredClass.extends(declaredClass) }

    private fun ClassData?.extends(ancestor: ClassData?): Boolean =
        this != null && ancestor != null && generateSequence(superClass) { it.superClass }.any { it == ancestor }

    private fun FieldData.isBooleanOf(itemBase: ClassData) = declaredClassName == itemBase.name && typeName == "boolean"

    private companion object {
        const val ACC_ENUM = 0x4000

        /** The fixed id of the empty-state rows of a pop-up and of the message hub. */
        const val EMPTY_STATE_ROW_ID = 412316860418L

        const val START_MAIN_ACTIVITY =
            "Landroid/content/pm/LauncherApps;->startMainActivity(" +
                "Landroid/content/ComponentName;Landroid/os/UserHandle;Landroid/graphics/Rect;Landroid/os/Bundle;)V"

        const val SHORTCUT_CONFIG_ACTIVITIES =
            "Landroid/content/pm/LauncherApps;->" +
                "getShortcutConfigActivityList(Ljava/lang/String;Landroid/os/UserHandle;)Ljava/util/List;"
    }

    private class ProfileFlags(val primaryUser: FieldData, val privateSpace: FieldData)

    private class TipSystem(
        val managerConstructor: MethodData,
        val register: MethodData,
        val factory: MethodData,
        val homePage: FieldData,
        val paused: FieldData,
        val factoryInstance: FieldData,
    )

    private class LaunchFields(val holder: FieldData, val component: FieldData)

    private class EmptyPopUpRow(
        val constructor: MethodData,
        val bind: MethodData,
        val click: MethodData,
        val text: FieldData,
        val host: FieldData,
        val notifications: FieldData,
    )

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
