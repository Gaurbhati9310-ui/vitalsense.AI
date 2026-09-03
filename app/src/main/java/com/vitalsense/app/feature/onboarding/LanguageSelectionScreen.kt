package com.vitalsense.app.feature.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vitalsense.app.core.ui.components.ButtonStyle
import com.vitalsense.app.core.ui.components.VitalSenseButton
import com.vitalsense.app.core.ui.theme.*

data class SupportedLanguageOption(
    val language: AppLanguage,
    val nativeName: String,
    val englishName: String,
    val isPrimarySupported: Boolean = true
)

@Composable
fun LanguageSelectionScreen(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedLanguage by remember { mutableStateOf(currentLanguage) }

    val languages = remember {
        listOf(
            SupportedLanguageOption(AppLanguage.ENGLISH, "English", "English", true),
            SupportedLanguageOption(AppLanguage.HINDI, "हिन्दी", "Hindi", true)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VitalSenseBackground)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
        ) {
            Text(
                text = "🌐 Choose your language",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                color = VitalSenseTextPrimary
            )

            Text(
                text = "Select the language you are most comfortable with. You can change this anytime from settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = VitalSenseTextSecondary
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                items(languages) { option ->
                    val isSelected = selectedLanguage == option.language

                    Surface(
                        onClick = {
                            selectedLanguage = option.language
                            onLanguageSelected(option.language)
                        },
                        shape = CardShape,
                        color = if (isSelected) VitalSensePrimaryContainer else VitalSenseSurface,
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) VitalSensePrimary else VitalSenseBorder
                        ),
                        shadowElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = option.nativeName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) VitalSensePrimary else VitalSenseTextPrimary
                                )
                                Text(
                                    text = option.englishName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VitalSenseTextSecondary
                                )
                            }

                            if (isSelected) {
                                Surface(
                                    shape = CircleShape,
                                    color = VitalSensePrimary,
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Transparent,
                                    border = BorderStroke(1.5.dp, VitalSenseBorder),
                                    modifier = Modifier.size(24.dp)
                                ) {}
                            }
                        }
                    }
                }
            }
        }

        // Bottom Continue Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            VitalSenseButton(
                text = "Continue →",
                onClick = onContinue,
                style = ButtonStyle.PRIMARY
            )
        }
    }
}
