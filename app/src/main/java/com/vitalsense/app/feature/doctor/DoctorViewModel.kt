package com.vitalsense.app.feature.doctor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalsense.app.core.data.model.*
import com.vitalsense.app.core.data.repository.VitalSenseRepository
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
class DoctorViewModel @Inject constructor(
    private val repository: VitalSenseRepository,
    private val appStateHolder: AppStateHolder
) : ViewModel() {

    val activeDoctor: StateFlow<Doctor> = appStateHolder.activeDoctor

    // Strictly scoped cases: Only cases routed to active doctor's specialty or assigned directly
    val scopedCases: StateFlow<List<ConditionRecord>> = activeDoctor.flatMapLatest { doctor ->
        repository.getCasesForDoctor(doctor.id, doctor.specialty)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Scoped appointments for active doctor
    val appointments: StateFlow<List<Appointment>> = activeDoctor.flatMapLatest { doctor ->
        repository.getAppointmentsForDoctor(doctor.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dispensary stock for availability checks
    val dispensaryStock: StateFlow<List<DispensaryItem>> = repository.getDispensaryStock()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected case for Case Detail View (§6.4)
    private val _selectedCase = MutableStateFlow<ConditionRecord?>(null)
    val selectedCase: StateFlow<ConditionRecord?> = _selectedCase.asStateFlow()

    // Prior prescriptions for the currently viewed patient
    val patientPrescriptions: StateFlow<List<Prescription>> = _selectedCase.flatMapLatest { case ->
        if (case != null) repository.getPrescriptionsForPatient(case.patientId)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // View-Only patient profile / health card
    val patientProfile: StateFlow<Patient?> = _selectedCase.flatMapLatest { case ->
        if (case != null) repository.getPatientById(case.patientId)
        else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectCase(record: ConditionRecord) {
        _selectedCase.value = record
    }

    fun clearSelectedCase() {
        _selectedCase.value = null
    }

    /**
     * Submit free-text medical response attached to the case (§2.2, §4.2, §4.4)
     */
    fun submitMedicalResponse(
        caseId: String,
        responseText: String,
        privateNotes: String? = null,
        newStatus: CaseStatus = CaseStatus.RESPONDED
    ) {
        val doctor = activeDoctor.value
        viewModelScope.launch {
            repository.respondToCase(
                caseId = caseId,
                doctorId = doctor.id,
                doctorName = doctor.name,
                responseText = responseText,
                privateNotes = privateNotes,
                newStatus = newStatus
            )
            // Update selected case in memory
            _selectedCase.update { current ->
                if (current?.id == caseId) {
                    current.copy(
                        status = newStatus,
                        doctorResponse = responseText,
                        doctorResponseTimestamp = System.currentTimeMillis(),
                        doctorResponseDoctorName = doctor.name,
                        privateDoctorNotes = privateNotes ?: current.privateDoctorNotes
                    )
                } else current
            }
        }
    }

    /**
     * Re-route / refer case to another specialist (§4.3)
     */
    fun referCase(
        caseId: String,
        targetSpecialty: DoctorSpecialty,
        referralNotes: String
    ) {
        val doctor = activeDoctor.value
        viewModelScope.launch {
            repository.referCaseToSpecialist(
                caseId = caseId,
                referringDoctor = doctor,
                targetSpecialty = targetSpecialty,
                referralNotes = referralNotes
            )
            _selectedCase.update { current ->
                if (current?.id == caseId) {
                    current.copy(
                        status = CaseStatus.REFERRED,
                        requestedDoctorType = targetSpecialty,
                        referredByDoctorId = doctor.id,
                        referredByDoctorName = doctor.name,
                        referralNotes = referralNotes
                    )
                } else current
            }
        }
    }

    /**
     * Issue a structured prescription tied to a case (§2.3, §5)
     */
    fun issuePrescription(
        caseId: String,
        patientId: String,
        patientName: String,
        medicines: List<PrescribedMedicine>,
        instructions: String
    ) {
        val doctor = activeDoctor.value
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val newPrescription = Prescription(
            id = "rx_${System.currentTimeMillis()}",
            caseId = caseId,
            patientId = patientId,
            patientName = patientName,
            doctorId = doctor.id,
            doctorName = doctor.name,
            doctorSpecialty = doctor.specialty.displayName,
            timestamp = System.currentTimeMillis(),
            dateFormatted = dateFormat.format(Date()),
            medicines = medicines,
            instructions = instructions,
            isOcrExtracted = false
        )

        viewModelScope.launch {
            repository.savePrescription(newPrescription)
        }
    }

    /**
     * Doctor proposes an appointment slot to the patient (§2.4)
     */
    fun proposeAppointment(
        patientId: String,
        patientName: String,
        dateFormatted: String,
        timeSlot: String
    ) {
        val doctor = activeDoctor.value
        val appointment = Appointment(
            id = "appt_${System.currentTimeMillis()}",
            patientId = patientId,
            patientName = patientName,
            doctorId = doctor.id,
            doctorName = doctor.name,
            doctorSpecialty = doctor.specialty.displayName,
            dateFormatted = dateFormatted,
            timeSlot = timeSlot,
            status = "Pending Patient Confirmation",
            proposedBy = UserRole.DOCTOR
        )

        viewModelScope.launch {
            repository.scheduleAppointment(appointment)
        }
    }

    /**
     * Accept incoming patient appointment (§2.4)
     */
    fun acceptAppointment(appointmentId: String) {
        viewModelScope.launch {
            repository.updateAppointmentStatus(appointmentId, "Confirmed")
        }
    }

    /**
     * Decline incoming appointment (§2.4)
     */
    fun declineAppointment(appointmentId: String) {
        viewModelScope.launch {
            repository.updateAppointmentStatus(appointmentId, "Declined")
        }
    }

    /**
     * Reschedule appointment with proposed new slot (§2.4)
     */
    fun rescheduleAppointment(appointmentId: String, newDate: String, newTime: String) {
        viewModelScope.launch {
            repository.updateAppointmentStatus(
                appointmentId = appointmentId,
                newStatus = "Rescheduled by Doctor ($newDate, $newTime)"
            )
        }
    }

    // --- LIVE QUEUE & DAY-OF CONSULTATION CONTROLS ---

    val todayDateFormatted: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // Live reactive queue for the active doctor today
    val todaysQueue: StateFlow<List<QueueEntry>> = activeDoctor.flatMapLatest { doctor ->
        repository.observeDoctorQueue(doctor.id, todayDateFormatted)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active doctor's configured slot/capacity for today
    val todaysSlotConfig: StateFlow<DoctorDaySlotConfig?> = activeDoctor.flatMapLatest { doctor ->
        repository.observeDoctorSlots(doctor.id, todayDateFormatted).map { it.firstOrNull() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun callNext() {
        val doctor = activeDoctor.value
        viewModelScope.launch {
            repository.callNext(doctor.id, todayDateFormatted)
        }
    }

    fun startConsultation(entryId: String) {
        viewModelScope.launch {
            try {
                repository.startConsultation(entryId)
            } catch (e: Exception) {
                // Log or propagate
            }
        }
    }

    fun completeConsultation(entryId: String, outcomeNotes: String? = null) {
        viewModelScope.launch {
            repository.completeConsultation(entryId, outcomeNotes)
        }
    }

    fun skipEntry(entryId: String) {
        viewModelScope.launch {
            repository.skipEntry(entryId)
        }
    }

    fun markNoShow(entryId: String) {
        viewModelScope.launch {
            repository.markNoShow(entryId)
        }
    }

    fun prioritizeEntry(entryId: String) {
        viewModelScope.launch {
            repository.prioritizeEntry(entryId)
        }
    }

    fun addWalkIn(patientId: String, patientName: String) {
        val doctor = activeDoctor.value
        viewModelScope.launch {
            repository.joinWalkInQueue(
                doctorId = doctor.id,
                patientId = patientId,
                patientName = patientName
            )
        }
    }

    fun saveSlotConfig(config: DoctorDaySlotConfig) {
        viewModelScope.launch {
            repository.defineDoctorSlot(config)
        }
    }

    fun toggleWalkIn(isOpen: Boolean) {
        val current = todaysSlotConfig.value
        val doctor = activeDoctor.value
        val updated = current?.copy(isWalkInOpen = isOpen) ?: DoctorDaySlotConfig(
            id = "slot_${doctor.id}_$todayDateFormatted",
            doctorId = doctor.id,
            dateFormatted = todayDateFormatted,
            startTime = "09:00",
            endTime = "17:00",
            capacity = 20,
            isWalkInOpen = isOpen
        )
        saveSlotConfig(updated)
    }
}

