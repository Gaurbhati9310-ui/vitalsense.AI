package com.vitalsense.app.core.data.util

import com.vitalsense.app.core.data.model.QueueEntry
import com.vitalsense.app.core.data.model.QueueEntryStatus

object QueueEtaCalculator {

    const val DEFAULT_AVG_CONSULTATION_SECONDS = 600L // 10 minutes default fallback
    private const val MIN_TODAY_SAMPLES = 3

    /**
     * Sorts waiting queue entries:
     * 1. Priority flag entries first (ordered by checkedInAt ASC)
     * 2. Non-priority entries second (ordered by checkedInAt ASC)
     */
    fun sortWaitingEntries(entries: List<QueueEntry>): List<QueueEntry> {
        return entries
            .filter { it.status == QueueEntryStatus.WAITING }
            .sortedWith(
                compareByDescending<QueueEntry> { it.priorityFlag }
                    .thenBy { it.checkedInAt }
            )
    }

    /**
     * Calculates the 1-based position for a specific WAITING entry ahead in the queue.
     * Position = number of other WAITING entries that sort ahead of it.
     * Returns 0 if entry is not in WAITING state or not found.
     */
    fun calculatePosition(targetEntryId: String, allEntriesForDoctorAndDate: List<QueueEntry>): Int {
        val sortedWaiting = sortWaitingEntries(allEntriesForDoctorAndDate)
        val index = sortedWaiting.indexOfFirst { it.id == targetEntryId }
        return if (index >= 0) index else 0
    }

    /**
     * Computes the average consultation duration in seconds using the statistical fallback chain:
     * 1. Mean duration of today's completed entries if today has >= 3 samples.
     * 2. Otherwise, mean duration of past 7-day completed entries if history exists.
     * 3. Otherwise, hardcoded default of 600 seconds.
     */
    fun averageConsultationSeconds(
        completedToday: List<QueueEntry>,
        completedRecentDays: List<QueueEntry> = emptyList(),
        defaultSeconds: Long = DEFAULT_AVG_CONSULTATION_SECONDS
    ): Long {
        val todayDurations = completedToday.mapNotNull { entry ->
            val start = entry.consultationStartedAt
            val end = entry.completedAt
            if (start != null && end != null && end > start) {
                (end - start) / 1000L
            } else null
        }

        if (todayDurations.size >= MIN_TODAY_SAMPLES) {
            val avg = todayDurations.average()
            if (!avg.isNaN() && avg > 0) return avg.toLong()
        }

        val recentDurations = (completedToday + completedRecentDays).mapNotNull { entry ->
            val start = entry.consultationStartedAt
            val end = entry.completedAt
            if (start != null && end != null && end > start) {
                (end - start) / 1000L
            } else null
        }

        if (recentDurations.isNotEmpty()) {
            val avg = recentDurations.average()
            if (!avg.isNaN() && avg > 0) return avg.toLong()
        }

        return defaultSeconds
    }

    /**
     * Calculates estimated wait seconds = position * avgConsultationSeconds.
     */
    fun estimatedWaitSeconds(position: Int, avgConsultationSeconds: Long): Long {
        if (position <= 0) return 0L
        return position * avgConsultationSeconds
    }

    /**
     * Formats wait duration into human-readable string (e.g., "~12 min", "< 5 min", "~1 hr 15 min").
     */
    fun formatEstimatedWait(waitSeconds: Long): String {
        if (waitSeconds <= 0) return "Next in line"
        val minutes = (waitSeconds + 30) / 60
        return when {
            minutes < 5 -> "< 5 min"
            minutes < 60 -> "~$minutes min"
            else -> {
                val hours = minutes / 60
                val remainingMins = minutes % 60
                if (remainingMins == 0L) "~$hours hr" else "~$hours hr $remainingMins min"
            }
        }
    }
}
