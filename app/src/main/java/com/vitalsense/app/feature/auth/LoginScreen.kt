package com.vitalsense.app.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitalsense.app.core.data.local.seed.SeedDataProvider
import com.vitalsense.app.core.data.model.*
import com.vitalsense.app.core.ui.components.*
import com.vitalsense.app.core.ui.theme.*

@Composable
fun LoginScreen(
    currentLanguage: AppLanguage,
    onToggleLanguage: () -> Unit,
    onPatientLogin: (Patient) -> Unit,
    onAshaLogin: (AshaWorker) -> Unit,
    onDoctorLogin: (Doctor) -> Unit,
    onAdminLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var selectedRole by remember { mutableStateOf(UserRole.DOCTOR) }

    // Form inputs
    var phoneInput by remember { mutableStateOf("") }
    var ashaIdInput by remember { mutableStateOf("") }
    var pinInput by remember { mutableStateOf("") }
    var doctorEmailInput by remember { mutableStateOf("") }
    var doctorPasswordInput by remember { mutableStateOf("") }
    var adminPasscodeInput by remember { mutableStateOf("") }

    val samplePatients = remember { SeedDataProvider.initialPatients }
    val sampleAshas = remember { SeedDataProvider.initialAshaWorkers }
    val sampleDoctors = remember { SeedDataProvider.initialDoctors }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(VitalSenseBackground)
            .padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = PaddingValues(top = Spacing.md, bottom = Spacing.xxl)
    ) {
        // 1. App Header & Reactive Language Switcher
        item {
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
                        color = VitalSensePrimaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.HealthAndSafety,
                                contentDescription = "VitalSense Logo",
                                tint = VitalSensePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = strings.appName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = VitalSenseTextPrimary
                        )
                        Text(
                            text = strings.tagline,
                            style = MaterialTheme.typography.bodySmall,
                            color = VitalSenseTextSecondary
                        )
                    }
                }

                // Language Toggle Pill
                Surface(
                    onClick = onToggleLanguage,
                    shape = PillShape,
                    color = VitalSenseSurface,
                    border = BorderStroke(1.dp, VitalSenseBorder),
                    shadowElevation = 0.dp,
                    modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)
                    ) {
                        Text(text = "🌐", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = currentLanguage.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = VitalSenseTextPrimary
                        )
                    }
                }
            }
        }

        // 2. Welcome Title
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(
                    text = strings.whoIsUsing,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = VitalSenseTextPrimary
                )
                Text(
                    text = strings.selectRoleDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = VitalSenseTextSecondary
                )
            }
        }

        // 3. 4-Role Selector Cards (2x2 Grid)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    RoleCard(
                        title = strings.rolePatient,
                        desc = strings.rolePatientDesc,
                        icon = Icons.Default.Person,
                        isSelected = selectedRole == UserRole.PATIENT,
                        onClick = { selectedRole = UserRole.PATIENT },
                        modifier = Modifier.weight(1f)
                    )
                    RoleCard(
                        title = strings.roleAsha,
                        desc = strings.roleAshaDesc,
                        icon = Icons.Default.VolunteerActivism,
                        isSelected = selectedRole == UserRole.ASHA,
                        onClick = { selectedRole = UserRole.ASHA },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    RoleCard(
                        title = strings.roleDoctor,
                        desc = strings.roleDoctorDesc,
                        icon = Icons.Default.MedicalServices,
                        isSelected = selectedRole == UserRole.DOCTOR,
                        onClick = { selectedRole = UserRole.DOCTOR },
                        modifier = Modifier.weight(1f)
                    )
                    RoleCard(
                        title = strings.roleAdmin,
                        desc = strings.roleAdminDesc,
                        icon = Icons.Default.AdminPanelSettings,
                        isSelected = selectedRole == UserRole.ADMIN,
                        onClick = { selectedRole = UserRole.ADMIN },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. Role Credentials Form & 1-Tap Demo Login
        item {
            VitalSenseCard(
                backgroundColor = VitalSenseSurface,
                border = BorderStroke(1.dp, VitalSenseBorder)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    // Portal Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Icon(
                            imageVector = when (selectedRole) {
                                UserRole.PATIENT -> Icons.Default.Person
                                UserRole.ASHA -> Icons.Default.VolunteerActivism
                                UserRole.DOCTOR -> Icons.Default.MedicalServices
                                UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                            },
                            contentDescription = null,
                            tint = VitalSensePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = when (selectedRole) {
                                UserRole.PATIENT -> strings.patientSignIn
                                UserRole.ASHA -> strings.ashaSignIn
                                UserRole.DOCTOR -> strings.doctorSignIn
                                UserRole.ADMIN -> strings.adminSignIn
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = VitalSenseTextPrimary
                        )
                    }

                    // Form Fields based on role
                    when (selectedRole) {
                        UserRole.PATIENT -> {
                            VitalSenseTextField(
                                value = phoneInput,
                                onValueChange = { phoneInput = it },
                                label = strings.mobileNumber,
                                placeholder = "+91 98111 22334"
                            )
                            VitalSenseTextField(
                                value = ashaIdInput,
                                onValueChange = { ashaIdInput = it },
                                label = strings.ashaHelperIdOptional,
                                placeholder = "e.g. ASHA-RAMPUR-01"
                            )
                        }

                        UserRole.ASHA -> {
                            VitalSenseTextField(
                                value = ashaIdInput,
                                onValueChange = { ashaIdInput = it },
                                label = strings.uniqueAshaId,
                                placeholder = "e.g. ASHA-RAMPUR-01"
                            )
                            VitalSenseTextField(
                                value = pinInput,
                                onValueChange = { pinInput = it },
                                label = strings.securityPin,
                                placeholder = "4-digit PIN",
                                isPassword = true
                            )
                        }

                        UserRole.DOCTOR -> {
                            VitalSenseTextField(
                                value = doctorEmailInput,
                                onValueChange = { doctorEmailInput = it },
                                label = strings.doctorEmail,
                                placeholder = "doctor.varma@vitalsense.in"
                            )
                            VitalSenseTextField(
                                value = doctorPasswordInput,
                                onValueChange = { doctorPasswordInput = it },
                                label = strings.password,
                                placeholder = "••••••••",
                                isPassword = true
                            )
                        }

                        UserRole.ADMIN -> {
                            VitalSenseTextField(
                                value = adminPasscodeInput,
                                onValueChange = { adminPasscodeInput = it },
                                label = strings.adminPasscode,
                                placeholder = "District Officer Passcode",
                                isPassword = true
                            )
                        }
                    }

                    // Primary Submit Button
                    VitalSenseButton(
                        text = when (selectedRole) {
                            UserRole.PATIENT -> strings.logInAsPatient
                            UserRole.ASHA -> strings.logInAsAsha
                            UserRole.DOCTOR -> strings.logInAsDoctor
                            UserRole.ADMIN -> strings.logInAsAdmin
                        } + " →",
                        onClick = {
                            when (selectedRole) {
                                UserRole.PATIENT -> onPatientLogin(samplePatients.first())
                                UserRole.ASHA -> onAshaLogin(sampleAshas.first())
                                UserRole.DOCTOR -> onDoctorLogin(sampleDoctors.first())
                                UserRole.ADMIN -> onAdminLogin()
                            }
                        },
                        style = ButtonStyle.PRIMARY
                    )

                    HorizontalDivider(color = VitalSenseBorderSubtle)

                    // 1-Tap Quick Demo Profile Logins
                    Text(
                        text = "⚡ " + strings.quickDemoLogin,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = VitalSenseTextSecondary
                    )

                    when (selectedRole) {
                        UserRole.PATIENT -> {
                            samplePatients.take(3).forEach { patient ->
                                Surface(
                                    onClick = { onPatientLogin(patient) },
                                    shape = PillShape,
                                    color = VitalSenseSurfaceSubtle,
                                    border = BorderStroke(1.dp, VitalSenseBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = patient.name,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = VitalSenseTextPrimary
                                        )
                                        Text(
                                            text = "${patient.villageName} (${patient.gender.firstOrNull() ?: 'P'}/${patient.age}y)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = VitalSenseTextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        UserRole.ASHA -> {
                            sampleAshas.forEach { asha ->
                                Surface(
                                    onClick = { onAshaLogin(asha) },
                                    shape = PillShape,
                                    color = VitalSenseSurfaceSubtle,
                                    border = BorderStroke(1.dp, VitalSenseBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = asha.name,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = VitalSenseTextPrimary
                                        )
                                        Text(
                                            text = "${asha.assignedVillages.firstOrNull() ?: "Rampur"} · ${asha.ashaUniqueId}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = VitalSenseTextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        UserRole.DOCTOR -> {
                            sampleDoctors.forEach { doc ->
                                Surface(
                                    onClick = { onDoctorLogin(doc) },
                                    shape = PillShape,
                                    color = VitalSenseSurfaceSubtle,
                                    border = BorderStroke(1.dp, VitalSenseBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = doc.name,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = VitalSenseTextPrimary
                                        )
                                        Text(
                                            text = doc.specialty.displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = VitalSensePrimary
                                        )
                                    }
                                }
                            }
                        }

                        UserRole.ADMIN -> {
                            Surface(
                                onClick = onAdminLogin,
                                shape = PillShape,
                                color = VitalSenseSurfaceSubtle,
                                border = BorderStroke(1.dp, VitalSenseBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "District Health Officer (Rampur)",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = VitalSenseTextPrimary
                                    )
                                    Text(
                                        text = "CMO Clearance",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VitalSensePrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Offline-First Reassurance Banner
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.xs),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📶 " + strings.offlineBanner,
                    style = MaterialTheme.typography.labelSmall,
                    color = VitalSenseTextSecondary
                )
            }
        }
    }
}

@Composable
private fun RoleCard(
    title: String,
    desc: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 84.dp),
        shape = CardShape,
        color = if (isSelected) VitalSensePrimaryContainer else VitalSenseSurface,
        border = BorderStroke(
            1.5.dp,
            if (isSelected) VitalSensePrimary else VitalSenseBorder
        ),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(Spacing.sm),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) VitalSensePrimary else VitalSenseTextSecondary,
                    modifier = Modifier.size(20.dp)
                )

                if (isSelected) {
                    Surface(
                        shape = CircleShape,
                        color = VitalSensePrimary,
                        modifier = Modifier.size(16.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xxs))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) VitalSensePrimary else VitalSenseTextPrimary
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.labelSmall,
                    color = VitalSenseTextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}
