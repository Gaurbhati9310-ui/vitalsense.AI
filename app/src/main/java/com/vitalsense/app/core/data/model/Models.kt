package com.vitalsense.app.core.data.model

data class Village(
    val id: String,
    val name: String,
    val district: String,
    val state: String,
    val population: Int,
    val latitude: Double,
    val longitude: Double,
    val activeCases: Int,
    val highRiskCount: Int
)

data class Patient(
    val id: String,
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

data class AshaWorker(
    val id: String,
    val name: String,
    val ashaUniqueId: String,
    val phone: String,
    val assignedVillages: List<String>,
    val activePatientCount: Int,
    val alertCount: Int
)

data class Doctor(
    val id: String,
    val name: String,
    val specialty: DoctorSpecialty,
    val qualification: String,
    val hospitalName: String,
    val distanceKm: Double,
    val phone: String,
    val availableDays: String
)

data class ConditionRecord(
    val id: String,
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

data class PrescribedMedicine(
    val name: String,
    val dosage: String,
    val frequency: String,
    val duration: String,
    val quantity: Int
)

data class Prescription(
    val id: String,
    val caseId: String? = null,
    val patientId: String,
    val patientName: String,
    val doctorId: String,
    val doctorName: String,
    val doctorSpecialty: String,
    val timestamp: Long,
    val dateFormatted: String,
    val medicines: List<PrescribedMedicine>,
    val instructions: String,
    val isOcrExtracted: Boolean = false
)

data class Appointment(
    val id: String,
    val patientId: String,
    val patientName: String,
    val doctorId: String,
    val doctorName: String,
    val doctorSpecialty: String,
    val dateFormatted: String,
    val timeSlot: String,
    val status: String, // "Confirmed", "Pending", "Declined", "Completed"
    val proposedBy: UserRole,
    val outcomeNotes: String? = null
)

data class BroadcastNotice(
    val id: String,
    val senderRole: UserRole,
    val senderName: String,
    val targetRole: String,
    val targetVillage: String?,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isUrgent: Boolean = false
)

data class DispensaryItem(
    val id: String,
    val medicineName: String,
    val category: String,
    val availableQuantity: Int,
    val unit: String,
    val reorderThreshold: Int
) {
    val isLowStock: Boolean
        get() = availableQuantity <= reorderThreshold
}

data class GovernmentScheme(
    val id: String,
    val title: String,
    val category: String,
    val targetBeneficiary: String,
    val benefitsSummary: String,
    val eligibility: String,
    val applicationUrl: String = ""
)

// --- Live Queue & Appointment Models ---

enum class QueueEntrySource {
    SCHEDULED,
    WALK_IN
}

enum class QueueEntryStatus {
    WAITING,
    CALLED,
    IN_CONSULTATION,
    COMPLETED,
    NO_SHOW,
    SKIPPED,
    CANCELLED
}

data class DoctorDaySlotConfig(
    val id: String,
    val doctorId: String,
    val dateFormatted: String,       // "yyyy-MM-dd"
    val startTime: String,           // "HH:mm"
    val endTime: String,             // "HH:mm"
    val capacity: Int,               // max scheduled bookings in this block
    val isWalkInOpen: Boolean = true // whether walk-ins can join today's queue
)

data class QueueEntry(
    val id: String,
    val doctorId: String,
    val doctorName: String,
    val dateFormatted: String,
    val tokenNumber: Int,
    val provisionalToken: Boolean = false, // true until an offline check-in is reconciled with the server
    val appointmentId: String? = null,     // null for walk-ins
    val patientId: String,
    val patientName: String,
    val source: QueueEntrySource,
    val status: QueueEntryStatus,
    val priorityFlag: Boolean = false,     // doctor-set manual priority bump
    val checkedInAt: Long,
    val calledAt: Long? = null,
    val consultationStartedAt: Long? = null,
    val completedAt: Long? = null,
    val outcomeNotes: String? = null,
    val isPendingSync: Boolean = false
)

data class DoctorQueueSummary(
    val doctorId: String,
    val doctorName: String,
    val dateFormatted: String,
    val waitingCount: Int,
    val currentToken: Int?,
    val avgWaitSeconds: Long,
    val isQueueOpen: Boolean
)

