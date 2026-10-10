package com.appbuild.gemce.domain.profile

import android.content.Context
import android.content.SharedPreferences
import com.appbuild.gemce.domain.model.LanguageMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UserProfileRepository {

    private const val PREFS_NAME = "gemce_user_profile_prefs"
    private const val KEY_ACADEMIC_LEVEL = "academic_level"
    private const val KEY_STRAND = "senior_high_strand"
    private const val KEY_COLLEGE_COURSE = "college_course"
    private const val KEY_PREFERRED_LANG = "preferred_language"
    private const val KEY_IS_CONFIGURED = "is_configured"

    private var prefs: SharedPreferences? = null

    private val _studentProfile = MutableStateFlow(
        StudentProfile(
            academicLevel = AcademicLevel.COLLEGE,
            seniorHighStrand = null,
            collegeCourse = null,
            preferredLanguage = LanguageMode.EN,
            isConfigured = false
        )
    )
    val studentProfile: StateFlow<StudentProfile> = _studentProfile.asStateFlow()

    fun initialize(context: Context) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp

        val isConfigured = sp.getBoolean(KEY_IS_CONFIGURED, false)
        val levelName = sp.getString(KEY_ACADEMIC_LEVEL, AcademicLevel.COLLEGE.name) ?: AcademicLevel.COLLEGE.name
        val level = try {
            AcademicLevel.valueOf(levelName)
        } catch (e: Exception) {
            AcademicLevel.COLLEGE
        }

        val strand = sp.getString(KEY_STRAND, null)
        val course = sp.getString(KEY_COLLEGE_COURSE, null)
        val langCode = sp.getString(KEY_PREFERRED_LANG, LanguageMode.EN.code) ?: LanguageMode.EN.code
        val lang = LanguageMode.values().find { it.code == langCode } ?: LanguageMode.EN

        _studentProfile.value = StudentProfile(
            academicLevel = level,
            seniorHighStrand = strand,
            collegeCourse = course,
            preferredLanguage = lang,
            isConfigured = isConfigured
        )
    }

    fun updateProfile(profile: StudentProfile) {
        _studentProfile.value = profile
        prefs?.edit()?.apply {
            putString(KEY_ACADEMIC_LEVEL, profile.academicLevel.name)
            putString(KEY_STRAND, profile.seniorHighStrand)
            putString(KEY_COLLEGE_COURSE, profile.collegeCourse)
            putString(KEY_PREFERRED_LANG, profile.preferredLanguage.code)
            putBoolean(KEY_IS_CONFIGURED, true)
            apply()
        }
    }
}
