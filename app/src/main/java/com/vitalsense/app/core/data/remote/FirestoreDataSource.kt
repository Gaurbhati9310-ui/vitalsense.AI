package com.vitalsense.app.core.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vitalsense.app.core.data.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "VitalSenseFirebase"

@Singleton
class FirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    // Collection references
    private val patientsCollection = firestore.collection("patients")
    private val conditionsCollection = firestore.collection("condition_records")
    private val prescriptionsCollection = firestore.collection("prescriptions")
    private val appointmentsCollection = firestore.collection("appointments")
    private val noticesCollection = firestore.collection("broadcast_notices")
    private val villagesCollection = firestore.collection("villages")
    private val queueEntriesCollection = firestore.collection("queue_entries")
    private val doctorSlotsCollection = firestore.collection("doctor_day_slots")
    private val queueCountersCollection = firestore.collection("queue_counters")


    init {
        // Ensure an authenticated session for Firestore security rules
        try {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                auth.signInAnonymously().addOnSuccessListener {
                    Log.d(TAG, "FirebaseAuth: Anonymous sign-in success. UID=${it.user?.uid}")
                }.addOnFailureListener { e ->
                    Log.w(TAG, "FirebaseAuth: Anonymous sign-in failed (Rules in test mode will still work): ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth init error: ${e.message}")
        }
    }

    // --- PUSH OPERATIONS (Writes) ---

    suspend fun uploadConditionRecord(record: ConditionRecord) {
        try {
            val data = hashMapOf(
                "id" to record.id,
                "patientId" to record.patientId,
                "patientName" to record.patientName,
                "villageId" to record.villageId,
                "villageName" to record.villageName,
                "category" to record.category.name,
                "severity" to record.severity.name,
                "requestedDoctorType" to record.requestedDoctorType.name,
                "notes" to record.notes,
                "timestamp" to record.timestamp,
                "ashaProxyLogged" to record.ashaProxyLogged,
                "status" to record.status.name,
                "assignedDoctorId" to (record.assignedDoctorId ?: ""),
                "assignedDoctorName" to (record.assignedDoctorName ?: ""),
                "doctorResponse" to (record.doctorResponse ?: ""),
                "doctorResponseTimestamp" to (record.doctorResponseTimestamp ?: 0L),
                "doctorResponseDoctorName" to (record.doctorResponseDoctorName ?: ""),
                "privateDoctorNotes" to (record.privateDoctorNotes ?: ""),
                "referredByDoctorId" to (record.referredByDoctorId ?: ""),
                "referredByDoctorName" to (record.referredByDoctorName ?: ""),
                "referralNotes" to (record.referralNotes ?: ""),
                "isPendingSync" to false
            )
            conditionsCollection.document(record.id).set(data).await()
            Log.d(TAG, "✅ Successfully uploaded condition_record: ${record.id} (${record.patientName})")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to upload condition_record: ${e.message}", e)
            throw e
        }
    }

    suspend fun uploadPrescription(prescription: Prescription) {
        try {
            val data = hashMapOf(
                "id" to prescription.id,
                "caseId" to (prescription.caseId ?: ""),
                "patientId" to prescription.patientId,
                "patientName" to prescription.patientName,
                "doctorId" to prescription.doctorId,
                "doctorName" to prescription.doctorName,
                "doctorSpecialty" to prescription.doctorSpecialty,
                "timestamp" to prescription.timestamp,
                "dateFormatted" to prescription.dateFormatted,
                "medicines" to prescription.medicines.map { med ->
                    hashMapOf(
                        "name" to med.name,
                        "dosage" to med.dosage,
                        "frequency" to med.frequency,
                        "duration" to med.duration,
                        "quantity" to med.quantity
                    )
                },
                "instructions" to prescription.instructions,
                "isOcrExtracted" to prescription.isOcrExtracted
            )
            prescriptionsCollection.document(prescription.id).set(data).await()
            Log.d(TAG, "✅ Successfully uploaded prescription: ${prescription.id}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to upload prescription: ${e.message}", e)
            throw e
        }
    }

    suspend fun uploadAppointment(appointment: Appointment) {
        try {
            val data = hashMapOf(
                "id" to appointment.id,
                "patientId" to appointment.patientId,
                "patientName" to appointment.patientName,
                "doctorId" to appointment.doctorId,
                "doctorName" to appointment.doctorName,
                "doctorSpecialty" to appointment.doctorSpecialty,
                "dateFormatted" to appointment.dateFormatted,
                "timeSlot" to appointment.timeSlot,
                "status" to appointment.status,
                "proposedBy" to appointment.proposedBy.name,
                "outcomeNotes" to (appointment.outcomeNotes ?: "")
            )
            appointmentsCollection.document(appointment.id).set(data).await()
            Log.d(TAG, "✅ Successfully uploaded appointment: ${appointment.id}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to upload appointment: ${e.message}", e)
            throw e
        }
    }

    suspend fun uploadPatient(patient: Patient) {
        try {
            val data = hashMapOf(
                "id" to patient.id,
                "name" to patient.name,
                "age" to patient.age,
                "gender" to patient.gender,
                "phone" to patient.phone,
                "villageId" to patient.villageId,
                "villageName" to patient.villageName,
                "ashaWorkerId" to patient.ashaWorkerId,
                "ashaWorkerName" to patient.ashaWorkerName,
                "currentRiskLevel" to patient.currentRiskLevel.name,
                "lastCondition" to patient.lastCondition,
                "lastVisitDate" to patient.lastVisitDate,
                "nextAppointmentDate" to patient.nextAppointmentDate,
                "emergencyContact" to patient.emergencyContact,
                "profilePhotoUrl" to patient.profilePhotoUrl
            )
            patientsCollection.document(patient.id).set(data).await()
            Log.d(TAG, "✅ Successfully uploaded patient: ${patient.id} (${patient.name})")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to upload patient: ${e.message}", e)
            throw e
        }
    }

    suspend fun uploadNotice(notice: BroadcastNotice) {
        try {
            val data = hashMapOf(
                "id" to notice.id,
                "senderRole" to notice.senderRole.name,
                "senderName" to notice.senderName,
                "targetRole" to notice.targetRole,
                "targetVillage" to notice.targetVillage,
                "title" to notice.title,
                "message" to notice.message,
                "timestamp" to notice.timestamp,
                "isUrgent" to notice.isUrgent
            )
            noticesCollection.document(notice.id).set(data).await()
            Log.d(TAG, "✅ Successfully uploaded broadcast_notice: ${notice.id} - ${notice.title}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to upload broadcast_notice: ${e.message}", e)
            throw e
        }
    }

    // --- REAL-TIME LISTENERS (Reads) ---

    fun getConditionRecordsStream(): Flow<List<ConditionRecord>> = callbackFlow {
        val listener = conditionsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Condition records stream error: ${error.message}")
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        val statusStr = doc.getString("status") ?: CaseStatus.PENDING_REVIEW.name
                        val status = runCatching { CaseStatus.valueOf(statusStr) }.getOrDefault(CaseStatus.PENDING_REVIEW)
                        ConditionRecord(
                            id = doc.getString("id") ?: doc.id,
                            patientId = doc.getString("patientId") ?: "",
                            patientName = doc.getString("patientName") ?: "",
                            villageId = doc.getString("villageId") ?: "",
                            villageName = doc.getString("villageName") ?: "",
                            category = ConditionCategory.valueOf(doc.getString("category") ?: ConditionCategory.GENERAL_MEDICINE.name),
                            severity = SeverityLevel.valueOf(doc.getString("severity") ?: SeverityLevel.LOW.name),
                            requestedDoctorType = DoctorSpecialty.valueOf(doc.getString("requestedDoctorType") ?: DoctorSpecialty.GENERAL_PHYSICIAN.name),
                            notes = doc.getString("notes") ?: "",
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                            ashaProxyLogged = doc.getBoolean("ashaProxyLogged") ?: false,
                            status = status,
                            assignedDoctorId = doc.getString("assignedDoctorId")?.takeIf { it.isNotBlank() },
                            assignedDoctorName = doc.getString("assignedDoctorName")?.takeIf { it.isNotBlank() },
                            doctorResponse = doc.getString("doctorResponse")?.takeIf { it.isNotBlank() },
                            doctorResponseTimestamp = doc.getLong("doctorResponseTimestamp")?.takeIf { it > 0 },
                            doctorResponseDoctorName = doc.getString("doctorResponseDoctorName")?.takeIf { it.isNotBlank() },
                            privateDoctorNotes = doc.getString("privateDoctorNotes")?.takeIf { it.isNotBlank() },
                            referredByDoctorId = doc.getString("referredByDoctorId")?.takeIf { it.isNotBlank() },
                            referredByDoctorName = doc.getString("referredByDoctorName")?.takeIf { it.isNotBlank() },
                            referralNotes = doc.getString("referralNotes")?.takeIf { it.isNotBlank() },
                            isPendingSync = false
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    fun getBroadcastNoticesStream(): Flow<List<BroadcastNotice>> = callbackFlow {
        val listener = noticesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Broadcast notices stream error: ${error.message}")
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        BroadcastNotice(
                            id = doc.getString("id") ?: doc.id,
                            senderRole = UserRole.valueOf(doc.getString("senderRole") ?: UserRole.ADMIN.name),
                            senderName = doc.getString("senderName") ?: "",
                            targetRole = doc.getString("targetRole") ?: "ALL",
                            targetVillage = doc.getString("targetVillage") ?: "All Villages",
                            title = doc.getString("title") ?: "",
                            message = doc.getString("message") ?: "",
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                            isUrgent = doc.getBoolean("isUrgent") ?: false
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    // --- LIVE QUEUE & DOCTOR SLOTS OPERATIONS ---

    /**
     * Atomically allocates the next incremental token for a doctor on a specific date.
     */
    suspend fun allocateNextToken(doctorId: String, dateFormatted: String): Int {
        val counterDocRef = queueCountersCollection.document("${doctorId}_$dateFormatted")
        return firestore.runTransaction { transaction ->
            val snapshot = transaction.get(counterDocRef)
            val currentToken = snapshot.getLong("nextToken")?.toInt() ?: 1
            transaction.set(counterDocRef, mapOf("nextToken" to (currentToken + 1)))
            currentToken
        }.await()
    }

    suspend fun uploadQueueEntry(entry: QueueEntry) {
        try {
            val data = hashMapOf(
                "id" to entry.id,
                "doctorId" to entry.doctorId,
                "doctorName" to entry.doctorName,
                "dateFormatted" to entry.dateFormatted,
                "tokenNumber" to entry.tokenNumber,
                "provisionalToken" to entry.provisionalToken,
                "appointmentId" to (entry.appointmentId ?: ""),
                "patientId" to entry.patientId,
                "patientName" to entry.patientName,
                "source" to entry.source.name,
                "status" to entry.status.name,
                "priorityFlag" to entry.priorityFlag,
                "checkedInAt" to entry.checkedInAt,
                "calledAt" to (entry.calledAt ?: 0L),
                "consultationStartedAt" to (entry.consultationStartedAt ?: 0L),
                "completedAt" to (entry.completedAt ?: 0L),
                "outcomeNotes" to (entry.outcomeNotes ?: ""),
                "isPendingSync" to false
            )
            queueEntriesCollection.document(entry.id).set(data).await()
            Log.d(TAG, "✅ Successfully uploaded queue_entry: ${entry.id} (Token #${entry.tokenNumber})")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to upload queue_entry: ${e.message}", e)
            throw e
        }
    }

    suspend fun uploadDoctorSlot(slot: DoctorDaySlotConfig) {
        try {
            val data = hashMapOf(
                "id" to slot.id,
                "doctorId" to slot.doctorId,
                "dateFormatted" to slot.dateFormatted,
                "startTime" to slot.startTime,
                "endTime" to slot.endTime,
                "capacity" to slot.capacity,
                "isWalkInOpen" to slot.isWalkInOpen
            )
            doctorSlotsCollection.document(slot.id).set(data).await()
            Log.d(TAG, "✅ Successfully uploaded doctor_day_slot: ${slot.id}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to upload doctor_day_slot: ${e.message}", e)
            throw e
        }
    }

    fun observeDoctorQueueStream(doctorId: String, date: String): Flow<List<QueueEntry>> = callbackFlow {
        val query = queueEntriesCollection
            .whereEqualTo("doctorId", doctorId)
            .whereEqualTo("dateFormatted", date)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Doctor queue stream error: ${error.message}")
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    mapDocToQueueEntry(doc)
                }.sortedBy { it.checkedInAt }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    fun observePatientQueueEntryStream(patientId: String, date: String): Flow<QueueEntry?> = callbackFlow {
        val query = queueEntriesCollection
            .whereEqualTo("patientId", patientId)
            .whereEqualTo("dateFormatted", date)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Patient queue entry stream error: ${error.message}")
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val entry = snapshot.documents.firstNotNullOfOrNull { doc ->
                    mapDocToQueueEntry(doc)
                }
                trySend(entry)
            }
        }
        awaitClose { listener.remove() }
    }

    fun observeDoctorSlotsStream(doctorId: String, date: String): Flow<List<DoctorDaySlotConfig>> = callbackFlow {
        val query = doctorSlotsCollection
            .whereEqualTo("doctorId", doctorId)
            .whereEqualTo("dateFormatted", date)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Doctor slots stream error: ${error.message}")
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        DoctorDaySlotConfig(
                            id = doc.getString("id") ?: doc.id,
                            doctorId = doc.getString("doctorId") ?: "",
                            dateFormatted = doc.getString("dateFormatted") ?: "",
                            startTime = doc.getString("startTime") ?: "09:00",
                            endTime = doc.getString("endTime") ?: "17:00",
                            capacity = doc.getLong("capacity")?.toInt() ?: 20,
                            isWalkInOpen = doc.getBoolean("isWalkInOpen") ?: true
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    fun observeAllQueueEntriesStream(date: String): Flow<List<QueueEntry>> = callbackFlow {
        val query = queueEntriesCollection.whereEqualTo("dateFormatted", date)
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "All queue entries stream error: ${error.message}")
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    mapDocToQueueEntry(doc)
                }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    private fun mapDocToQueueEntry(doc: com.google.firebase.firestore.DocumentSnapshot): QueueEntry? {
        return try {
            val sourceStr = doc.getString("source") ?: QueueEntrySource.SCHEDULED.name
            val statusStr = doc.getString("status") ?: QueueEntryStatus.WAITING.name
            val source = runCatching { QueueEntrySource.valueOf(sourceStr) }.getOrDefault(QueueEntrySource.SCHEDULED)
            val status = runCatching { QueueEntryStatus.valueOf(statusStr) }.getOrDefault(QueueEntryStatus.WAITING)

            QueueEntry(
                id = doc.getString("id") ?: doc.id,
                doctorId = doc.getString("doctorId") ?: "",
                doctorName = doc.getString("doctorName") ?: "",
                dateFormatted = doc.getString("dateFormatted") ?: "",
                tokenNumber = doc.getLong("tokenNumber")?.toInt() ?: 0,
                provisionalToken = doc.getBoolean("provisionalToken") ?: false,
                appointmentId = doc.getString("appointmentId")?.takeIf { it.isNotBlank() },
                patientId = doc.getString("patientId") ?: "",
                patientName = doc.getString("patientName") ?: "",
                source = source,
                status = status,
                priorityFlag = doc.getBoolean("priorityFlag") ?: false,
                checkedInAt = doc.getLong("checkedInAt") ?: System.currentTimeMillis(),
                calledAt = doc.getLong("calledAt")?.takeIf { it > 0 },
                consultationStartedAt = doc.getLong("consultationStartedAt")?.takeIf { it > 0 },
                completedAt = doc.getLong("completedAt")?.takeIf { it > 0 },
                outcomeNotes = doc.getString("outcomeNotes")?.takeIf { it.isNotBlank() },
                isPendingSync = false
            )
        } catch (e: Exception) {
            null
        }
    }
}