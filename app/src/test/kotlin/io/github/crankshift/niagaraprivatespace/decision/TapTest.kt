package io.github.crankshift.niagaraprivatespace.decision

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TapTest {
    private val pendingApp = PendingApp<String>()

    private fun tap(app: String, kind: ProfileKind, privateSpaceLocked: Boolean, nowMillis: Long) =
        pendingApp.tap(kind, privateSpaceLocked, nowMillis) { app }

    @Test
    fun `an unlocked private app opens directly`() {
        val action = tap("Signal", ProfileKind.PRIVATE, privateSpaceLocked = false, nowMillis = 0)

        assertEquals(TapAction.OPEN_NOW, action)
    }

    @Test
    fun `personal and work apps open directly, whether or not Private Space is locked`() {
        for (kind in listOf(ProfileKind.PERSONAL, ProfileKind.WORK)) {
            for (locked in listOf(true, false)) {
                val action = tap("Maps", kind, locked, nowMillis = 0)

                assertEquals(TapAction.OPEN_NOW, action, "$kind, locked=$locked")
            }
        }
    }

    @Test
    fun `the app is only asked for when a tap has to wait on it`() {
        var asked = 0
        val app = { asked++.let { "Signal" } }

        pendingApp.tap(ProfileKind.PERSONAL, privateSpaceLocked = true, nowMillis = 0, app)
        pendingApp.tap(ProfileKind.WORK, privateSpaceLocked = false, nowMillis = 0, app)
        pendingApp.tap(ProfileKind.PRIVATE, privateSpaceLocked = false, nowMillis = 0, app)
        assertEquals(0, asked)

        pendingApp.tap(ProfileKind.PRIVATE, privateSpaceLocked = true, nowMillis = 0, app)
        assertEquals(1, asked)
    }

    @Test
    fun `a locked private app asks for the unlock first`() {
        val action = tap("Signal", ProfileKind.PRIVATE, privateSpaceLocked = true, nowMillis = 0)

        assertEquals(TapAction.UNLOCK_FIRST, action)
    }

    @Test
    fun `the app tapped while locked opens once Private Space becomes available`() {
        tap("Signal", ProfileKind.PRIVATE, privateSpaceLocked = true, nowMillis = 0)

        val opened = pendingApp.profileBecameAvailable(ProfileKind.PRIVATE, nowMillis = 5_000)

        assertEquals("Signal", opened)
    }

    @Test
    fun `the app opens only once, however many times Private Space becomes available`() {
        tap("Signal", ProfileKind.PRIVATE, privateSpaceLocked = true, nowMillis = 0)
        pendingApp.profileBecameAvailable(ProfileKind.PRIVATE, nowMillis = 5_000)

        val again = pendingApp.profileBecameAvailable(ProfileKind.PRIVATE, nowMillis = 6_000)

        assertNull(again)
    }

    @Test
    fun `nothing opens when Private Space becomes available and no tap is waiting`() {
        assertNull(pendingApp.profileBecameAvailable(ProfileKind.PRIVATE, nowMillis = 0))
    }

    @Test
    fun `a cancelled unlock forgets the pending app`() {
        tap("Signal", ProfileKind.PRIVATE, privateSpaceLocked = true, nowMillis = 0)

        pendingApp.unlockCancelled()

        assertNull(pendingApp.profileBecameAvailable(ProfileKind.PRIVATE, nowMillis = 5_000))
    }

    @Test
    fun `a second tap replaces the pending app`() {
        tap("Signal", ProfileKind.PRIVATE, privateSpaceLocked = true, nowMillis = 0)
        tap("Slack", ProfileKind.PRIVATE, privateSpaceLocked = true, nowMillis = 2_000)

        assertEquals("Slack", pendingApp.profileBecameAvailable(ProfileKind.PRIVATE, nowMillis = 5_000))
        assertNull(pendingApp.profileBecameAvailable(ProfileKind.PRIVATE, nowMillis = 6_000))
    }

    @Test
    fun `an unlock that finishes within half a minute still opens the app`() {
        tap("Signal", ProfileKind.PRIVATE, privateSpaceLocked = true, nowMillis = 0)

        assertEquals("Signal", pendingApp.profileBecameAvailable(ProfileKind.PRIVATE, nowMillis = 30_000))
    }

    @Test
    fun `an unlock that finishes after a minute and a second opens nothing, one just inside a minute still does`() {
        tap("Signal", ProfileKind.PRIVATE, privateSpaceLocked = true, nowMillis = 0)
        assertEquals("Signal", pendingApp.profileBecameAvailable(ProfileKind.PRIVATE, nowMillis = 59_999))

        tap("Signal", ProfileKind.PRIVATE, privateSpaceLocked = true, nowMillis = 0)
        assertNull(pendingApp.profileBecameAvailable(ProfileKind.PRIVATE, nowMillis = 61_000))
    }

    @Test
    fun `an app tapped five minutes before Private Space became available is forgotten`() {
        // The user backed out of the unlock screen, which Android reports to nobody, and unlocked later some other way.
        tap("Signal", ProfileKind.PRIVATE, privateSpaceLocked = true, nowMillis = 0)

        assertNull(pendingApp.profileBecameAvailable(ProfileKind.PRIVATE, nowMillis = 5 * 60_000))
    }

    @Test
    fun `the work profile becoming available neither opens nor forgets the pending app`() {
        tap("Signal", ProfileKind.PRIVATE, privateSpaceLocked = true, nowMillis = 0)

        assertNull(pendingApp.profileBecameAvailable(ProfileKind.WORK, nowMillis = 2_000))
        assertEquals("Signal", pendingApp.profileBecameAvailable(ProfileKind.PRIVATE, nowMillis = 5_000))
    }

    @Test
    fun `locking Private Space again forgets the pending app`() {
        tap("Signal", ProfileKind.PRIVATE, privateSpaceLocked = true, nowMillis = 0)

        pendingApp.profileBecameUnavailable(ProfileKind.PRIVATE)

        assertNull(pendingApp.profileBecameAvailable(ProfileKind.PRIVATE, nowMillis = 5_000))
    }

    @Test
    fun `the work profile pausing does not forget the pending app`() {
        tap("Signal", ProfileKind.PRIVATE, privateSpaceLocked = true, nowMillis = 0)

        pendingApp.profileBecameUnavailable(ProfileKind.WORK)

        assertEquals("Signal", pendingApp.profileBecameAvailable(ProfileKind.PRIVATE, nowMillis = 5_000))
    }

    @Test
    fun `a tap that opens another app forgets the pending app`() {
        tap("Signal", ProfileKind.PRIVATE, privateSpaceLocked = true, nowMillis = 0)
        tap("Maps", ProfileKind.PERSONAL, privateSpaceLocked = true, nowMillis = 2_000)

        assertNull(pendingApp.profileBecameAvailable(ProfileKind.PRIVATE, nowMillis = 5_000))
    }
}
