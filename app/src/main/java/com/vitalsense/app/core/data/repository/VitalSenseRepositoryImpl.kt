package com.vitalsense.app.core.data.repository

import com.google.gson.Gson
import com.vitalsense.app.core.data.local.VitalSenseDatabase
import com.vitalsense.app.core.data.local.entity.*
import com.vitalsense.app.core.data.local.seed.SeedDataProvider
import com.vitalsense.app.core.data.model.*
import com.vitalsense.app.core.data.remote.FirestoreDataSource
import com.vitalsense.app.core.data.util.QueueEtaCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class VitalSenseRepositoryImpl @Inject constructor(
    private val database: VitalSenseDatabase,
    private val firestoreDataSource: FirestoreDataSource,
    private val syncManager: com.vitalsense.app.core.sync.SyncManager
) : VitalSenseRepository {

    private val gson = Gson()
    private val dao = database.vitalSenseDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    // In-memory reactive state caches for instant UI response (zero lag on stage)
    private val _villages = MutableStateFlow(SeedDataProvider.initialVillages)
    private val _patients = MutableStateFlow(SeedDataProvider.initialPatients)
    private val _ashaWorkers = MutableStateFlow(SeedDataProvider.initialAshaWorkers)
    private val _doctors = MutableStateFlow(SeedDataProvider.initialDoctors)
    private val _conditions = MutableStateFlow(SeedDataProvider.initialConditionRecords)
    private val _prescriptions = MutableStateFlow(SeedDataProvider.initialPrescriptions)
    private val _appointments = MutableStateFlow(SeedDataProvider.initialAppointments)
    private val _notices = MutableStateFlow(SeedDataProvider.initialNotices)
    private val _dispensary = MutableStateFlow(SeedDataProvider.initialDispensaryItems)
    private val _schemes = MutableStateFlow(SeedDataProvider.initialSchemes)
    private val _queueEntries = MutableStateFlow<List<QueueEntry>>(emptyList())
    private val _doctorSlots = MutableStateFlow<List<DoctorDaySlotConfig>>(emptyList())


    init {
        // 1. Pre-seed local Room database on first launch
        scope.launch {
            try {
                dao.insertVillages(SeedDataProvider.getVillageEntities())
                dao.insertAshaWorkers(SeedDataProvider.getAshaEntities())
                dao.insertDoctors(SeedDataProvider.getDoctorEntities())
                dao.insertPatients(SeedDataProvider.getPatientEntities())
                dao.insertConditionRecords(SeedDataProvider.getConditionEntities())
                dao.insertPrescriptions(SeedDataProvider.getPrescriptionEntities())
                dao.insertAppointments(SeedDataProvider.getAppointmentEntities())
                dao.insertDispensaryItems(SeedDataProvider.getDispensaryEntities())
                dao.insertNotices(SeedDataProvider.getNoticeEntities())
                dao.insertSchemes(SeedDataProvider.getSchemeEntities())
            } catch (e: Exception) {
                // Fallback to in-memory state
            }
        }

        // 2. Start real-time Firestore listeners when online
        scope.launch {
            try {
                firestoreDataSource.getConditionRecordsStream().collect { remoteRecords ->
                    if (remoteRecords.isNotEmpty()) {
                        _conditions.update { remoteRecords }
                    }
                }
            } catch (e: Exception) {
                // Offline fallback
            }
        }

        scope.launch {
            try {
                firestoreDataSource.getBroadcastNoticesStream().collect { remoteNotices ->
                    if (remoteNotices.isNotEmpty()) {
                        _notices.update { remoteNotices }
                    }
                }
            } catch (e: Exception) {
                // Offline fallback
            }
        }
    }

    // --- Villages ---
    override fun getVillages(): Flow<List<Village>> = _villages.asStateFlow()

    override suspend fun addVillage(village: Village) {
        _villages.update { it + village }
        scope.launch {
            dao.insertVillages(listOf(
                VillageEntity(
                    village.id, village.name, village.district, village.state,
                    village.population, village.latitude, village.longitude,
                    village.activeCases, village.highRiskCount
                )
            ))
        }
    }

    // --- Patients ---
    override fun getPatients(): Flow<List<Patient>> = _patients.asStateFlow()

    override fun getPatientById(id: String): Flow<Patient?> = _patients.map { list ->
        list.find { it.id == id }
    }

    override fun getPatientsForAsha(ashaId: String): Flow<List<Patient>> = _patients.map { list ->
        list.filter { it.ashaWorkerId == ashaId }
    }

    override suspend fun savePatient(patient: Patient) {
        // 1. Instant local state update
        _patients.update { list ->
            val index = list.indexOfFirst { it.id == patient.id }
            if (index >= 0) {
                list.toMutableList().apply { set(index, patient) }
            } else {
                list + patient
            }
        }

        // 2. Persist to Room SQLite
        scope.launch {
            dao.insertPatient(
                PatientEntity(
                    patient.id, patient.name, patient.age, patient.gender, patient.phone,
                    patient.villageId, patient.villageName, patient.ashaWorkerId,
                    patient.ashaWorkerName, patient.currentRiskLevel, patient.lastCondition,
                    patient.lastVisitDate, patient.nextAppointmentDate, patient.emergencyContact,
                    patient.profilePhotoUrl
                )
            )
            // 3. Remote Cloud Firestore sync
            try {
                firestoreDataSource.uploadPatient(patient)
            } catch (e: Exception) {
                // Offline: remains saved locally in Room
            }
        }
    }

    // --- ASHA Workers ---
    override fun getAshaWorkers(): Flow<List<AshaWorker>> = _ashaWorkers.asStateFlow()

    override fun getAshaWorkerById(id: String): Flow<AshaWorker?> = _ashaWorkers.map { list ->
        list.find { it.id == id || it.ashaUniqueId == id }
    }

    // --- Doctors ---
    override fun getDoctors(): Flow<List<Doctor>> = _doctors.asStateFlow()

    override fun getDoctorById(id: String): Flow<Doctor?> = _doctors.map { list ->
        list.find { it.id == id }
    }

    // --- Condition Records ---
    override fun getConditionRecords(): Flow<List<ConditionRecord>> = _conditions.asStateFlow()

    override fun getConditionRecordsForPatient(patientId: String): Flow<List<ConditionRecord>> = _conditions.map { list ->
        list.filter { it.patientId == patientId }
    }

    override fun getCasesForDoctor(doctorId: String, specialty: DoctorSpecialty): Flow<List<ConditionRecord>> = _conditions.map { list ->
        list.filter { record ->
            record.requestedDoctorType == specialty || record.assignedDoctorId == doctorId
        }.sortedWith(
            compareBy<ConditionRecord> { record ->
                when (record.severity) {
                    SeverityLevel.SEVERE -> 0
                    SeverityLevel.HIGH -> 1
                    SeverityLevel.MODERATE -> 2
                    SeverityLevel.LOW -> 3
                }
            }.thenByDescending { it.timestamp }
        )
    }

    override suspend fun logCondition(record: ConditionRecord) {
        // 1. Instant in-memory update
        _conditions.update { listOf(record) + it }

        // Update patient's current risk level
        _patients.update { patients ->
            patients.map { p ->
                if (p.id == record.patientId) {
                    p.copy(
                        currentRiskLevel = record.severity,
                        lastCondition = record.notes.ifBlank { "${record.category.displayName} (${record.severity.displayName})" },
                        lastVisitDate = "Today"
                    )
                } else p
            }
        }

        // Update village outbreak count
        _villages.update { villages ->
            villages.map { v ->
                if (v.id == record.villageId) {
                    v.copy(
                        activeCases = v.activeCases + 1,
                        highRiskCount = if (record.severity == SeverityLevel.HIGH || record.severity == SeverityLevel.SEVERE) v.highRiskCount + 1 else v.highRiskCount
                    )
                } else v
            }
        }

        // 2. Persist to Room & Cloud Firestore via Durable Outbox
        scope.launch {
            dao.insertConditionRecord(
                ConditionRecordEntity(
                    record.id, record.patientId, record.patientName, record.villageId,
                    record.villageName, record.category, record.severity,
                    record.requestedDoctorType, record.notes, record.timestamp,
                    record.ashaProxyLogged, record.status, record.assignedDoctorId,
                    record.assignedDoctorName, record.doctorResponse, record.doctorResponseTimestamp,
                    record.doctorResponseDoctorName, record.privateDoctorNotes,
                    record.referredByDoctorId, record.referredByDoctorName,
                    record.referralNotes, isPendingSync = true
                )
            )

            // Queue in Outbox
            val outboxId = "outbox_cond_${record.id}"
            dao.insertOutboxRecord(
                com.vitalsense.app.core.data.local.entity.OutboxEntity(
                    id = outboxId,
                    actionType = "CONDITION_RECORD",
                    entityId = record.id,
                    payloadJson = gson.toJson(record)
                )
            )

            try {
                firestoreDataSource.uploadConditionRecord(record)
                dao.deleteOutboxRecord(outboxId)
                dao.insertConditionRecord(
                    ConditionRecordEntity(
                        record.id, record.patientId, record.patientName, record.villageId,
                        record.villageName, record.category, record.severity,
                        record.requestedDoctorType, record.notes, record.timestamp,
                        record.ashaProxyLogged, record.status, record.assignedDoctorId,
                        record.assignedDoctorName, record.doctorResponse, record.doctorResponseTimestamp,
                        record.doctorResponseDoctorName, record.privateDoctorNotes,
                        record.referredByDoctorId, record.referredByDoctorName,
                        record.referralNotes, isPendingSync = false
                    )
                )
            } catch (e: Exception) {
                // Device offline: Outbox record remains persisted in Room SQLite; WorkManager will sync on network reconnection
                syncManager.triggerImmediateSync()
            }
        }
    }

    override suspend fun respondToCase(
        caseId: String,
        doctorId: String,
        doctorName: String,
        responseText: String,
        privateNotes: String?,
        newStatus: CaseStatus
    ) {
        val now = System.currentTimeMillis()
        var updatedRecord: ConditionRecord? = null

        _conditions.update { list ->
            list.map { record ->
                if (record.id == caseId) {
                    val updated = record.copy(
                        status = newStatus,
                        doctorResponse = responseText,
                        doctorResponseTimestamp = now,
                        doctorResponseDoctorName = doctorName,
                        privateDoctorNotes = privateNotes ?: record.privateDoctorNotes,
                        assignedDoctorId = doctorId,
                        assignedDoctorName = doctorName
                    )
                    updatedRecord = updated
                    updated
                } else record
            }
        }

        updatedRecord?.let { record ->
            scope.launch {
                dao.insertConditionRecord(
                    ConditionRecordEntity(
                        record.id, record.patientId, record.patientName, record.villageId,
                        record.villageName, record.category, record.severity,
                        record.requestedDoctorType, record.notes, record.timestamp,
                        record.ashaProxyLogged, record.status, record.assignedDoctorId,
                        record.assignedDoctorName, record.doctorResponse, record.doctorResponseTimestamp,
                        record.doctorResponseDoctorName, record.privateDoctorNotes,
                        record.referredByDoctorId, record.referredByDoctorName,
                        record.referralNotes, isPendingSync = false
                    )
                )
                try {
                    firestoreDataSource.uploadConditionRecord(record)
                } catch (e: Exception) {
                    // Stays in Room
                }
            }
        }
    }

    override suspend fun referCaseToSpecialist(
        caseId: String,
        referringDoctor: Doctor,
        targetSpecialty: DoctorSpecialty,
        referralNotes: String
    ) {
        var updatedRecord: ConditionRecord? = null

        _conditions.update { list ->
            list.map { record ->
                if (record.id == caseId) {
                    val updated = record.copy(
                        status = CaseStatus.REFERRED,
                        requestedDoctorType = targetSpecialty,
                        referredByDoctorId = referringDoctor.id,
                        referredByDoctorName = referringDoctor.name,
                        referralNotes = referralNotes,
                        assignedDoctorId = null,
                        assignedDoctorName = null
                    )
                    updatedRecord = updated
                    updated
                } else record
            }
        }

        updatedRecord?.let { record ->
            scope.launch {
                dao.insertConditionRecord(
                    ConditionRecordEntity(
                        record.id, record.patientId, record.patientName, record.villageId,
                        record.villageName, record.category, record.severity,
                        record.requestedDoctorType, record.notes, record.timestamp,
                        record.ashaProxyLogged, record.status, record.assignedDoctorId,
                        record.assignedDoctorName, record.doctorResponse, record.doctorResponseTimestamp,
                        record.doctorResponseDoctorName, record.privateDoctorNotes,
                        record.referredByDoctorId, record.referredByDoctorName,
                        record.referralNotes, isPendingSync = false
                    )
                )
                try {
                    firestoreDataSource.uploadConditionRecord(record)
                } catch (e: Exception) {
                    // Stays in Room
                }
            }
        }
    }

    // --- Prescriptions ---
    override fun getPrescriptions(): Flow<List<Prescription>> = _prescriptions.asStateFlow()

    override fun getPrescriptionsForPatient(patientId: String): Flow<List<Prescription>> = _prescriptions.map { list ->
        list.filter { it.patientId == patientId }
    }

    override fun getPrescriptionsByCase(caseId: String): Flow<List<Prescription>> = _prescriptions.map { list ->
        list.filter { it.caseId == caseId }
    }

    override suspend fun savePrescription(prescription: Prescription) {
        _prescriptions.update { listOf(prescription) + it }

        // Also mark the case as RESPONDED if tied to a case
        if (prescription.caseId != null) {
            _conditions.update { list ->
                list.map { c ->
                    if (c.id == prescription.caseId && c.status == CaseStatus.PENDING_REVIEW) {
                        c.copy(status = CaseStatus.RESPONDED)
                    } else c
                }
            }
        }

        scope.launch {
            dao.insertPrescription(
                PrescriptionEntity(
                    prescription.id, prescription.caseId, prescription.patientId, prescription.patientName,
                    prescription.doctorId, prescription.doctorName, prescription.doctorSpecialty,
                    prescription.timestamp, prescription.dateFormatted,
                    gson.toJson(prescription.medicines), prescription.instructions,
                    prescription.isOcrExtracted
                )
            )

            val outboxId = "outbox_rx_${prescription.id}"
            dao.insertOutboxRecord(
                com.vitalsense.app.core.data.local.entity.OutboxEntity(
                    id = outboxId,
                    actionType = "PRESCRIPTION",
                    entityId = prescription.id,
                    payloadJson = gson.toJson(prescription)
                )
            )

            try {
                firestoreDataSource.uploadPrescription(prescription)
                dao.deleteOutboxRecord(outboxId)
            } catch (e: Exception) {
                syncManager.triggerImmediateSync()
            }
        }
    }

    // --- Appointments ---
    override fun getAppointments(): Flow<List<Appointment>> = _appointments.asStateFlow()

    override fun getAppointmentsForPatient(patientId: String): Flow<List<Appointment>> = _appointments.map { list ->
        list.filter { it.patientId == patientId }
    }

    override fun getAppointmentsForDoctor(doctorId: String): Flow<List<Appointment>> = _appointments.map { list ->
        list.filter { it.doctorId == doctorId }
    }

    override suspend fun scheduleAppointment(appointment: Appointment) {
        _appointments.update { listOf(appointment) + it }

        _patients.update { patients ->
            patients.map { p ->
                if (p.id == appointment.patientId) {
                    p.copy(nextAppointmentDate = "${appointment.dateFormatted} (${appointment.timeSlot})")
                } else p
            }
        }

        scope.launch {
            dao.insertAppointment(
                AppointmentEntity(
                    appointment.id, appointment.patientId, appointment.patientName,
                    appointment.doctorId, appointment.doctorName, appointment.doctorSpecialty,
                    appointment.dateFormatted, appointment.timeSlot, appointment.status,
                    appointment.proposedBy, appointment.outcomeNotes
                )
            )

            val outboxId = "outbox_appt_${appointment.id}"
            dao.insertOutboxRecord(
                com.vitalsense.app.core.data.local.entity.OutboxEntity(
                    id = outboxId,
                    actionType = "APPOINTMENT",
                    entityId = appointment.id,
                    payloadJson = gson.toJson(appointment)
                )
            )

            try {
                firestoreDataSource.uploadAppointment(appointment)
                dao.deleteOutboxRecord(outboxId)
            } catch (e: Exception) {
                syncManager.triggerImmediateSync()
            }
        }
    }

    override suspend fun updateAppointmentStatus(
        appointmentId: String,
        newStatus: String,
        outcomeNotes: String?
    ) {
        var updatedAppointment: Appointment? = null

        _appointments.update { list ->
            list.map { appt ->
                if (appt.id == appointmentId) {
                    val updated = appt.copy(
                        status = newStatus,
                        outcomeNotes = outcomeNotes ?: appt.outcomeNotes
                    )
                    updatedAppointment = updated
                    updated
                } else appt
            }
        }

        updatedAppointment?.let { appt ->
            scope.launch {
                dao.insertAppointment(
                    AppointmentEntity(
                        appt.id, appt.patientId, appt.patientName,
                        appt.doctorId, appt.doctorName, appt.doctorSpecialty,
                        appt.dateFormatted, appt.timeSlot, appt.status,
                        appt.proposedBy, appt.outcomeNotes
                    )
                )

                val outboxId = "outbox_appt_status_${appt.id}"
                dao.insertOutboxRecord(
                    com.vitalsense.app.core.data.local.entity.OutboxEntity(
                        id = outboxId,
                        actionType = "APPOINTMENT",
                        entityId = appt.id,
                        payloadJson = gson.toJson(appt)
                    )
                )

                try {
                    firestoreDataSource.uploadAppointment(appt)
                    dao.deleteOutboxRecord(outboxId)
                } catch (e: Exception) {
                    syncManager.triggerImmediateSync()
                }
            }
        }
    }

    // --- Broadcast Notices ---
    override fun getNotices(): Flow<List<BroadcastNotice>> = _notices.asStateFlow()

    override suspend fun sendNotice(notice: BroadcastNotice) {
        android.util.Log.d("VitalSenseFirebase", "📢 sendNotice triggered: ${notice.title}")
        _notices.update { listOf(notice) + it }

        scope.launch {
            dao.insertNotice(
                BroadcastNoticeEntity(
                    notice.id, notice.senderRole, notice.senderName, notice.targetRole,
                    notice.targetVillage, notice.title, notice.message, notice.timestamp,
                    notice.isUrgent
                )
            )

            val outboxId = "outbox_notice_${notice.id}"
            dao.insertOutboxRecord(
                com.vitalsense.app.core.data.local.entity.OutboxEntity(
                    id = outboxId,
                    actionType = "BROADCAST_NOTICE",
                    entityId = notice.id,
                    payloadJson = gson.toJson(notice)
                )
            )

            try {
                firestoreDataSource.uploadNotice(notice)
                dao.deleteOutboxRecord(outboxId)
            } catch (e: Exception) {
                syncManager.triggerImmediateSync()
            }
        }
    }

    // --- Dispensary Stock ---
    override fun getDispensaryStock(): Flow<List<DispensaryItem>> = _dispensary.asStateFlow()

    // --- Government Schemes ---
    override fun getGovernmentSchemes(): Flow<List<GovernmentScheme>> = _schemes.asStateFlow()

    // --- Emergency SOS ---
    override suspend fun triggerEmergencySos(
        patient: Patient,
        locationLat: Double?,
        locationLng: Double?
    ): Boolean {
        android.util.Log.d("VitalSenseFirebase", "🚨 triggerEmergencySos called for patient: ${patient.name}")
        val sosNotice = BroadcastNotice(
            id = "sos_${System.currentTimeMillis()}",
            senderRole = UserRole.PATIENT,
            senderName = "${patient.name} (SOS ALERT)",
            targetRole = "ASHA_DOCTOR",
            targetVillage = patient.villageName,
            title = "🚨 EMERGENCY SOS: ${patient.name}",
            message = "Patient ${patient.name} (${patient.villageName}, Age ${patient.age}) triggered an Emergency SOS! Contact: ${patient.phone}. Location: Lat ${locationLat ?: 26.8467}, Lng ${locationLng ?: 80.9462}.",
            timestamp = System.currentTimeMillis(),
            isUrgent = true
        )
        sendNotice(sosNotice)
        return true
    }

    // --- Live Queue & Doctor Slots Implementation ---

    private fun getTodayFormatted(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    override fun observeDoctorQueue(doctorId: String, date: String): Flow<List<QueueEntry>> {
        // Start listening to Firestore remote stream in background
        scope.launch {
            try {
                firestoreDataSource.observeDoctorQueueStream(doctorId, date).collect { remoteList ->
                    if (remoteList.isNotEmpty()) {
                        _queueEntries.update { current ->
                            val other = current.filterNot { it.doctorId == doctorId && it.dateFormatted == date }
                            other + remoteList
                        }
                        dao.upsertQueueEntries(remoteList.map { it.toEntity() })
                    }
                }
            } catch (e: Exception) {
                // Offline fallback
            }
        }

        return _queueEntries.map { list ->
            list.filter { it.doctorId == doctorId && it.dateFormatted == date }
        }.onStart {
            scope.launch {
                val cached = dao.observeDoctorQueue(doctorId, date).firstOrNull()
                if (!cached.isNullOrEmpty()) {
                    val mapped = cached.map { it.toModel() }
                    _queueEntries.update { current ->
                        val other = current.filterNot { it.doctorId == doctorId && it.dateFormatted == date }
                        other + mapped
                    }
                }
            }
        }
    }

    override fun observePatientQueueEntry(patientId: String, date: String): Flow<QueueEntry?> {
        scope.launch {
            try {
                firestoreDataSource.observePatientQueueEntryStream(patientId, date).collect { remoteEntry ->
                    if (remoteEntry != null) {
                        _queueEntries.update { current ->
                            val other = current.filterNot { it.id == remoteEntry.id }
                            listOf(remoteEntry) + other
                        }
                        dao.upsertQueueEntry(remoteEntry.toEntity())
                    }
                }
            } catch (e: Exception) {
                // Offline fallback
            }
        }

        return _queueEntries.map { list ->
            list.find { it.patientId == patientId && it.dateFormatted == date && it.status != QueueEntryStatus.CANCELLED }
        }.onStart {
            scope.launch {
                val cached = dao.observePatientQueueEntry(patientId, date).firstOrNull()
                if (cached != null) {
                    val model = cached.toModel()
                    _queueEntries.update { current ->
                        val other = current.filterNot { it.id == model.id }
                        listOf(model) + other
                    }
                }
            }
        }
    }

    override fun observeDoctorSlots(doctorId: String, date: String): Flow<List<DoctorDaySlotConfig>> {
        scope.launch {
            try {
                firestoreDataSource.observeDoctorSlotsStream(doctorId, date).collect { remoteSlots ->
                    if (remoteSlots.isNotEmpty()) {
                        _doctorSlots.update { current ->
                            val other = current.filterNot { it.doctorId == doctorId && it.dateFormatted == date }
                            other + remoteSlots
                        }
                        remoteSlots.forEach { dao.upsertDoctorSlot(it.toEntity()) }
                    }
                }
            } catch (e: Exception) {
                // Offline fallback
            }
        }

        return _doctorSlots.map { list ->
            list.filter { it.doctorId == doctorId && it.dateFormatted == date }
        }.onStart {
            scope.launch {
                val cached = dao.observeDoctorSlots(doctorId, date).firstOrNull()
                if (!cached.isNullOrEmpty()) {
                    val mapped = cached.map { it.toModel() }
                    _doctorSlots.update { current ->
                        val other = current.filterNot { it.doctorId == doctorId && it.dateFormatted == date }
                        other + mapped
                    }
                }
            }
        }
    }

    override fun observeAllDoctorQueueSummaries(date: String): Flow<List<DoctorQueueSummary>> {
        return combine(
            getDoctors(),
            _queueEntries,
            _doctorSlots
        ) { doctors, entries, slots ->
            doctors.map { doctor ->
                val doctorEntries = entries.filter { it.doctorId == doctor.id && it.dateFormatted == date }
                val waitingCount = doctorEntries.count { it.status == QueueEntryStatus.WAITING }
                val activeServing = doctorEntries.firstOrNull { it.status == QueueEntryStatus.IN_CONSULTATION }
                    ?: doctorEntries.firstOrNull { it.status == QueueEntryStatus.CALLED }
                val completedToday = doctorEntries.filter { it.status == QueueEntryStatus.COMPLETED }
                val avgWait = QueueEtaCalculator.averageConsultationSeconds(completedToday)
                val slot = slots.firstOrNull { it.doctorId == doctor.id && it.dateFormatted == date }
                val isQueueOpen = slot?.isWalkInOpen ?: true

                DoctorQueueSummary(
                    doctorId = doctor.id,
                    doctorName = doctor.name,
                    dateFormatted = date,
                    waitingCount = waitingCount,
                    currentToken = activeServing?.tokenNumber,
                    avgWaitSeconds = avgWait,
                    isQueueOpen = isQueueOpen
                )
            }
        }
    }

    override suspend fun defineDoctorSlot(slot: DoctorDaySlotConfig) {
        _doctorSlots.update { current ->
            val other = current.filterNot { it.id == slot.id }
            listOf(slot) + other
        }

        scope.launch {
            dao.upsertDoctorSlot(slot.toEntity())

            val outboxId = "outbox_slot_${slot.id}"
            dao.insertOutboxRecord(
                OutboxEntity(
                    id = outboxId,
                    actionType = "DOCTOR_SLOT",
                    entityId = slot.id,
                    payloadJson = gson.toJson(slot)
                )
            )

            try {
                firestoreDataSource.uploadDoctorSlot(slot)
                dao.deleteOutboxRecord(outboxId)
            } catch (e: Exception) {
                syncManager.triggerImmediateSync()
            }
        }
    }

    override suspend fun checkInAppointment(appointmentId: String): QueueEntry {
        val appointment = _appointments.value.find { it.id == appointmentId }
            ?: dao.getAllAppointments().firstOrNull()?.find { it.id == appointmentId }?.let {
                Appointment(
                    it.id, it.patientId, it.patientName, it.doctorId, it.doctorName,
                    it.doctorSpecialty, it.dateFormatted, it.timeSlot, it.status, it.proposedBy, it.outcomeNotes
                )
            }
            ?: throw IllegalArgumentException("Appointment with ID $appointmentId not found.")

        val date = getTodayFormatted()
        val existing = _queueEntries.value.find {
            it.appointmentId == appointmentId && it.dateFormatted == date && it.status != QueueEntryStatus.CANCELLED
        }
        if (existing != null) return existing

        var allocatedToken: Int
        var isProvisional = false

        try {
            allocatedToken = firestoreDataSource.allocateNextToken(appointment.doctorId, date)
        } catch (e: Exception) {
            // Offline fallback: generate local placeholder token
            allocatedToken = -(System.currentTimeMillis() % 10000).toInt()
            isProvisional = true
        }

        val entry = QueueEntry(
            id = "qe_${System.currentTimeMillis()}_${appointment.patientId}",
            doctorId = appointment.doctorId,
            doctorName = appointment.doctorName,
            dateFormatted = date,
            tokenNumber = allocatedToken,
            provisionalToken = isProvisional,
            appointmentId = appointment.id,
            patientId = appointment.patientId,
            patientName = appointment.patientName,
            source = QueueEntrySource.SCHEDULED,
            status = QueueEntryStatus.WAITING,
            priorityFlag = false,
            checkedInAt = System.currentTimeMillis(),
            isPendingSync = isProvisional
        )

        _queueEntries.update { listOf(entry) + it }

        scope.launch {
            dao.upsertQueueEntry(entry.toEntity())

            if (isProvisional) {
                val outboxId = "outbox_queue_${entry.id}"
                dao.insertOutboxRecord(
                    OutboxEntity(
                        id = outboxId,
                        actionType = "QUEUE_ENTRY",
                        entityId = entry.id,
                        payloadJson = gson.toJson(entry)
                    )
                )
                syncManager.triggerImmediateSync()
            } else {
                try {
                    firestoreDataSource.uploadQueueEntry(entry)
                } catch (e: Exception) {
                    val outboxId = "outbox_queue_${entry.id}"
                    dao.insertOutboxRecord(
                        OutboxEntity(
                            id = outboxId,
                            actionType = "QUEUE_ENTRY",
                            entityId = entry.id,
                            payloadJson = gson.toJson(entry)
                        )
                    )
                    syncManager.triggerImmediateSync()
                }
            }
        }

        return entry
    }

    override suspend fun joinWalkInQueue(
        doctorId: String,
        patientId: String,
        patientName: String
    ): QueueEntry {
        val doctor = _doctors.value.find { it.id == doctorId }
            ?: dao.getDoctorById(doctorId).firstOrNull()?.let {
                Doctor(it.id, it.name, it.specialty, it.qualification, it.hospitalName, it.distanceKm, it.phone, it.availableDays)
            }
            ?: throw IllegalArgumentException("Doctor with ID $doctorId not found.")

        val date = getTodayFormatted()
        val existing = _queueEntries.value.find {
            it.doctorId == doctorId && it.patientId == patientId && it.dateFormatted == date && it.status != QueueEntryStatus.CANCELLED
        }
        if (existing != null) return existing

        var allocatedToken: Int
        var isProvisional = false

        try {
            allocatedToken = firestoreDataSource.allocateNextToken(doctorId, date)
        } catch (e: Exception) {
            allocatedToken = -(System.currentTimeMillis() % 10000).toInt()
            isProvisional = true
        }

        val entry = QueueEntry(
            id = "qe_${System.currentTimeMillis()}_$patientId",
            doctorId = doctorId,
            doctorName = doctor.name,
            dateFormatted = date,
            tokenNumber = allocatedToken,
            provisionalToken = isProvisional,
            appointmentId = null,
            patientId = patientId,
            patientName = patientName,
            source = QueueEntrySource.WALK_IN,
            status = QueueEntryStatus.WAITING,
            priorityFlag = false,
            checkedInAt = System.currentTimeMillis(),
            isPendingSync = isProvisional
        )

        _queueEntries.update { listOf(entry) + it }

        scope.launch {
            dao.upsertQueueEntry(entry.toEntity())

            val outboxId = "outbox_queue_${entry.id}"
            dao.insertOutboxRecord(
                OutboxEntity(
                    id = outboxId,
                    actionType = "QUEUE_ENTRY",
                    entityId = entry.id,
                    payloadJson = gson.toJson(entry)
                )
            )

            if (!isProvisional) {
                try {
                    firestoreDataSource.uploadQueueEntry(entry)
                    dao.deleteOutboxRecord(outboxId)
                } catch (e: Exception) {
                    syncManager.triggerImmediateSync()
                }
            } else {
                syncManager.triggerImmediateSync()
            }
        }

        return entry
    }

    override suspend fun callNext(doctorId: String, date: String) {
        val currentEntries = _queueEntries.value.filter { it.doctorId == doctorId && it.dateFormatted == date }
        val sortedWaiting = QueueEtaCalculator.sortWaitingEntries(currentEntries)

        if (sortedWaiting.isEmpty()) return

        val nextEntry = sortedWaiting.first()
        val updated = nextEntry.copy(
            status = QueueEntryStatus.CALLED,
            calledAt = System.currentTimeMillis()
        )

        updateQueueEntryInternal(updated)
    }

    override suspend fun startConsultation(entryId: String) {
        val entry = _queueEntries.value.find { it.id == entryId } ?: return

        // Invariant check: ensure no other entry for the same doctor is currently IN_CONSULTATION
        val activeConsultation = _queueEntries.value.find {
            it.doctorId == entry.doctorId && it.dateFormatted == entry.dateFormatted &&
                    it.id != entryId && it.status == QueueEntryStatus.IN_CONSULTATION
        }

        if (activeConsultation != null) {
            throw IllegalStateException("Another consultation with Token #${activeConsultation.tokenNumber} is already in progress for Dr. ${entry.doctorName}.")
        }

        val updated = entry.copy(
            status = QueueEntryStatus.IN_CONSULTATION,
            consultationStartedAt = System.currentTimeMillis()
        )

        updateQueueEntryInternal(updated)
    }

    override suspend fun completeConsultation(entryId: String, outcomeNotes: String?) {
        val entry = _queueEntries.value.find { it.id == entryId } ?: return
        val updated = entry.copy(
            status = QueueEntryStatus.COMPLETED,
            completedAt = System.currentTimeMillis(),
            outcomeNotes = outcomeNotes
        )
        updateQueueEntryInternal(updated)
    }

    override suspend fun markNoShow(entryId: String) {
        val entry = _queueEntries.value.find { it.id == entryId } ?: return
        val updated = entry.copy(
            status = QueueEntryStatus.NO_SHOW
        )
        updateQueueEntryInternal(updated)
    }

    override suspend fun skipEntry(entryId: String) {
        val entry = _queueEntries.value.find { it.id == entryId } ?: return
        // If skipped before, mark as NO_SHOW; otherwise re-enter at the back of the queue
        val isSecondSkip = entry.outcomeNotes?.contains("SKIPPED_ONCE") == true
        val updated = if (isSecondSkip) {
            entry.copy(status = QueueEntryStatus.NO_SHOW, outcomeNotes = "Marked No-Show after 2 skips")
        } else {
            entry.copy(
                status = QueueEntryStatus.WAITING,
                checkedInAt = System.currentTimeMillis(), // moves to back of current tier
                calledAt = null,
                priorityFlag = false,
                outcomeNotes = "SKIPPED_ONCE"
            )
        }
        updateQueueEntryInternal(updated)
    }

    override suspend fun prioritizeEntry(entryId: String) {
        val entry = _queueEntries.value.find { it.id == entryId } ?: return
        val updated = entry.copy(
            priorityFlag = !entry.priorityFlag
        )
        updateQueueEntryInternal(updated)
    }

    override suspend fun cancelQueueEntry(entryId: String) {
        val entry = _queueEntries.value.find { it.id == entryId } ?: return
        val updated = entry.copy(
            status = QueueEntryStatus.CANCELLED
        )
        updateQueueEntryInternal(updated)
    }

    private fun updateQueueEntryInternal(updated: QueueEntry) {
        _queueEntries.update { current ->
            current.map { if (it.id == updated.id) updated else it }
        }

        scope.launch {
            dao.upsertQueueEntry(updated.toEntity())

            val outboxId = "outbox_qe_status_${updated.id}"
            dao.insertOutboxRecord(
                OutboxEntity(
                    id = outboxId,
                    actionType = "QUEUE_ENTRY",
                    entityId = updated.id,
                    payloadJson = gson.toJson(updated)
                )
            )

            try {
                firestoreDataSource.uploadQueueEntry(updated)
                dao.deleteOutboxRecord(outboxId)
            } catch (e: Exception) {
                syncManager.triggerImmediateSync()
            }
        }
    }

    // --- Entity / Model Mappers ---

    private fun QueueEntry.toEntity() = QueueEntryEntity(
        id = id,
        doctorId = doctorId,
        doctorName = doctorName,
        dateFormatted = dateFormatted,
        tokenNumber = tokenNumber,
        provisionalToken = provisionalToken,
        appointmentId = appointmentId,
        patientId = patientId,
        patientName = patientName,
        source = source,
        status = status,
        priorityFlag = priorityFlag,
        checkedInAt = checkedInAt,
        calledAt = calledAt,
        consultationStartedAt = consultationStartedAt,
        completedAt = completedAt,
        outcomeNotes = outcomeNotes,
        isPendingSync = isPendingSync
    )

    private fun QueueEntryEntity.toModel() = QueueEntry(
        id = id,
        doctorId = doctorId,
        doctorName = doctorName,
        dateFormatted = dateFormatted,
        tokenNumber = tokenNumber,
        provisionalToken = provisionalToken,
        appointmentId = appointmentId,
        patientId = patientId,
        patientName = patientName,
        source = source,
        status = status,
        priorityFlag = priorityFlag,
        checkedInAt = checkedInAt,
        calledAt = calledAt,
        consultationStartedAt = consultationStartedAt,
        completedAt = completedAt,
        outcomeNotes = outcomeNotes,
        isPendingSync = isPendingSync
    )

    private fun DoctorDaySlotConfig.toEntity() = DoctorDaySlotEntity(
        id = id,
        doctorId = doctorId,
        dateFormatted = dateFormatted,
        startTime = startTime,
        endTime = endTime,
        capacity = capacity,
        isWalkInOpen = isWalkInOpen
    )

    private fun DoctorDaySlotEntity.toModel() = DoctorDaySlotConfig(
        id = id,
        doctorId = doctorId,
        dateFormatted = dateFormatted,
        startTime = startTime,
        endTime = endTime,
        capacity = capacity,
        isWalkInOpen = isWalkInOpen
    )
}