package com.vitalsense.app.feature.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vitalsense.app.core.data.model.*
import com.vitalsense.app.core.data.repository.VitalSenseRepository
import com.vitalsense.app.core.state.AppStateHolder
import com.vitalsense.app.core.ui.components.TopRoleSwitcherBar
import com.vitalsense.app.feature.admin.AdminHomeScreen
import com.vitalsense.app.feature.admin.AdminQueueOversightViewModel
import com.vitalsense.app.feature.admin.QueueOversightScreen
import com.vitalsense.app.feature.asha.AshaHomeScreen
import com.vitalsense.app.feature.auth.AuthEntryScreen
import com.vitalsense.app.feature.auth.CreateAccountScreen
import com.vitalsense.app.feature.auth.LoginScreen
import com.vitalsense.app.feature.doctor.CaseDetailScreen
import com.vitalsense.app.feature.doctor.DoctorHomeScreen
import com.vitalsense.app.feature.doctor.DoctorQueueScreen
import com.vitalsense.app.feature.doctor.DoctorViewModel
import com.vitalsense.app.feature.onboarding.LanguageSelectionScreen
import com.vitalsense.app.feature.onboarding.SplashScreen
import com.vitalsense.app.feature.onboarding.WelcomeScreen
import com.vitalsense.app.feature.patient.AppointmentsScreen
import com.vitalsense.app.feature.patient.PatientHomeScreen
import com.vitalsense.app.feature.patient.PatientQueueViewModel
import com.vitalsense.app.feature.patient.PatientViewModel
import com.vitalsense.app.feature.patient.QueueStatusScreen
import kotlinx.coroutines.launch

enum class OnboardingState {
    SPLASH,
    WELCOME,
    LANGUAGE,
    AUTH_CHOICE,
    REGISTER,
    LOGIN
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun VitalSenseNavGraph(
    appStateHolder: AppStateHolder,
    repository: VitalSenseRepository,
    patientViewModel: PatientViewModel = hiltViewModel(),
    doctorViewModel: DoctorViewModel = hiltViewModel(),
    patientQueueViewModel: PatientQueueViewModel = hiltViewModel(),
    adminQueueOversightViewModel: AdminQueueOversightViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // Core global state
    val isLoggedIn by appStateHolder.isLoggedIn.collectAsStateWithLifecycle()
    val hasCompletedOnboarding by appStateHolder.hasCompletedOnboarding.collectAsStateWithLifecycle()
    val currentRole by appStateHolder.currentRole.collectAsStateWithLifecycle()
    val activePatient by appStateHolder.activePatient.collectAsStateWithLifecycle()
    val activeAsha by appStateHolder.activeAsha.collectAsStateWithLifecycle()
    val activeDoctor by appStateHolder.activeDoctor.collectAsStateWithLifecycle()
    val activeProxyPatient by appStateHolder.activeProxyPatient.collectAsStateWithLifecycle()
    val isOffline by appStateHolder.isOffline.collectAsStateWithLifecycle()

    // Doctor specific scoped streams
    val doctorCases by doctorViewModel.scopedCases.collectAsStateWithLifecycle()
    val doctorAppointments by doctorViewModel.appointments.collectAsStateWithLifecycle()
    val doctorDispensaryStock by doctorViewModel.dispensaryStock.collectAsStateWithLifecycle()
    val selectedDoctorCase by doctorViewModel.selectedCase.collectAsStateWithLifecycle()
    val patientPrescriptions by doctorViewModel.patientPrescriptions.collectAsStateWithLifecycle()
    val patientProfile by doctorViewModel.patientProfile.collectAsStateWithLifecycle()
    val doctorTodaysQueue by doctorViewModel.todaysQueue.collectAsStateWithLifecycle()
    val doctorTodaysSlot by doctorViewModel.todaysSlotConfig.collectAsStateWithLifecycle()

    // Patient queue & appointment streams
    val patientQueueEntry by patientQueueViewModel.activeQueueEntry.collectAsStateWithLifecycle()
    val peopleAheadCount by patientQueueViewModel.peopleAheadCount.collectAsStateWithLifecycle()
    val estimatedWaitSeconds by patientQueueViewModel.estimatedWaitSeconds.collectAsStateWithLifecycle()
    val doctorsList by patientQueueViewModel.doctors.collectAsStateWithLifecycle()

    // Admin queue oversight streams
    val adminDoctorSummaries by adminQueueOversightViewModel.doctorQueueSummaries.collectAsStateWithLifecycle()
    val adminSelectedDoctorSummary by adminQueueOversightViewModel.selectedDoctorForDrillDown.collectAsStateWithLifecycle()
    val adminSelectedDoctorQueue by adminQueueOversightViewModel.selectedDoctorFullQueue.collectAsStateWithLifecycle()

    // Data streams from repository for general components
    val villages by repository.getVillages().collectAsStateWithLifecycle(initialValue = emptyList())
    val patients by repository.getPatients().collectAsStateWithLifecycle(initialValue = emptyList())
    val notices by repository.getNotices().collectAsStateWithLifecycle(initialValue = emptyList())
    val allPrescriptions by repository.getPrescriptions().collectAsStateWithLifecycle(initialValue = emptyList())
    val allConditions by repository.getConditionRecords().collectAsStateWithLifecycle(initialValue = emptyList())
    val allAppointments by repository.getAppointments().collectAsStateWithLifecycle(initialValue = emptyList())

    val currentLanguage by appStateHolder.currentLanguage.collectAsStateWithLifecycle()

    // Effective Patient for UI (Proxied Patient if set by ASHA, otherwise default patient)
    val effectivePatient = activeProxyPatient ?: activePatient

    val activeUserName = when (currentRole) {
        UserRole.PATIENT -> effectivePatient.name
        UserRole.ASHA -> activeAsha.name
        UserRole.DOCTOR -> activeDoctor.name
        UserRole.ADMIN -> "District CMO (Rampur)"
    }

    // Dynamic Top Role Switcher Bar with role-specific items
    Scaffold(
        topBar = {
            if (isLoggedIn) {
                TopRoleSwitcherBar(
                    currentRole = currentRole,
                    activeUserName = activeUserName,
                    activeProxyPatient = activeProxyPatient,
                    onExitProxy = {
                        appStateHolder.clearProxy()
                        appStateHolder.switchRole(UserRole.ASHA)
                    },
                    isOffline = isOffline,
                    onToggleOffline = {
                        appStateHolder.toggleOffline()
                    },
                    currentLanguage = currentLanguage,
                    onToggleLanguage = {
                        appStateHolder.toggleLanguage()
                    },
                    onLogout = {
                        doctorViewModel.clearSelectedCase()
                        appStateHolder.logout()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = isLoggedIn,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(250))
                },
                label = "AuthTransition"
            ) { loggedIn ->
                if (!loggedIn) {
                    var onboardingState by remember {
                        mutableStateOf(if (hasCompletedOnboarding) OnboardingState.LOGIN else OnboardingState.SPLASH)
                    }

                    AnimatedContent(
                        targetState = onboardingState,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(280)) togetherWith fadeOut(animationSpec = tween(220))
                        },
                        label = "OnboardingTransition"
                    ) { state ->
                        when (state) {
                            OnboardingState.SPLASH -> {
                                SplashScreen(
                                    onSplashFinished = {
                                        onboardingState = if (hasCompletedOnboarding) OnboardingState.LOGIN else OnboardingState.WELCOME
                                    }
                                )
                            }
                            OnboardingState.WELCOME -> {
                                WelcomeScreen(
                                    onContinue = { onboardingState = OnboardingState.LANGUAGE }
                                )
                            }
                            OnboardingState.LANGUAGE -> {
                                LanguageSelectionScreen(
                                    currentLanguage = currentLanguage,
                                    onLanguageSelected = { appStateHolder.setLanguage(it) },
                                    onContinue = { onboardingState = OnboardingState.AUTH_CHOICE }
                                )
                            }
                            OnboardingState.AUTH_CHOICE -> {
                                AuthEntryScreen(
                                    onNavigateToLogin = { onboardingState = OnboardingState.LOGIN },
                                    onNavigateToRegister = { onboardingState = OnboardingState.REGISTER },
                                    onToggleLanguage = { appStateHolder.toggleLanguage() },
                                    currentLanguage = currentLanguage
                                )
                            }
                            OnboardingState.REGISTER -> {
                                CreateAccountScreen(
                                    onBack = { onboardingState = OnboardingState.AUTH_CHOICE },
                                    onAccountCreated = { newPatient ->
                                        coroutineScope.launch {
                                            repository.savePatient(newPatient)
                                            appStateHolder.completeOnboarding()
                                            appStateHolder.loginAsPatient(newPatient)
                                        }
                                    }
                                )
                            }
                            OnboardingState.LOGIN -> {
                                LoginScreen(
                                    currentLanguage = currentLanguage,
                                    onToggleLanguage = { appStateHolder.toggleLanguage() },
                                    onPatientLogin = { selectedPatient ->
                                        appStateHolder.completeOnboarding()
                                        appStateHolder.loginAsPatient(selectedPatient)
                                    },
                                    onAshaLogin = { selectedAsha ->
                                        appStateHolder.completeOnboarding()
                                        appStateHolder.loginAsAsha(selectedAsha)
                                    },
                                    onDoctorLogin = { selectedDoctor ->
                                        appStateHolder.completeOnboarding()
                                        appStateHolder.loginAsDoctor(selectedDoctor)
                                    },
                                    onAdminLogin = {
                                        appStateHolder.completeOnboarding()
                                        appStateHolder.loginAsAdmin()
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentRole,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                            },
                            label = "RoleTransition"
                        ) { role ->
                            when (role) {
                                UserRole.PATIENT -> {
                                    var showMentalWellness by remember { mutableStateOf(false) }
                                    var showAppointments by remember { mutableStateOf(false) }
                                    var showQueueStatus by remember { mutableStateOf(false) }

                                    when {
                                        showQueueStatus && patientQueueEntry != null -> {
                                            BackHandler {
                                                showQueueStatus = false
                                            }

                                            QueueStatusScreen(
                                                entry = patientQueueEntry!!,
                                                peopleAhead = peopleAheadCount,
                                                estimatedWaitSeconds = estimatedWaitSeconds,
                                                onCancel = { entryId ->
                                                    patientQueueViewModel.cancelQueueEntry(entryId)
                                                    showQueueStatus = false
                                                },
                                                onBack = { showQueueStatus = false }
                                            )
                                        }

                                        showAppointments -> {
                                            BackHandler {
                                                showAppointments = false
                                            }

                                            AppointmentsScreen(
                                                appointments = allAppointments.filter { it.patientId == effectivePatient.id },
                                                activeQueueEntry = patientQueueEntry,
                                                doctors = doctorsList,
                                                onCheckIn = { apptId ->
                                                    patientQueueViewModel.checkIn(apptId)
                                                },
                                                onJoinWalkIn = { docId ->
                                                    patientQueueViewModel.joinWalkIn(docId)
                                                },
                                                onViewQueueStatus = {
                                                    showQueueStatus = true
                                                },
                                                onRequestNew = {
                                                    // Request new appointment
                                                },
                                                onBack = { showAppointments = false }
                                            )
                                        }

                                        showMentalWellness -> {
                                            BackHandler {
                                                showMentalWellness = false
                                            }

                                            com.vitalsense.app.feature.patient.mentalhealth.MentalWellnessScreen(
                                                patient = effectivePatient,
                                                onLogMood = { notes, severity ->
                                                    patientViewModel.logMentalWellness(
                                                        patient = effectivePatient,
                                                        moodNotes = notes,
                                                        severityLevel = severity,
                                                        isProxy = activeProxyPatient != null
                                                    )
                                                },
                                                onBack = { showMentalWellness = false }
                                            )
                                        }

                                        else -> {
                                            if (activeProxyPatient != null) {
                                                BackHandler {
                                                    appStateHolder.clearProxy()
                                                    appStateHolder.switchRole(UserRole.ASHA)
                                                }
                                            } else {
                                                BackHandler {
                                                    appStateHolder.logout()
                                                }
                                            }

                                            PatientHomeScreen(
                                                patient = effectivePatient,
                                                notices = notices,
                                                prescriptions = allPrescriptions.filter { it.patientId == effectivePatient.id },
                                                activeQueueEntry = patientQueueEntry,
                                                onCategoryClick = { category ->
                                                    if (category == ConditionCategory.MENTAL_HEALTH) {
                                                        showMentalWellness = true
                                                    }
                                                },
                                                onViewHealthCard = {
                                                    // Health card view hook
                                                },
                                                onOpenAppointments = {
                                                    showAppointments = true
                                                },
                                                onOpenQueueStatus = {
                                                    showQueueStatus = true
                                                },
                                                onTriggerSos = {
                                                    coroutineScope.launch {
                                                        repository.triggerEmergencySos(effectivePatient, null, null)
                                                    }
                                                },
                                                onSavePrescription = { rx ->
                                                    coroutineScope.launch {
                                                        repository.savePrescription(rx)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }

                                UserRole.ASHA -> {
                                    BackHandler {
                                        appStateHolder.logout()
                                    }

                                    AshaHomeScreen(
                                        asha = activeAsha,
                                        patients = patients.filter { it.ashaWorkerId == activeAsha.id },
                                        notices = notices,
                                        onSelectProxyPatient = { selectedPatient ->
                                            appStateHolder.setProxyPatient(selectedPatient)
                                            appStateHolder.switchRole(UserRole.PATIENT)
                                        },
                                        onRegisterPatientClick = {},
                                        onSendNoticeClick = {},
                                        onSavePrescription = { rx ->
                                            coroutineScope.launch {
                                                repository.savePrescription(rx)
                                            }
                                        }
                                    )
                                }

                                UserRole.DOCTOR -> {
                                    var showDoctorQueue by remember { mutableStateOf(false) }
                                    val activeCase = selectedDoctorCase

                                    when {
                                        showDoctorQueue -> {
                                            BackHandler {
                                                showDoctorQueue = false
                                            }

                                            DoctorQueueScreen(
                                                doctor = activeDoctor,
                                                queueEntries = doctorTodaysQueue,
                                                slotConfig = doctorTodaysSlot,
                                                patients = patients,
                                                onCallNext = { doctorViewModel.callNext() },
                                                onStartConsultation = { entryId -> doctorViewModel.startConsultation(entryId) },
                                                onCompleteConsultation = { entryId, notes -> doctorViewModel.completeConsultation(entryId, notes) },
                                                onSkip = { entryId -> doctorViewModel.skipEntry(entryId) },
                                                onMarkNoShow = { entryId -> doctorViewModel.markNoShow(entryId) },
                                                onTogglePriority = { entryId -> doctorViewModel.prioritizeEntry(entryId) },
                                                onAddWalkIn = { patId, patName -> doctorViewModel.addWalkIn(patId, patName) },
                                                onSaveSlotConfig = { config -> doctorViewModel.saveSlotConfig(config) },
                                                onBack = { showDoctorQueue = false }
                                            )
                                        }

                                        activeCase != null -> {
                                            BackHandler {
                                                doctorViewModel.clearSelectedCase()
                                            }

                                            CaseDetailScreen(
                                                record = activeCase,
                                                patient = patientProfile,
                                                priorPrescriptions = patientPrescriptions,
                                                dispensaryStock = doctorDispensaryStock,
                                                currentDoctor = activeDoctor,
                                                allConditions = allConditions.filter { it.patientId == activeCase.patientId },
                                                allAppointments = allAppointments.filter { it.patientId == activeCase.patientId },
                                                onBack = { doctorViewModel.clearSelectedCase() },
                                                onSubmitResponse = { responseText, privateNotes ->
                                                    doctorViewModel.submitMedicalResponse(
                                                        caseId = activeCase.id,
                                                        responseText = responseText,
                                                        privateNotes = privateNotes
                                                    )
                                                },
                                                onIssuePrescription = { medicines, instructions ->
                                                    doctorViewModel.issuePrescription(
                                                        caseId = activeCase.id,
                                                        patientId = activeCase.patientId,
                                                        patientName = activeCase.patientName,
                                                        medicines = medicines,
                                                        instructions = instructions
                                                    )
                                                },
                                                onProposeAppointment = { date, timeSlot ->
                                                    doctorViewModel.proposeAppointment(
                                                        patientId = activeCase.patientId,
                                                        patientName = activeCase.patientName,
                                                        dateFormatted = date,
                                                        timeSlot = timeSlot
                                                    )
                                                },
                                                onReferCase = { targetSpecialty, referralNotes ->
                                                    doctorViewModel.referCase(
                                                        caseId = activeCase.id,
                                                        targetSpecialty = targetSpecialty,
                                                        referralNotes = referralNotes
                                                    )
                                                }
                                            )
                                        }

                                        else -> {
                                            BackHandler {
                                                appStateHolder.logout()
                                            }

                                            DoctorHomeScreen(
                                                doctor = activeDoctor,
                                                cases = doctorCases,
                                                appointments = doctorAppointments,
                                                dispensaryStock = doctorDispensaryStock,
                                                patients = patients,
                                                notices = notices,
                                                allConditions = allConditions,
                                                allPrescriptions = allPrescriptions,
                                                onOpenQueue = {
                                                    showDoctorQueue = true
                                                },
                                                onSelectCase = { record ->
                                                    doctorViewModel.selectCase(record)
                                                },
                                                onAcceptAppointment = { apptId ->
                                                    doctorViewModel.acceptAppointment(apptId)
                                                },
                                                onDeclineAppointment = { apptId ->
                                                    doctorViewModel.declineAppointment(apptId)
                                                },
                                                onProposeAppointment = { patId, patName, date, slot ->
                                                    doctorViewModel.proposeAppointment(
                                                        patientId = patId,
                                                        patientName = patName,
                                                        dateFormatted = date,
                                                        timeSlot = slot
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }

                                UserRole.ADMIN -> {
                                    var showQueueOversight by remember { mutableStateOf(false) }

                                    if (showQueueOversight) {
                                        BackHandler {
                                            showQueueOversight = false
                                        }

                                        QueueOversightScreen(
                                            summaries = adminDoctorSummaries,
                                            selectedDoctorSummary = adminSelectedDoctorSummary,
                                            selectedDoctorQueue = adminSelectedDoctorQueue,
                                            onSelectDoctor = { summary ->
                                                adminQueueOversightViewModel.selectDoctorForDrillDown(summary)
                                            },
                                            onDismissDrillDown = {
                                                adminQueueOversightViewModel.clearDrillDown()
                                            },
                                            onBack = { showQueueOversight = false }
                                        )
                                    } else {
                                        BackHandler {
                                            appStateHolder.logout()
                                        }

                                        AdminHomeScreen(
                                            villages = villages,
                                            notices = notices,
                                            dispensaryStock = doctorDispensaryStock,
                                            onOpenQueueOversight = {
                                                showQueueOversight = true
                                            },
                                            onSendBroadcast = { title, message, village ->
                                                coroutineScope.launch {
                                                    val broadcast = BroadcastNotice(
                                                        id = "notice_${System.currentTimeMillis()}",
                                                        senderRole = UserRole.ADMIN,
                                                        senderName = "Chief Medical Officer",
                                                        targetRole = "ALL",
                                                        targetVillage = village,
                                                        title = title,
                                                        message = message,
                                                        timestamp = System.currentTimeMillis(),
                                                        isUrgent = false
                                                    )
                                                    repository.sendNotice(broadcast)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
