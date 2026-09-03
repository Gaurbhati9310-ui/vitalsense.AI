package com.vitalsense.app.feature.patient

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitalsense.app.core.data.model.Appointment
import com.vitalsense.app.core.data.model.Doctor
import com.vitalsense.app.core.data.model.QueueEntry
import com.vitalsense.app.core.data.model.QueueEntryStatus
import com.vitalsense.app.core.ui.components.VitalSenseButton
import com.vitalsense.app.core.ui.components.VitalSenseCard
import com.vitalsense.app.core.ui.components.VitalSenseDialog
import com.vitalsense.app.core.ui.theme.*

@Composable
fun AppointmentsScreen(
    appointments: List<Appointment>,
    activeQueueEntry: QueueEntry? = null,
    doctors: List<Doctor> = emptyList(),
    onCheckIn: (appointmentId: String) -> Unit = {},
    onJoinWalkIn: (doctorId: String) -> Unit = {},
    onViewQueueStatus: () -> Unit = {},
    onRequestNew: () -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showWalkInDialog by remember { mutableStateOf(false) }
    var selectedDoctorForWalkIn by remember { mutableStateOf<Doctor?>(doctors.firstOrNull()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(GlumeBackground)
            .padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = PaddingValues(top = Spacing.sm, bottom = Spacing.xxl)
    ) {
        // 1. Top Header Bar
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
                    Text(
                        text = "My Appointments",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = GlumeTextPrimary
                    )
                }

                TextButton(onClick = { showWalkInDialog = true }) {
                    Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, modifier = Modifier.size(16.dp), tint = MintGreen)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Join Walk-in", color = MintGreen)
                }
            }
        }

        // 2. Active Queue Status Banner (if checked in today)
        if (activeQueueEntry != null && activeQueueEntry.status != QueueEntryStatus.CANCELLED && activeQueueEntry.status != QueueEntryStatus.COMPLETED) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GlumePrimaryPurple.copy(alpha = 0.2f),
                    border = BorderStroke(1.5.dp, GlumePrimaryPurple),
                    onClick = onViewQueueStatus,
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
                                        text = if (activeQueueEntry.provisionalToken) "~" else "#${activeQueueEntry.tokenNumber}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = GlumeTextPrimary
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "You are checked in today!",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = GlumeTextPrimary
                                )
                                Text(
                                    text = "Status: ${activeQueueEntry.status.name} · Tap to view live ETA",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GlumeTextSecondary
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "View Queue",
                            tint = GlumePrimaryPurple
                        )
                    }
                }
            }
        }

        // 3. Propose New Appointment Action
        item {
            VitalSenseButton(
                text = "Propose New Appointment",
                onClick = onRequestNew,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 4. Appointments List Section
        item {
            Text(
                text = "SCHEDULED APPOINTMENTS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = GlumeTextSecondary
            )
        }

        if (appointments.isEmpty()) {
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
                            text = "No scheduled appointments found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GlumeTextSecondary
                        )
                    }
                }
            }
        } else {
            items(appointments, key = { it.id }) { appt ->
                val isCheckedInForThis = activeQueueEntry?.appointmentId == appt.id

                VitalSenseCard(
                    backgroundColor = GlumeSurfaceCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Dr. ${appt.doctorName}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = GlumeTextPrimary
                            )

                            Surface(
                                shape = PillShape,
                                color = when (appt.status) {
                                    "Confirmed" -> MintGreen.copy(alpha = 0.15f)
                                    "Declined" -> CoralAlert.copy(alpha = 0.15f)
                                    else -> AmberWarning.copy(alpha = 0.15f)
                                }
                            ) {
                                Text(
                                    text = appt.status,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = when (appt.status) {
                                        "Confirmed" -> MintGreen
                                        "Declined" -> CoralAlert
                                        else -> AmberWarning
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "${appt.doctorSpecialty} · ${appt.dateFormatted} at ${appt.timeSlot}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GlumeTextSecondary
                        )

                        if (!appt.outcomeNotes.isNullOrBlank()) {
                            Text(
                                text = "Notes: ${appt.outcomeNotes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = GlumeTextSecondary
                            )
                        }

                        // Check-in button for today's appointment
                        if (appt.status == "Confirmed" || appt.status.startsWith("Pending")) {
                            Spacer(modifier = Modifier.height(4.dp))
                            if (isCheckedInForThis) {
                                OutlinedButton(
                                    onClick = onViewQueueStatus,
                                    shape = PillShape,
                                    border = BorderStroke(1.dp, GlumePrimaryPurple),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GlumePrimaryPurple),
                                    modifier = Modifier.fillMaxWidth().height(36.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Checked In — View Live Position")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        onCheckIn(appt.id)
                                        onViewQueueStatus()
                                    },
                                    shape = PillShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MintGreen,
                                        contentColor = GlumeBackground
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(36.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Check In for Today", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWalkInDialog) {
        VitalSenseDialog(
            title = "Join Today's Walk-in Queue",
            onDismissRequest = { showWalkInDialog = false }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    text = "Select a doctor to join their live walk-in queue for today:",
                    style = MaterialTheme.typography.bodySmall,
                    color = GlumeTextSecondary
                )

                doctors.forEach { doc ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedDoctorForWalkIn?.id == doc.id) GlumePrimaryPurple.copy(alpha = 0.2f) else GlumeSurfaceCard,
                        border = BorderStroke(1.dp, if (selectedDoctorForWalkIn?.id == doc.id) GlumePrimaryPurple else GlumeBorder),
                        onClick = { selectedDoctorForWalkIn = doc },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = doc.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                                    color = GlumeTextPrimary
                                )
                                Text(
                                    text = doc.specialty.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GlumeTextSecondary
                                )
                            }

                            Text(
                                text = "${doc.hospitalName} (${doc.distanceKm} km)",
                                style = MaterialTheme.typography.bodySmall,
                                color = GlumeTextSecondary
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    TextButton(
                        onClick = { showWalkInDialog = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = GlumeTextSecondary)
                    }

                    VitalSenseButton(
                        text = "Join Queue",
                        onClick = {
                            selectedDoctorForWalkIn?.let { doc ->
                                onJoinWalkIn(doc.id)
                                showWalkInDialog = false
                                onViewQueueStatus()
                            }
                        },
                        enabled = selectedDoctorForWalkIn != null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}