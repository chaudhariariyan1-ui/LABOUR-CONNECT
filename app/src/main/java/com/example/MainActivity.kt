package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.*
import com.example.domain.AppLocalization
import com.example.domain.LocalAppStrings
import com.example.ui.AuthSessionState
import com.example.ui.ShramSetuViewModel
import com.example.ui.components.DataExportDialog
import com.example.ui.screens.*
import com.example.ui.theme.ShramSetuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = ShramSetuDatabase.getInstance(applicationContext)
        val prefs = SessionPreferences(applicationContext)
        val repository = ShramSetuRepository(db, prefs)

        setContent {
            val viewModel: ShramSetuViewModel = viewModel(
                factory = ShramSetuViewModel.Factory(repository)
            )
            val language by viewModel.language.collectAsStateWithLifecycle()
            val strings = remember(language) { AppLocalization.stringsFor(language) }

            CompositionLocalProvider(LocalAppStrings provides strings) {
                ShramSetuTheme {
                    ShramSetuAppRoot(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun ShramSetuAppRoot(viewModel: ShramSetuViewModel) {
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()
    val simulatedOtpBanner by viewModel.simulatedOtpBanner.collectAsStateWithLifecycle()
    val exportedJson by viewModel.exportedJson.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackMessage) {
        val msg = feedbackMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = sessionState) {
            AuthSessionState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            AuthSessionState.Unauthenticated -> {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "landing"
                ) {
                    composable("landing") {
                        AuthAndLandingScreen(
                            currentLanguage = language,
                            onLanguageSelected = viewModel::setLanguage,
                            onLogin = viewModel::login,
                            onQuickDemoLogin = viewModel::quickDemoLogin,
                            onNavigateWorkerRegister = { navController.navigate("register_worker") },
                            onNavigateEmployerRegister = { navController.navigate("register_employer") }
                        )
                    }
                    composable("register_worker") {
                        WorkerRegistrationWizard(
                            currentLanguage = language,
                            simulatedOtpBanner = simulatedOtpBanner,
                            onRequestOtp = viewModel::requestOtp,
                            onVerifyOtp = viewModel::verifyOtp,
                            onSubmit = { input ->
                                viewModel.registerWorker(input) {
                                    // Session state automatically switches to Authenticated Worker Dashboard
                                }
                            },
                            onBack = {
                                viewModel.clearOtpBanner()
                                navController.popBackStack()
                            }
                        )
                    }
                    composable("register_employer") {
                        EmployerRegistrationScreen(
                            currentLanguage = language,
                            simulatedOtpBanner = simulatedOtpBanner,
                            onRequestOtp = viewModel::requestOtp,
                            onVerifyOtp = viewModel::verifyOtp,
                            onSubmit = { input ->
                                viewModel.registerEmployer(input) {
                                    // Session state automatically switches to Authenticated Employer Dashboard
                                }
                            },
                            onBack = {
                                viewModel.clearOtpBanner()
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }

            is AuthSessionState.Authenticated -> {
                val user = state.user
                key(user.id) {
                    when (user.role) {
                        UserRoles.WORKER -> {
                            val worker by viewModel.currentWorkerProfile.collectAsStateWithLifecycle()
                            val jobs by viewModel.activeJobs.collectAsStateWithLifecycle()
                            val apps by viewModel.workerApplications.collectAsStateWithLifecycle()
                            val msgs by viewModel.messages.collectAsStateWithLifecycle()
                            val notifs by viewModel.notifications.collectAsStateWithLifecycle()
                            val cmps by viewModel.complaints.collectAsStateWithLifecycle()

                            WorkerDashboardScreen(
                                user = user,
                                worker = worker,
                                currentLanguage = language,
                                jobs = jobs,
                                applications = apps,
                                messages = msgs,
                                notifications = notifs,
                                complaints = cmps,
                                onLanguageSelected = viewModel::setLanguage,
                                onLogout = viewModel::logout,
                                onApplyJob = viewModel::applyForJob,
                                onRespondApplication = viewModel::updateApplicationStatus,
                                onUpdateWorker = { updated -> viewModel.updateWorkerProfile(updated) },
                                onSendMessage = { rId, rName, content -> viewModel.sendMessage(rId, rName, content) },
                                onMarkNotificationsRead = viewModel::markNotificationsRead,
                                onSubmitComplaint = viewModel::submitComplaint,
                                onUpdatePrivacy = viewModel::updatePrivacySettings,
                                onExportData = viewModel::exportMyData,
                                onDeleteAccount = viewModel::deleteAccount
                            )
                        }

                        UserRoles.EMPLOYER -> {
                            val employer by viewModel.currentEmployerProfile.collectAsStateWithLifecycle()
                            val workers by viewModel.allActiveWorkers.collectAsStateWithLifecycle()
                            val allJobs by viewModel.allJobs.collectAsStateWithLifecycle()
                            val empJobs = remember(allJobs, employer) {
                                val empId = employer?.employerId
                                if (empId == null) emptyList() else allJobs.filter { it.employerId == empId }
                            }
                            val apps by viewModel.employerApplications.collectAsStateWithLifecycle()
                            val msgs by viewModel.messages.collectAsStateWithLifecycle()
                            val notifs by viewModel.notifications.collectAsStateWithLifecycle()
                            val cmps by viewModel.complaints.collectAsStateWithLifecycle()

                            EmployerDashboardScreen(
                                user = user,
                                employer = employer,
                                currentLanguage = language,
                                workers = workers,
                                employerJobs = empJobs,
                                applications = apps,
                                messages = msgs,
                                notifications = notifs,
                                complaints = cmps,
                                onLanguageSelected = viewModel::setLanguage,
                                onLogout = viewModel::logout,
                                onPostJob = viewModel::postJob,
                                onToggleJobStatus = viewModel::toggleJobStatus,
                                onSendInvitation = viewModel::sendWorkInvitation,
                                onUpdateApplicationStatus = viewModel::updateApplicationStatus,
                                onResubmitDocs = viewModel::resubmitEmployerDocs,
                                onSendMessage = { rId, rName, content -> viewModel.sendMessage(rId, rName, content) },
                                onMarkNotificationsRead = viewModel::markNotificationsRead,
                                onSubmitComplaint = viewModel::submitComplaint,
                                onExportData = viewModel::exportMyData,
                                onDeleteAccount = viewModel::deleteAccount
                            )
                        }

                        UserRoles.SUPER_ADMIN -> {
                            val workers by viewModel.allWorkersAdmin.collectAsStateWithLifecycle()
                            val employers by viewModel.allEmployers.collectAsStateWithLifecycle()
                            val jobs by viewModel.allJobs.collectAsStateWithLifecycle()
                            val apps by viewModel.allApplicationsAdmin.collectAsStateWithLifecycle()
                            val cmps by viewModel.complaints.collectAsStateWithLifecycle()
                            val logs by viewModel.auditLogs.collectAsStateWithLifecycle()

                            AdminDashboardScreen(
                                user = user,
                                currentLanguage = language,
                                workers = workers,
                                employers = employers,
                                jobs = jobs,
                                applications = apps,
                                complaints = cmps,
                                auditLogs = logs,
                                onLanguageSelected = viewModel::setLanguage,
                                onLogout = viewModel::logout,
                                onVerifyEmployer = viewModel::adminVerifyEmployer,
                                onVerifyWorker = viewModel::adminVerifyWorker,
                                onToggleWorkerSuspension = viewModel::adminToggleWorkerSuspension,
                                onUpdateComplaint = viewModel::adminUpdateComplaint
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }

    if (exportedJson != null) {
        DataExportDialog(
            jsonContent = exportedJson!!,
            onDismiss = viewModel::clearExportedJson
        )
    }
}
