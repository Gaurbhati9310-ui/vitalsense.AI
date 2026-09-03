package com.vitalsense.app.core.ui.theme

import androidx.compose.ui.graphics.Color

// =====================================================================
// VitalSense Light-First Healthcare Design Palette
// Inspired by clarity, trust, and simplicity of digital health services
// =====================================================================

// --- Primary Healthcare Blue & Navy Accents ---
val VitalSensePrimary = Color(0xFF0F62FE)            // Vibrant, accessible healthcare blue
val VitalSensePrimaryDark = Color(0xFF0043CE)        // Deep active/pressed blue
val VitalSensePrimaryLight = Color(0xFF4589FF)       // Light blue accent
val VitalSensePrimaryContainer = Color(0xFFEDF5FF)   // 6% blue tint container for badges & cards
val VitalSensePrimaryVariant = Color(0xFF0D5C96)

// --- Backgrounds & Crisp Surfaces ---
val VitalSenseBackground = Color(0xFFF8FAFC)        // Soft neutral off-white background (#F8FAFC)
val VitalSenseSurface = Color(0xFFFFFFFF)           // Crisp pure white card surface
val VitalSenseSurfaceElevated = Color(0xFFFFFFFF)   // Elevated cards with subtle 1dp border
val VitalSenseSurfaceSubtle = Color(0xFFF1F5F9)     // Light neutral container for tags, pills & inputs
val VitalSenseSurfaceHighlight = Color(0xFFE2E8F0)  // Active hover/selection surface

// --- High-Contrast Typography Tokens (Light Theme) ---
val VitalSenseTextPrimary = Color(0xFF0F172A)       // Deep slate navy for headings & primary values
val VitalSenseTextSecondary = Color(0xFF475569)     // Muted slate for descriptions & secondary info
val VitalSenseTextTertiary = Color(0xFF94A3B8)      // Light slate for timestamps, placeholders & captions

// --- Crisp Borders & Dividers ---
val VitalSenseBorder = Color(0xFFE2E8F0)            // Subtle clean card & input border
val VitalSenseBorderSubtle = Color(0xFFF1F5F9)      // Hairline inner divider
val VitalSenseBorderFocused = Color(0xFF0F62FE)     // Focused active input border

// --- Semantic Healthcare Status Colors ---
val VitalSenseSuccess = Color(0xFF16A34A)           // Forest green for healthy, completed & synced states
val VitalSenseSuccessContainer = Color(0xFFDCFCE7)  // Soft green container
val VitalSenseSuccessText = Color(0xFF15803D)

val VitalSenseWarning = Color(0xFFD97706)           // Amber for warnings & moderate risk
val VitalSenseWarningContainer = Color(0xFFFEF3C7)  // Soft amber container
val VitalSenseWarningText = Color(0xFFB45309)

val VitalSenseEmergency = Color(0xFFDC2626)         // Crimson Red ONLY for Emergency SOS & critical triage
val VitalSenseEmergencyContainer = Color(0xFFFEE2E2)// Soft red container
val VitalSenseEmergencyText = Color(0xFFB91C1C)

val VitalSenseInfo = Color(0xFF0284C7)              // Sky blue for notices & informative tags
val VitalSenseInfoContainer = Color(0xFFE0F2FE)

// =====================================================================
// Backward-Compatible Aliases (Mapped to Light-First Healthcare Palette)
// =====================================================================
val GlumeBackground = VitalSenseBackground
val GlumeSurfaceCard = VitalSenseSurface
val GlumeSurfaceElevated = VitalSenseSurfaceSubtle
val GlumeSurfaceSubtle = VitalSenseSurfaceSubtle
val GlumeBorder = VitalSenseBorder
val GlumeBorderSubtle = VitalSenseBorderSubtle
val GlumePrimaryPurple = VitalSensePrimary
val GlumePrimaryPurpleVariant = VitalSensePrimaryDark
val GlumePrimaryPurpleContainer = VitalSensePrimaryContainer
val GlumePrimaryPurpleLight = VitalSensePrimary
val GlumeTextPrimary = VitalSenseTextPrimary
val GlumeTextSecondary = VitalSenseTextSecondary
val GlumeTextTertiary = VitalSenseTextTertiary
val GlumeSuccessMint = VitalSenseSuccess
val GlumeSuccessContainer = VitalSenseSuccessContainer
val GlumeSuccessText = VitalSenseSuccessText
val GlumeAlertCoral = VitalSenseEmergency
val GlumeAlertContainer = VitalSenseEmergencyContainer
val GlumeAlertText = VitalSenseEmergencyText
val GlumeWarningAmber = VitalSenseWarning
val GlumeWarningContainer = VitalSenseWarningContainer

val SurfaceWhite = VitalSenseSurface
val SurfaceCream = VitalSenseSurfaceSubtle
val WarmCreamBackground = VitalSenseBackground
val TextPrimaryNearBlack = VitalSenseTextPrimary
val TextSecondaryMuted = VitalSenseTextSecondary
val TextTertiarySubtle = VitalSenseTextTertiary
val SoftMintSuccess = VitalSenseSuccess
val SoftMintText = VitalSenseSuccessText
val CoralAlert = VitalSenseEmergency
val CoralAlertDark = VitalSenseEmergencyText
val AmberWarning = VitalSenseWarning
val AmberWarningDark = VitalSenseWarningText
val OrangeHighRisk = VitalSenseEmergency
val CardBorderColor = VitalSenseBorder
val CardBorderSubtle = VitalSenseBorderSubtle
val InputBorderColor = VitalSenseBorder
val InputBorderFocused = VitalSenseBorderFocused
val DividerSubtle = VitalSenseBorderSubtle
val LimePrimary = VitalSensePrimary
val DarkCharcoal = VitalSensePrimaryDark
val LavenderSecondary = VitalSenseSurfaceSubtle
val BlushPinkTertiary = VitalSensePrimaryContainer
val PatientLightBackground = VitalSenseBackground
val PatientLightCard = VitalSenseSurface
val PatientLightCardElevated = VitalSenseSurfaceSubtle
val PatientLightBorder = VitalSenseBorder
val PatientLightTextPrimary = VitalSenseTextPrimary
val PatientLightTextSecondary = VitalSenseTextSecondary
val MintGreen = VitalSenseSuccess
val SoftGreyText = VitalSenseTextSecondary
