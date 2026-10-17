package io.github.crankshift.niagaraprivatespace.decision

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ActivationTest {
    private fun reportOf(vararg found: Hook) = ResolutionReport(found.toSet())

    private val essentialHooks =
        arrayOf(
            Hook.LONG_PRESS_OPTIONS,
            Hook.PRIVATE_SPACE_FLAG,
            Hook.PRIMARY_USER_FLAG,
            Hook.PICKER_FILTER,
            Hook.PICKER_FILTER_OVERRIDES,
            Hook.PICKER_FILTER_LEISURE_APPS,
            Hook.PICKER_ROW,
            Hook.SUGGESTION_ELIGIBILITY,
            Hook.SUGGESTION_ITEM,
            Hook.OPTIONS_SHEET,
            Hook.ADD_SHORTCUT_ITEMS,
            Hook.FAVORITES_ROWS,
            Hook.OTHER_HOME_ROWS,
            Hook.APP_LAUNCH,
            Hook.LAUNCH_TARGET,
            Hook.LAUNCH_COMPONENT,
            Hook.FOLDER_AND_POP_UP_ROWS,
            Hook.FOLDER_AND_POP_UP_ROW_ITEM,
        )

    /** The unlock message row: borrowed from Niagara, so a Niagara update may take it away. */
    private val unlockMessageRowHooks =
        arrayOf(
            Hook.EMPTY_POP_UP_ROW,
            Hook.EMPTY_POP_UP_ROW_BIND,
            Hook.EMPTY_POP_UP_ROW_CLICK,
            Hook.EMPTY_POP_UP_ROW_TEXT,
            Hook.POP_UP_HOST,
            Hook.POP_UP_NOTIFICATIONS,
        )

    private val everyHook = essentialHooks + unlockMessageRowHooks

    @Test
    fun `these lists name every hook the finder can report`() {
        assertEquals(everyHook.toSet(), Hook.entries.toSet())
    }

    @Test
    fun `the module activates when every hook was found`() {
        val activation = Decisions.activation(reportOf(*everyHook))

        assertTrue(activation.active)
        assertTrue(activation.missing.isEmpty())
    }

    @Test
    fun `a missing essential hook keeps the whole module inactive, whichever it is`() {
        for (missing in essentialHooks) {
            val activation = Decisions.activation(reportOf(*everyHook.filter { it != missing }.toTypedArray()))

            assertFalse(activation.active, "module must stay inactive without $missing")
            assertEquals(listOf(missing), activation.missing, "$missing should be the only hook reported missing")
        }
    }

    @Test
    fun `a missing hook of the unlock message row keeps the module active, whichever it is`() {
        for (missing in unlockMessageRowHooks) {
            val activation = Decisions.activation(reportOf(*everyHook.filter { it != missing }.toTypedArray()))

            assertTrue(activation.active, "module must stay active without $missing")
            assertEquals(listOf(missing), activation.missing, "$missing should be the only hook reported missing")
        }
    }

    @Test
    fun `the unlock message row is available only when all its hooks were found`() {
        assertEquals(setOf(OptionalPart.UNLOCK_MESSAGE_ROW), Decisions.activation(reportOf(*everyHook)).availableParts)

        for (missing in unlockMessageRowHooks) {
            val activation = Decisions.activation(reportOf(*everyHook.filter { it != missing }.toTypedArray()))

            assertEquals(emptySet<OptionalPart>(), activation.availableParts, "without $missing")
        }
    }

    @Test
    fun `no optional part is available when the module is inactive`() {
        val activation = Decisions.activation(reportOf(*unlockMessageRowHooks))

        assertFalse(activation.active)
        assertEquals(emptySet<OptionalPart>(), activation.availableParts)
    }

    @Test
    fun `an empty report keeps the module inactive and names every hook as missing`() {
        val activation = Decisions.activation(reportOf())

        assertFalse(activation.active)
        assertEquals(listOf(*everyHook), activation.missing)
    }

    @Test
    fun `the log line of an inactive module names only what is missing`() {
        val allButOne = everyHook.filter { it != Hook.LONG_PRESS_OPTIONS }.toTypedArray()

        val line = Decisions.activation(reportOf(*allButOne)).logLine()

        assertTrue(line.startsWith("inactive"), line)
        assertTrue(line.contains("LONG_PRESS_OPTIONS"), line)
        assertFalse(line.contains("PRIVATE_SPACE_FLAG"), line)
    }

    @Test
    fun `the log line of an active module says which optional part it runs without`() {
        val withoutClick = everyHook.filter { it != Hook.EMPTY_POP_UP_ROW_CLICK }.toTypedArray()

        val line = Decisions.activation(reportOf(*withoutClick)).logLine()

        assertTrue(line.startsWith("active"), line)
        assertTrue(line.contains("without UNLOCK_MESSAGE_ROW"), line)
        assertTrue(line.contains("EMPTY_POP_UP_ROW_CLICK"), line)
    }

    @Test
    fun `the log line of a module with every part has nothing to say about optional parts`() {
        val line = Decisions.activation(reportOf(*everyHook)).logLine()

        assertFalse(line.contains("without"), line)
    }

    @Test
    fun `the log line of an active module lists what was found`() {
        val line = Decisions.activation(reportOf(*everyHook)).logLine()

        assertTrue(line.startsWith("active"), line)
        assertTrue(line.contains("LONG_PRESS_OPTIONS"), line)
        assertTrue(line.contains("PRIVATE_SPACE_FLAG"), line)
        assertTrue(line.contains("PRIMARY_USER_FLAG"), line)
        assertTrue(line.contains("PICKER_FILTER_OVERRIDES"), line)
        assertTrue(line.contains("ADD_SHORTCUT_ITEMS"), line)
        assertTrue(line.contains("APP_LAUNCH"), line)
    }
}
