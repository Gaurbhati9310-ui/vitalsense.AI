package com.vitalsense.app.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vitalsense.app.core.ui.theme.*

enum class ButtonStyle {
    PRIMARY,   // Healthcare Blue (#0F62FE) with white text
    DARK,      // Deep Navy Blue (#0043CE)
    SECONDARY, // Subtle neutral surface (#F1F5F9)
    DANGER,    // Emergency Red (#DC2626)
    OUTLINED   // Outlined with 1dp border
}

@Composable
fun VitalSenseButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.PRIMARY,
    icon: (@Composable () -> Unit)? = null,
    minHeight: Dp = 48.dp,
    enabled: Boolean = true
) {
    val containerColor = when (style) {
        ButtonStyle.PRIMARY -> VitalSensePrimary
        ButtonStyle.DARK -> VitalSensePrimaryDark
        ButtonStyle.SECONDARY -> VitalSenseSurfaceSubtle
        ButtonStyle.DANGER -> VitalSenseEmergency
        ButtonStyle.OUTLINED -> Color.Transparent
    }

    val contentColor = when (style) {
        ButtonStyle.PRIMARY -> Color.White
        ButtonStyle.DARK -> Color.White
        ButtonStyle.SECONDARY -> VitalSenseTextPrimary
        ButtonStyle.DANGER -> Color.White
        ButtonStyle.OUTLINED -> VitalSensePrimary
    }

    val border = if (style == ButtonStyle.OUTLINED) {
        BorderStroke(1.dp, VitalSenseBorder)
    } else null

    Button(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .fillMaxWidth(),
        enabled = enabled,
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = VitalSenseSurfaceSubtle,
            disabledContentColor = VitalSenseTextTertiary
        ),
        border = border,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(Spacing.xs))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
