package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.domain.AppLanguage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface AuthSessionState {
    data object Loading : AuthSessionState
    data object Unauthenticated : AuthSessionState
    data class Authenticated(val user: UserEntity) : AuthSessionState
}

@OptIn(ExperimentalCoroutinesApi::class)
class ShramSetuViewModel(
    private val repository: ShramSetuRepository
) : ViewModel() {

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _simulatedOtpBanner = MutableStateFlow<String?>(null)
    val simulatedOtpBanner: StateFlow<String?> = _simulatedOtpBanner.asStateFlow()

    private val _exportedJson = MutableStateFlow<String?>(null)
    val exportedJson: StateFlow<String?> = _exportedJson.asStateFlow()

    val language: StateFlow<AppLanguage> = repository.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppLanguage.ENGLISH)

    val sessionState: StateFlow<AuthSessionState> = repository.activeUserIdFlow
        .flatMapLatest { userId ->
            if (userId.isNullOrBlank()) {
                flowOf(AuthSessionState.Unauthenticated)
            } else {
                repository.observeUser(userId).map { user ->
                    if (user == null) AuthSessionState.Unauthenticated
                    else AuthSessionState.Authenticated(user)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AuthSessionState.Loading)

    private val activeUserFlow: Flow<UserEntity?> = sessionState.map {
        (it as? AuthSessionState.Authenticated)?.user
    }

    val currentWorkerProfile: StateFlow<WorkerProfileEntity?> = activeUserFlow
        .flatMapLatest { user ->
            if (user?.role == UserRoles.WORKER) repository.observeWorkerProfile(user.id)
            else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val currentEmployerProfile: StateFlow<EmployerEntity?> = activeUserFlow
        .flatMapLatest { user ->
            if (user?.role == UserRoles.EMPLOYER) repository.observeEmployerProfile(user.id)
            else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val activeJobs: StateFlow<List<JobPostEntity>> = repository.observeActiveJobs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allJobs: StateFlow<List<JobPostEntity>> = repository.observeAllJobs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allActiveWorkers: StateFlow<List<WorkerProfileEntity>> = repository.observeAllActiveWorkers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allWorkersAdmin: StateFlow<List<WorkerProfileEntity>> = repository.observeAllWorkersAdmin()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allEmployers: StateFlow<List<EmployerEntity>> = repository.observeAllEmployers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val workerApplications: StateFlow<List<JobApplicationEntity>> = activeUserFlow
        .flatMapLatest { user ->
            if (user?.role == UserRoles.WORKER) repository.observeWorkerApplications(user.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val employerApplications: StateFlow<List<JobApplicationEntity>> = activeUserFlow
        .flatMapLatest { user ->
            if (user?.role == UserRoles.EMPLOYER) repository.observeEmployerApplications(user.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allApplicationsAdmin: StateFlow<List<JobApplicationEntity>> = repository.observeAllApplications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val messages: StateFlow<List<MessageEntity>> = activeUserFlow
        .flatMapLatest { user ->
            if (user != null) repository.observeMessages(user.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = activeUserFlow
        .flatMapLatest { user ->
            if (user != null) repository.observeNotifications(user.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val complaints: StateFlow<List<ComplaintEntity>> = activeUserFlow
        .flatMapLatest { user ->
            when (user?.role) {
                UserRoles.SUPER_ADMIN -> repository.observeAllComplaints()
                null -> flowOf(emptyList())
                else -> repository.observeUserComplaints(user.id)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.observeAuditLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        viewModelScope.launch {
            repository.initializeDatabase()
        }
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun clearOtpBanner() {
        _simulatedOtpBanner.value = null
    }

    fun clearExportedJson() {
        _exportedJson.value = null
    }

    fun setLanguage(lang: AppLanguage) {
        viewModelScope.launch {
            repository.setLanguage(lang)
        }
    }

    fun requestOtp(mobile: String, onSent: () -> Unit = {}) {
        repository.requestOtp(mobile)
            .onSuccess { otp ->
                _simulatedOtpBanner.value = "SMS Verification Code for +91 $mobile: $otp (valid 5 min)"
                onSent()
            }
            .onFailure {
                _feedbackMessage.value = it.message ?: "Failed to send OTP"
            }
    }

    fun verifyOtp(mobile: String, code: String, onVerified: () -> Unit) {
        repository.verifyOtp(mobile, code)
            .onSuccess {
                _simulatedOtpBanner.value = null
                _feedbackMessage.value = "Mobile number verified successfully."
                onVerified()
            }
            .onFailure {
                _feedbackMessage.value = it.message ?: "OTP verification failed"
            }
    }

    fun login(mobile: String, pin: String) {
        viewModelScope.launch {
            repository.login(mobile, pin)
                .onFailure { _feedbackMessage.value = it.message ?: "Sign in failed" }
        }
    }

    fun quickDemoLogin(role: String) {
        viewModelScope.launch {
            repository.quickDemoLogin(role)
                .onFailure { _feedbackMessage.value = it.message ?: "Demo sign-in unavailable" }
        }
    }

    fun logout() {
        viewModelScope.launch {
            val user = (sessionState.value as? AuthSessionState.Authenticated)?.user
            repository.logout(user?.id, user?.role)
        }
    }

    fun registerWorker(input: WorkerRegistrationInput, onSuccess: (WorkerProfileEntity) -> Unit) {
        viewModelScope.launch {
            repository.registerWorker(input)
                .onSuccess {
                    _feedbackMessage.value = "Registration complete! Digital ID: ${it.workerId}"
                    onSuccess(it)
                }
                .onFailure {
                    _feedbackMessage.value = it.message ?: "Registration failed"
                }
        }
    }

    fun updateWorkerProfile(updated: WorkerProfileEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateWorkerProfile(updated)
                .onSuccess {
                    _feedbackMessage.value = "Worker profile updated."
                    onDone()
                }
                .onFailure {
                    _feedbackMessage.value = it.message ?: "Update failed"
                }
        }
    }

    fun registerEmployer(input: EmployerRegistrationInput, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.registerEmployer(input)
                .onSuccess {
                    _feedbackMessage.value = "Company submitted for verification."
                    onSuccess()
                }
                .onFailure {
                    _feedbackMessage.value = it.message ?: "Company registration failed"
                }
        }
    }

    fun resubmitEmployerDocs(employer: EmployerEntity, gstOrCin: String, regDocNum: String, docUri: String) {
        viewModelScope.launch {
            repository.updateEmployerDocuments(employer, gstOrCin, regDocNum, docUri)
                .onSuccess { _feedbackMessage.value = "Documents resubmitted for review." }
                .onFailure { _feedbackMessage.value = it.message }
        }
    }

    fun postJob(
        employer: EmployerEntity,
        title: String,
        category: String,
        tradeRequired: String,
        state: String,
        district: String,
        workLocation: String,
        dailyWage: Int,
        monthlySalary: Int,
        vacancies: Int,
        minExperienceYears: Int,
        description: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            repository.postJob(
                employer, title, category, tradeRequired, state, district,
                workLocation, dailyWage, monthlySalary, vacancies, minExperienceYears, description
            )
                .onSuccess {
                    _feedbackMessage.value = "Job posted live across India!"
                    onDone()
                }
                .onFailure { _feedbackMessage.value = it.message }
        }
    }

    fun toggleJobStatus(job: JobPostEntity) {
        val user = (sessionState.value as? AuthSessionState.Authenticated)?.user ?: return
        viewModelScope.launch {
            repository.toggleJobStatus(user.id, job)
                .onFailure { _feedbackMessage.value = it.message }
        }
    }

    fun applyForJob(worker: WorkerProfileEntity, job: JobPostEntity, note: String) {
        viewModelScope.launch {
            repository.applyForJob(worker, job, note)
                .onSuccess { _feedbackMessage.value = "Application sent to ${job.companyName}." }
                .onFailure { _feedbackMessage.value = it.message }
        }
    }

    fun sendWorkInvitation(employer: EmployerEntity, worker: WorkerProfileEntity, job: JobPostEntity, note: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.sendWorkInvitation(employer, worker, job, note)
                .onSuccess {
                    _feedbackMessage.value = "Work invitation sent to ${worker.fullName}."
                    onDone()
                }
                .onFailure { _feedbackMessage.value = it.message }
        }
    }

    fun updateApplicationStatus(application: JobApplicationEntity, newStatus: String) {
        val user = (sessionState.value as? AuthSessionState.Authenticated)?.user ?: return
        viewModelScope.launch {
            repository.updateApplicationStatus(user.id, user.role, application, newStatus)
                .onSuccess { _feedbackMessage.value = "Status updated to $newStatus." }
                .onFailure { _feedbackMessage.value = it.message }
        }
    }

    fun sendMessage(receiverUserId: String, receiverName: String, content: String, onSent: () -> Unit = {}) {
        val user = (sessionState.value as? AuthSessionState.Authenticated)?.user ?: return
        viewModelScope.launch {
            repository.sendMessage(user, receiverUserId, receiverName, content)
                .onSuccess { onSent() }
                .onFailure { _feedbackMessage.value = it.message }
        }
    }

    fun markNotificationsRead() {
        val user = (sessionState.value as? AuthSessionState.Authenticated)?.user ?: return
        viewModelScope.launch { repository.markNotificationsRead(user.id) }
    }

    fun submitComplaint(targetName: String, category: String, description: String, onDone: () -> Unit) {
        val user = (sessionState.value as? AuthSessionState.Authenticated)?.user ?: return
        viewModelScope.launch {
            repository.submitComplaint(user, targetName, category, description)
                .onSuccess {
                    _feedbackMessage.value = "Complaint filed. Super Admin & Labour Desk notified."
                    onDone()
                }
                .onFailure { _feedbackMessage.value = it.message }
        }
    }

    fun adminVerifyEmployer(employer: EmployerEntity, approve: Boolean, reason: String) {
        val user = (sessionState.value as? AuthSessionState.Authenticated)?.user ?: return
        viewModelScope.launch {
            repository.adminVerifyEmployer(user.id, employer, approve, reason)
                .onSuccess { _feedbackMessage.value = if (approve) "${employer.companyName} verified." else "Employer rejected." }
                .onFailure { _feedbackMessage.value = it.message }
        }
    }

    fun adminVerifyWorker(worker: WorkerProfileEntity, approve: Boolean) {
        val user = (sessionState.value as? AuthSessionState.Authenticated)?.user ?: return
        viewModelScope.launch {
            repository.adminVerifyWorker(user.id, worker, approve)
                .onSuccess { _feedbackMessage.value = "Worker verification updated." }
                .onFailure { _feedbackMessage.value = it.message }
        }
    }

    fun adminToggleWorkerSuspension(worker: WorkerProfileEntity) {
        val user = (sessionState.value as? AuthSessionState.Authenticated)?.user ?: return
        viewModelScope.launch {
            repository.adminToggleWorkerSuspension(user.id, worker)
                .onSuccess { _feedbackMessage.value = "Worker account status updated." }
                .onFailure { _feedbackMessage.value = it.message }
        }
    }

    fun adminUpdateComplaint(complaint: ComplaintEntity, status: String, note: String) {
        val user = (sessionState.value as? AuthSessionState.Authenticated)?.user ?: return
        viewModelScope.launch {
            repository.adminUpdateComplaint(user.id, complaint, status, note)
                .onSuccess { _feedbackMessage.value = "Complaint updated to $status." }
                .onFailure { _feedbackMessage.value = it.message }
        }
    }

    fun updatePrivacySettings(sharePhone: Boolean, shareExactAddress: Boolean, jobAlertsEnabled: Boolean) {
        val user = (sessionState.value as? AuthSessionState.Authenticated)?.user ?: return
        viewModelScope.launch {
            repository.updatePrivacySettings(user, sharePhone, shareExactAddress, jobAlertsEnabled)
            _feedbackMessage.value = "Privacy preferences saved."
        }
    }

    fun exportMyData() {
        val user = (sessionState.value as? AuthSessionState.Authenticated)?.user ?: return
        viewModelScope.launch {
            _exportedJson.value = repository.exportUserDataJson(user)
        }
    }

    fun deleteAccount() {
        val user = (sessionState.value as? AuthSessionState.Authenticated)?.user ?: return
        viewModelScope.launch {
            repository.deleteAccount(user)
            _feedbackMessage.value = "Your account and personal profile have been deleted."
        }
    }

    class Factory(private val repository: ShramSetuRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ShramSetuViewModel(repository) as T
        }
    }
}
