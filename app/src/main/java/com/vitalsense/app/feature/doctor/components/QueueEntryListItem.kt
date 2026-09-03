package com.vitalsense.app.feature.doctor.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.vitalsense.app.core.data.model.QueueEntry
import com.vitalsense.app.core.data.model.QueueEntrySource
import com.vitalsense.app.core.data.model.QueueEntryStatus
import com.vitalsense.app.core.ui.components.VitalSenseCard
import com.vitalsense.app.core.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun QueueEntryListItem(
    entry: QueueEntry,
    isReadOnly: Boolean = false,
    onStartConsultation: (String) -> Unit = {},
    onCompleteConsultation: (String) -> Unit = {},
    onSkip: (String) -> Unit = {},
    onMarkNoShow: (String) -> Unit = {},
    onTogglePriority: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val statusColor = when (entry.status) {
        QueueEntryStatus.WAITING -> AmberWarning
        QueueEntryStatus.CALLED -> GlumePrimaryPurple
        QueueEntryStatus.IN_CONSULTATION -> MintGreen
        QueueEntryStatus.COMPLETED -> SoftGreyText
        QueueEntryStatus.NO_SHOW -> CoralAlert
        QueueEntryStatus.SKIPPED -> AmberWarning
        QueueEntryStatus.CANCELLED -> SoftGreyText
    }

    val sourceBg = if (entry.source == QueueEntrySource.SCHEDULED) GlumePrimaryPurple.copy(alpha = 0.15f) else MintGreen.copy(alpha = 0.15f)
    val sourceText = if (entry.source == QueueEntrySource.SCHEDULED) GlumePrimaryPurple else MintGreen

    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val checkInTimeString = timeFormat.format(Date(entry.checkedInAt))

    VitalSenseCard(
        backgroundColor = if (entry.status == QueueEntryStatus.IN_CONSULTATION) GlumeSurfaceElevated else GlumeSurfaceCard,
        border = BorderStroke(if (entry.priorityFlag) 1.5.dp else 1.dp, if (entry.priorityFlag) CoralAlert else GlumeBorder),
        contentPadding = Spacing.sm,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            // Row 1: Token Badge, Patient Name, Source Chip & Priority Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    // Token Circle
                    Surface(
                        shape = CircleShape,
                        color = if (entry.provisionalToken) SoftGreyText.copy(alpha = 0.2f) else GlumePrimaryPurple.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, if (entry.provisionalToken) SoftGreyText else GlumePrimaryPurple),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (entry.provisionalToken) "~" else "#${entry.tokenNumber}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = GlumeTextPrimary
                            )
                        }
                    }

                    Column {
                        Text(
                            text = entry.patientName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = GlumeTextPrimary
                        )
                        Text(
                            text = "Checked in at $checkInTimeString",
                            style = MaterialTheme.typography.bodySmall,
                            color = GlumeTextSecondary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (entry.priorityFlag) {
                        Surface(
                            shape = PillShape,
                            color = CoralAlert.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, CoralAlert)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PriorityHigh,
                                    contentDescription = "Priority",
                                    tint = CoralAlert,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "PRIORITY",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = CoralAlert
                                )
                            }
                        }
                    }

                    Surface(
                        shape = PillShape,
                        color = sourceBg
                    ) {
                        Text(
                            text = if (entry.source == QueueEntrySource.SCHEDULED) "Scheduled" else "Walk-in",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = sourceText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Row 2: Status Indicator Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = entry.status.name.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (!entry.outcomeNotes.isNullOrBlank()) {
                    Text(
                        text = "Notes: ${entry.outcomeNotes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlumeTextSecondary,
                        maxLines = 1
                    )
                }
            }

            // Row 3: Action Buttons (Only for interactive Doctor view)
            if (!isReadOnly) {
                HorizontalDivider(color = GlumeBorder.copy(alpha = 0.5f), thickness = 0.5.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (entry.status) {
                        QueueEntryStatus.WAITING -> {
                            IconButton(
                                onClick = { onTogglePriority(entry.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (entry.priorityFlag) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Toggle Priority",
                                    tint = if (entry.priorityFlag) CoralAlert else SoftGreyText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            TextButton(
                                onClick = { onSkip(entry.id) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Skip",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AmberWarning
                                )
                            }
                            TextButton(
                                onClick = { onMarkNoShow(entry.id) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "No-Show",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CoralAlert
                                )
                            }
                        }

                        QueueEntryStatus.CALLED -> {
                            Button(
                                onClick = { onStartConsultation(entry.id) },
                                shape = PillShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MintGreen,
                                    contentColor = GlumeBackground
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Start", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = { onSkip(entry.id) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Skip", style = MaterialTheme.typography.labelMedium, color = AmberWarning)
                            }
                            TextButton(
                                onClick = { onMarkNoShow(entry.id) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("No-Show", style = MaterialTheme.typography.labelMedium, color = CoralAlert)
                            }
                        }

                        QueueEntryStatus.IN_CONSULTATION -> {
                            Button(
                                onClick = { onCompleteConsultation(entry.id) },
                                shape = PillShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GlumePrimaryPurple,
                                    contentColor = GlumeTextPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Complete", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }

                        else -> {
                            // Completed / No Show / Cancelled - static row
                        }
                    }
                }
            }
        }
    }
}
