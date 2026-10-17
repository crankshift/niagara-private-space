package io.github.crankshift.niagaraprivatespace.decision

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ActivationTest {
    private fun reportOf(vararg found: Hook) = ResolutionReport(found.toSet())

    private val essentialHooks = HookGroups.essential.toTypedArray()
    private val unlockMessageRowHooks = HookGroups.unlockMessageRow.toTypedArray()
    private val tipSystemHooks = HookGroups.tipSystem.toTypedArray()
    private val everyHook = HookGroups.every.toTypedArray()

    @Test
    fun `these lists name every hook the finder can report`() {
        assertEquals(HookGroups.every.toSet(), Hook.entries.toSet())
        assertEquals(HookGroups.every.size, Hook.entries.size, "a hook is in two groups")
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
    fun `a missing hook of the unlock message row or the tip system keeps the module active, whichever it is`() {
        for (missing in unlockMessageRowHooks + tipSystemHooks) {
            val activation = Decisions.activation(reportOf(*everyHook.filter { it != missing }.toTypedArray()))

            assertTrue(activation.active, "module must stay active without $missing")
            assertEquals(listOf(missing), activation.missing, "$missing should be the only hook reported missing")
        }
    }

    @Test
    fun `an optional part is available only when all its hooks were found`() {
        val both = setOf(OptionalPart.UNLOCK_MESSAGE_ROW, OptionalPart.TIP_SYSTEM)
        assertEquals(both, Decisions.activation(reportOf(*everyHook)).availableParts)

        for (missing in unlockMessageRowHooks) {
            val activation = Decisions.activation(reportOf(*everyHook.filter { it != missing }.toTypedArray()))

            assertEquals(setOf(OptionalPart.TIP_SYSTEM), activation.availableParts, "without $missing")
        }
        for (missing in tipSystemHooks) {
            val activation = Decisions.activation(reportOf(*everyHook.filter { it != missing }.toTypedArray()))

            assertEquals(setOf(OptionalPart.UNLOCK_MESSAGE_ROW), activation.availableParts, "without $missing")
        }
    }

    @Test
    fun `the tip system is available to an inactive module, which is when the notice needs it`() {
        val activation = Decisions.activation(reportOf(*tipSystemHooks))

        assertFalse(activation.active)
        assertEquals(setOf(OptionalPart.TIP_SYSTEM), activation.availableParts)
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
