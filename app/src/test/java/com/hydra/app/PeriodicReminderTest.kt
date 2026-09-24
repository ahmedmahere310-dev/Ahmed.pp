package com.hydra.app

import com.hydra.app.data.local.HydraUserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class PeriodicReminderTest {

    @Test
    fun testDefaultPeriodicPreferences() {
        val prefs = HydraUserPreferences()
        assertTrue("Periodic water should be enabled by default", prefs.periodicWaterEnabled)
        assertEquals("Default water interval should be 90 minutes", 90, prefs.waterIntervalMinutes)
        assertTrue("Periodic food should be enabled by default", prefs.periodicFoodEnabled)
        assertEquals("Default food interval should be 240 minutes", 240, prefs.foodIntervalMinutes)
        assertEquals("Quiet hours start at 22:00", 22, prefs.quietHoursStart)
        assertEquals("Quiet hours end at 8:00", 8, prefs.quietHoursEnd)
    }

    @Test
    fun testQuietHoursCalculation() {
        val quietStart = 22
        val quietEnd = 8

        // Test hour 23 (11 PM) - should be in quiet hours
        val isQuietAt11Pm = isHourInQuiet(23, quietStart, quietEnd)
        assertTrue("11 PM should be in quiet hours", isQuietAt11Pm)

        // Test hour 3 (3 AM) - should be in quiet hours
        val isQuietAt3Am = isHourInQuiet(3, quietStart, quietEnd)
        assertTrue("3 AM should be in quiet hours", isQuietAt3Am)

        // Test hour 12 (12 PM) - should not be in quiet hours
        val isQuietAt12Pm = isHourInQuiet(12, quietStart, quietEnd)
        assertFalse("12 PM should not be in quiet hours", isQuietAt12Pm)

        // Test hour 9 (9 AM) - should not be in quiet hours
        val isQuietAt9Am = isHourInQuiet(9, quietStart, quietEnd)
        assertFalse("9 AM should not be in quiet hours", isQuietAt9Am)
    }

    private fun isHourInQuiet(hour: Int, quietStart: Int, quietEnd: Int): Boolean {
        return if (quietStart > quietEnd) {
            hour >= quietStart || hour < quietEnd
        } else {
            hour in quietStart until quietEnd
        }
    }
}
