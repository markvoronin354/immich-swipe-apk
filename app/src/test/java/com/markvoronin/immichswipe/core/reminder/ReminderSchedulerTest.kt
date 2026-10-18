package com.markvoronin.immichswipe.core.reminder

import com.markvoronin.immichswipe.core.ReminderInterval
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ReminderSchedulerTest {

    @Test
    fun `random delay stays within predefined interval ranges`() {
        val random = Random(42)
        listOf(ReminderInterval.OFTEN, ReminderInterval.RARELY).forEach { interval ->
            repeat(1000) {
                val hours = ReminderScheduler.randomDelayHours(interval, random = random)
                assertTrue(hours >= interval.minDays * 24)
                assertTrue(hours <= interval.maxDays * 24)
            }
        }
    }

    @Test
    fun `random delay stays within custom range`() {
        val random = Random(42)
        repeat(1000) {
            val hours = ReminderScheduler.randomDelayHours(
                ReminderInterval.CUSTOM,
                customMinDays = 3,
                customMaxDays = 10,
                random = random
            )
            assertTrue(hours >= 3 * 24)
            assertTrue(hours <= 10 * 24)
        }
    }

    @Test
    fun `random delay is not constant`() {
        val random = Random(1)
        val delays = List(50) { ReminderScheduler.randomDelayHours(ReminderInterval.OFTEN, random = random) }.toSet()
        assertTrue(delays.size > 1)
    }

    @Test
    fun `interval ranges are 1-2 weeks and 3-5 weeks`() {
        assertEquals(7L to 14L, ReminderInterval.OFTEN.minDays to ReminderInterval.OFTEN.maxDays)
        assertEquals(21L to 35L, ReminderInterval.RARELY.minDays to ReminderInterval.RARELY.maxDays)
    }
}