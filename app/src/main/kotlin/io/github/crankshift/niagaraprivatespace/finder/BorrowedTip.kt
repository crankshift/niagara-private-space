package io.github.crankshift.niagaraprivatespace.finder

/**
 * The tip type the notice borrows. From reading Niagara's code, it is shown only on Xiaomi phones, so on any other
 * phone nothing of Niagara's registers or removes it. The enum's constants are fields with obfuscated names, but
 * each constant keeps its name as a string (that is what `Enum.valueOf` reads), so the type is looked up by this
 * name.
 */
object BorrowedTip {
    const val TYPE_NAME = "XIAOMI_FULL_SCREEN_GESTURES_MIUI_12"
}
