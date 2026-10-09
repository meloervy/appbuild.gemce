package com.appbuild.gemce.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.appbuild.gemce.domain.model.LanguageMode
import com.appbuild.gemce.domain.profile.AcademicLevel
import com.appbuild.gemce.domain.profile.AcademicOptions
import com.appbuild.gemce.domain.profile.StudentProfile

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingSetupDialog(
    initialProfile: StudentProfile,
    onSaveProfile: (StudentProfile) -> Unit,
    onDismissRequest: (() -> Unit)? = null
) {
    var selectedLevel by remember { mutableStateOf(initialProfile.academicLevel) }
    var selectedStrand by remember { mutableStateOf(initialProfile.seniorHighStrand ?: AcademicOptions.seniorHighStrands.first()) }
    var collegeCourseText by remember { mutableStateOf(initialProfile.collegeCourse ?: "BS Computer Science / IT / IS") }
    var selectedLanguage by remember { mutableStateOf(initialProfile.preferredLanguage) }

    Dialog(onDismissRequest = { onDismissRequest?.invoke() }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Personalize Your Study Buddy",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Select your academic level so Gemce can calibrate difficulty, technical depth, and curriculum context for you.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // 1. Academic Level
                Text(
                    text = "1. Academic Level",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AcademicLevel.values().forEach { level ->
                        FilterChip(
                            selected = selectedLevel == level,
                            onClick = { selectedLevel = level },
                            label = { Text(level.shortLabel) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Specific Strand or Course
                when (selectedLevel) {
                    AcademicLevel.JUNIOR_HIGH -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "💡 Gemce will use clear foundational analogies and step-by-step explanations suited for high school subjects.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    AcademicLevel.SENIOR_HIGH -> {
                        Text(
                            text = "2. Senior High Strand / Track",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AcademicOptions.seniorHighStrands.forEach { strand ->
                                val shortStrand = strand.substringBefore(" (").take(15)
                                FilterChip(
                                    selected = selectedStrand == strand,
                                    onClick = { selectedStrand = strand },
                                    label = { Text(shortStrand) }
                                )
                            }
                        }
                        Text(
                            text = "Selected: $selectedStrand",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    AcademicLevel.COLLEGE -> {
                        Text(
                            text = "2. College Course / Program",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = collegeCourseText,
                            onValueChange = { collegeCourseText = it },
                            label = { Text("Your Degree / Major") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Quick suggestions:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            AcademicOptions.commonCollegeCourses.take(6).forEach { course ->
                                val chipLabel = course.substringBefore(" /").take(14)
                                FilterChip(
                                    selected = collegeCourseText == course,
                                    onClick = { collegeCourseText = course },
                                    label = { Text(chipLabel) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Preferred Study Language (Default English)
                Text(
                    text = "3. Preferred Study Language",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Default is English. You can switch to Tagalog, Taglish, or Bisaya anytime.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        LanguageMode.EN,
                        LanguageMode.FIL,
                        LanguageMode.TAGLISH,
                        LanguageMode.CEB
                    ).forEach { lang ->
                        FilterChip(
                            selected = selectedLanguage == lang,
                            onClick = { selectedLanguage = lang },
                            label = { Text(lang.label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDismissRequest != null && initialProfile.isConfigured) {
                        OutlinedButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Cancel")
                        }
                    }
                    Button(
                        onClick = {
                            val newProfile = StudentProfile(
                                academicLevel = selectedLevel,
                                seniorHighStrand = if (selectedLevel == AcademicLevel.SENIOR_HIGH) selectedStrand else null,
                                collegeCourse = if (selectedLevel == AcademicLevel.COLLEGE) collegeCourseText.trim() else null,
                                preferredLanguage = selectedLanguage,
                                isConfigured = true
                            )
                            onSaveProfile(newProfile)
                        }
                    ) {
                        Text(if (initialProfile.isConfigured) "Save Changes" else "Start Studying")
                    }
                }
            }
        }
    }
}
