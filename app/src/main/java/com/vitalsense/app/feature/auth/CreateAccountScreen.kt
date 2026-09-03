package com.vitalsense.app.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitalsense.app.core.data.model.Patient
import com.vitalsense.app.core.data.model.SeverityLevel
import com.vitalsense.app.core.ui.components.ButtonStyle
import com.vitalsense.app.core.ui.components.VitalSenseButton
import com.vitalsense.app.core.ui.components.VitalSenseCard
import com.vitalsense.app.core.ui.components.VitalSenseTextField
import com.vitalsense.app.core.ui.theme.*

@Composable
fun CreateAccountScreen(
    onBack: () -> Unit,
    onAccountCreated: (Patient) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableStateOf(1) }

    // Step 1: Personal Info
    var name by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("Female") }

    // Step 2: Contact & Demographics
    var mobileNumber by remember { mutableStateOf("") }
    var villageName by remember { mutableStateOf("Sundarpura") }
    var emergencyContact by remember { mutableStateOf("") }

    // Step 3: Consent & Confirmation
    var consentGiven by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VitalSenseBackground)
            .padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = VitalSenseTextPrimary
                )
            }
            Column {
                Text(
                    text = "Create Patient Account",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = VitalSenseTextPrimary
                )
                Text(
                    text = "Step $currentStep of 3 · Registration",
                    style = MaterialTheme.typography.bodySmall,
                    color = VitalSenseTextSecondary
                )
            }
        }

        // Clean Step Progress Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (step in 1..3) {
                val isCompleted = step < currentStep
                val isCurrent = step == currentStep

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            color = when {
                                isCompleted -> VitalSenseSuccess
                                isCurrent -> VitalSensePrimary
                                else -> VitalSenseBorder
                            },
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            contentPadding = PaddingValues(bottom = Spacing.xxl)
        ) {
            when (currentStep) {
                1 -> {
                    item {
                        Text(
                            text = "1. Personal Information",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = VitalSenseTextPrimary
                        )
                        Text(
                            text = "Please enter the patient's full legal name, age, and gender.",
                            style = MaterialTheme.typography.bodySmall,
                            color = VitalSenseTextSecondary
                        )
                    }

                    item {
                        VitalSenseCard(
                            backgroundColor = VitalSenseSurface,
                            border = BorderStroke(1.dp, VitalSenseBorder)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                VitalSenseTextField(
                                    value = name,
                                    onValueChange = { name = it; errorMessage = null },
                                    label = "Full Name",
                                    placeholder = "e.g. Meera Devi"
                                )

                                VitalSenseTextField(
                                    value = ageText,
                                    onValueChange = { ageText = it; errorMessage = null },
                                    label = "Age (Years)",
                                    placeholder = "e.g. 34"
                                )

                                Text(
                                    text = "Gender",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = VitalSenseTextSecondary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                ) {
                                    listOf("Female", "Male", "Other").forEach { genderOption ->
                                        val isSelected = selectedGender == genderOption
                                        Surface(
                                            onClick = { selectedGender = genderOption },
                                            shape = PillShape,
                                            color = if (isSelected) VitalSensePrimaryContainer else VitalSenseSurfaceSubtle,
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected) VitalSensePrimary else VitalSenseBorder
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = genderOption,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (isSelected) VitalSensePrimary else VitalSenseTextPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    item {
                        Text(
                            text = "2. Contact & Demographics",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = VitalSenseTextPrimary
                        )
                        Text(
                            text = "Provide mobile number, village PHC linkage, and emergency contact.",
                            style = MaterialTheme.typography.bodySmall,
                            color = VitalSenseTextSecondary
                        )
                    }

                    item {
                        VitalSenseCard(
                            backgroundColor = VitalSenseSurface,
                            border = BorderStroke(1.dp, VitalSenseBorder)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                VitalSenseTextField(
                                    value = mobileNumber,
                                    onValueChange = { mobileNumber = it; errorMessage = null },
                                    label = "Primary Mobile Number",
                                    placeholder = "+91 98765 43210"
                                )

                                VitalSenseTextField(
                                    value = villageName,
                                    onValueChange = { villageName = it },
                                    label = "Village / Settlement",
                                    placeholder = "e.g. Sundarpura"
                                )

                                VitalSenseTextField(
                                    value = emergencyContact,
                                    onValueChange = { emergencyContact = it },
                                    label = "Emergency Contact Number",
                                    placeholder = "+91 98765 00000"
                                )
                            }
                        }
                    }
                }

                3 -> {
                    item {
                        Text(
                            text = "3. Consent & Confirmation",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = VitalSenseTextPrimary
                        )
                        Text(
                            text = "Review registration details and confirm health record creation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = VitalSenseTextSecondary
                        )
                    }

                    item {
                        VitalSenseCard(
                            backgroundColor = VitalSenseSurface,
                            border = BorderStroke(1.dp, VitalSenseBorder)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                Text(
                                    text = "Registration Summary",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = VitalSenseTextPrimary
                                )
                                Text(
                                    text = "• Patient: $name, $ageText yrs ($selectedGender)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = VitalSenseTextPrimary
                                )
                                Text(
                                    text = "• Village: $villageName · Contact: $mobileNumber",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VitalSenseTextSecondary
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = Spacing.xs),
                                    color = VitalSenseBorderSubtle
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                ) {
                                    Checkbox(
                                        checked = consentGiven,
                                        onCheckedChange = { consentGiven = it },
                                        colors = CheckboxDefaults.colors(checkedColor = VitalSensePrimary)
                                    )
                                    Text(
                                        text = "I consent to creating my VitalSense & ABHA digital health record.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = VitalSenseTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (errorMessage != null) {
                item {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = VitalSenseEmergency
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    if (currentStep > 1) {
                        VitalSenseButton(
                            text = "Previous",
                            onClick = { currentStep-- },
                            style = ButtonStyle.OUTLINED,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    VitalSenseButton(
                        text = if (currentStep == 3) "Complete Registration" else "Next Step →",
                        onClick = {
                            if (currentStep == 1) {
                                if (name.isBlank()) {
                                    errorMessage = "Please enter the patient's full name."
                                    return@VitalSenseButton
                                }
                                if (ageText.toIntOrNull() == null || ageText.toInt() <= 0) {
                                    errorMessage = "Please enter a valid age."
                                    return@VitalSenseButton
                                }
                                currentStep = 2
                            } else if (currentStep == 2) {
                                if (mobileNumber.isBlank()) {
                                    errorMessage = "Please enter a mobile number."
                                    return@VitalSenseButton
                                }
                                currentStep = 3
                            } else {
                                if (!consentGiven) {
                                    errorMessage = "Please accept the health record consent."
                                    return@VitalSenseButton
                                }
                                val newPatient = Patient(
                                    id = "p_${System.currentTimeMillis()}",
                                    name = name.trim(),
                                    age = ageText.toIntOrNull() ?: 30,
                                    gender = selectedGender,
                                    phone = mobileNumber.trim(),
                                    villageId = "v_${villageName.lowercase().trim()}",
                                    villageName = villageName.trim(),
                                    ashaWorkerId = "asha_01",
                                    ashaWorkerName = "Sunita Sharma",
                                    currentRiskLevel = SeverityLevel.LOW,
                                    lastCondition = "Initial Registration",
                                    lastVisitDate = "2026-09-03",
                                    nextAppointmentDate = null,
                                    emergencyContact = if (emergencyContact.isNotBlank()) emergencyContact.trim() else mobileNumber.trim()
                                )
                                onAccountCreated(newPatient)
                            }
                        },
                        style = ButtonStyle.PRIMARY,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
