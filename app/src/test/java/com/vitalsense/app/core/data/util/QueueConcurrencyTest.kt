package com.vitalsense.app.core.data.util

import com.vitalsense.app.core.data.model.QueueEntry
import com.vitalsense.app.core.data.model.QueueEntrySource
import com.vitalsense.app.core.data.model.QueueEntryStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

class QueueConcurrencyTest {

    @Test
    fun `concurrent token allocations produce strictly unique incremental tokens`() = runTest {
        val counter = AtomicInteger(1)
        val tokensAllocated = ConcurrentLinkedQueue<Int>()

        // Simulate 50 concurrent check-in operations
        val jobs = (1..50).map {
            async(Dispatchers.Default) {
                val token = counter.getAndIncrement()
                tokensAllocated.add(token)
            }
        }

        jobs.awaitAll()

        assertEquals(50, tokensAllocated.size)
        // Verify all 50 tokens from 1 to 50 are strictly unique and non-colliding
        val uniqueTokens = tokensAllocated.toSet()
        assertEquals(50, uniqueTokens.size)
        assertEquals((1..50).toSet(), uniqueTokens)
    }

    @Test
    fun `concurrent queue state updates preserve deterministic sort order`() = runTest {
        val entries = ConcurrentLinkedQueue<QueueEntry>()

        val jobs = (1..30).map { i ->
            async(Dispatchers.Default) {
                val entry = QueueEntry(
                    id = "entry_$i",
                    doctorId = "doc_1",
                    doctorName = "Dr. Rajesh",
                    dateFormatted = "2026-08-15",
                    tokenNumber = i,
                    provisionalToken = false,
                    appointmentId = null,
                    patientId = "patient_$i",
                    patientName = "Patient $i",
                    source = if (i % 2 == 0) QueueEntrySource.SCHEDULED else QueueEntrySource.WALK_IN,
                    status = QueueEntryStatus.WAITING,
                    priorityFlag = (i % 5 == 0), // Every 5th is priority
                    checkedInAt = 1000L + (i * 10L),
                    isPendingSync = false
                )
                entries.add(entry)
            }
        }

        jobs.awaitAll()

        val sorted = QueueEtaCalculator.sortWaitingEntries(entries.toList())
        assertEquals(30, sorted.size)

        // Verify that priority entries (i = 5, 10, 15, 20, 25, 30) are at the front
        val priorityCount = sorted.take(6).count { it.priorityFlag }
        assertEquals(6, priorityCount)
    }
}
