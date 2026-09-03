package com.vitalsense.app.feature.doctor.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vitalsense.app.core.data.model.*
import com.vitalsense.app.core.ui.components.VitalSenseCard
import com.vitalsense.app.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionComposerDialog(
    patient: Patient?,
    patientNameFallback: String,
    caseId: String,
    dispensaryStock: List<DispensaryItem>,
    onDismiss: () -> Unit,
    onIssuePrescription: (medicines: List<PrescribedMedicine>, instructions: String) -> Unit
) {
    var instructions by remember { mutableStateOf("Take medications after meals as directed. Drink plenty of boiled lukewarm water.") }
    val medicinesList = remember {
        mutableStateListOf(
            PrescribedMedicine("Amoxicillin 500mg", "1 capsule", "3 times daily after food", "5 days", 15),
            PrescribedMedicine("Paracetamol 650mg", "1 tablet", "SOS (if fever > 100°F)", "3 days", 6)
        )
    }

    var newMedName by remember { mutableStateOf("") }
    var newMedDosage by remember { mutableStateOf("1 tablet") }
    var newMedFrequency by remember { mutableStateOf("Twice daily after meals") }
    var newMedDuration by remember { mutableStateOf("5 days") }
    var newMedQuantity by remember { mutableStateOf("10") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = DialogShape,
            color = GlumeSurfaceCard,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, GlumeBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.lg)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "💊 Issue Structured Prescription",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = GlumeTextPrimary
                        )
                        Text(
                            text = "Patient: ${patient?.name ?: patientNameFallback} (${patient?.villageName ?: "Rural PHC"})",
                            style = MaterialTheme.typography.bodySmall,
                            color = GlumeTextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Text(text = "✕", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = GlumeTextSecondary)
                    }
                }

                HorizontalDivider(color = GlumeBorder, modifier = Modifier.padding(vertical = Spacing.xs))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    // Current Medicines in this Prescription
                    item {
                        Text(
                            text = "Prescribed Medicines (${medicinesList.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = GlumeTextPrimary
                        )
                    }

                    itemsIndexed(medicinesList) { index, med ->
                        VitalSenseCard(
                            backgroundColor = GlumeSurfaceElevated,
                            border = BorderStroke(1.dp, GlumeBorder)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = med.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = GlumeTextPrimary
                                    )
                                    Text(
                                        text = "${med.dosage} · ${med.frequency} for ${med.duration}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GlumeTextSecondary
                                    )
                                    Text(
                                        text = "Qty to dispense: ${med.quantity} units",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = GlumePrimaryPurpleLight
                                    )
                                }
                                IconButton(onClick = { medicinesList.removeAt(index) }) {
                                    Text(text = "🗑️", fontSize = 16.sp)
                                }
                            }
                        }
                    }

                    // Add Medicine Form
                    item {
                        VitalSenseCard(
                            backgroundColor = GlumeSurfaceElevated,
                            border = BorderStroke(1.dp, GlumeBorder)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                Text(
                                    text = if (caseId.isNotBlank()) "+ Add Another Medicine (Case #$caseId)" else "+ Add Another Medicine",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = GlumeTextPrimary
                                )

                                if (dispensaryStock.isNotEmpty()) {
                                    Text(
                                        text = "Available in Dispensary:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GlumeTextSecondary
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)
                                    ) {
                                        dispensaryStock.take(3).forEach { stock ->
                                            Surface(
                                                shape = PillShape,
                                                color = if (stock.isLowStock) CoralAlert.copy(alpha = 0.15f) else MintGreen.copy(alpha = 0.15f),
                                                onClick = { newMedName = stock.medicineName }
                                            ) {
                                                Text(
                                                    text = "${stock.medicineName} (${stock.availableQuantity})",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (stock.isLowStock) CoralAlert else MintGreen,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = newMedName,
                                    onValueChange = { newMedName = it },
                                    label = { Text("Medicine Name", color = GlumeTextSecondary) },
                                    placeholder = { Text("e.g. Azithromycin 250mg", color = GlumeTextTertiary) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = InputShape,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = GlumeSurfaceCard,
                                        unfocusedContainerColor = GlumeSurfaceCard,
                                        focusedBorderColor = GlumePrimaryPurple,
                                        unfocusedBorderColor = GlumeBorder,
                                        focusedTextColor = GlumeTextPrimary,
                                        unfocusedTextColor = GlumeTextPrimary
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                ) {
                                    OutlinedTextField(
                                        value = newMedDosage,
                                        onValueChange = { newMedDosage = it },
                                        label = { Text("Dosage", color = GlumeTextSecondary) },
                                        modifier = Modifier.weight(1f),
                                        shape = InputShape,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = GlumeSurfaceCard,
                                            unfocusedContainerColor = GlumeSurfaceCard,
                                            focusedBorderColor = GlumePrimaryPurple,
                                            unfocusedBorderColor = GlumeBorder,
                                            focusedTextColor = GlumeTextPrimary,
                                            unfocusedTextColor = GlumeTextPrimary
                                        )
                                    )
                                    OutlinedTextField(
                                        value = newMedQuantity,
                                        onValueChange = { newMedQuantity = it },
                                        label = { Text("Qty", color = GlumeTextSecondary) },
                                        modifier = Modifier.weight(0.7f),
                                        shape = InputShape,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = GlumeSurfaceCard,
                                            unfocusedContainerColor = GlumeSurfaceCard,
                                            focusedBorderColor = GlumePrimaryPurple,
                                            unfocusedBorderColor = GlumeBorder,
                                            focusedTextColor = GlumeTextPrimary,
                                            unfocusedTextColor = GlumeTextPrimary
                                        )
                                    )
                                }

                                OutlinedTextField(
                                    value = newMedFrequency,
                                    onValueChange = { newMedFrequency = it },
                                    label = { Text("Frequency & Timing", color = GlumeTextSecondary) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = InputShape,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = GlumeSurfaceCard,
                                        unfocusedContainerColor = GlumeSurfaceCard,
                                        focusedBorderColor = GlumePrimaryPurple,
                                        unfocusedBorderColor = GlumeBorder,
                                        focusedTextColor = GlumeTextPrimary,
                                        unfocusedTextColor = GlumeTextPrimary
                                    )
                                )

                                OutlinedTextField(
                                    value = newMedDuration,
                                    onValueChange = { newMedDuration = it },
                                    label = { Text("Duration", color = GlumeTextSecondary) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = InputShape,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = GlumeSurfaceCard,
                                        unfocusedContainerColor = GlumeSurfaceCard,
                                        focusedBorderColor = GlumePrimaryPurple,
                                        unfocusedBorderColor = GlumeBorder,
                                        focusedTextColor = GlumeTextPrimary,
                                        unfocusedTextColor = GlumeTextPrimary
                                    )
                                )

                                Button(
                                    onClick = {
                                        if (newMedName.isNotBlank()) {
                                            medicinesList.add(
                                                PrescribedMedicine(
                                                    name = newMedName.trim(),
                                                    dosage = newMedDosage.trim(),
                                                    frequency = newMedFrequency.trim(),
                                                    duration = newMedDuration.trim(),
                                                    quantity = newMedQuantity.toIntOrNull() ?: 10
                                                )
                                            )
                                            newMedName = ""
                                        }
                                    },
                                    shape = PillShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = GlumePrimaryPurple),
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("+ Add to Prescription", color = GlumeTextPrimary, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    // Special Instructions
                    item {
                        Text(
                            text = "Dietary & Follow-Up Instructions",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = GlumeTextPrimary
                        )
                        OutlinedTextField(
                            value = instructions,
                            onValueChange = { instructions = it },
                            label = { Text("Instructions for Patient & ASHA", color = GlumeTextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = InputShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = GlumeSurfaceElevated,
                                unfocusedContainerColor = GlumeSurfaceCard,
                                focusedBorderColor = GlumePrimaryPurple,
                                unfocusedBorderColor = GlumeBorder,
                                focusedTextColor = GlumeTextPrimary,
                                unfocusedTextColor = GlumeTextPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Issue Button
                Button(
                    onClick = {
                        onIssuePrescription(medicinesList.toList(), instructions)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlumePrimaryPurple,
                        contentColor = GlumeTextPrimary
                    ),
                    enabled = medicinesList.isNotEmpty()
                ) {
                    Text(
                        text = "Issue Prescription to Patient & Dispensary ✓",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
