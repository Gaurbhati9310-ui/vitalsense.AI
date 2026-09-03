package com.vitalsense.app.core.data.util

import com.vitalsense.app.core.data.model.QueueEntry
import com.vitalsense.app.core.data.model.QueueEntrySource
import com.vitalsense.app.core.data.model.QueueEntryStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class QueueEtaCalculatorTest {

    private fun createEntry(
        id: String,
        status: QueueEntryStatus,
        checkedInAt: Long = 1000L,
        startedAt: Long? = null,
        completedAt: Long? = null,
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
            source = QueueEntrySource.SCHEDULED,
            status = status,
            priorityFlag = priority,
            checkedInAt = checkedInAt,
            calledAt = startedAt,
            consultationStartedAt = startedAt,
            completedAt = completedAt,
            isPendingSync = false
        )
    }

    @Test
    fun `averageConsultationSeconds returns default 600s when no completed history exists`() {
        val avg = QueueEtaCalculator.averageConsultationSeconds(
            completedToday = emptyList(),
            completedRecentDays = emptyList()
        )
        assertEquals(600L, avg)
    }

    @Test
    fun `averageConsultationSeconds uses today mean when at least 3 completed entries exist`() {
        val now = 100000L
        val sample1 = createEntry("1", QueueEntryStatus.COMPLETED, startedAt = now, completedAt = now + 300_000L) // 300s (5 min)
        val sample2 = createEntry("2", QueueEntryStatus.COMPLETED, startedAt = now, completedAt = now + 600_000L) // 600s (10 min)
        val sample3 = createEntry("3", QueueEntryStatus.COMPLETED, startedAt = now, completedAt = now + 900_000L) // 900s (15 min)

        val avg = QueueEtaCalculator.averageConsultationSeconds(
            completedToday = listOf(sample1, sample2, sample3)
        )
        // Mean of 300, 600, 900 = 600 seconds
        assertEquals(600L, avg)
    }

    @Test
    fun `averageConsultationSeconds falls back to past 7 days when today has fewer than 3 samples`() {
        val now = 100000L
        val todaySample = createEntry("1", QueueEntryStatus.COMPLETED, startedAt = now, completedAt = now + 1200_000L) // 1200s (1 sample today)
        val recentSample1 = createEntry("2", QueueEntryStatus.COMPLETED, startedAt = now, completedAt = now + 400_000L) // 400s
        val recentSample2 = createEntry("3", QueueEntryStatus.COMPLETED, startedAt = now, completedAt = now + 600_000L) // 600s

        val avg = QueueEtaCalculator.averageConsultationSeconds(
            completedToday = listOf(todaySample),
            completedRecentDays = listOf(recentSample1, recentSample2)
        )
        // Combined mean: (1200 + 400 + 600) / 3 = 733 seconds
        assertEquals(733L, avg)
    }

    @Test
    fun `calculatePosition correctly counts waiting patients ahead`() {
        val entry1 = createEntry("1", QueueEntryStatus.WAITING, checkedInAt = 100L)
        val entry2 = createEntry("2", QueueEntryStatus.WAITING, checkedInAt = 200L)
        val entry3 = createEntry("3", QueueEntryStatus.WAITING, checkedInAt = 300L)
        val list = listOf(entry2, entry3, entry1)

        assertEquals(0, QueueEtaCalculator.calculatePosition("1", list))
        assertEquals(1, QueueEtaCalculator.calculatePosition("2", list))
        assertEquals(2, QueueEtaCalculator.calculatePosition("3", list))
    }

    @Test
    fun `estimatedWaitSeconds computes product of position and average duration`() {
        val waitSeconds = QueueEtaCalculator.estimatedWaitSeconds(position = 4, avgConsultationSeconds = 450L)
        assertEquals(1800L, waitSeconds) // 30 minutes
    }

    @Test
    fun `formatEstimatedWait formats ranges accurately`() {
        assertEquals("Next in line", QueueEtaCalculator.formatEstimatedWait(0L))
        assertEquals("< 5 min", QueueEtaCalculator.formatEstimatedWait(180L))
        assertEquals("~15 min", QueueEtaCalculator.formatEstimatedWait(900L))
        assertEquals("~1 hr", QueueEtaCalculator.formatEstimatedWait(3600L))
        assertEquals("~1 hr 15 min", QueueEtaCalculator.formatEstimatedWait(4500L))
    }
}
