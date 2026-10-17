package io.github.crankshift.niagaraprivatespace.decision

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NoticeTest {
    private val tipSystem = HookGroups.tipSystem

    private fun activationFinding(vararg found: Hook) = Decisions.activation(ResolutionReport(found.toSet()))

    private val supported = activationFinding(*Hook.entries.toTypedArray())
    private val unsupported = activationFinding(*tipSystem.toTypedArray())

    @Test
    fun `the user is told only when the module could not activate`() {
        assertTrue(unsupported.notify)
        assertFalse(supported.notify)
    }

    @Test
    fun `losing only the unlock message row or the tip system is not worth telling the user`() {
        val withoutMessageRow = activationFinding(*Hook.entries.filter { it != Hook.EMPTY_POP_UP_ROW }.toTypedArray())
        val withoutTipSystem = activationFinding(*Hook.entries.filter { it !in tipSystem }.toTypedArray())

        assertFalse(withoutMessageRow.notify)
        assertFalse(withoutTipSystem.notify)
    }

    @Test
    fun `a module whose hooks could not be installed is inactive, notifies, and keeps the tip system`() {
        val failed = supported.afterFailedInstall()

        assertFalse(failed.active)
        assertTrue(failed.notify)
        assertEquals(NoticeChannel.TIP, Decisions.noticeChannel(failed))
        assertTrue(Decisions.noticeDue(failed, niagaraVersionCode = 1638, lastNotifiedVersionCode = null))
    }

    @Test
    fun `the notice is due the first time the module cannot activate on a Niagara version`() {
        assertTrue(Decisions.noticeDue(unsupported, niagaraVersionCode = 1638, lastNotifiedVersionCode = null))
    }

    @Test
    fun `the notice is not repeated for the Niagara version it was already shown for`() {
        assertFalse(Decisions.noticeDue(unsupported, niagaraVersionCode = 1638, lastNotifiedVersionCode = 1638))
    }

    @Test
    fun `the notice is due again for a new Niagara version`() {
        assertTrue(Decisions.noticeDue(unsupported, niagaraVersionCode = 1650, lastNotifiedVersionCode = 1638))
    }

    @Test
    fun `the notice is never due when the module activated`() {
        assertFalse(Decisions.noticeDue(supported, niagaraVersionCode = 1638, lastNotifiedVersionCode = null))
        assertFalse(Decisions.noticeDue(supported, niagaraVersionCode = 1650, lastNotifiedVersionCode = 1638))
    }

    @Test
    fun `the notice is a tip when Niagara's tip system was found`() {
        assertEquals(NoticeChannel.TIP, Decisions.noticeChannel(unsupported))
    }

    @Test
    fun `the notice is a Toast when any part of Niagara's tip system is missing`() {
        for (missing in tipSystem) {
            val activation = activationFinding(*tipSystem.filter { it != missing }.toTypedArray())

            assertEquals(NoticeChannel.TOAST, Decisions.noticeChannel(activation), "without $missing")
        }
        assertEquals(NoticeChannel.TOAST, Decisions.noticeChannel(activationFinding()))
    }

    @Test
    fun `the notice names the Niagara version`() {
        assertEquals(
            "Niagara Private Space doesn't support Niagara 1.17.0 yet",
            Decisions.noticeText(niagaraVersionName = "1.17.0"),
        )
    }
}
