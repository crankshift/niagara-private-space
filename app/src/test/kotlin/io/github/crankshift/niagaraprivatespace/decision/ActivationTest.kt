package io.github.crankshift.niagaraprivatespace.decision

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ActivationTest {
    private fun reportOf(vararg found: Hook) = ResolutionReport(found.toSet())

    private val everyHook =
        arrayOf(
            Hook.LONG_PRESS_OPTIONS,
            Hook.PRIVATE_SPACE_FLAG,
            Hook.PRIMARY_USER_FLAG,
            Hook.PICKER_FILTER,
            Hook.PICKER_FILTER_OVERRIDES,
            Hook.PICKER_FILTER_LEISURE_APPS,
            Hook.SUGGESTION_ELIGIBILITY,
            Hook.SUGGESTION_ITEM,
            Hook.OPTIONS_SHEET,
            Hook.ADD_SHORTCUT_ITEMS,
            Hook.FAVORITES_ROWS,
            Hook.OTHER_HOME_ROWS,
        )

    @Test
    fun `the module needs every hook it finds in Niagara`() {
        assertEquals(everyHook.toSet(), Hook.entries.toSet())
    }

    @Test
    fun `the module activates when every hook was found`() {
        val activation = Decisions.activation(reportOf(*everyHook))

        assertTrue(activation.active)
        assertTrue(activation.missing.isEmpty())
    }

    @Test
    fun `a missing hook keeps the whole module inactive, whichever it is`() {
        for (missing in everyHook) {
            val activation = Decisions.activation(reportOf(*everyHook.filter { it != missing }.toTypedArray()))

            assertFalse(activation.active, "module must stay inactive without $missing")
            assertEquals(listOf(missing), activation.missing, "$missing should be the only hook reported missing")
        }
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
    fun `the log line of an active module lists what was found`() {
        val line = Decisions.activation(reportOf(*everyHook)).logLine()

        assertTrue(line.startsWith("active"), line)
        assertTrue(line.contains("LONG_PRESS_OPTIONS"), line)
        assertTrue(line.contains("PRIVATE_SPACE_FLAG"), line)
        assertTrue(line.contains("PRIMARY_USER_FLAG"), line)
        assertTrue(line.contains("PICKER_FILTER_OVERRIDES"), line)
        assertTrue(line.contains("ADD_SHORTCUT_ITEMS"), line)
    }
}
