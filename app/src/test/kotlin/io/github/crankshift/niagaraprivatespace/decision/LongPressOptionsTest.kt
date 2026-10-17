package io.github.crankshift.niagaraprivatespace.decision

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LongPressOptionsTest {
    @Test
    fun `a private app gets the long-press options of a work app`() {
        assertEquals(ProfileKind.WORK, Decisions.profileForLongPressOptions(ProfileKind.PRIVATE))
    }

    @Test
    fun `a personal app keeps the long-press options of a personal app`() {
        assertEquals(ProfileKind.PERSONAL, Decisions.profileForLongPressOptions(ProfileKind.PERSONAL))
    }

    @Test
    fun `a work app keeps the long-press options of a work app`() {
        assertEquals(ProfileKind.WORK, Decisions.profileForLongPressOptions(ProfileKind.WORK))
    }
}
