package com.example.culator

import org.junit.Assert.*
import org.junit.Test

class HistoryTest {
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
