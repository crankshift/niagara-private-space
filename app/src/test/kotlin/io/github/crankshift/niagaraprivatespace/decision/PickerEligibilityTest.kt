package io.github.crankshift.niagaraprivatespace.decision

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PickerEligibilityTest {
    @Test
    fun `a private app is eligible in a picker that accepts work apps`() {
        assertTrue(Decisions.isEligibleInPicker(ProfileKind.PRIVATE, PickerRule.ANY_PROFILE, privateSpaceLocked = false))
    }

    @Test
    fun `a work app is eligible in a picker that accepts any profile`() {
        assertTrue(Decisions.isEligibleInPicker(ProfileKind.WORK, PickerRule.ANY_PROFILE, privateSpaceLocked = false))
    }

    @Test
    fun `a personal app stays eligible in every picker`() {
        assertTrue(Decisions.isEligibleInPicker(ProfileKind.PERSONAL, PickerRule.ANY_PROFILE, privateSpaceLocked = false))
        assertTrue(Decisions.isEligibleInPicker(ProfileKind.PERSONAL, PickerRule.PERSONAL_APPS_ONLY, privateSpaceLocked = false))
    }

    @Test
    fun `Leisure Apps still refuses a work app`() {
        assertFalse(Decisions.isEligibleInPicker(ProfileKind.WORK, PickerRule.PERSONAL_APPS_ONLY, privateSpaceLocked = false))
    }

    @Test
    fun `Leisure Apps accepts a private app although it asks for a personal app`() {
        assertTrue(Decisions.isEligibleInPicker(ProfileKind.PRIVATE, PickerRule.PERSONAL_APPS_ONLY, privateSpaceLocked = false))
    }

    @Test
    fun `no picker offers a private app while Private Space is locked`() {
        for (rule in PickerRule.entries) {
            assertFalse(Decisions.isEligibleInPicker(ProfileKind.PRIVATE, rule, privateSpaceLocked = true), "private, $rule")
        }
    }

    @Test
    fun `locking Private Space changes nothing for personal and work apps`() {
        assertTrue(Decisions.isEligibleInPicker(ProfileKind.PERSONAL, PickerRule.PERSONAL_APPS_ONLY, privateSpaceLocked = true))
        assertTrue(Decisions.isEligibleInPicker(ProfileKind.WORK, PickerRule.ANY_PROFILE, privateSpaceLocked = true))
        assertFalse(Decisions.isEligibleInPicker(ProfileKind.WORK, PickerRule.PERSONAL_APPS_ONLY, privateSpaceLocked = true))
    }

    @Test
    fun `a picker that accepts work apps sees an unlocked private app as a work app`() {
        assertEquals(ProfileKind.WORK, Decisions.profileSeenByPicker(ProfileKind.PRIVATE, PickerRule.ANY_PROFILE, privateSpaceLocked = false))
    }

    @Test
    fun `Leisure Apps sees an unlocked private app as a personal app`() {
        assertEquals(
            ProfileKind.PERSONAL,
            Decisions.profileSeenByPicker(ProfileKind.PRIVATE, PickerRule.PERSONAL_APPS_ONLY, privateSpaceLocked = false),
        )
    }

    @Test
    fun `a locked private app stays private to every picker, so Niagara leaves it out`() {
        for (rule in PickerRule.entries) {
            assertEquals(ProfileKind.PRIVATE, Decisions.profileSeenByPicker(ProfileKind.PRIVATE, rule, privateSpaceLocked = true), "$rule")
        }
    }

    @Test
    fun `personal and work apps are seen as themselves by every picker`() {
        for (rule in PickerRule.entries) {
            assertEquals(ProfileKind.PERSONAL, Decisions.profileSeenByPicker(ProfileKind.PERSONAL, rule, privateSpaceLocked = false), "personal, $rule")
            assertEquals(ProfileKind.WORK, Decisions.profileSeenByPicker(ProfileKind.WORK, rule, privateSpaceLocked = false), "work, $rule")
        }
    }
}
