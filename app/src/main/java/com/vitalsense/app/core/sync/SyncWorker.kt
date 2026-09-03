package com.vitalsense.app.core.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.vitalsense.app.core.data.local.VitalSenseDatabase
import com.vitalsense.app.core.data.model.*
import com.vitalsense.app.core.data.remote.FirestoreDataSource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background WorkManager worker that flushes durable outbox records to Cloud Firestore.
 * Implements the offline-first outbox pattern defined in Section 3 of System Design.
 */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val TAG = "VitalSenseSyncWorker"
    private val gson = Gson()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "🔄 VitalSense SyncWorker started. Flushing pending offline outbox records...")
        val database = VitalSenseDatabase.getDatabase(applicationContext)
        val dao = database.vitalSenseDao()
        val firestore = FirebaseFirestore.getInstance()
        val firestoreDataSource = FirestoreDataSource(firestore)

        try {
            val pendingRecords = dao.getPendingOutboxRecords()
            Log.d(TAG, "Found ${pendingRecords.size} pending outbox items to synchronize.")

            for (record in pendingRecords) {
                try {
                    when (record.actionType) {
                        "CONDITION_RECORD" -> {
                            val condition = gson.fromJson(record.payloadJson, ConditionRecord::class.java)
                            firestoreDataSource.uploadConditionRecord(condition)
                            dao.deleteOutboxRecord(record.id)
                            Log.d(TAG, "✓ Flushed condition record: ${record.entityId}")
                        }
                        "PRESCRIPTION" -> {
                            val rx = gson.fromJson(record.payloadJson, Prescription::class.java)
                            firestoreDataSource.uploadPrescription(rx)
                            dao.deleteOutboxRecord(record.id)
                            Log.d(TAG, "✓ Flushed prescription: ${record.entityId}")
                        }
                        "APPOINTMENT" -> {
                            val appt = gson.fromJson(record.payloadJson, Appointment::class.java)
                            firestoreDataSource.uploadAppointment(appt)
                            dao.deleteOutboxRecord(record.id)
                            Log.d(TAG, "✓ Flushed appointment: ${record.entityId}")
                        }
                        "BROADCAST_NOTICE" -> {
                            val notice = gson.fromJson(record.payloadJson, BroadcastNotice::class.java)
                            firestoreDataSource.uploadNotice(notice)
                            dao.deleteOutboxRecord(record.id)
                            Log.d(TAG, "✓ Flushed broadcast notice: ${record.entityId}")
                        }
                        "PATIENT" -> {
                            val patient = gson.fromJson(record.payloadJson, Patient::class.java)
                            firestoreDataSource.uploadPatient(patient)
                            dao.deleteOutboxRecord(record.id)
                            Log.d(TAG, "✓ Flushed patient record: ${record.entityId}")
                        }
                        "DOCTOR_SLOT" -> {
                            val slot = gson.fromJson(record.payloadJson, DoctorDaySlotConfig::class.java)
                            firestoreDataSource.uploadDoctorSlot(slot)
                            dao.deleteOutboxRecord(record.id)
                            Log.d(TAG, "✓ Flushed doctor slot: ${record.entityId}")
                        }
                        "QUEUE_ENTRY" -> {
                            var entry = gson.fromJson(record.payloadJson, QueueEntry::class.java)
                            if (entry.provisionalToken || entry.tokenNumber <= 0) {
                                try {
                                    val realToken = firestoreDataSource.allocateNextToken(entry.doctorId, entry.dateFormatted)
                                    entry = entry.copy(tokenNumber = realToken, provisionalToken = false, isPendingSync = false)
                                    // Update Room
                                    val entity = dao.getQueueEntryById(entry.id)
                                    if (entity != null) {
                                        dao.upsertQueueEntry(entity.copy(tokenNumber = realToken, provisionalToken = false, isPendingSync = false))
                                    }
                                } catch (e: Exception) {
                                    // Could not allocate real token yet, will retry
                                }
                            }
                            firestoreDataSource.uploadQueueEntry(entry)
                            dao.deleteOutboxRecord(record.id)
                            Log.d(TAG, "✓ Flushed queue entry: ${record.entityId} (Token #${entry.tokenNumber})")
                        }
                        else -> {
                            dao.deleteOutboxRecord(record.id)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Sync retry for record ${record.id}: ${e.message}")
                    // Will retry on next network sync pass
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "❌ SyncWorker execution failure: ${e.message}", e)
            Result.retry()
        }
    }
}
