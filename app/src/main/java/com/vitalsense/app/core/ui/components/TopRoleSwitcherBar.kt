package com.vitalsense.app.core.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vitalsense.app.core.data.model.Patient
import com.vitalsense.app.core.data.model.UserRole
import com.vitalsense.app.core.ui.theme.*

@Composable
fun TopRoleSwitcherBar(
    currentRole: UserRole,
    activeUserName: String = "",
    activeProxyPatient: Patient? = null,
    onExitProxy: () -> Unit = {},
    isOffline: Boolean = false,
    onToggleOffline: () -> Unit = {},
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    onToggleLanguage: () -> Unit = {},
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(VitalSenseSurface)
    ) {
        // Main App Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App Logo & Role Scoped User Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Surface(
                    shape = CircleShape,
                    color = VitalSensePrimaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (currentRole) {
                                UserRole.PATIENT -> Icons.Default.Person
                                UserRole.ASHA -> Icons.Default.VolunteerActivism
                                UserRole.DOCTOR -> Icons.Default.MedicalServices
                                UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                            },
                            contentDescription = "Role Avatar",
                            tint = VitalSensePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = if (activeUserName.isNotBlank()) activeUserName else strings.appName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = VitalSenseTextPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = when (currentRole) {
                            UserRole.PATIENT -> strings.patientPortal
                            UserRole.ASHA -> strings.ashaPortal
                            UserRole.DOCTOR -> strings.doctorPortal
                            UserRole.ADMIN -> strings.adminPortal
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = VitalSenseTextSecondary
                    )
                }
            }

            // Right Actions: Language Toggle, Connectivity Pill & Logout
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)
            ) {
                // Global Language Switcher Pill
                Surface(
                    onClick = onToggleLanguage,
                    shape = PillShape,
                    color = VitalSenseSurfaceSubtle,
                    border = BorderStroke(1.dp, VitalSenseBorder),
                    modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)
                    ) {
                        Text(text = "🌐", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = if (currentLanguage == AppLanguage.ENGLISH) "हिंदी" else "EN",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = VitalSenseTextPrimary
                        )
                    }
                }

                // Connectivity Mode Pill
                Surface(
                    onClick = onToggleOffline,
                    shape = PillShape,
                    color = if (isOffline) VitalSenseSurfaceSubtle else VitalSenseSuccessContainer,
                    border = BorderStroke(1.dp, if (isOffline) VitalSenseBorder else VitalSenseSuccess.copy(alpha = 0.3f)),
                    modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isOffline) VitalSenseTextTertiary else VitalSenseSuccess)
                        )
                        Text(
                            text = if (isOffline) strings.offline else strings.online,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isOffline) VitalSenseTextSecondary else VitalSenseSuccessText
                        )
                    }
                }

                // Logout / Exit Button
                Surface(
                    onClick = onLogout,
                    shape = PillShape,
                    color = VitalSenseSurfaceSubtle,
                    border = BorderStroke(1.dp, VitalSenseBorder),
                    modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Exit",
                            tint = VitalSenseTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = strings.exit,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = VitalSenseTextPrimary
                        )
                    }
                }
            }
        }

        // ASHA Proxy Indicator Banner
        AnimatedVisibility(
            visible = activeProxyPatient != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            if (activeProxyPatient != null) {
                Surface(
                    color = VitalSensePrimaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.xxs),
                    shape = CardShape,
                    border = BorderStroke(1.dp, VitalSensePrimary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolunteerActivism,
                                contentDescription = null,
                                tint = VitalSensePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    text = strings.actingAsProxy,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = VitalSensePrimary
                                )
                                Text(
                                    text = "${activeProxyPatient.name} (${activeProxyPatient.villageName})",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = VitalSenseTextPrimary
                                )
                            }
                        }
                        Button(
                            onClick = onExitProxy,
                            shape = PillShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VitalSensePrimary,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(text = strings.exitProxy, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = VitalSenseBorder
        )
    }
}
