package com.vitalsense.app.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vitalsense.app.core.data.model.ConditionCategory
import com.vitalsense.app.core.ui.theme.*

@Composable
fun CategoryChip(
    category: ConditionCategory,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val categoryName = when (category) {
        ConditionCategory.GENERAL_MEDICINE -> strings.catGeneralMedicine
        ConditionCategory.MATERNAL_HEALTH -> strings.catMaternalHealth
        ConditionCategory.FITNESS -> strings.catFitness
        ConditionCategory.NUTRITION -> strings.catNutrition
        ConditionCategory.MENTAL_HEALTH -> strings.catMentalHealth
        ConditionCategory.EMERGENCY -> strings.catEmergency
    }

    Surface(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 44.dp),
        shape = CardShape,
        color = if (isSelected) VitalSensePrimaryContainer else VitalSenseSurface,
        border = if (isSelected) BorderStroke(1.5.dp, VitalSensePrimary) else BorderStroke(1.dp, VitalSenseBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(text = category.emoji, style = MaterialTheme.typography.titleMedium)
            Text(
                text = categoryName,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) VitalSensePrimary else VitalSenseTextPrimary
                )
            )
        }
    }
}
