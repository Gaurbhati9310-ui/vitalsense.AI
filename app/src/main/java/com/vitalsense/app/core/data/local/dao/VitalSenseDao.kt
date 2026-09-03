package com.vitalsense.app.core.data.local.dao

import androidx.room.*
import com.vitalsense.app.core.data.local.entity.*
import com.vitalsense.app.core.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VitalSenseDao {

    // --- Villages ---
    @Query("SELECT * FROM villages ORDER BY activeCases DESC")
    fun getAllVillages(): Flow<List<VillageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVillages(villages: List<VillageEntity>)

    // --- Patients ---
    @Query("SELECT * FROM patients")
    fun getAllPatients(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE id = :id")
    fun getPatientById(id: String): Flow<PatientEntity?>

    @Query("SELECT * FROM patients WHERE ashaWorkerId = :ashaId")
    fun getPatientsByAsha(ashaId: String): Flow<List<PatientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatients(patients: List<PatientEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: PatientEntity)

    // --- ASHA Workers ---
    @Query("SELECT * FROM asha_workers")
    fun getAllAshaWorkers(): Flow<List<AshaWorkerEntity>>

    @Query("SELECT * FROM asha_workers WHERE id = :id OR ashaUniqueId = :uniqueId LIMIT 1")
    fun getAshaWorker(id: String, uniqueId: String): Flow<AshaWorkerEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAshaWorkers(ashaWorkers: List<AshaWorkerEntity>)

    // --- Doctors ---
    @Query("SELECT * FROM doctors")
    fun getAllDoctors(): Flow<List<DoctorEntity>>

    @Query("SELECT * FROM doctors WHERE id = :id")
    fun getDoctorById(id: String): Flow<DoctorEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctors(doctors: List<DoctorEntity>)

    // --- Condition Records ---
    @Query("SELECT * FROM condition_records ORDER BY timestamp DESC")
    fun getAllConditionRecords(): Flow<List<ConditionRecordEntity>>

    @Query("SELECT * FROM condition_records WHERE patientId = :patientId ORDER BY timestamp DESC")
    fun getConditionsForPatient(patientId: String): Flow<List<ConditionRecordEntity>>

    @Query("SELECT * FROM condition_records WHERE requestedDoctorType = :specialty OR assignedDoctorId = :doctorId ORDER BY CASE severity WHEN 'SEVERE' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MODERATE' THEN 3 ELSE 4 END, timestamp DESC")
    fun getCasesForDoctor(specialty: DoctorSpecialty, doctorId: String): Flow<List<ConditionRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConditionRecord(record: ConditionRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConditionRecords(records: List<ConditionRecordEntity>)

    // --- Prescriptions ---
    @Query("SELECT * FROM prescriptions ORDER BY timestamp DESC")
    fun getAllPrescriptions(): Flow<List<PrescriptionEntity>>

    @Query("SELECT * FROM prescriptions WHERE patientId = :patientId ORDER BY timestamp DESC")
    fun getPrescriptionsForPatient(patientId: String): Flow<List<PrescriptionEntity>>

    @Query("SELECT * FROM prescriptions WHERE caseId = :caseId ORDER BY timestamp DESC")
    fun getPrescriptionsByCase(caseId: String): Flow<List<PrescriptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescription(prescription: PrescriptionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescriptions(prescriptions: List<PrescriptionEntity>)

    // --- Appointments ---
    @Query("SELECT * FROM appointments ORDER BY dateFormatted ASC")
    fun getAllAppointments(): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE patientId = :patientId")
    fun getAppointmentsForPatient(patientId: String): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE doctorId = :doctorId")
    fun getAppointmentsForDoctor(doctorId: String): Flow<List<AppointmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: AppointmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointments(appointments: List<AppointmentEntity>)

    // --- Broadcast Notices ---
    @Query("SELECT * FROM broadcast_notices ORDER BY timestamp DESC")
    fun getAllNotices(): Flow<List<BroadcastNoticeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotice(notice: BroadcastNoticeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotices(notices: List<BroadcastNoticeEntity>)

    // --- Dispensary Stock ---
    @Query("SELECT * FROM dispensary_stock ORDER BY medicineName ASC")
    fun getAllDispensaryItems(): Flow<List<DispensaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDispensaryItems(items: List<DispensaryEntity>)

    // --- Government Schemes ---
    @Query("SELECT * FROM government_schemes")
    fun getAllSchemes(): Flow<List<GovernmentSchemeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchemes(schemes: List<GovernmentSchemeEntity>)

    // --- Outbox Queue (Offline-First Sync) ---
    @Query("SELECT * FROM outbox_records ORDER BY timestamp ASC")
    suspend fun getPendingOutboxRecords(): List<OutboxEntity>

    @Query("SELECT COUNT(*) FROM outbox_records")
    fun getPendingOutboxCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutboxRecord(outbox: OutboxEntity)

    @Query("DELETE FROM outbox_records WHERE id = :id")
    suspend fun deleteOutboxRecord(id: String)

    // --- Live Queue & Doctor Slots ---
    @Query("SELECT * FROM queue_entries WHERE doctorId = :doctorId AND dateFormatted = :date ORDER BY checkedInAt ASC")
    fun observeDoctorQueue(doctorId: String, date: String): Flow<List<QueueEntryEntity>>

    @Query("SELECT * FROM queue_entries WHERE patientId = :patientId AND dateFormatted = :date LIMIT 1")
    fun observePatientQueueEntry(patientId: String, date: String): Flow<QueueEntryEntity?>

    @Query("SELECT * FROM queue_entries WHERE id = :entryId LIMIT 1")
    suspend fun getQueueEntryById(entryId: String): QueueEntryEntity?

    @Query("SELECT * FROM queue_entries WHERE doctorId = :doctorId AND dateFormatted = :date AND status = 'COMPLETED' ORDER BY completedAt DESC LIMIT :limit")
    suspend fun getRecentCompletedEntries(doctorId: String, date: String, limit: Int): List<QueueEntryEntity>

    @Query("SELECT * FROM queue_entries WHERE doctorId = :doctorId AND status = 'COMPLETED' ORDER BY completedAt DESC LIMIT :limit")
    suspend fun getRecentCompletedEntriesAcrossDates(doctorId: String, limit: Int): List<QueueEntryEntity>

    @Query("SELECT * FROM queue_entries WHERE dateFormatted = :date")
    fun observeAllQueueEntriesForDate(date: String): Flow<List<QueueEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQueueEntry(entry: QueueEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQueueEntries(entries: List<QueueEntryEntity>)

    @Query("SELECT * FROM doctor_day_slots WHERE doctorId = :doctorId AND dateFormatted = :date")
    fun observeDoctorSlots(doctorId: String, date: String): Flow<List<DoctorDaySlotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDoctorSlot(slot: DoctorDaySlotEntity)
}


