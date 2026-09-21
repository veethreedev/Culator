package com.example.culator

import org.junit.Assert.*
import org.junit.Test

class HistoryTest {
    @Test fun retentionUsesSelectedPeriod() {
        val day = 24L * 60 * 60 * 1000
        val now = 400 * day
        val today = HistoryEntry("1", "1", now)
        val monthOld = today.copy(timestamp = now - 30 * day)
        val yearOld = today.copy(timestamp = now - 365 * day)
        val entries = listOf(yearOld, monthOld, today)
        assertEquals(listOf(today), recentHistory(entries, now, day))
        assertEquals(listOf(today, monthOld), recentHistory(entries, now, 30 * day))
        assertEquals(listOf(today, monthOld, yearOld), recentHistory(entries, now, 365 * day))
        assertEquals(listOf(today), recentHistory(entries, now + 1, 30 * day))
    }

    @Test fun confirmationPreservesEquationAndTimestamp() {
        assertEquals(HistoryEntry("2+3×4", "14", 1234L), confirmedEntry("2+3×4", 1234L))
        assertNull(confirmedEntry("1÷0", 1234L))
        assertNull(confirmedEntry("2+", 1234L))
        assertNull(confirmedEntry("", 1234L))
    }

    @Test fun retentionKeepsExactlyOneWeekAndSortsNewestFirst() {
        val now = HISTORY_RETENTION_MS * 2
        val boundary = HistoryEntry("1", "1", now - HISTORY_RETENTION_MS)
        val expired = boundary.copy(timestamp = boundary.timestamp - 1)
        val newest = boundary.copy(timestamp = now)
        assertEquals(listOf(newest, boundary), recentHistory(listOf(expired, boundary, newest), now))
        assertEquals(listOf(newest), recentHistory(listOf(boundary, newest), now + 1))
    }
}
