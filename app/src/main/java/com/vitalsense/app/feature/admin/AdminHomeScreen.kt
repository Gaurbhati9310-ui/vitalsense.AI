package com.vitalsense.app.feature.admin

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitalsense.app.core.data.model.*
import com.vitalsense.app.core.ui.components.*
import com.vitalsense.app.core.ui.theme.*
import kotlin.math.max

@Composable
fun AdminHomeScreen(
    villages: List<Village>,
    notices: List<BroadcastNotice>,
    dispensaryStock: List<DispensaryItem> = emptyList(),
    onSendBroadcast: (title: String, message: String, village: String?) -> Unit,
    onOpenQueueOversight: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var showBroadcastDialog by remember { mutableStateOf(false) }
    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastMessage by remember { mutableStateOf("") }
    var selectedVillageName by remember { mutableStateOf("All Villages") }
    var isFormError by remember { mutableStateOf(false) }

    val adminIssuedDirectives = notices.filter { it.senderRole == UserRole.ADMIN }
    val totalActiveCases = villages.sumOf { it.activeCases }
    val totalPopulation = villages.sumOf { it.population }
    val outbreakCount = villages.count { it.highRiskCount > 0 }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(GlumeBackground)
            .padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = PaddingValues(top = Spacing.sm, bottom = Spacing.xxl)
    ) {
        // 1. Admin Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(
                    text = strings.districtCommand,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = GlumeTextPrimary
                )
                Text(
                    text = "Surveillance Region: Rampur District, UP (Pop: $totalPopulation)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GlumeTextSecondary
                )
            }
        }

        // Live Queue Oversight Action Card
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = GlumePrimaryPurple.copy(alpha = 0.2f),
                border = BorderStroke(1.5.dp, GlumePrimaryPurple),
                onClick = onOpenQueueOversight,
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
                                Text("⏱️", fontSize = 18.sp)
                            }
                        }

                        Column {
                            Text(
                                text = "District Queue & Wait Times",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = GlumeTextPrimary
                            )
                            Text(
                                text = "Live patient queue lengths & throughput",
                                style = MaterialTheme.typography.bodySmall,
                                color = GlumeTextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = onOpenQueueOversight,
                        shape = PillShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlumePrimaryPurple,
                            contentColor = GlumeTextPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Inspect Queues", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // 2. Summary stats (Glume 3-Column Compact Grid)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                GlumeStatCard(
                    label = strings.totalActiveCases,
                    value = "$totalActiveCases",
                    icon = "🚨",
                    modifier = Modifier.weight(1f),
                    badgeText = if (totalActiveCases > 0) "Active" else null,
                    badgeColor = GlumeAlertCoral
                )
                GlumeStatCard(
                    label = "Monitored",
                    value = "${villages.size}",
                    icon = "🏡",
                    modifier = Modifier.weight(1f),
                    badgeText = "Villages",
                    badgeColor = GlumePrimaryPurple
                )
                GlumeStatCard(
                    label = "Outbreaks",
                    value = "$outbreakCount",
                    icon = "⚠️",
                    modifier = Modifier.weight(1f),
                    badgeText = if (outbreakCount > 0) "Clusters" else "Safe",
                    badgeColor = if (outbreakCount > 0) GlumeAlertCoral else GlumeSuccessMint
                )
            }
        }

        // 3. Section: Village Disease Trend Heat Map Cards
        item {
            Text(
                text = strings.outbreakSurveillance,
                style = MaterialTheme.typography.headlineMedium,
                color = GlumeTextPrimary
            )
        }

        // 3.1 Geo-Density Outbreak Mapping HUD (Glume Dark Canvas with Glowing Nodes)
        item {
            VitalSenseCard(
                backgroundColor = GlumeSurfaceCard,
                elevation = 0.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Geo-Density Outbreak Mapping",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = GlumeTextPrimary
                            )
                            Text(
                                text = "Spatial clustering based on ASHA condition telemetry",
                                style = MaterialTheme.typography.bodySmall,
                                color = GlumeTextSecondary
                            )
                        }
                        Surface(shape = PillShape, color = GlumeAlertContainer) {
                            Text(
                                text = "LIVE RADAR",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = GlumeAlertCoral),
                                modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp)
                            )
                        }
                    }

                    // Radar HUD Canvas
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(VitalSenseSurfaceSubtle, shape = CardShape)
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        // Grid lines
                        for (i in 1..3) {
                            drawLine(
                                color = VitalSenseBorder,
                                start = Offset(0f, canvasHeight * (i / 4f)),
                                end = Offset(canvasWidth, canvasHeight * (i / 4f)),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = VitalSenseBorder,
                                start = Offset(canvasWidth * (i / 4f), 0f),
                                end = Offset(canvasWidth * (i / 4f), canvasHeight),
                                strokeWidth = 1f
                            )
                        }

                        // Radar concentric sweep circles
                        drawCircle(
                            color = VitalSensePrimary.copy(alpha = 0.15f),
                            radius = canvasHeight * 0.45f,
                            center = Offset(canvasWidth / 2f, canvasHeight / 2f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                        )

                        // Cluster Nodes
                        villages.forEachIndexed { index, village ->
                            val xFraction = when (index % 4) {
                                0 -> 0.22f
                                1 -> 0.78f
                                2 -> 0.45f
                                else -> 0.65f
                            }
                            val yFraction = when (index % 3) {
                                0 -> 0.30f
                                1 -> 0.70f
                                else -> 0.48f
                            }

                            val nodeColor = if (village.highRiskCount > 0) GlumeAlertCoral else if (village.activeCases > 5) GlumeWarningAmber else GlumeSuccessMint

                            val circleRadius = (max(village.activeCases, 3) * 2.2f).coerceIn(8f, 22f)

                            // Outer Pulse Glow
                            drawCircle(
                                color = nodeColor.copy(alpha = 0.25f),
                                radius = circleRadius * 1.8f,
                                center = Offset(canvasWidth * xFraction, canvasHeight * yFraction)
                            )
                            // Inner Solid Core
                            drawCircle(
                                color = nodeColor,
                                radius = circleRadius,
                                center = Offset(canvasWidth * xFraction, canvasHeight * yFraction)
                            )
                        }
                    }

                    // Cluster Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        villages.forEach { village ->
                            val nodeColor = if (village.highRiskCount > 0) GlumeAlertCoral else if (village.activeCases > 5) GlumeWarningAmber else GlumeSuccessMint
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(nodeColor)
                                )
                                Text(
                                    text = "${village.name}: ${village.activeCases}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GlumeTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3.2 Monitored Village Cards List
        items(villages) { village ->
            val isHighRisk = village.highRiskCount > 0
            val riskLevel = if (village.highRiskCount > 2) SeverityLevel.SEVERE else if (village.highRiskCount > 0) SeverityLevel.HIGH else if (village.activeCases > 5) SeverityLevel.MODERATE else SeverityLevel.LOW

            VitalSenseCard(
                backgroundColor = if (isHighRisk) GlumeAlertContainer else GlumeSurfaceCard,
                border = BorderStroke(1.dp, if (isHighRisk) GlumeAlertCoral.copy(alpha = 0.4f) else GlumeBorder)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = village.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = GlumeTextPrimary
                            )
                            Text(
                                text = "Population: ${village.population} · Active Cases: ${village.activeCases} · High Risk: ${village.highRiskCount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = GlumeTextSecondary
                            )
                        }
                        SeverityBadge(severity = riskLevel)
                    }

                    // Progress Bar
                    val ratio = (village.activeCases.toFloat() / max(village.population, 1) * 100f).coerceIn(0f, 100f)
                    LinearProgressIndicator(
                        progress = { (ratio / 10f).coerceIn(0.05f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(PillShape),
                        color = when (riskLevel) {
                            SeverityLevel.SEVERE -> GlumeAlertCoral
                            SeverityLevel.HIGH -> GlumeAlertCoral
                            SeverityLevel.MODERATE -> GlumeWarningAmber
                            SeverityLevel.LOW -> GlumeSuccessMint
                        },
                        trackColor = GlumeSurfaceElevated,
                    )
                }
            }
        }

        // 4. Broadcast Action Button (Single Full-Width Purple CTA)
        item {
            VitalSenseButton(
                text = "📢 Broadcast Health Directive / Alert",
                onClick = { showBroadcastDialog = true },
                style = ButtonStyle.PRIMARY
            )
        }

        // 5. Active Directives Sent by Admin
        if (adminIssuedDirectives.isNotEmpty()) {
            item {
                Text(
                    text = "Dispatched Health Directives (${adminIssuedDirectives.size})",
                    style = MaterialTheme.typography.headlineMedium,
                    color = GlumeTextPrimary
                )
            }

            items(adminIssuedDirectives) { directive ->
                VitalSenseCard {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = directive.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = GlumeTextPrimary
                            )
                            Surface(shape = PillShape, color = GlumeSuccessContainer) {
                                Text(
                                    text = "DISPATCHED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = GlumeSuccessText
                                    ),
                                    modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = directive.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = GlumeTextPrimary
                        )
                        Text(
                            text = "Target: ${directive.targetVillage ?: "All Villages"} · Sender: ${directive.senderName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GlumeTextSecondary
                        )
                    }
                }
            }
        }

        // 6. District Dispensary Stock Check
        if (dispensaryStock.isNotEmpty()) {
            item {
                Text(
                    text = "District Pharmacy & Dispensary Inventory",
                    style = MaterialTheme.typography.headlineMedium,
                    color = GlumeTextPrimary
                )
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
        }
    }

    // Broadcast Notice Modal Dialog
    if (showBroadcastDialog) {
        VitalSenseDialog(
            onDismissRequest = { showBroadcastDialog = false },
            title = "Broadcast Health Directive",
            subtitle = "Push real-time alert to Doctors, ASHA workers & Patients",
            icon = { Text("📢", fontSize = 22.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        if (broadcastTitle.isBlank() || broadcastMessage.isBlank()) {
                            isFormError = true
                        } else {
                            val villageParam = if (selectedVillageName == "All Villages") null else selectedVillageName
                            onSendBroadcast(broadcastTitle.trim(), broadcastMessage.trim(), villageParam)
                            broadcastTitle = ""
                            broadcastMessage = ""
                            showBroadcastDialog = false
                            isFormError = false
                        }
                    },
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = GlumePrimaryPurple)
                ) {
                    Text("Broadcast Now", color = GlumeTextPrimary, style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBroadcastDialog = false },
                    shape = PillShape
                ) {
                    Text(strings.cancel, color = GlumeTextSecondary, style = MaterialTheme.typography.labelLarge)
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                VitalSenseTextField(
                    value = broadcastTitle,
                    onValueChange = { broadcastTitle = it },
                    label = "Directive Title",
                    placeholder = "e.g. Water Contamination Boil Notice",
                    isError = isFormError && broadcastTitle.isBlank(),
                    errorMessage = "Title is required"
                )

                VitalSenseTextField(
                    value = broadcastMessage,
                    onValueChange = { broadcastMessage = it },
                    label = "Directive Message",
                    placeholder = "Detailed guidance, precautionary steps, and dispatch protocols...",
                    singleLine = false,
                    maxLines = 4,
                    isError = isFormError && broadcastMessage.isBlank(),
                    errorMessage = "Message is required"
                )

                Text(
                    text = "Target Village / Audience",
                    style = MaterialTheme.typography.labelSmall,
                    color = GlumeTextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    listOf("All Villages", "Rampur", "Dhimri").forEach { vName ->
                        val isSelected = selectedVillageName == vName
                        Surface(
                            onClick = { selectedVillageName = vName },
                            shape = PillShape,
                            color = if (isSelected) GlumePrimaryPurpleContainer else GlumeSurfaceCard,
                            border = if (isSelected) BorderStroke(1.5.dp, GlumePrimaryPurple) else BorderStroke(1.dp, GlumeBorder)
                        ) {
                            Text(
                                text = vName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) GlumePrimaryPurpleLight else GlumeTextPrimary
                                ),
                                modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                            )
                        }
                    }
                }
            }
        }
    }
}
