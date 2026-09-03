package com.vitalsense.app.feature.admin

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitalsense.app.core.data.model.DoctorQueueSummary
import com.vitalsense.app.core.data.model.QueueEntry
import com.vitalsense.app.core.data.util.QueueEtaCalculator
import com.vitalsense.app.core.ui.components.VitalSenseCard
import com.vitalsense.app.core.ui.components.VitalSenseDialog
import com.vitalsense.app.core.ui.theme.*
import com.vitalsense.app.feature.doctor.components.QueueEntryListItem

@Composable
fun QueueOversightScreen(
    summaries: List<DoctorQueueSummary>,
    selectedDoctorSummary: DoctorQueueSummary? = null,
    selectedDoctorQueue: List<QueueEntry> = emptyList(),
    onSelectDoctor: (DoctorQueueSummary) -> Unit = {},
    onDismissDrillDown: () -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val totalWaiting = summaries.sumOf { it.waitingCount }
    val openQueuesCount = summaries.count { it.isQueueOpen }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(GlumeBackground)
            .padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = PaddingValues(top = Spacing.sm, bottom = Spacing.xxl)
    ) {
        // 1. Top Header
        item {
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
                Column {
                    Text(
                        text = "District Queue Oversight",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = GlumeTextPrimary
                    )
                    Text(
                        text = "Real-time clinical throughput & patient wait surveillance",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlumeTextSecondary
                    )
                }
            }
        }

        // 2. Aggregate Metric Cards Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                VitalSenseCard(
                    backgroundColor = GlumeSurfaceCard,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "TOTAL WAITING",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = GlumeTextSecondary
                        )
                        Text(
                            text = "$totalWaiting",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = GlumeTextPrimary
                        )
                        Text(
                            text = "Across ${summaries.size} doctors",
                            style = MaterialTheme.typography.bodySmall,
                            color = GlumeTextSecondary
                        )
                    }
                }

                VitalSenseCard(
                    backgroundColor = GlumeSurfaceCard,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "ACTIVE CLINICS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = GlumeTextSecondary
                        )
                        Text(
                            text = "$openQueuesCount / ${summaries.size}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MintGreen
                        )
                        Text(
                            text = "Walk-ins accepting",
                            style = MaterialTheme.typography.bodySmall,
                            color = GlumeTextSecondary
                        )
                    }
                }
            }
        }

        // 3. Section Title
        item {
            Text(
                text = "CLINICAL QUEUES BY DOCTOR",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = GlumeTextSecondary
            )
        }

        // 4. Doctor Queue Summary List
        items(summaries, key = { it.doctorId }) { summary ->
            VitalSenseCard(
                backgroundColor = GlumeSurfaceCard,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = summary.doctorName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = GlumeTextPrimary
                            )
                            Text(
                                text = if (summary.currentToken != null) "Serving Token #${summary.currentToken}" else "No active consultation",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (summary.currentToken != null) MintGreen else SoftGreyText
                            )
                        }

                        Surface(
                            shape = PillShape,
                            color = if (summary.isQueueOpen) MintGreen.copy(alpha = 0.15f) else CoralAlert.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (summary.isQueueOpen) "Walk-ins Open" else "Walk-ins Closed",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = if (summary.isQueueOpen) MintGreen else CoralAlert,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            Column {
                                Text("Waiting", style = MaterialTheme.typography.labelSmall, color = GlumeTextSecondary)
                                Text("${summary.waitingCount}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = GlumeTextPrimary)
                            }

                            Column {
                                Text("Avg Wait", style = MaterialTheme.typography.labelSmall, color = GlumeTextSecondary)
                                Text(QueueEtaCalculator.formatEstimatedWait(summary.avgWaitSeconds), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = GlumeTextPrimary)
                            }
                        }

                        OutlinedButton(
                            onClick = { onSelectDoctor(summary) },
                            shape = PillShape,
                            border = BorderStroke(1.dp, GlumePrimaryPurple),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GlumePrimaryPurple),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Inspect Queue", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }

    // 5. Read-Only Drill-down Modal
    if (selectedDoctorSummary != null) {
        VitalSenseDialog(
            title = "Live Queue: ${selectedDoctorSummary.doctorName}",
            onDismissRequest = onDismissDrillDown
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GlumeSurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = SoftGreyText, modifier = Modifier.size(14.dp))
                        Text(
                            text = "Read-Only Administrative Oversight View",
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftGreyText
                        )
                    }
                }

                if (selectedDoctorQueue.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.lg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No patient entries in this doctor's queue today.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GlumeTextSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        items(selectedDoctorQueue, key = { it.id }) { entry ->
                            QueueEntryListItem(
                                entry = entry,
                                isReadOnly = true
                            )
                        }
                    }
                }

                Button(
                    onClick = onDismissDrillDown,
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = GlumePrimaryPurple),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close Inspector")
                }
            }
        }
    }
}
