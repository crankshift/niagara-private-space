package io.github.crankshift.niagaraprivatespace.decision

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ActivationTest {
    private fun reportOf(vararg found: Hook) = ResolutionReport(found.toSet())

    private val everyHook = arrayOf(Hook.LONG_PRESS_OPTIONS, Hook.PRIVATE_SPACE_FLAG, Hook.PRIMARY_USER_FLAG)

    @Test
    fun `the module needs the long-press options builder and both profile flags`() {
        assertEquals(everyHook.toSet(), Hook.entries.toSet())
    }

    @Test
    fun `the module activates when every hook was found`() {
        val activation = Decisions.activation(reportOf(*everyHook))

        assertTrue(activation.active)
        assertTrue(activation.missing.isEmpty())
    }

    @Test
    fun `a missing long-press options builder keeps the whole module inactive`() {
        val activation = Decisions.activation(reportOf(Hook.PRIVATE_SPACE_FLAG, Hook.PRIMARY_USER_FLAG))

        assertFalse(activation.active)
        assertEquals(listOf(Hook.LONG_PRESS_OPTIONS), activation.missing)
    }

    @Test
    fun `a missing Private Space flag keeps the whole module inactive`() {
        val activation = Decisions.activation(reportOf(Hook.LONG_PRESS_OPTIONS, Hook.PRIMARY_USER_FLAG))

        assertFalse(activation.active)
        assertEquals(listOf(Hook.PRIVATE_SPACE_FLAG), activation.missing)
    }

    @Test
    fun `a missing primary-user flag keeps the whole module inactive`() {
        val activation = Decisions.activation(reportOf(Hook.LONG_PRESS_OPTIONS, Hook.PRIVATE_SPACE_FLAG))

        assertFalse(activation.active)
        assertEquals(listOf(Hook.PRIMARY_USER_FLAG), activation.missing)
    }

    @Test
    fun `an empty report keeps the module inactive and names every hook as missing`() {
        val activation = Decisions.activation(reportOf())

        assertFalse(activation.active)
        assertEquals(listOf(*everyHook), activation.missing)
    }

    @Test
    fun `the log line of an inactive module names only what is missing`() {
        val line = Decisions.activation(reportOf(Hook.PRIVATE_SPACE_FLAG, Hook.PRIMARY_USER_FLAG)).logLine()

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
    }
}
