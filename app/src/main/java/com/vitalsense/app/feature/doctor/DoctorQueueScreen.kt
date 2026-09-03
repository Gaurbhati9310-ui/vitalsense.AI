package com.vitalsense.app.feature.doctor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitalsense.app.R
import com.vitalsense.app.core.data.model.*
import com.vitalsense.app.core.data.util.QueueEtaCalculator
import com.vitalsense.app.core.ui.components.*
import com.vitalsense.app.core.ui.theme.*
import com.vitalsense.app.feature.doctor.components.DoctorSlotConfigDialog
import com.vitalsense.app.feature.doctor.components.QueueEntryListItem
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DoctorQueueScreen(
    doctor: Doctor,
    queueEntries: List<QueueEntry>,
    slotConfig: DoctorDaySlotConfig?,
    patients: List<Patient> = emptyList(),
    onCallNext: () -> Unit,
    onStartConsultation: (String) -> Unit,
    onCompleteConsultation: (String, String?) -> Unit,
    onSkip: (String) -> Unit,
    onMarkNoShow: (String) -> Unit,
    onTogglePriority: (String) -> Unit,
    onAddWalkIn: (patientId: String, patientName: String) -> Unit,
    onSaveSlotConfig: (DoctorDaySlotConfig) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSlotConfigDialog by remember { mutableStateOf(false) }
    var showAddWalkInDialog by remember { mutableStateOf(false) }
    var completingEntryId by remember { mutableStateOf<String?>(null) }
    var outcomeNotesText by remember { mutableStateOf("") }

    val todayDateFormatted = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    val activeInConsultation = queueEntries.firstOrNull { it.status == QueueEntryStatus.IN_CONSULTATION }
    val activeCalled = queueEntries.firstOrNull { it.status == QueueEntryStatus.CALLED }
    val waitingEntries = QueueEtaCalculator.sortWaitingEntries(queueEntries)
    val completedEntries = queueEntries.filter {
        it.status == QueueEntryStatus.COMPLETED || it.status == QueueEntryStatus.NO_SHOW || it.status == QueueEntryStatus.CANCELLED
    }.sortedByDescending { it.completedAt ?: it.checkedInAt }

    val avgWaitSeconds = QueueEtaCalculator.averageConsultationSeconds(
        completedToday = queueEntries.filter { it.status == QueueEntryStatus.COMPLETED }
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(GlumeBackground)
            .padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = PaddingValues(top = Spacing.sm, bottom = Spacing.xxl)
    ) {
        // 1. Header Bar: Title, Back button & Slot Config
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GlumeTextPrimary)
                    }
                    Column {
                        Text(
                            text = "Live Patient Queue",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = GlumeTextPrimary
                        )
                        Text(
                            text = "${doctor.name} · ${doctor.specialty.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GlumeTextSecondary
                        )
                    }
                }

                IconButton(onClick = { showSlotConfigDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Configure Slots",
                        tint = GlumePrimaryPurple
                    )
                }
            }
        }

        // 2. Daily Slot Status Banner
        item {
            Surface(
                shape = PillShape,
                color = GlumeSurfaceCard,
                border = BorderStroke(1.dp, GlumeBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (slotConfig?.isWalkInOpen != false) MintGreen else CoralAlert,
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Text(
                            text = if (slotConfig?.isWalkInOpen != false) "Walk-ins Open" else "Walk-ins Closed",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = GlumeTextPrimary
                        )
                    }

                    Text(
                        text = "Capacity: ${queueEntries.size} / ${slotConfig?.capacity ?: 20}",
                        style = MaterialTheme.typography.labelSmall,
                        color = GlumeTextSecondary
                    )

                    Text(
                        text = "Avg: ${QueueEtaCalculator.formatEstimatedWait(avgWaitSeconds)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = GlumeTextSecondary
                    )
                }
            }
        }

        // 3. Hero Card: Serving Token & Active Consultation
        item {
            VitalSenseCard(
                backgroundColor = if (activeInConsultation != null) GlumeSurfaceElevated else GlumeSurfaceCard,
                border = BorderStroke(if (activeInConsultation != null) 1.5.dp else 1.dp, if (activeInConsultation != null) MintGreen else GlumeBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Text(
                        text = "NOW SERVING",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = if (activeInConsultation != null) MintGreen else GlumeTextSecondary
                    )

                    when {
                        activeInConsultation != null -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MintGreen.copy(alpha = 0.2f),
                                        border = BorderStroke(1.5.dp, MintGreen),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "#${activeInConsultation.tokenNumber}",
                                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                                color = MintGreen
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = activeInConsultation.patientName,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = GlumeTextPrimary
                                        )
                                        Text(
                                            text = "In consultation (${activeInConsultation.source.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = GlumeTextSecondary
                                        )
                                    }
                                }

                                Button(
                                    onClick = { completingEntryId = activeInConsultation.id },
                                    shape = PillShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MintGreen,
                                        contentColor = GlumeBackground
                                    )
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Complete", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        activeCalled != null -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = GlumePrimaryPurple.copy(alpha = 0.2f),
                                        border = BorderStroke(1.5.dp, GlumePrimaryPurple),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "#${activeCalled.tokenNumber}",
                                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                                color = GlumePrimaryPurple
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = activeCalled.patientName,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = GlumeTextPrimary
                                        )
                                        Text(
                                            text = "Called — Patient walking in",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AmberWarning
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onStartConsultation(activeCalled.id) },
                                    shape = PillShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GlumePrimaryPurple,
                                        contentColor = GlumeTextPrimary
                                    )
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Start", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        else -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "No active consultation",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = GlumeTextSecondary
                                    )
                                    Text(
                                        text = "${waitingEntries.size} patients waiting in line",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GlumeTextSecondary
                                    )
                                }

                                Button(
                                    onClick = onCallNext,
                                    enabled = waitingEntries.isNotEmpty(),
                                    shape = PillShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GlumePrimaryPurple,
                                        contentColor = GlumeTextPrimary
                                    )
                                ) {
                                    Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Call Next", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Quick Action Row: Call Next + Add Walk-in
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                VitalSenseButton(
                    text = "Call Next Patient",
                    onClick = onCallNext,
                    enabled = waitingEntries.isNotEmpty() && activeInConsultation == null,
                    modifier = Modifier.weight(1f)
                )

                OutlinedButton(
                    onClick = { showAddWalkInDialog = true },
                    shape = PillShape,
                    border = BorderStroke(1.dp, GlumeBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GlumeTextPrimary),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Walk-in")
                }
            }
        }

        // 5. Waiting Queue List Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WAITING PATIENTS (${waitingEntries.size})",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = GlumeTextSecondary
                )

                if (waitingEntries.isNotEmpty()) {
                    Text(
                        text = "Prioritized first",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberWarning
                    )
                }
            }
        }

        if (waitingEntries.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GlumeSurfaceCard,
                    border = BorderStroke(1.dp, GlumeBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.lg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No patients currently in the waiting queue.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GlumeTextSecondary
                        )
                    }
                }
            }
        } else {
            items(waitingEntries, key = { it.id }) { entry ->
                QueueEntryListItem(
                    entry = entry,
                    isReadOnly = false,
                    onStartConsultation = onStartConsultation,
                    onCompleteConsultation = { completingEntryId = it },
                    onSkip = onSkip,
                    onMarkNoShow = onMarkNoShow,
                    onTogglePriority = onTogglePriority
                )
            }
        }

        // 6. Completed / Handled Section
        if (completedEntries.isNotEmpty()) {
            item {
                Text(
                    text = "COMPLETED TODAY (${completedEntries.size})",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = GlumeTextSecondary,
                    modifier = Modifier.padding(top = Spacing.md)
                )
            }

            items(completedEntries, key = { it.id }) { entry ->
                QueueEntryListItem(
                    entry = entry,
                    isReadOnly = true
                )
            }
        }
    }

    // --- Dialogs ---

    if (showSlotConfigDialog) {
        DoctorSlotConfigDialog(
            currentConfig = slotConfig,
            doctorId = doctor.id,
            dateFormatted = todayDateFormatted,
            onDismiss = { showSlotConfigDialog = false },
            onSaveConfig = onSaveSlotConfig
        )
    }

    if (showAddWalkInDialog) {
        var selectedPatient by remember { mutableStateOf<Patient?>(patients.firstOrNull()) }
        var manualName by remember { mutableStateOf("") }
        var isManualEntry by remember { mutableStateOf(false) }

        VitalSenseDialog(
            title = "Register Walk-in Patient",
            onDismissRequest = { showAddWalkInDialog = false }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    text = "Add a patient to today's live queue as a walk-in.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GlumeTextSecondary
                )

                if (!isManualEntry && patients.isNotEmpty()) {
                    Text(
                        text = "Select from registered patients:",
                        style = MaterialTheme.typography.labelMedium,
                        color = GlumeTextPrimary
                    )

                    patients.take(5).forEach { pat ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedPatient?.id == pat.id) GlumePrimaryPurple.copy(alpha = 0.2f) else GlumeSurfaceCard,
                            border = BorderStroke(1.dp, if (selectedPatient?.id == pat.id) GlumePrimaryPurple else GlumeBorder),
                            onClick = { selectedPatient = pat },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.sm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${pat.name} (${pat.villageName})",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = GlumeTextPrimary
                                )
                                Text(
                                    text = "Age ${pat.age}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GlumeTextSecondary
                                )
                            }
                        }
                    }

                    TextButton(onClick = { isManualEntry = true }) {
                        Text("Or type a new patient name...", color = GlumePrimaryPurple)
                    }
                } else {
                    VitalSenseTextField(
                        value = manualName,
                        onValueChange = { manualName = it },
                        label = "Patient Full Name",
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (patients.isNotEmpty()) {
                        TextButton(onClick = { isManualEntry = false }) {
                            Text("← Back to patient list", color = GlumePrimaryPurple)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    TextButton(
                        onClick = { showAddWalkInDialog = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = GlumeTextSecondary)
                    }

                    VitalSenseButton(
                        text = "Add to Queue",
                        onClick = {
                            if (isManualEntry && manualName.isNotBlank()) {
                                onAddWalkIn("walkin_${System.currentTimeMillis()}", manualName.trim())
                                showAddWalkInDialog = false
                            } else if (selectedPatient != null) {
                                onAddWalkIn(selectedPatient!!.id, selectedPatient!!.name)
                                showAddWalkInDialog = false
                            }
                        },
                        enabled = (isManualEntry && manualName.isNotBlank()) || (!isManualEntry && selectedPatient != null),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (completingEntryId != null) {
        VitalSenseDialog(
            title = "Complete Consultation",
            onDismissRequest = {
                completingEntryId = null
                outcomeNotesText = ""
            }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                VitalSenseTextField(
                    value = outcomeNotesText,
                    onValueChange = { outcomeNotesText = it },
                    label = "Consultation Outcome Notes (Optional)",
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    TextButton(
                        onClick = {
                            completingEntryId = null
                            outcomeNotesText = ""
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = GlumeTextSecondary)
                    }

                    VitalSenseButton(
                        text = "Mark Complete",
                        onClick = {
                            completingEntryId?.let { id ->
                                onCompleteConsultation(id, outcomeNotesText.takeIf { it.isNotBlank() })
                            }
                            completingEntryId = null
                            outcomeNotesText = ""
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
