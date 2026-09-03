package com.vitalsense.app.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

// --- VitalSense Light-First Healthcare Color Scheme ---
private val VitalSenseLightColorScheme = lightColorScheme(
    primary = VitalSensePrimary,
    onPrimary = VitalSenseSurface,
    primaryContainer = VitalSensePrimaryContainer,
    onPrimaryContainer = VitalSensePrimary,

    secondary = VitalSensePrimaryVariant,
    onSecondary = VitalSenseSurface,
    secondaryContainer = VitalSenseSurfaceSubtle,
    onSecondaryContainer = VitalSenseTextSecondary,

    tertiary = VitalSenseSuccess,
    onTertiary = VitalSenseSurface,
    tertiaryContainer = VitalSenseSuccessContainer,
    onTertiaryContainer = VitalSenseSuccessText,

    background = VitalSenseBackground,
    onBackground = VitalSenseTextPrimary,

    surface = VitalSenseSurface,
    onSurface = VitalSenseTextPrimary,
    surfaceVariant = VitalSenseSurfaceSubtle,
    onSurfaceVariant = VitalSenseTextSecondary,

    error = VitalSenseEmergency,
    onError = VitalSenseSurface,
    errorContainer = VitalSenseEmergencyContainer,
    onErrorContainer = VitalSenseEmergencyText,

    outline = VitalSenseBorder,
    outlineVariant = VitalSenseBorderSubtle
)

@Composable
fun VitalSenseTheme(
    language: AppLanguage = AppLanguage.ENGLISH,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalSpacing provides VitalSenseSpacing(),
        LocalAppStrings provides AppLanguageManager.getStrings(language)
    ) {
        MaterialTheme(
            colorScheme = VitalSenseLightColorScheme,
            typography = VitalSenseTypography,
            shapes = VitalSenseShapes,
            content = content
        )
    }
}
