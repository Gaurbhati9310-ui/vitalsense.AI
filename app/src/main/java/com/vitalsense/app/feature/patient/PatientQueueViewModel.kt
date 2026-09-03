package com.vitalsense.app.feature.patient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalsense.app.core.data.model.Doctor
import com.vitalsense.app.core.data.model.Patient
import com.vitalsense.app.core.data.model.QueueEntry
import com.vitalsense.app.core.data.model.QueueEntryStatus
import com.vitalsense.app.core.data.repository.VitalSenseRepository
import com.vitalsense.app.core.data.util.QueueEtaCalculator
import com.vitalsense.app.core.state.AppStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PatientQueueViewModel @Inject constructor(
    private val repository: VitalSenseRepository,
    private val appStateHolder: AppStateHolder
) : ViewModel() {

    val activePatient: StateFlow<Patient> = appStateHolder.activePatient

    val todayDateFormatted: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // Active patient's queue entry for today (if checked in)
    val activeQueueEntry: StateFlow<QueueEntry?> = activePatient.flatMapLatest { patient ->
        repository.observePatientQueueEntry(patient.id, todayDateFormatted)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Full queue for the doctor associated with the patient's active entry
    val doctorQueue: StateFlow<List<QueueEntry>> = activeQueueEntry.flatMapLatest { entry ->
        if (entry != null) {
            repository.observeDoctorQueue(entry.doctorId, entry.dateFormatted)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Number of people ahead in line
    val peopleAheadCount: StateFlow<Int> = combine(activeQueueEntry, doctorQueue) { entry, queue ->
        if (entry != null && entry.status == QueueEntryStatus.WAITING) {
            QueueEtaCalculator.calculatePosition(entry.id, queue)
        } else {
            0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Estimated wait duration in seconds
    val estimatedWaitSeconds: StateFlow<Long> = combine(
        peopleAheadCount,
        doctorQueue,
        activeQueueEntry
    ) { ahead, queue, entry ->
        if (entry != null && entry.status == QueueEntryStatus.WAITING) {
            val completedToday = queue.filter { it.status == QueueEntryStatus.COMPLETED }
            val avgSeconds = QueueEtaCalculator.averageConsultationSeconds(completedToday)
            QueueEtaCalculator.estimatedWaitSeconds(ahead, avgSeconds)
        } else {
            0L
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val doctors: StateFlow<List<Doctor>> = repository.getDoctors()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun checkIn(appointmentId: String) {
        viewModelScope.launch {
            repository.checkInAppointment(appointmentId)
        }
    }

    fun joinWalkIn(doctorId: String) {
        val patient = activePatient.value
        viewModelScope.launch {
            repository.joinWalkInQueue(
                doctorId = doctorId,
                patientId = patient.id,
                patientName = patient.name
            )
        }
    }

    fun cancelQueueEntry(entryId: String) {
        viewModelScope.launch {
            repository.cancelQueueEntry(entryId)
        }
    }
}
