package com.vitalsense.app.core.state

import android.content.Context
import android.content.SharedPreferences
import com.vitalsense.app.core.data.local.seed.SeedDataProvider
import com.vitalsense.app.core.data.model.*
import com.vitalsense.app.core.ui.theme.AppLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppStateHolder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("vitalsense_preferences", Context.MODE_PRIVATE)
    }

    private val KEY_ONBOARDED = "has_completed_onboarding"
    private val KEY_LANGUAGE = "selected_language_code"
    private val KEY_LOGGED_IN = "is_logged_in"
    private val KEY_SAVED_ROLE = "saved_user_role"

    private val _hasCompletedOnboarding = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDED, false))
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean(KEY_LOGGED_IN, false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val savedRoleName = prefs.getString(KEY_SAVED_ROLE, UserRole.PATIENT.name) ?: UserRole.PATIENT.name
    private val initialRole = try { UserRole.valueOf(savedRoleName) } catch (e: Exception) { UserRole.PATIENT }

    private val _currentRole = MutableStateFlow(initialRole)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    private val savedLangCode = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
    private val initialLang = if (savedLangCode == "hi") AppLanguage.HINDI else AppLanguage.ENGLISH

    private val _currentLanguage = MutableStateFlow(initialLang)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _activePatient = MutableStateFlow(SeedDataProvider.initialPatients.first())
    val activePatient: StateFlow<Patient> = _activePatient.asStateFlow()

    private val _activeAsha = MutableStateFlow(SeedDataProvider.initialAshaWorkers.first())
    val activeAsha: StateFlow<AshaWorker> = _activeAsha.asStateFlow()

    private val _activeDoctor = MutableStateFlow(SeedDataProvider.initialDoctors.first())
    val activeDoctor: StateFlow<Doctor> = _activeDoctor.asStateFlow()

    private val _activeProxyPatient = MutableStateFlow<Patient?>(null)
    val activeProxyPatient: StateFlow<Patient?> = _activeProxyPatient.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    fun completeOnboarding() {
        _hasCompletedOnboarding.value = true
        prefs.edit().putBoolean(KEY_ONBOARDED, true).apply()
    }

    fun resetOnboarding() {
        _hasCompletedOnboarding.value = false
        prefs.edit().putBoolean(KEY_ONBOARDED, false).apply()
    }

    fun login(role: UserRole) {
        _currentRole.value = role
        _isLoggedIn.value = true
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_SAVED_ROLE, role.name)
            .apply()
    }

    fun loginAsPatient(patient: Patient) {
        _activePatient.value = patient
        _currentRole.value = UserRole.PATIENT
        _isLoggedIn.value = true
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_SAVED_ROLE, UserRole.PATIENT.name)
            .apply()
    }

    fun loginAsAsha(asha: AshaWorker) {
        _activeAsha.value = asha
        _currentRole.value = UserRole.ASHA
        _isLoggedIn.value = true
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_SAVED_ROLE, UserRole.ASHA.name)
            .apply()
    }

    fun loginAsDoctor(doctor: Doctor) {
        _activeDoctor.value = doctor
        _currentRole.value = UserRole.DOCTOR
        _isLoggedIn.value = true
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_SAVED_ROLE, UserRole.DOCTOR.name)
            .apply()
    }

    fun loginAsAdmin() {
        _currentRole.value = UserRole.ADMIN
        _isLoggedIn.value = true
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_SAVED_ROLE, UserRole.ADMIN.name)
            .apply()
    }

    fun logout() {
        _isLoggedIn.value = false
        _activeProxyPatient.value = null
        prefs.edit().putBoolean(KEY_LOGGED_IN, false).apply()
    }

    fun switchRole(newRole: UserRole) {
        _currentRole.value = newRole
        prefs.edit().putString(KEY_SAVED_ROLE, newRole.name).apply()
    }

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
    }

    fun toggleLanguage() {
        val newLang = if (_currentLanguage.value == AppLanguage.ENGLISH) AppLanguage.HINDI else AppLanguage.ENGLISH
        setLanguage(newLang)
    }

    fun selectPatient(patient: Patient) {
        _activePatient.value = patient
    }

    fun selectAsha(asha: AshaWorker) {
        _activeAsha.value = asha
    }

    fun selectDoctor(doctor: Doctor) {
        _activeDoctor.value = doctor
    }

    fun setProxyPatient(patient: Patient?) {
        _activeProxyPatient.value = patient
    }

    fun clearProxy() {
        _activeProxyPatient.value = null
    }

    fun toggleOffline() {
        _isOffline.value = !_isOffline.value
    }
}
