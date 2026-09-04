package com.example.myTools.period

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class PeriodDataManagerTest {

    private val manager = PeriodDataManager(null)

    private fun createMillis(year: Int, month: Int, day: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun testAverageCycleLength() {
        val records = listOf(
            PeriodRecord(createMillis(2025, 1, 1), createMillis(2025, 1, 5)),
            PeriodRecord(createMillis(2025, 1, 29), createMillis(2025, 2, 2)), // 28 days later
            PeriodRecord(createMillis(2025, 2, 26), createMillis(2025, 3, 2))  // 28 days later
        )
        val avgCycle = manager.getAverageCycleLength(records)
        assertEquals(28, avgCycle)
    }

    @Test
    fun testAveragePeriodLength() {
        val records = listOf(
            PeriodRecord(createMillis(2025, 1, 1), createMillis(2025, 1, 5)), // 5 days
            PeriodRecord(createMillis(2025, 2, 1), createMillis(2025, 2, 7))  // 7 days
        )
        val avgPeriod = manager.getAveragePeriodLength(records)
        assertEquals(6, avgPeriod)
    }

    @Test
    fun testCurrentPhaseCalculation() {
        // Record 1: Jan 1 to Jan 5
        // Record 2: Feb 1 to Feb 5 (Cycle length: 31 days)
        val records = listOf(
            PeriodRecord(createMillis(2025, 1, 1), createMillis(2025, 1, 5)),
            PeriodRecord(createMillis(2025, 2, 1), createMillis(2025, 2, 5))
        )

        // Jan 3: inside actual menstruation -> Phase 2 (月經期)
        assertEquals(2, manager.getCurrentPhase(createMillis(2025, 1, 3), records))

        // Jan 8: follicular safe period -> Phase 0 (安全期)
        assertEquals(0, manager.getCurrentPhase(createMillis(2025, 1, 8), records))

        // Cycle 1: next period start Feb 1.
        // Ovulation day = Feb 1 - 14 days = Jan 18.
        // Fertile window = Jan 13..Jan 21.
        // Jan 15: inside fertile window -> Phase 1 (排卵期)
        assertEquals(1, manager.getCurrentPhase(createMillis(2025, 1, 15), records))

        // Jan 25: luteal safe period -> Phase 0 (安全期)
        assertEquals(0, manager.getCurrentPhase(createMillis(2025, 1, 25), records))

        // Feb 2: inside actual menstruation -> Phase 2 (月經期)
        assertEquals(2, manager.getCurrentPhase(createMillis(2025, 2, 2), records))

        // Next predicted period start = Feb 1 + 31 days = Mar 4.
        // Mar 4..Mar 8: predicted menstruation -> Phase 3 (預測月經期)
        assertEquals(3, manager.getCurrentPhase(createMillis(2025, 3, 4), records))

        // Ovulation day for predicted cycle 2 = Mar 4 + 31 - 14 = Mar 21.
        // Fertile window = Mar 16..Mar 24.
        // Mar 18: inside fertile window -> Phase 1 (排卵期)
        assertEquals(1, manager.getCurrentPhase(createMillis(2025, 3, 18), records))
    }
}
