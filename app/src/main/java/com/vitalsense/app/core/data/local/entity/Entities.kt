package com.vitalsense.app.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vitalsense.app.core.data.model.*

@Entity(tableName = "villages")
data class VillageEntity(
    @PrimaryKey val id: String,
    val name: String,
    val district: String,
    val state: String,
    val population: Int,
    val latitude: Double,
    val longitude: Double,
    val activeCases: Int,
    val highRiskCount: Int
)

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey val id: String,
    val name: String,
    val age: Int,
    val gender: String,
    val phone: String,
    val villageId: String,
    val villageName: String,
    val ashaWorkerId: String,
    val ashaWorkerName: String,
    val currentRiskLevel: SeverityLevel,
    val lastCondition: String,
    val lastVisitDate: String,
    val nextAppointmentDate: String?,
    val emergencyContact: String,
    val profilePhotoUrl: String? = null
)

@Entity(tableName = "asha_workers")
data class AshaWorkerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val ashaUniqueId: String,
    val phone: String,
    val assignedVillagesJson: String,
    val activePatientCount: Int,
    val alertCount: Int
)

@Entity(tableName = "doctors")
data class DoctorEntity(
    @PrimaryKey val id: String,
    val name: String,
    val specialty: DoctorSpecialty,
    val qualification: String,
    val hospitalName: String,
    val distanceKm: Double,
    val phone: String,
    val availableDays: String
)

@Entity(tableName = "condition_records")
data class ConditionRecordEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val patientName: String,
    val villageId: String,
    val villageName: String,
    val category: ConditionCategory,
    val severity: SeverityLevel,
    val requestedDoctorType: DoctorSpecialty,
    val notes: String,
    val timestamp: Long,
    val ashaProxyLogged: Boolean = false,
    val status: CaseStatus = CaseStatus.PENDING_REVIEW,
    val assignedDoctorId: String? = null,
    val assignedDoctorName: String? = null,
    val doctorResponse: String? = null,
    val doctorResponseTimestamp: Long? = null,
    val doctorResponseDoctorName: String? = null,
    val privateDoctorNotes: String? = null,
    val referredByDoctorId: String? = null,
    val referredByDoctorName: String? = null,
    val referralNotes: String? = null,
    val isPendingSync: Boolean = false
)

@Entity(tableName = "prescriptions")
data class PrescriptionEntity(
    @PrimaryKey val id: String,
    val caseId: String? = null,
    val patientId: String,
    val patientName: String,
    val doctorId: String,
    val doctorName: String,
    val doctorSpecialty: String,
    val timestamp: Long,
    val dateFormatted: String,
    val medicinesJson: String,
    val instructions: String,
    val isOcrExtracted: Boolean = false
)

@Entity(tableName = "appointments")
data class AppointmentEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val patientName: String,
    val doctorId: String,
    val doctorName: String,
    val doctorSpecialty: String,
    val dateFormatted: String,
    val timeSlot: String,
    val status: String,
    val proposedBy: UserRole,
    val outcomeNotes: String? = null
)

@Entity(tableName = "broadcast_notices")
data class BroadcastNoticeEntity(
    @PrimaryKey val id: String,
    val senderRole: UserRole,
    val senderName: String,
    val targetRole: String,
    val targetVillage: String?,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isUrgent: Boolean = false
)

@Entity(tableName = "dispensary_stock")
data class DispensaryEntity(
    @PrimaryKey val id: String,
    val medicineName: String,
    val category: String,
    val availableQuantity: Int,
    val unit: String,
    val reorderThreshold: Int
)

@Entity(tableName = "government_schemes")
data class GovernmentSchemeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val targetBeneficiary: String,
    val benefitsSummary: String,
    val eligibility: String,
    val applicationUrl: String = ""
)

@Entity(tableName = "doctor_day_slots")
data class DoctorDaySlotEntity(
    @PrimaryKey val id: String,
    val doctorId: String,
    val dateFormatted: String,
    val startTime: String,
    val endTime: String,
    val capacity: Int,
    val isWalkInOpen: Boolean
)

@Entity(tableName = "queue_entries")
data class QueueEntryEntity(
    @PrimaryKey val id: String,
    val doctorId: String,
    val doctorName: String,
    val dateFormatted: String,
    val tokenNumber: Int,
    val provisionalToken: Boolean,
    val appointmentId: String?,
    val patientId: String,
    val patientName: String,
    val source: QueueEntrySource,
    val status: QueueEntryStatus,
    val priorityFlag: Boolean,
    val checkedInAt: Long,
    val calledAt: Long?,
    val consultationStartedAt: Long?,
    val completedAt: Long?,
    val outcomeNotes: String?,
    val isPendingSync: Boolean
)

