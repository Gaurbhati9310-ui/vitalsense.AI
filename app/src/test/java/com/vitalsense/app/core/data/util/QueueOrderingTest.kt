package com.vitalsense.app.core.data.util

import com.vitalsense.app.core.data.model.QueueEntry
import com.vitalsense.app.core.data.model.QueueEntrySource
import com.vitalsense.app.core.data.model.QueueEntryStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class QueueOrderingTest {

    private fun createEntry(
        id: String,
        source: QueueEntrySource,
        status: QueueEntryStatus = QueueEntryStatus.WAITING,
        checkedInAt: Long,
        priority: Boolean = false
    ): QueueEntry {
        return QueueEntry(
            id = id,
            doctorId = "doc_1",
            doctorName = "Dr. Rajesh",
            dateFormatted = "2026-08-15",
            tokenNumber = 1,
            provisionalToken = false,
            appointmentId = null,
            patientId = "pat_$id",
            patientName = "Patient $id",
            source = source,
            status = status,
            priorityFlag = priority,
            checkedInAt = checkedInAt,
            isPendingSync = false
        )
    }

    @Test
    fun `hybrid queue sorts scheduled and walk-in by checkedInAt ASC`() {
        val scheduled1 = createEntry("s1", QueueEntrySource.SCHEDULED, checkedInAt = 100L)
        val walkin1 = createEntry("w1", QueueEntrySource.WALK_IN, checkedInAt = 150L)
        val scheduled2 = createEntry("s2", QueueEntrySource.SCHEDULED, checkedInAt = 200L)
        val walkin2 = createEntry("w2", QueueEntrySource.WALK_IN, checkedInAt = 80L)

        val unsorted = listOf(scheduled1, walkin1, scheduled2, walkin2)
        val sorted = QueueEtaCalculator.sortWaitingEntries(unsorted)

        // Expected order: w2 (80), s1 (100), w1 (150), s2 (200)
        assertEquals(listOf("w2", "s1", "w1", "s2"), sorted.map { it.id })
    }

    @Test
    fun `priority entries sort ahead of non-priority entries`() {
        val normal1 = createEntry("n1", QueueEntrySource.SCHEDULED, checkedInAt = 100L, priority = false)
        val normal2 = createEntry("n2", QueueEntrySource.WALK_IN, checkedInAt = 200L, priority = false)
        val priority1 = createEntry("p1", QueueEntrySource.WALK_IN, checkedInAt = 300L, priority = true)
        val priority2 = createEntry("p2", QueueEntrySource.SCHEDULED, checkedInAt = 150L, priority = true)

        val unsorted = listOf(normal1, normal2, priority1, priority2)
        val sorted = QueueEtaCalculator.sortWaitingEntries(unsorted)

        // Expected order: p2 (150, priority), p1 (300, priority), n1 (100, regular), n2 (200, regular)
        assertEquals(listOf("p2", "p1", "n1", "n2"), sorted.map { it.id })
    }

    @Test
    fun `completed, called and cancelled entries are excluded from waiting sort`() {
        val waiting = createEntry("w1", QueueEntrySource.SCHEDULED, checkedInAt = 100L)
        val inConsult = createEntry("c1", QueueEntrySource.SCHEDULED, status = QueueEntryStatus.IN_CONSULTATION, checkedInAt = 50L)
        val completed = createEntry("d1", QueueEntrySource.SCHEDULED, status = QueueEntryStatus.COMPLETED, checkedInAt = 40L)
        val cancelled = createEntry("x1", QueueEntrySource.SCHEDULED, status = QueueEntryStatus.CANCELLED, checkedInAt = 80L)

        val sorted = QueueEtaCalculator.sortWaitingEntries(listOf(waiting, inConsult, completed, cancelled))
        assertEquals(1, sorted.size)
        assertEquals("w1", sorted.first().id)
    }
}
