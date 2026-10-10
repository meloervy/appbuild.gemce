package com.appbuild.gemce.domain.profile

import com.appbuild.gemce.domain.model.LanguageMode
import kotlinx.serialization.Serializable

@Serializable
enum class AcademicLevel(val displayName: String, val shortLabel: String) {
    JUNIOR_HIGH("High School / Junior High (Grades 7-10)", "Junior High"),
    SENIOR_HIGH("Senior High School (Grades 11-12)", "Senior High"),
    COLLEGE("College / University", "College")
}

object AcademicOptions {
    val seniorHighStrands = listOf(
        "GAS (General Academic Strand)",
        "HUMSS (Humanities & Social Sciences)",
        "ABM (Accountancy, Business & Management)",
        "STEM (Science, Technology, Engineering & Math)",
        "TVL - ICT (Information & Comm. Technology)",
        "TVL - Home Economics",
        "TVL - Industrial Arts / Agri-Fishery",
        "Arts & Design Track",
        "Sports Track"
    )

    val commonCollegeCourses = listOf(
        "General Education / Liberal Arts",
        "BS Accountancy / Management Accounting",
        "Bachelor of Secondary Education",
        "BS Nursing / Health Sciences",
        "BS Computer Science / IT / IS",
        "BS Psychology / Behavioral Sciences",
        "BA Communication / Journalism",
        "BS Civil / Mechanical / Electrical Eng.",
        "BS Biology / Chemistry / Physics",
        "BS Criminology / Law Enforcement",
        "Custom / Other Degree"
    )
}

@Serializable
data class StudentProfile(
    val academicLevel: AcademicLevel = AcademicLevel.COLLEGE,
    val seniorHighStrand: String? = null,
    val collegeCourse: String? = null,
    val preferredLanguage: LanguageMode = LanguageMode.EN,
    val isConfigured: Boolean = false
) {
    fun getFullAcademicContext(): String {
        return when (academicLevel) {
            AcademicLevel.JUNIOR_HIGH -> "Junior High School (Grades 7-10)"
            AcademicLevel.SENIOR_HIGH -> "Senior High School — ${seniorHighStrand ?: "General Strand"}"
            AcademicLevel.COLLEGE -> "College / University${if (collegeCourse.isNullOrBlank()) "" else " — $collegeCourse"}"
        }
    }

    fun getBadgeText(): String {
        return when (academicLevel) {
            AcademicLevel.JUNIOR_HIGH -> "High School"
            AcademicLevel.SENIOR_HIGH -> seniorHighStrand?.substringBefore(" ") ?: "Senior High"
            AcademicLevel.COLLEGE -> if (collegeCourse.isNullOrBlank()) "College" else collegeCourse.substringBefore(" /")
        }
    }
}
