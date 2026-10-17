package io.github.crankshift.niagaraprivatespace.decision

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FolderContentsTest {
    private data class App(val name: String, val kind: ProfileKind)

    private val signal = App("Signal", ProfileKind.PRIVATE)
    private val slack = App("Slack", ProfileKind.PRIVATE)
    private val maps = App("Maps", ProfileKind.PERSONAL)
    private val teams = App("Teams", ProfileKind.WORK)

    private fun contentsOf(
        apps: List<App>,
        locked: Boolean,
        messageRowAvailable: Boolean = true,
    ) = Decisions.folderOrPopUpContents(apps, { it.kind }, locked, messageRowAvailable)

    @Test
    fun `a folder of only private apps shows the unlock message and no apps while locked`() {
        val contents = contentsOf(listOf(signal, slack), locked = true)

        assertEquals(emptyList<App>(), contents.rows)
        assertTrue(contents.showUnlockMessage)
    }

    @Test
    fun `a mixed folder keeps its personal and work apps in order, with the unlock message, while locked`() {
        val contents = contentsOf(listOf(maps, signal, teams, slack), locked = true)

        assertEquals(listOf(maps, teams), contents.rows)
        assertTrue(contents.showUnlockMessage)
    }

    @Test
    fun `an unlocked folder keeps every app and shows no unlock message`() {
        val contents = contentsOf(listOf(maps, signal, teams, slack), locked = false)

        assertEquals(listOf(maps, signal, teams, slack), contents.rows)
        assertFalse(contents.showUnlockMessage)
    }

    @Test
    fun `a folder with no private apps is untouched, whether or not Private Space is locked`() {
        for (locked in listOf(true, false)) {
            val contents = contentsOf(listOf(maps, teams), locked)

            assertEquals(listOf(maps, teams), contents.rows, "locked=$locked")
            assertFalse(contents.showUnlockMessage, "locked=$locked")
        }
    }

    @Test
    fun `an empty folder stays empty, with no unlock message`() {
        val contents = contentsOf(emptyList(), locked = true)

        assertEquals(emptyList<App>(), contents.rows)
        assertFalse(contents.showUnlockMessage)
    }

    @Test
    fun `without the unlock message row, private apps still hide while locked and no message is asked for`() {
        val contents = contentsOf(listOf(maps, signal), locked = true, messageRowAvailable = false)

        assertEquals(listOf(maps), contents.rows)
        assertFalse(contents.showUnlockMessage)
    }

    @Test
    fun `without the unlock message row, a folder of only private apps shows nothing at all while locked`() {
        val contents = contentsOf(listOf(signal, slack), locked = true, messageRowAvailable = false)

        assertEquals(emptyList<App>(), contents.rows)
        assertFalse(contents.showUnlockMessage)
    }
}
