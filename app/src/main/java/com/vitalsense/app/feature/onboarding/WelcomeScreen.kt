package com.vitalsense.app.feature.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitalsense.app.core.ui.components.ButtonStyle
import com.vitalsense.app.core.ui.components.VitalSenseButton
import com.vitalsense.app.core.ui.components.VitalSenseCard
import com.vitalsense.app.core.ui.theme.*

@Composable
fun WelcomeScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VitalSenseBackground)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Brand Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.md),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Surface(
                    shape = CircleShape,
                    color = VitalSensePrimaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = VitalSensePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = "VitalSense",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = VitalSenseTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            Text(
                text = "Healthcare,\nconnected.",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    lineHeight = 34.sp
                ),
                color = VitalSenseTextPrimary
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            Text(
                text = "Access clinical care, manage your health records, and stay connected with doctors and ASHA workers.",
                style = MaterialTheme.typography.bodyMedium,
                color = VitalSenseTextSecondary
            )
        }

        // Center Pillar Cards
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            PillarItem(
                icon = Icons.Default.HealthAndSafety,
                title = "Clinical Standard Care",
                description = "Direct consultation and triage with PHC doctors and specialists."
            )
            PillarItem(
                icon = Icons.Default.Lock,
                title = "Trusted & Secure",
                description = "Digital health records linked with national ABHA healthcare identity."
            )
            PillarItem(
                icon = Icons.Default.WifiOff,
                title = "Offline-First Reliability",
                description = "Complete vital monitoring and prescriptions work seamlessly without internet."
            )
        }

        // Bottom CTA Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            VitalSenseButton(
                text = "Get Started →",
                onClick = onContinue,
                style = ButtonStyle.PRIMARY
            )

            Text(
                text = "Trusted · Secure · Accessible",
                style = MaterialTheme.typography.labelSmall,
                color = VitalSenseTextTertiary
            )
        }
    }
}

@Composable
private fun PillarItem(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CardShape,
        color = VitalSenseSurface,
        border = BorderStroke(1.dp, VitalSenseBorder),
        shadowElevation = 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(Spacing.sm),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Surface(
                shape = CircleShape,
                color = VitalSensePrimaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = VitalSensePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = VitalSenseTextPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = VitalSenseTextSecondary
                )
            }
        }
    }
}
