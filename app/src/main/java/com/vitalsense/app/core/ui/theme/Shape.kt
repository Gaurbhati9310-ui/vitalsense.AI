package com.vitalsense.app.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val VitalSenseShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),    // Clean standard card radius (12dp)
    large = RoundedCornerShape(16.dp),     // Dialogs & sheets (16dp)
    extraLarge = RoundedCornerShape(50.dp) // Full Pill buttons/chips
)

val CardShape = RoundedCornerShape(12.dp)
val StatCardShape = RoundedCornerShape(12.dp)
val PillShape = RoundedCornerShape(50.dp)
val InputShape = RoundedCornerShape(8.dp)
val DialogShape = RoundedCornerShape(16.dp)
val BottomSheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
