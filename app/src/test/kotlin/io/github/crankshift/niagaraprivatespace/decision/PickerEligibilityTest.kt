package io.github.crankshift.niagaraprivatespace.decision

import io.github.crankshift.niagaraprivatespace.decision.PickerRule.ANY_PROFILE
import io.github.crankshift.niagaraprivatespace.decision.PickerRule.PERSONAL_APPS_ONLY
import io.github.crankshift.niagaraprivatespace.decision.ProfileKind.PERSONAL
import io.github.crankshift.niagaraprivatespace.decision.ProfileKind.PRIVATE
import io.github.crankshift.niagaraprivatespace.decision.ProfileKind.WORK
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PickerEligibilityTest {
    private fun eligible(kind: ProfileKind, rule: PickerRule, locked: Boolean = false) =
        Decisions.isEligibleInPicker(kind, rule, privateSpaceLocked = locked)

    private fun seenBy(kind: ProfileKind, rule: PickerRule, locked: Boolean = false) =
        Decisions.profileSeenByPicker(kind, rule, privateSpaceLocked = locked)

    @Test
    fun `a private app is eligible in a picker that accepts work apps`() {
        assertTrue(eligible(PRIVATE, ANY_PROFILE))
    }

    @Test
    fun `a work app is eligible in a picker that accepts any profile`() {
        assertTrue(eligible(WORK, ANY_PROFILE))
    }

    @Test
    fun `a personal app stays eligible in every picker`() {
        assertTrue(eligible(PERSONAL, ANY_PROFILE))
        assertTrue(eligible(PERSONAL, PERSONAL_APPS_ONLY))
    }

    @Test
    fun `Leisure Apps still refuses a work app`() {
        assertFalse(eligible(WORK, PERSONAL_APPS_ONLY))
    }

    @Test
    fun `Leisure Apps accepts a private app although it asks for a personal app`() {
        assertTrue(eligible(PRIVATE, PERSONAL_APPS_ONLY))
    }

    @Test
    fun `no picker offers a private app while Private Space is locked`() {
        for (rule in PickerRule.entries) {
            assertFalse(eligible(PRIVATE, rule, locked = true), "private, $rule")
        }
    }

    @Test
    fun `locking Private Space changes nothing for personal and work apps`() {
        assertTrue(eligible(PERSONAL, PERSONAL_APPS_ONLY, locked = true))
        assertTrue(eligible(WORK, ANY_PROFILE, locked = true))
        assertFalse(eligible(WORK, PERSONAL_APPS_ONLY, locked = true))
    }

    @Test
    fun `a picker that accepts work apps sees an unlocked private app as a work app`() {
        assertEquals(WORK, seenBy(PRIVATE, ANY_PROFILE))
    }

    @Test
    fun `Leisure Apps sees an unlocked private app as a personal app`() {
        assertEquals(PERSONAL, seenBy(PRIVATE, PERSONAL_APPS_ONLY))
    }

    @Test
    fun `a locked private app stays private to every picker, so Niagara leaves it out`() {
        for (rule in PickerRule.entries) {
            assertEquals(PRIVATE, seenBy(PRIVATE, rule, locked = true), "$rule")
        }
    }

    @Test
    fun `personal and work apps are seen as themselves by every picker`() {
        for (rule in PickerRule.entries) {
            assertEquals(PERSONAL, seenBy(PERSONAL, rule), "personal, $rule")
            assertEquals(WORK, seenBy(WORK, rule), "work, $rule")
        }
    }
}
