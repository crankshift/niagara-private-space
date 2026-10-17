package io.github.crankshift.niagaraprivatespace.decision

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ListingTest {
    @Test
    fun `a private app may be suggested while Private Space is unlocked`() {
        assertTrue(Decisions.mayBeListed(ProfileKind.PRIVATE, privateSpaceLocked = false))
    }

    @Test
    fun `a private app is not suggested while Private Space is locked`() {
        assertFalse(Decisions.mayBeListed(ProfileKind.PRIVATE, privateSpaceLocked = true))
    }

    @Test
    fun `personal and work apps are suggested whether or not Private Space is locked`() {
        for (kind in listOf(ProfileKind.PERSONAL, ProfileKind.WORK)) {
            assertTrue(Decisions.mayBeListed(kind, privateSpaceLocked = true), "$kind while locked")
            assertTrue(Decisions.mayBeListed(kind, privateSpaceLocked = false), "$kind while unlocked")
        }
    }

    @Test
    fun `a list that offers apps sees an unlocked private app as a work app`() {
        val seen = Decisions.profileSeenWhenListing(ProfileKind.PRIVATE, privateSpaceLocked = false)

        assertEquals(ProfileKind.WORK, seen)
    }

    @Test
    fun `a list that offers apps still sees a locked private app as private, so Niagara leaves it out`() {
        val seen = Decisions.profileSeenWhenListing(ProfileKind.PRIVATE, privateSpaceLocked = true)

        assertEquals(ProfileKind.PRIVATE, seen)
    }

    @Test
    fun `a list that offers apps sees personal and work apps as themselves`() {
        for (locked in listOf(true, false)) {
            val personal = Decisions.profileSeenWhenListing(ProfileKind.PERSONAL, locked)
            val work = Decisions.profileSeenWhenListing(ProfileKind.WORK, locked)

            assertEquals(ProfileKind.PERSONAL, personal, "personal, locked=$locked")
            assertEquals(ProfileKind.WORK, work, "work, locked=$locked")
        }
    }
}
