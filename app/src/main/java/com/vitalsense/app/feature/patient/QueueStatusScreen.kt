package com.vitalsense.app.feature.patient

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitalsense.app.core.data.model.QueueEntry
import com.vitalsense.app.core.data.model.QueueEntrySource
import com.vitalsense.app.core.data.model.QueueEntryStatus
import com.vitalsense.app.core.data.util.QueueEtaCalculator
import com.vitalsense.app.core.ui.components.VitalSenseCard
import com.vitalsense.app.core.ui.components.VitalSenseDialog
import com.vitalsense.app.core.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun QueueStatusScreen(
    entry: QueueEntry,
    peopleAhead: Int,
    estimatedWaitSeconds: Long,
    onCancel: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCancelDialog by remember { mutableStateOf(false) }

    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val checkInTimeString = timeFormat.format(Date(entry.checkedInAt))

    val statusColor = when (entry.status) {
        QueueEntryStatus.WAITING -> AmberWarning
        QueueEntryStatus.CALLED -> GlumePrimaryPurple
        QueueEntryStatus.IN_CONSULTATION -> MintGreen
        QueueEntryStatus.COMPLETED -> SoftGreyText
        QueueEntryStatus.NO_SHOW -> CoralAlert
        QueueEntryStatus.SKIPPED -> AmberWarning
        QueueEntryStatus.CANCELLED -> SoftGreyText
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GlumeBackground)
            .padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // 1. Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GlumeTextPrimary)
            }
            Text(
                text = "Live Queue Status",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = GlumeTextPrimary
            )
        }

        // 2. Active Call Banner (if Called or In Consultation)
        if (entry.status == QueueEntryStatus.CALLED) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = GlumePrimaryPurple.copy(alpha = 0.2f),
                border = BorderStroke(1.5.dp, GlumePrimaryPurple),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = GlumePrimaryPurple,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "It's your turn!",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = GlumeTextPrimary
                        )
                        Text(
                            text = "Please proceed to Dr. ${entry.doctorName}'s consultation room.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GlumeTextSecondary
                        )
                    }
                }
            }
        } else if (entry.status == QueueEntryStatus.IN_CONSULTATION) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MintGreen.copy(alpha = 0.2f),
                border = BorderStroke(1.5.dp, MintGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = null,
                        tint = MintGreen,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "Consultation in progress",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MintGreen
                        )
                        Text(
                            text = "Dr. ${entry.doctorName} is attending to you now.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GlumeTextSecondary
                        )
                    }
                }
            }
        }

        // 3. Hero Token Card
        VitalSenseCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = GlumeSurfaceCard,
            border = BorderStroke(
                if (entry.status == QueueEntryStatus.CALLED) 1.5.dp else 1.dp,
                if (entry.status == QueueEntryStatus.CALLED) GlumePrimaryPurple else GlumeBorder
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    text = "YOUR LIVE TOKEN",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = GlumeTextSecondary
                )

                Surface(
                    shape = CircleShape,
                    color = if (entry.provisionalToken) SoftGreyText.copy(alpha = 0.2f) else GlumePrimaryPurple.copy(alpha = 0.2f),
                    border = BorderStroke(2.dp, if (entry.provisionalToken) SoftGreyText else GlumePrimaryPurple),
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (entry.provisionalToken) "~" else "#${entry.tokenNumber}",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 36.sp
                            ),
                            color = if (entry.provisionalToken) SoftGreyText else GlumePrimaryPurple
                        )
                    }
                }

                if (entry.provisionalToken) {
                    Text(
                        text = "Confirming your position…",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AmberWarning
                    )
                    Text(
                        text = "Offline check-in saved locally. Will sync authoritative token upon connection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlumeTextSecondary
                    )
                } else {
                    Surface(
                        shape = PillShape,
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = entry.status.name.replace("_", " "),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // 4. Position & ETA Metrics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // People Ahead Card
            VitalSenseCard(
                modifier = Modifier.weight(1f),
                backgroundColor = GlumeSurfaceCard
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "PEOPLE AHEAD",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = GlumeTextSecondary
                    )
                    Text(
                        text = if (entry.status == QueueEntryStatus.WAITING) "$peopleAhead" else "0",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = GlumeTextPrimary
                    )
                    Text(
                        text = if (peopleAhead == 0 && entry.status == QueueEntryStatus.WAITING) "You're next!" else "In line",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (peopleAhead == 0) MintGreen else GlumeTextSecondary
                    )
                }
            }

            // Estimated Wait Time Card
            VitalSenseCard(
                modifier = Modifier.weight(1f),
                backgroundColor = GlumeSurfaceCard
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "EST. WAIT TIME",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = GlumeTextSecondary
                    )
                    Text(
                        text = if (entry.status == QueueEntryStatus.WAITING) QueueEtaCalculator.formatEstimatedWait(estimatedWaitSeconds) else "Active",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = GlumeTextPrimary
                    )
                    Text(
                        text = "Live calculation",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlumeTextSecondary
                    )
                }
            }
        }

        // 5. Details Card
        VitalSenseCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = GlumeSurfaceCard
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Text(
                    text = "Doctor: Dr. ${entry.doctorName}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = GlumeTextPrimary
                )
                Text(
                    text = "Checked In: $checkInTimeString (${if (entry.source == QueueEntrySource.SCHEDULED) "Scheduled Appointment" else "Walk-in"})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GlumeTextSecondary
                )
                Text(
                    text = "Date: ${entry.dateFormatted}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GlumeTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 6. Cancel Queue Spot Button
        if (entry.status == QueueEntryStatus.WAITING) {
            OutlinedButton(
                onClick = { showCancelDialog = true },
                shape = PillShape,
                border = BorderStroke(1.dp, CoralAlert.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralAlert),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cancel Queue Spot")
            }
        }
    }

    if (showCancelDialog) {
        VitalSenseDialog(
            title = "Cancel Queue Spot?",
            onDismissRequest = { showCancelDialog = false }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Text(
                    text = "Are you sure you want to give up your position (#${entry.tokenNumber}) in today's live queue?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GlumeTextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    TextButton(
                        onClick = { showCancelDialog = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Keep Position", color = GlumeTextSecondary)
                    }

                    Button(
                        onClick = {
                            onCancel(entry.id)
                            showCancelDialog = false
                            onBack()
                        },
                        shape = PillShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoralAlert,
                            contentColor = GlumeTextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Yes, Cancel", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
