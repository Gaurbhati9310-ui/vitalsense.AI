package com.vitalsense.app.core.data.repository

import com.vitalsense.app.core.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth repository for VitalSense.
 */
interface VitalSenseRepository {

    // --- Villages ---
    fun getVillages(): Flow<List<Village>>
    suspend fun addVillage(village: Village)

    // --- Patients ---
    fun getPatients(): Flow<List<Patient>>
    fun getPatientById(id: String): Flow<Patient?>
    fun getPatientsForAsha(ashaId: String): Flow<List<Patient>>
    suspend fun savePatient(patient: Patient)

    // --- ASHA Workers ---
    fun getAshaWorkers(): Flow<List<AshaWorker>>
    fun getAshaWorkerById(id: String): Flow<AshaWorker?>

    // --- Doctors ---
    fun getDoctors(): Flow<List<Doctor>>
    fun getDoctorById(id: String): Flow<Doctor?>

    // --- Condition Records ---
    fun getConditionRecords(): Flow<List<ConditionRecord>>
    fun getConditionRecordsForPatient(patientId: String): Flow<List<ConditionRecord>>
    fun getCasesForDoctor(doctorId: String, specialty: DoctorSpecialty): Flow<List<ConditionRecord>>
    suspend fun logCondition(record: ConditionRecord)
    suspend fun respondToCase(caseId: String, doctorId: String, doctorName: String, responseText: String, privateNotes: String?, newStatus: CaseStatus = CaseStatus.RESPONDED)
    suspend fun referCaseToSpecialist(caseId: String, referringDoctor: Doctor, targetSpecialty: DoctorSpecialty, referralNotes: String)

    // --- Prescriptions ---
    fun getPrescriptions(): Flow<List<Prescription>>
    fun getPrescriptionsForPatient(patientId: String): Flow<List<Prescription>>
    fun getPrescriptionsByCase(caseId: String): Flow<List<Prescription>>
    suspend fun savePrescription(prescription: Prescription)

    // --- Appointments ---
    fun getAppointments(): Flow<List<Appointment>>
    fun getAppointmentsForPatient(patientId: String): Flow<List<Appointment>>
    fun getAppointmentsForDoctor(doctorId: String): Flow<List<Appointment>>
    suspend fun scheduleAppointment(appointment: Appointment)
    suspend fun updateAppointmentStatus(appointmentId: String, newStatus: String, outcomeNotes: String? = null)

    // --- Broadcast Notices ---
    fun getNotices(): Flow<List<BroadcastNotice>>
    suspend fun sendNotice(notice: BroadcastNotice)

    // --- Dispensary Stock ---
    fun getDispensaryStock(): Flow<List<DispensaryItem>>

    // --- Government Schemes ---
    fun getGovernmentSchemes(): Flow<List<GovernmentScheme>>

    // --- Emergency SOS ---
    suspend fun triggerEmergencySos(patient: Patient, locationLat: Double?, locationLng: Double?): Boolean

    // --- Live Queue & Doctor Slots ---
    fun observeDoctorQueue(doctorId: String, date: String): Flow<List<QueueEntry>>
    fun observePatientQueueEntry(patientId: String, date: String): Flow<QueueEntry?>
    fun observeDoctorSlots(doctorId: String, date: String): Flow<List<DoctorDaySlotConfig>>
    fun observeAllDoctorQueueSummaries(date: String): Flow<List<DoctorQueueSummary>>

    suspend fun defineDoctorSlot(slot: DoctorDaySlotConfig)
    suspend fun checkInAppointment(appointmentId: String): QueueEntry
    suspend fun joinWalkInQueue(doctorId: String, patientId: String, patientName: String): QueueEntry
    suspend fun callNext(doctorId: String, date: String)
    suspend fun startConsultation(entryId: String)
    suspend fun completeConsultation(entryId: String, outcomeNotes: String?)
    suspend fun markNoShow(entryId: String)
    suspend fun skipEntry(entryId: String)
    suspend fun prioritizeEntry(entryId: String)
    suspend fun cancelQueueEntry(entryId: String)
}

