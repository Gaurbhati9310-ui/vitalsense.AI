package com.vitalsense.app.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitalsense.app.core.ui.theme.*

/**
 * VitalSense Stat Card:
 * Clean, light-first metric card displaying icon/label + bold value.
 */
@Composable
fun GlumeStatCard(
    label: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    badgeText: String? = null,
    badgeColor: Color = VitalSensePrimary,
    onClick: (() -> Unit)? = null
) {
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = modifier.defaultMinSize(minHeight = 80.dp),
        shape = StatCardShape,
        color = VitalSenseSurface,
        border = BorderStroke(1.dp, VitalSenseBorder),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top row: Label & Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = VitalSenseTextSecondary,
                    maxLines = 1
                )
                Text(
                    text = icon,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xxs))

            // Bottom row: Bold Value + Unit / Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = VitalSenseTextPrimary
                    )
                    if (unit != null) {
                        Text(
                            text = unit,
                            style = MaterialTheme.typography.bodySmall,
                            color = VitalSenseTextSecondary,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }

                if (badgeText != null) {
                    Surface(
                        shape = PillShape,
                        color = badgeColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * VitalSense Circular Progress Ring Component:
 * Renders completion percentage / count inside an arc ring.
 */
@Composable
fun GlumeProgressRing(
    progressFraction: Float, // 0.0 to 1.0
    size: Dp = 64.dp,
    strokeWidth: Dp = 6.dp,
    ringColor: Color = VitalSenseSuccess,
    trackColor: Color = VitalSenseSurfaceSubtle,
    modifier: Modifier = Modifier,
    centerContent: @Composable () -> Unit = {
        Text(
            text = "${(progressFraction * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = VitalSenseTextPrimary
        )
    }
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()

            // Background Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Progress Arc
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progressFraction.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        centerContent()
    }
}
