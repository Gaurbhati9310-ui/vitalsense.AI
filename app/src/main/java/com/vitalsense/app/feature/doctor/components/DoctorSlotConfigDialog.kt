package com.vitalsense.app.feature.doctor.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vitalsense.app.core.data.model.DoctorDaySlotConfig
import com.vitalsense.app.core.ui.components.VitalSenseButton
import com.vitalsense.app.core.ui.components.VitalSenseDialog
import com.vitalsense.app.core.ui.components.VitalSenseTextField
import com.vitalsense.app.core.ui.theme.*

@Composable
fun DoctorSlotConfigDialog(
    currentConfig: DoctorDaySlotConfig?,
    doctorId: String,
    dateFormatted: String,
    onDismiss: () -> Unit,
    onSaveConfig: (DoctorDaySlotConfig) -> Unit
) {
    var startTime by remember { mutableStateOf(currentConfig?.startTime ?: "09:00") }
    var endTime by remember { mutableStateOf(currentConfig?.endTime ?: "17:00") }
    var capacityStr by remember { mutableStateOf((currentConfig?.capacity ?: 20).toString()) }
    var isWalkInOpen by remember { mutableStateOf(currentConfig?.isWalkInOpen ?: true) }

    VitalSenseDialog(
        title = "Daily Queue & Slot Settings",
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(
                text = "Date: $dateFormatted",
                style = MaterialTheme.typography.bodyMedium,
                color = GlumeTextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                VitalSenseTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = "Start Time (HH:mm)",
                    modifier = Modifier.weight(1f)
                )

                VitalSenseTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = "End Time (HH:mm)",
                    modifier = Modifier.weight(1f)
                )
            }

            VitalSenseTextField(
                value = capacityStr,
                onValueChange = { capacityStr = it.filter { char -> char.isDigit() } },
                label = "Daily Booking Capacity",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Accept Walk-in Patients",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = GlumeTextPrimary
                    )
                    Text(
                        text = if (isWalkInOpen) "Walk-ins can register today" else "Walk-in registration closed",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlumeTextSecondary
                    )
                }

                Switch(
                    checked = isWalkInOpen,
                    onCheckedChange = { isWalkInOpen = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GlumeTextPrimary,
                        checkedTrackColor = MintGreen,
                        uncheckedTrackColor = GlumeSurfaceElevated
                    )
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", color = GlumeTextSecondary)
                }

                VitalSenseButton(
                    text = "Save Settings",
                    onClick = {
                        val capacity = capacityStr.toIntOrNull() ?: 20
                        val config = DoctorDaySlotConfig(
                            id = currentConfig?.id ?: "slot_${doctorId}_$dateFormatted",
                            doctorId = doctorId,
                            dateFormatted = dateFormatted,
                            startTime = startTime,
                            endTime = endTime,
                            capacity = capacity,
                            isWalkInOpen = isWalkInOpen
                        )
                        onSaveConfig(config)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
