package com.vitalsense.app.feature.doctor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitalsense.app.core.data.model.*
import com.vitalsense.app.core.ui.components.*
import com.vitalsense.app.core.ui.theme.*
import com.vitalsense.app.feature.doctor.components.PatientHistoryDialog
import com.vitalsense.app.feature.doctor.components.ScheduleAppointmentDialog

@Composable
fun DoctorHomeScreen(
    doctor: Doctor,
    cases: List<ConditionRecord>,
    appointments: List<Appointment>,
    dispensaryStock: List<DispensaryItem>,
    patients: List<Patient> = emptyList(),
    notices: List<BroadcastNotice> = emptyList(),
    allConditions: List<ConditionRecord> = emptyList(),
    allPrescriptions: List<Prescription> = emptyList(),
    onSelectCase: (ConditionRecord) -> Unit,
    onOpenQueue: () -> Unit = {},
    onAcceptAppointment: (String) -> Unit = {},
    onDeclineAppointment: (String) -> Unit = {},
    onProposeAppointment: (patientId: String, patientName: String, date: String, timeSlot: String) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var showScheduleDialog by remember { mutableStateOf(false) }
    var selectedPatientForHistory by remember { mutableStateOf<Patient?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val pendingCases = cases.filter { it.status == CaseStatus.PENDING_REVIEW || it.status == CaseStatus.IN_PROGRESS }
    val respondedCases = cases.count { it.status == CaseStatus.RESPONDED || it.status == CaseStatus.CLOSED }
    val totalCases = cases.size
    val completionFraction = if (totalCases > 0) respondedCases.toFloat() / totalCases else 1.0f

    val severeCount = cases.count { it.severity == SeverityLevel.SEVERE || it.severity == SeverityLevel.HIGH }
    val lowStockCount = dispensaryStock.count { it.isLowStock }

    val emergencySosAlerts = notices.filter { it.isUrgent && it.senderRole == UserRole.PATIENT }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(GlumeBackground)
            .padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = PaddingValues(top = Spacing.sm, bottom = Spacing.xxl)
    ) {
        // 1. Glume Header Greeting: "Hi, Dr. Rajesh!"
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(
                    text = "Hi, ${doctor.name}!",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = GlumeTextPrimary
                )
                Text(
                    text = "${doctor.specialty.displayName} · ${doctor.hospitalName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GlumeTextSecondary
                )
            }
        }

        // Live Queue Action Banner
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = GlumePrimaryPurple.copy(alpha = 0.2f),
                border = BorderStroke(1.5.dp, GlumePrimaryPurple),
                onClick = onOpenQueue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = GlumePrimaryPurple,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "🔢",
                                    fontSize = 18.sp
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Today's Live Patient Queue",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = GlumeTextPrimary
                            )
                            Text(
                                text = "Call next, manage walk-ins & consult",
                                style = MaterialTheme.typography.bodySmall,
                                color = GlumeTextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = onOpenQueue,
                        shape = PillShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlumePrimaryPurple,
                            contentColor = GlumeTextPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Open Queue", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // 2. Glume Hero Completion Ring Card
        item {
            VitalSenseCard(
                backgroundColor = GlumeSurfaceCard,
                elevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
                    ) {
                        Text(
                            text = "CLINICAL TRIAGE TODAY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = GlumeTextSecondary
                        )
                        Text(
                            text = "$respondedCases of $totalCases Cases Responded",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = GlumeTextPrimary
                        )
                        Text(
                            text = if (pendingCases.isEmpty()) "All caught up! Excellent work." else "${pendingCases.size} cases awaiting review",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (pendingCases.isEmpty()) GlumeSuccessText else GlumePrimaryPurpleLight
                        )
                    }

                    GlumeProgressRing(
                        progressFraction = completionFraction,
                        size = 72.dp,
                        strokeWidth = 7.dp,
                        ringColor = GlumeSuccessMint,
                        trackColor = GlumeSurfaceElevated
                    )
                }
            }
        }

        // 3. Glume Stat Display Pattern (3-Column Compact Grid)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                // Pending Cases Stat Card
                GlumeStatCard(
                    label = strings.pendingCases,
                    value = "${pendingCases.size}",
                    icon = "⏳",
                    modifier = Modifier.weight(1f),
                    badgeText = if (pendingCases.isNotEmpty()) "Queue" else null,
                    badgeColor = GlumePrimaryPurple
                )

                // Critical Cases Stat Card
                GlumeStatCard(
                    label = strings.criticalCases,
                    value = "$severeCount",
                    icon = "🚨",
                    modifier = Modifier.weight(1f),
                    badgeText = if (severeCount > 0) "Urgent" else null,
                    badgeColor = if (severeCount > 0) GlumeAlertCoral else GlumeSuccessMint
                )

                // Scheduled Appointments Stat Card
                GlumeStatCard(
                    label = strings.scheduledAppts,
                    value = "${appointments.size}",
                    icon = "📅",
                    modifier = Modifier.weight(1f),
                    badgeText = "Today",
                    badgeColor = GlumeSuccessMint
                )
            }
        }

        // 4. Emergency Patient SOS Alerts (With Subtle Coral Glow)
        if (emergencySosAlerts.isNotEmpty()) {
            item {
                Text(
                    text = "${strings.emergencyPatientAlerts} (${emergencySosAlerts.size})",
                    style = MaterialTheme.typography.headlineMedium,
                    color = GlumeAlertCoral
                )
            }

            items(emergencySosAlerts) { sos ->
                VitalSenseCard(
                    backgroundColor = GlumeAlertContainer,
                    border = BorderStroke(1.dp, GlumeAlertCoral.copy(alpha = 0.5f))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sos.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = GlumeAlertText
                            )
                            Surface(
                                shape = PillShape,
                                color = GlumeAlertCoral
                            ) {
                                Text(
                                    text = strings.highPriority,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = GlumeTextPrimary
                                    ),
                                    modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = sos.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = GlumeTextPrimary
                        )
                        Text(
                            text = "${strings.village}: ${sos.targetVillage ?: "General"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GlumeTextSecondary
                        )
                    }
                }
            }
        }

        // 5. Specialist Triage Queue Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${strings.specialistQueue} (${cases.size})",
                    style = MaterialTheme.typography.headlineMedium,
                    color = GlumeTextPrimary
                )
                Text(
                    text = "${doctor.specialty.displayName} Stream",
                    style = MaterialTheme.typography.labelSmall,
                    color = GlumeTextSecondary
                )
            }
        }

        if (cases.isEmpty()) {
            item {
                VitalSenseCard {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Text(text = "🎉", fontSize = 32.sp)
                        Text(
                            text = strings.noPendingCases,
                            style = MaterialTheme.typography.bodyMedium,
                            color = GlumeTextSecondary
                        )
                    }
                }
            }
        } else {
            items(cases) { record ->
                val isSevere = record.severity == SeverityLevel.SEVERE || record.severity == SeverityLevel.HIGH
                val isMentalHealth = record.category == ConditionCategory.MENTAL_HEALTH || record.requestedDoctorType == DoctorSpecialty.PSYCHOLOGIST

                VitalSenseCard(
                    backgroundColor = if (isSevere) GlumeAlertContainer else GlumeSurfaceCard,
                    border = BorderStroke(1.dp, if (isSevere) GlumeAlertCoral.copy(alpha = 0.4f) else GlumeBorder)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = record.patientName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = GlumeTextPrimary
                                )
                                Text(
                                    text = "${strings.village}: ${record.villageName} · ${record.category.displayName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GlumeTextSecondary
                                )
                            }
                            SeverityBadge(severity = record.severity)
                        }

                        if (isMentalHealth) {
                            Surface(shape = PillShape, color = GlumePrimaryPurpleContainer) {
                                Row(
                                    modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)
                                ) {
                                    Text(text = "🧠", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        text = "Mental Health Referral",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = GlumePrimaryPurpleLight
                                    )
                                }
                            }
                        }

                        Text(
                            text = "${strings.symptoms} ${record.notes}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GlumeTextPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Status Pill
                            Surface(
                                shape = PillShape,
                                color = GlumeSurfaceElevated
                            ) {
                                Text(
                                    text = record.status.displayName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 4.dp),
                                    color = GlumeTextSecondary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                OutlinedButton(
                                    onClick = {
                                        selectedPatientForHistory = patients.find { it.id == record.patientId }
                                            ?: Patient(
                                                id = record.patientId,
                                                name = record.patientName,
                                                age = 30,
                                                gender = "Not specified",
                                                phone = "N/A",
                                                villageId = "vil_1",
                                                villageName = record.villageName,
                                                ashaWorkerId = "asha_1",
                                                ashaWorkerName = "ASHA Assigned",
                                                currentRiskLevel = record.severity,
                                                lastCondition = record.notes,
                                                lastVisitDate = "Recent",
                                                nextAppointmentDate = null,
                                                emergencyContact = "108"
                                            )
                                    },
                                    shape = PillShape,
                                    border = BorderStroke(1.dp, GlumeBorder),
                                    contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.xxs),
                                    modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                                ) {
                                    Text(text = strings.history, style = MaterialTheme.typography.labelSmall, color = GlumeTextPrimary)
                                }

                                Button(
                                    onClick = { onSelectCase(record) },
                                    shape = PillShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GlumePrimaryPurple,
                                        contentColor = GlumeTextPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xxs),
                                    modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                                ) {
                                    Text(text = strings.review, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Section: Upcoming Consultations
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${strings.upcomingConsultations} (${appointments.size})",
                    style = MaterialTheme.typography.headlineMedium,
                    color = GlumeTextPrimary
                )

                Button(
                    onClick = { showScheduleDialog = true },
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlumePrimaryPurple,
                        contentColor = GlumeTextPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.xxs),
                    modifier = Modifier.defaultMinSize(minHeight = 34.dp)
                ) {
                    Text(text = strings.proposeAppt, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (appointments.isEmpty()) {
            item {
                VitalSenseCard {
                    Text(
                        text = "No appointments scheduled.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GlumeTextSecondary
                    )
                }
            }
        } else {
            items(appointments) { appointment ->
                val isPending = appointment.status.contains("Pending", ignoreCase = true)
                VitalSenseCard(
                    backgroundColor = if (isPending) GlumeWarningContainer else GlumeSurfaceCard
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = appointment.patientName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = GlumeTextPrimary
                                )
                                Text(
                                    text = "${appointment.dateFormatted} at ${appointment.timeSlot}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GlumeTextSecondary
                                )
                            }
                            Surface(
                                shape = PillShape,
                                color = if (isPending) GlumeWarningContainer else GlumeSuccessContainer
                            ) {
                                Text(
                                    text = appointment.status,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp),
                                    color = if (isPending) GlumeWarningAmber else GlumeSuccessText
                                )
                            }
                        }

                        if (isPending) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.End),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { onDeclineAppointment(appointment.id) },
                                    shape = PillShape
                                ) {
                                    Text("Decline", color = GlumeAlertCoral, style = MaterialTheme.typography.labelSmall)
                                }
                                Button(
                                    onClick = { onAcceptAppointment(appointment.id) },
                                    shape = PillShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = GlumeSuccessMint),
                                    contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.xxs),
                                    modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                                ) {
                                    Text("Accept ✓", color = GlumeBackground, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. Section: Dispensary Stock Check
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.dispensaryStock,
                    style = MaterialTheme.typography.headlineMedium,
                    color = GlumeTextPrimary
                )
                if (lowStockCount > 0) {
                    Surface(shape = PillShape, color = GlumeAlertContainer) {
                        Text(
                            text = "$lowStockCount ${strings.lowStock}",
                            style = MaterialTheme.typography.labelSmall.copy(color = GlumeAlertCoral, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        items(dispensaryStock) { item ->
            VitalSenseCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = item.medicineName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = GlumeTextPrimary
                        )
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = GlumeTextSecondary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Text(
                            text = "${item.availableQuantity} ${item.unit}",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (item.isLowStock) GlumeAlertCoral else GlumeTextPrimary
                            )
                        )
                        if (item.isLowStock) {
                            Surface(shape = PillShape, color = GlumeAlertContainer) {
                                Text(
                                    text = "LOW",
                                    style = MaterialTheme.typography.labelSmall.copy(color = GlumeAlertCoral, fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 8. Patient Records Directory
        if (patients.isNotEmpty()) {
            item {
                Text(
                    text = strings.patientDirectory,
                    style = MaterialTheme.typography.headlineMedium,
                    color = GlumeTextPrimary
                )
            }

            item {
                VitalSenseTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = strings.searchPatient,
                    placeholder = strings.searchPlaceholder
                )
            }

            val filteredPatients = patients.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.villageName.contains(searchQuery, ignoreCase = true)
            }

            items(filteredPatients) { pat ->
                VitalSenseCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = pat.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = GlumeTextPrimary
                            )
                            Text(
                                text = "${pat.villageName} · Age: ${pat.age} (${pat.gender}) · ${strings.ashaAssigned}: ${pat.ashaWorkerName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = GlumeTextSecondary
                            )
                        }
                        Button(
                            onClick = { selectedPatientForHistory = pat },
                            shape = PillShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GlumePrimaryPurpleContainer,
                                contentColor = GlumePrimaryPurpleLight
                            ),
                            contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.xxs),
                            modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                        ) {
                            Text(strings.history, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }

    if (showScheduleDialog) {
        ScheduleAppointmentDialog(
            patients = patients,
            onDismiss = { showScheduleDialog = false },
            onPropose = { patientId, patientName, date, timeSlot ->
                onProposeAppointment(patientId, patientName, date, timeSlot)
            }
        )
    }

    selectedPatientForHistory?.let { patient ->
        PatientHistoryDialog(
            patient = patient,
            conditions = allConditions,
            prescriptions = allPrescriptions,
            appointments = appointments,
            onDismiss = { selectedPatientForHistory = null }
        )
    }
}
