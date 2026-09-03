package com.vitalsense.app.feature.patient

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

@Composable
fun PatientHomeScreen(
    patient: Patient,
    notices: List<BroadcastNotice> = emptyList(),
    prescriptions: List<Prescription> = emptyList(),
    activeQueueEntry: QueueEntry? = null,
    onCategoryClick: (ConditionCategory) -> Unit = {},
    onViewHealthCard: () -> Unit = {},
    onOpenAppointments: () -> Unit = {},
    onOpenQueueStatus: () -> Unit = {},
    onTriggerSos: () -> Unit = {},
    onSavePrescription: (Prescription) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var showSosConfirmation by remember { mutableStateOf(false) }
    var sosSentSuccess by remember { mutableStateOf(false) }
    var showPrescriptionUploadDialog by remember { mutableStateOf(false) }

    val adminAdvisories = notices.filter {
        it.senderRole == UserRole.ADMIN || it.targetRole == "ALL" || it.targetRole == "PATIENT"
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    val bgColor = VitalSenseBackground
    val cardBgColor = VitalSenseSurface
    val cardBorderColor = VitalSenseBorder
    val textPrimaryColor = VitalSenseTextPrimary
    val textSecondaryColor = VitalSenseTextSecondary

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = PaddingValues(top = Spacing.sm, bottom = Spacing.xxl)
    ) {

        // 1. Personalized Greeting (Glume Bold Headline Style)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${strings.namaste}, ${patient.name.split(" ").first()}!",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp
                            ),
                            color = textPrimaryColor
                        )
                        Text(
                            text = "${strings.village}: ${patient.villageName} · ${strings.ashaAssigned}: ${patient.ashaWorkerName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textSecondaryColor
                        )
                    }
                    SeverityBadge(severity = patient.currentRiskLevel)
                }
            }
        }

        // Live Queue Active Status Banner (if checked in)
        if (activeQueueEntry != null && activeQueueEntry.status != QueueEntryStatus.CANCELLED && activeQueueEntry.status != QueueEntryStatus.COMPLETED) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GlumePrimaryPurple.copy(alpha = 0.2f),
                    border = BorderStroke(1.5.dp, GlumePrimaryPurple),
                    onClick = onOpenQueueStatus,
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
                                    text = "Your Live Consultation Token",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = textPrimaryColor
                                )
                                Text(
                                    text = "Status: ${activeQueueEntry.status.name} · Dr. ${activeQueueEntry.doctorName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textSecondaryColor
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

        // Appointments & Live Queue Quick Access Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cardBgColor,
                    border = BorderStroke(1.dp, cardBorderColor),
                    onClick = onOpenAppointments,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Text("📅", fontSize = 20.sp)
                        Column {
                            Text(
                                text = "Appointments",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = textPrimaryColor
                            )
                            Text(
                                text = "Check in & slots",
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondaryColor
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cardBgColor,
                    border = BorderStroke(1.dp, cardBorderColor),
                    onClick = {
                        if (activeQueueEntry != null) onOpenQueueStatus() else onOpenAppointments()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Text("🔢", fontSize = 20.sp)
                        Column {
                            Text(
                                text = "Live Queue",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = textPrimaryColor
                            )
                            Text(
                                text = if (activeQueueEntry != null) "Token #${activeQueueEntry.tokenNumber}" else "Join walk-in",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (activeQueueEntry != null) MintGreen else textSecondaryColor
                            )
                        }
                    }
                }
            }
        }

        // 2. Hero Card: Offline Health Card & Daily Status (Glume Rounded Card)
        item {
            VitalSenseCard(
                backgroundColor = cardBgColor,
                border = BorderStroke(1.dp, cardBorderColor),
                onClick = onViewHealthCard
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(GlumePrimaryPurpleContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🪪", fontSize = 20.sp)
                            }
                            Column {
                                Text(
                                    text = strings.offlineHealthCard,
                                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.5.sp),
                                    color = GlumePrimaryPurple
                                )
                                Text(
                                    text = "Permanent QR & Offline Record",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textSecondaryColor
                                )
                            }
                        }

                        Surface(
                            shape = PillShape,
                            color = GlumePrimaryPurple
                        ) {
                            Text(
                                text = strings.viewCard,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = GlumeTextPrimary,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 6.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = cardBorderColor)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = strings.activeCondition,
                                style = MaterialTheme.typography.labelSmall,
                                color = textSecondaryColor
                            )
                            Text(
                                text = patient.lastCondition,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = textPrimaryColor
                            )
                        }

                        Surface(
                            shape = PillShape,
                            color = GlumeSuccessContainer
                        ) {
                            Text(
                                text = strings.cachedOffline,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = GlumeSuccessText
                                ),
                                modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "${strings.nextCheckup} ${patient.nextAppointmentDate ?: strings.noneScheduled}",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondaryColor
                    )
                }
            }
        }

        // 3. Section Title: Health Categories (Icon-First & High Contrast)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(
                    text = strings.howCanWeHelp,
                    style = MaterialTheme.typography.headlineMedium,
                    color = textPrimaryColor
                )
                Text(
                    text = strings.tapServiceDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondaryColor
                )
            }
        }

        // 3.1 Categories 2-Column Grid (Large accessible tap targets)
        item {
            val categories = listOf(
                ConditionCategory.GENERAL_MEDICINE,
                ConditionCategory.MATERNAL_HEALTH,
                ConditionCategory.FITNESS,
                ConditionCategory.NUTRITION,
                ConditionCategory.MENTAL_HEALTH
            )

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                categories.chunked(2).forEach { rowCategories ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        rowCategories.forEach { category ->
                            CategoryChip(
                                category = category,
                                isSelected = false,
                                onClick = { onCategoryClick(category) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowCategories.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // 4. My Prescriptions Section (Glume Dark Slate Card Style)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${strings.myPrescriptions} (${prescriptions.size})",
                    style = MaterialTheme.typography.headlineMedium,
                    color = textPrimaryColor
                )

                Button(
                    onClick = { showPrescriptionUploadDialog = true },
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlumePrimaryPurple,
                        contentColor = GlumeTextPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.xxs),
                    modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                ) {
                    Text(strings.uploadRx, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (prescriptions.isEmpty()) {
            item {
                VitalSenseCard(
                    backgroundColor = cardBgColor,
                    border = BorderStroke(1.dp, cardBorderColor)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = strings.noPrescriptions,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = textPrimaryColor
                            )
                            Text(
                                text = strings.scanOrWrite,
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondaryColor
                            )
                        }
                        OutlinedButton(
                            onClick = { showPrescriptionUploadDialog = true },
                            shape = PillShape,
                            border = BorderStroke(1.dp, GlumePrimaryPurple)
                        ) {
                            Text(strings.uploadRx, style = MaterialTheme.typography.labelSmall, color = GlumePrimaryPurple)
                        }
                    }
                }
            }
        } else {
            items(prescriptions) { rx ->
                VitalSenseCard(
                    backgroundColor = cardBgColor,
                    border = BorderStroke(1.dp, cardBorderColor)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = rx.doctorName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = textPrimaryColor
                                )
                                Text(
                                    text = "${rx.doctorSpecialty} · ${rx.dateFormatted}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textSecondaryColor
                                )
                            }
                            if (rx.isOcrExtracted) {
                                Surface(shape = PillShape, color = GlumeSuccessContainer) {
                                    Text(
                                        text = "AI Scanned",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = GlumeSuccessText),
                                        modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        rx.medicines.forEach { med ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "• ${med.name} (${med.dosage})",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = textPrimaryColor
                                )
                                Text(
                                    text = "${med.frequency} · ${med.duration}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textSecondaryColor
                                )
                            }
                        }

                        if (rx.instructions.isNotBlank()) {
                            Text(
                                text = "Note: ${rx.instructions}",
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondaryColor
                            )
                        }
                    }
                }
            }
        }

        // 5. District Health Advisories
        if (adminAdvisories.isNotEmpty()) {
            item {
                Text(
                    text = strings.districtAdvisories,
                    style = MaterialTheme.typography.headlineMedium,
                    color = textPrimaryColor
                )
            }

            items(adminAdvisories) { advisory ->
                VitalSenseCard(
                    backgroundColor = if (advisory.isUrgent) GlumeAlertContainer else cardBgColor,
                    border = BorderStroke(1.dp, if (advisory.isUrgent) GlumeAlertCoral.copy(alpha = 0.4f) else cardBorderColor)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = advisory.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (advisory.isUrgent) GlumeAlertText else textPrimaryColor
                            )
                            if (advisory.isUrgent) {
                                Surface(shape = PillShape, color = GlumeAlertCoral) {
                                    Text(
                                        text = strings.urgent,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = GlumeTextPrimary
                                        ),
                                        modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = advisory.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textPrimaryColor
                        )
                        Text(
                            text = "${strings.issuedBy} ${advisory.senderName} (${advisory.senderRole.name})",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondaryColor
                        )
                    }
                }
            }
        }

        // 6. Persistent Emergency SOS Banner (Single Full-Width Rounded Button/Card)
        item {
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Surface(
                onClick = { showSosConfirmation = true },
                modifier = Modifier.fillMaxWidth(),
                shape = CardShape,
                color = GlumeAlertCoral,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(GlumeTextPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🚨", fontSize = 24.sp)
                        }
                        Column {
                            Text(
                                text = strings.emergencySos,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = GlumeTextPrimary
                                )
                            )
                            Text(
                                text = strings.emergencySosDesc,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = GlumeTextPrimary.copy(alpha = 0.9f)
                                )
                            )
                        }
                    }
                    Surface(
                        shape = PillShape,
                        color = GlumeTextPrimary
                    ) {
                        Text(
                            text = strings.trigger,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = GlumeAlertCoral
                            ),
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                        )
                    }
                }
            }
        }
    }

    val sosMsg = "EMERGENCY SOS from ${patient.name} (${patient.villageName}, Age ${patient.age}). Contact: ${patient.phone}."

    // Custom Styled SOS Confirmation Dialog
    if (showSosConfirmation) {
        VitalSenseDialog(
            onDismissRequest = { showSosConfirmation = false },
            title = strings.confirmSosTitle,
            icon = { Text("🚨", fontSize = 22.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showSosConfirmation = false
                        onTriggerSos()
                        sosSentSuccess = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlumeAlertCoral),
                    shape = PillShape,
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                ) {
                    Text(strings.yesSendAlert, color = GlumeTextPrimary, style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSosConfirmation = false },
                    shape = PillShape,
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                ) {
                    Text(strings.cancel, color = GlumeTextSecondary, style = MaterialTheme.typography.labelLarge)
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = strings.confirmSosMsg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GlumeTextPrimary
                )
                Text(
                    text = "• ${strings.ashaAssigned}: ${patient.ashaWorkerName}\n• Available Doctors\n• Emergency SMS: ${patient.emergencyContact}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = GlumePrimaryPurpleLight
                )
            }
        }
    }

    // Custom Styled SOS Sent Dialog with 0-Internet Fallbacks
    if (sosSentSuccess) {
        VitalSenseDialog(
            onDismissRequest = { sosSentSuccess = false },
            title = strings.sosDispatchedTitle,
            icon = { Text("🚨", fontSize = 22.sp) },
            confirmButton = {
                Button(
                    onClick = { sosSentSuccess = false },
                    colors = ButtonDefaults.buttonColors(containerColor = GlumePrimaryPurple),
                    shape = PillShape,
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                ) {
                    Text(strings.done, color = GlumeTextPrimary, style = MaterialTheme.typography.labelLarge)
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = strings.sosDispatchedMsg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GlumeTextPrimary
                )

                HorizontalDivider(color = GlumeBorder)

                Text(
                    text = strings.zeroInternetFallbacks,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = GlumeTextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    OutlinedButton(
                        onClick = {
                            com.vitalsense.app.core.util.EmergencySosHelper.sendCellularSmsFallback(
                                context = context,
                                recipientPhone = patient.emergencyContact,
                                message = sosMsg
                            )
                        },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp),
                        shape = PillShape,
                        border = BorderStroke(1.dp, GlumeBorder)
                    ) {
                        Text(strings.smsAsha, style = MaterialTheme.typography.labelSmall, color = GlumeTextPrimary)
                    }

                    Button(
                        onClick = {
                            com.vitalsense.app.core.util.EmergencySosHelper.dialEmergencyCall(
                                context = context,
                                phoneNumber = "108"
                            )
                        },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp),
                        shape = PillShape,
                        colors = ButtonDefaults.buttonColors(containerColor = GlumeAlertCoral)
                    ) {
                        Text(strings.call108, color = GlumeTextPrimary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }

    if (showPrescriptionUploadDialog) {
        com.vitalsense.app.feature.prescriptions.PrescriptionUploadDialog(
            patient = patient,
            isAshaProxy = false,
            onDismiss = { showPrescriptionUploadDialog = false },
            onSavePrescription = onSavePrescription
        )
    }
}
