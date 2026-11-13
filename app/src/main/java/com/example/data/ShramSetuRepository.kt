package com.example.data

import com.example.domain.AppLanguage
import com.example.domain.RateLimiter
import com.example.domain.SecurityHelper
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class WorkerRegistrationInput(
    val fullName: String,
    val fatherName: String,
    val dateOfBirth: String,
    val gender: String,
    val photoUri: String,
    val mobileNumber: String,
    val whatsappNumber: String,
    val pin: String,
    val state: String,
    val district: String,
    val currentAddress: String,
    val permanentAddress: String,
    val category: String,
    val primaryTrade: String,
    val skillsCsv: String,
    val experienceYears: Int,
    val expectedDailyWage: Int,
    val expectedMonthlySalary: Int,
    val preferredLocation: String,
    val availabilityStatus: String,
    val identityMethod: String,
    val identityRawReference: String,
    val consentIdentityVerify: Boolean,
    val consentTermsAndPrivacy: Boolean,
    val languageCode: String
)

data class EmployerRegistrationInput(
    val companyName: String,
    val companyType: String,
    val gstOrCinNumber: String,
    val registrationDocNumber: String,
    val verificationDocUri: String,
    val contactPerson: String,
    val officialEmail: String,
    val mobileNumber: String,
    val pin: String,
    val state: String,
    val district: String,
    val officeAddress: String,
    val consentTermsAndPrivacy: Boolean,
    val languageCode: String
)

class ShramSetuRepository(
    private val db: ShramSetuDatabase,
    private val prefs: SessionPreferences
) {
    private val userDao = db.userDao()
    private val workerDao = db.workerDao()
    private val employerDao = db.employerDao()
    private val jobDao = db.jobPostDao()
    private val applicationDao = db.jobApplicationDao()
    private val messageDao = db.messageDao()
    private val notificationDao = db.notificationDao()
    private val complaintDao = db.complaintDao()
    private val auditDao = db.auditLogDao()

    private val activeOtps = ConcurrentHashMap<String, Pair<String, Long>>()

    val activeUserIdFlow: Flow<String?> = prefs.activeUserId
    val languageFlow: Flow<AppLanguage> = prefs.language

    suspend fun initializeDatabase() {
        DatabaseSeeder.seedIfEmpty(db)
    }

    suspend fun setLanguage(language: AppLanguage) {
        prefs.setLanguage(language)
    }

    fun observeUser(userId: String): Flow<UserEntity?> = userDao.observeUserById(userId)
    fun observeWorkerProfile(userId: String): Flow<WorkerProfileEntity?> = workerDao.observeWorkerByUserId(userId)
    fun observeAllActiveWorkers(): Flow<List<WorkerProfileEntity>> = workerDao.observeAllActiveWorkers()
    fun observeAllWorkersAdmin(): Flow<List<WorkerProfileEntity>> = workerDao.observeAllWorkersAdmin()
    fun observeEmployerProfile(userId: String): Flow<EmployerEntity?> = employerDao.observeEmployerByUserId(userId)
    fun observeAllEmployers(): Flow<List<EmployerEntity>> = employerDao.observeAllEmployers()
    fun observeActiveJobs(): Flow<List<JobPostEntity>> = jobDao.observeActiveJobs()
    fun observeAllJobs(): Flow<List<JobPostEntity>> = jobDao.observeAllJobs()
    fun observeJobsByEmployer(employerId: String): Flow<List<JobPostEntity>> = jobDao.observeJobsByEmployer(employerId)
    fun observeWorkerApplications(workerUserId: String): Flow<List<JobApplicationEntity>> =
        applicationDao.observeByWorkerUser(workerUserId)
    fun observeEmployerApplications(employerUserId: String): Flow<List<JobApplicationEntity>> =
        applicationDao.observeByEmployerUser(employerUserId)
    fun observeAllApplications(): Flow<List<JobApplicationEntity>> = applicationDao.observeAllApplications()
    fun observeMessages(userId: String): Flow<List<MessageEntity>> = messageDao.observeMessagesForUser(userId)
    fun observeNotifications(userId: String): Flow<List<NotificationEntity>> = notificationDao.observeForUser(userId)
    fun observeAllComplaints(): Flow<List<ComplaintEntity>> = complaintDao.observeAll()
    fun observeUserComplaints(userId: String): Flow<List<ComplaintEntity>> = complaintDao.observeByReporter(userId)
    fun observeAuditLogs(): Flow<List<AuditLogEntity>> = auditDao.observeRecent()

    fun requestOtp(mobileNumber: String): Result<String> {
        val cleaned = mobileNumber.filter { it.isDigit() }
        if (cleaned.length != 10) {
            return Result.failure(IllegalArgumentException("Enter a valid 10-digit Indian mobile number."))
        }
        val waitSec = RateLimiter.checkAndRecord("otp_$cleaned", maxAttempts = 3, windowMillis = 60_000L)
        if (waitSec != null) {
            return Result.failure(IllegalStateException("Rate limit exceeded. Try OTP again in ${waitSec}s."))
        }
        val otp = SecurityHelper.generateSixDigitOtp()
        activeOtps[cleaned] = otp to (System.currentTimeMillis() + 5 * 60_000L)
        return Result.success(otp)
    }

    fun verifyOtp(mobileNumber: String, enteredOtp: String): Result<Unit> {
        val cleaned = mobileNumber.filter { it.isDigit() }
        val record = activeOtps[cleaned]
            ?: return Result.failure(IllegalStateException("Please request an OTP first."))
        if (System.currentTimeMillis() > record.second) {
            activeOtps.remove(cleaned)
            return Result.failure(IllegalStateException("OTP expired. Please request a new OTP."))
        }
        if (enteredOtp.trim() != record.first) {
            return Result.failure(IllegalArgumentException("Incorrect OTP. Please verify the 6-digit code."))
        }
        activeOtps.remove(cleaned)
        return Result.success(Unit)
    }

    suspend fun login(mobileNumber: String, pin: String): Result<UserEntity> {
        val cleaned = mobileNumber.filter { it.isDigit() }
        val waitSec = RateLimiter.checkAndRecord("login_$cleaned", maxAttempts = 5, windowMillis = 60_000L)
        if (waitSec != null) {
            return Result.failure(IllegalStateException("Too many sign-in attempts. Wait ${waitSec}s."))
        }
        val user = userDao.getUserByMobile(cleaned)
            ?: return Result.failure(IllegalArgumentException("No active account found for $cleaned."))
        if (!SecurityHelper.verifySecret(pin.trim(), user.salt, user.passwordHash)) {
            logAudit(user.id, user.role, "LOGIN_FAILED", "USER", user.id, "Invalid PIN attempt")
            return Result.failure(IllegalArgumentException("Incorrect security PIN."))
        }
        prefs.setActiveUserId(user.id)
        logAudit(user.id, user.role, "LOGIN_SUCCESS", "USER", user.id, "Authenticated session started")
        return Result.success(user)
    }

    suspend fun quickDemoLogin(role: String): Result<UserEntity> {
        val user = userDao.getFirstUserByRole(role)
            ?: return Result.failure(IllegalStateException("Demo user for $role not found."))
        prefs.setActiveUserId(user.id)
        logAudit(user.id, user.role, "DEMO_SESSION_LOGIN", "USER", user.id, "Switched to $role portal")
        return Result.success(user)
    }

    suspend fun logout(currentUserId: String?, currentRole: String?) {
        if (currentUserId != null && currentRole != null) {
            logAudit(currentUserId, currentRole, "LOGOUT", "USER", currentUserId, "Session closed")
        }
        prefs.setActiveUserId(null)
    }

    suspend fun registerWorker(input: WorkerRegistrationInput): Result<WorkerProfileEntity> {
        val cleanedMobile = input.mobileNumber.filter { it.isDigit() }
        if (cleanedMobile.length != 10) {
            return Result.failure(IllegalArgumentException("Valid 10-digit mobile number required."))
        }
        if (userDao.getUserByMobile(cleanedMobile) != null) {
            return Result.failure(IllegalArgumentException("An account with mobile $cleanedMobile already exists. Please sign in."))
        }
        if (!input.consentTermsAndPrivacy || !input.consentIdentityVerify) {
            return Result.failure(IllegalArgumentException("Consent for terms and privacy-preserving verification is mandatory."))
        }

        val userId = "usr_${UUID.randomUUID().toString().take(10)}"
        val salt = SecurityHelper.generateSalt()
        val pinHash = SecurityHelper.hashSecret(input.pin.trim(), salt)
        val now = System.currentTimeMillis()
        val workerId = SecurityHelper.generateDigitalWorkerId(input.state, now)

        val user = UserEntity(
            id = userId,
            role = UserRoles.WORKER,
            mobileNumber = cleanedMobile,
            passwordHash = pinHash,
            salt = salt,
            fullName = input.fullName.trim(),
            preferredLanguage = input.languageCode,
            consentAccepted = true,
            consentTimestamp = now,
            privacySharePhone = false,
            privacyShareExactAddress = false,
            createdAt = now,
            updatedAt = now
        )

        val maskedToken = SecurityHelper.tokenizeIdentityReference(
            method = input.identityMethod,
            rawReference = input.identityRawReference
        )

        val worker = WorkerProfileEntity(
            workerId = workerId,
            userId = userId,
            fullName = input.fullName.trim(),
            fatherName = input.fatherName.trim(),
            dateOfBirth = input.dateOfBirth.trim(),
            gender = input.gender,
            photoUri = input.photoUri,
            mobileNumber = cleanedMobile,
            whatsappNumber = input.whatsappNumber.filter { it.isDigit() }.ifBlank { cleanedMobile },
            state = input.state,
            district = input.district,
            currentAddress = input.currentAddress.trim(),
            permanentAddress = input.permanentAddress.trim(),
            category = input.category,
            primaryTrade = input.primaryTrade,
            skillsCsv = input.skillsCsv.ifBlank { input.primaryTrade },
            experienceYears = input.experienceYears,
            expectedDailyWage = input.expectedDailyWage,
            expectedMonthlySalary = input.expectedMonthlySalary,
            preferredLocation = input.preferredLocation.trim().ifBlank { "${input.district}, ${input.state}" },
            availabilityStatus = input.availabilityStatus,
            mobileOtpVerified = true,
            identityVerificationMethod = input.identityMethod,
            identityTokenMasked = maskedToken,
            identityVerified = VerificationStatuses.PENDING,
            consentIdentityVerify = true,
            createdAt = now,
            updatedAt = now
        )

        userDao.upsertUser(user)
        workerDao.upsertWorker(worker)
        notificationDao.insert(
            NotificationEntity(
                notificationId = "ntf_${UUID.randomUUID().toString().take(8)}",
                userId = userId,
                title = "Welcome to ShramSetu! Digital ID Issued",
                body = "Your FREE Digital Worker ID ($workerId) is active. Consent-based identity verification is queued for review.",
                type = "VERIFICATION",
                createdAt = now
            )
        )
        logAudit(
            userId,
            UserRoles.WORKER,
            "WORKER_REGISTERED",
            "WORKER",
            workerId,
            "Registered ${input.category} (${input.primaryTrade}) in ${input.district}, ${input.state} with tokenized ${input.identityMethod}"
        )
        prefs.setActiveUserId(userId)
        return Result.success(worker)
    }

    suspend fun updateWorkerProfile(updated: WorkerProfileEntity): Result<Unit> {
        val current = userDao.getUserById(updated.userId)
            ?: return Result.failure(IllegalStateException("User not found."))
        if (current.role != UserRoles.WORKER && current.role != UserRoles.SUPER_ADMIN) {
            return Result.failure(SecurityException("Unauthorized profile update."))
        }
        val stamped = updated.copy(updatedAt = System.currentTimeMillis())
        workerDao.upsertWorker(stamped)
        userDao.updateUser(current.copy(fullName = stamped.fullName, updatedAt = System.currentTimeMillis()))
        logAudit(current.id, current.role, "WORKER_PROFILE_UPDATED", "WORKER", stamped.workerId, "Updated trade/availability/location")
        return Result.success(Unit)
    }

    suspend fun registerEmployer(input: EmployerRegistrationInput): Result<EmployerEntity> {
        val cleanedMobile = input.mobileNumber.filter { it.isDigit() }
        if (cleanedMobile.length != 10) {
            return Result.failure(IllegalArgumentException("Valid 10-digit mobile number required."))
        }
        if (userDao.getUserByMobile(cleanedMobile) != null) {
            return Result.failure(IllegalArgumentException("An account with mobile $cleanedMobile already exists."))
        }
        if (!input.consentTermsAndPrivacy) {
            return Result.failure(IllegalArgumentException("You must accept the platform Terms & Privacy Policy."))
        }

        val now = System.currentTimeMillis()
        val userId = "usr_emp_${UUID.randomUUID().toString().take(8)}"
        val employerId = "emp_${UUID.randomUUID().toString().take(8)}"
        val salt = SecurityHelper.generateSalt()
        val pinHash = SecurityHelper.hashSecret(input.pin.trim(), salt)

        val user = UserEntity(
            id = userId,
            role = UserRoles.EMPLOYER,
            mobileNumber = cleanedMobile,
            passwordHash = pinHash,
            salt = salt,
            fullName = input.contactPerson.trim(),
            preferredLanguage = input.languageCode,
            consentAccepted = true,
            consentTimestamp = now,
            privacySharePhone = true,
            createdAt = now,
            updatedAt = now
        )

        val employer = EmployerEntity(
            employerId = employerId,
            userId = userId,
            companyName = input.companyName.trim(),
            companyType = input.companyType,
            gstOrCinNumber = input.gstOrCinNumber.trim().uppercase(),
            registrationDocNumber = input.registrationDocNumber.trim().uppercase(),
            verificationDocUri = input.verificationDocUri,
            contactPerson = input.contactPerson.trim(),
            officialEmail = input.officialEmail.trim(),
            mobileNumber = cleanedMobile,
            state = input.state,
            district = input.district,
            officeAddress = input.officeAddress.trim(),
            verificationStatus = VerificationStatuses.PENDING,
            createdAt = now,
            updatedAt = now
        )

        userDao.upsertUser(user)
        employerDao.upsertEmployer(employer)
        notificationDao.insert(
            NotificationEntity(
                notificationId = "ntf_${UUID.randomUUID().toString().take(8)}",
                userId = userId,
                title = "Company Registration Submitted",
                body = "${employer.companyName} is queued for Super Admin document verification. You can prepare draft job posts meanwhile.",
                type = "VERIFICATION",
                createdAt = now
            )
        )
        logAudit(userId, UserRoles.EMPLOYER, "EMPLOYER_REGISTERED", "EMPLOYER", employerId, "Registered ${employer.companyName} (${employer.state})")
        prefs.setActiveUserId(userId)
        return Result.success(employer)
    }

    suspend fun updateEmployerDocuments(
        employer: EmployerEntity,
        gstOrCin: String,
        regDocNum: String,
        docUri: String
    ): Result<Unit> {
        val updated = employer.copy(
            gstOrCinNumber = gstOrCin.trim().uppercase(),
            registrationDocNumber = regDocNum.trim().uppercase(),
            verificationDocUri = docUri,
            verificationStatus = VerificationStatuses.PENDING,
            rejectionReason = "",
            updatedAt = System.currentTimeMillis()
        )
        employerDao.upsertEmployer(updated)
        logAudit(employer.userId, UserRoles.EMPLOYER, "EMPLOYER_DOCS_RESUBMITTED", "EMPLOYER", employer.employerId, "Resubmitted verification docs")
        return Result.success(Unit)
    }

    suspend fun postJob(
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
        description: String
    ): Result<JobPostEntity> {
        if (employer.verificationStatus != VerificationStatuses.VERIFIED) {
            return Result.failure(IllegalStateException("Only verified employers can publish live job opportunities. Your company verification is ${employer.verificationStatus}."))
        }
        val job = JobPostEntity(
            jobId = "job_${UUID.randomUUID().toString().take(8)}",
            employerId = employer.employerId,
            companyName = employer.companyName,
            employerVerified = true,
            title = title.trim(),
            category = category,
            tradeRequired = tradeRequired,
            state = state,
            district = district,
            workLocation = workLocation.trim(),
            dailyWage = dailyWage,
            monthlySalary = monthlySalary,
            vacancies = vacancies.coerceAtLeast(1),
            minExperienceYears = minExperienceYears.coerceAtLeast(0),
            description = description.trim(),
            status = "ACTIVE",
            createdAt = System.currentTimeMillis()
        )
        jobDao.upsertJob(job)
        logAudit(employer.userId, UserRoles.EMPLOYER, "JOB_POSTED", "JOB", job.jobId, "Published '${job.title}' in ${job.district}, ${job.state}")
        return Result.success(job)
    }

    suspend fun toggleJobStatus(employerUserId: String, job: JobPostEntity): Result<Unit> {
        val employer = employerDao.getEmployerByUserId(employerUserId)
            ?: return Result.failure(SecurityException("Employer account required."))
        if (employer.employerId != job.employerId) {
            return Result.failure(SecurityException("Cannot modify another employer's job post."))
        }
        val nextStatus = if (job.status == "ACTIVE") "PAUSED" else "ACTIVE"
        jobDao.upsertJob(job.copy(status = nextStatus))
        logAudit(employerUserId, UserRoles.EMPLOYER, "JOB_STATUS_CHANGED", "JOB", job.jobId, "Changed status to $nextStatus")
        return Result.success(Unit)
    }

    suspend fun applyForJob(worker: WorkerProfileEntity, job: JobPostEntity, note: String): Result<Unit> {
        val existing = applicationDao.findExisting(job.jobId, worker.workerId)
        if (existing != null) {
            return Result.failure(IllegalStateException("You already have an active ${existing.status.lowercase()} record for this job."))
        }
        val employer = employerDao.getEmployerById(job.employerId)
            ?: return Result.failure(IllegalStateException("Employer record unavailable."))
        val now = System.currentTimeMillis()
        val app = JobApplicationEntity(
            applicationId = "app_${UUID.randomUUID().toString().take(8)}",
            jobId = job.jobId,
            jobTitle = job.title,
            companyName = job.companyName,
            workerId = worker.workerId,
            workerUserId = worker.userId,
            employerId = employer.employerId,
            employerUserId = employer.userId,
            workerName = worker.fullName,
            workerTrade = worker.primaryTrade,
            workerLocation = "${worker.district}, ${worker.state}",
            status = "APPLIED",
            type = "APPLICATION",
            note = note.trim(),
            createdAt = now,
            updatedAt = now
        )
        applicationDao.upsert(app)
        notificationDao.insert(
            NotificationEntity(
                notificationId = "ntf_${UUID.randomUUID().toString().take(8)}",
                userId = employer.userId,
                title = "New Job Application: ${worker.primaryTrade}",
                body = "${worker.fullName} (${worker.experienceYears} yrs exp) applied for ${job.title}.",
                type = "JOB_ALERT",
                createdAt = now
            )
        )
        logAudit(worker.userId, UserRoles.WORKER, "JOB_APPLIED", "APPLICATION", app.applicationId, "Worker ${worker.workerId} applied to ${job.jobId}")
        return Result.success(Unit)
    }

    suspend fun sendWorkInvitation(
        employer: EmployerEntity,
        worker: WorkerProfileEntity,
        job: JobPostEntity,
        note: String
    ): Result<Unit> {
        if (employer.verificationStatus != VerificationStatuses.VERIFIED) {
            return Result.failure(IllegalStateException("Verified employer status required to send work invitations."))
        }
        val existing = applicationDao.findExisting(job.jobId, worker.workerId)
        if (existing != null) {
            return Result.failure(IllegalStateException("An application or invitation already exists for this worker & job."))
        }
        val now = System.currentTimeMillis()
        val invite = JobApplicationEntity(
            applicationId = "inv_${UUID.randomUUID().toString().take(8)}",
            jobId = job.jobId,
            jobTitle = job.title,
            companyName = employer.companyName,
            workerId = worker.workerId,
            workerUserId = worker.userId,
            employerId = employer.employerId,
            employerUserId = employer.userId,
            workerName = worker.fullName,
            workerTrade = worker.primaryTrade,
            workerLocation = "${worker.district}, ${worker.state}",
            status = "INVITED",
            type = "INVITATION",
            note = note.trim(),
            createdAt = now,
            updatedAt = now
        )
        applicationDao.upsert(invite)
        notificationDao.insert(
            NotificationEntity(
                notificationId = "ntf_${UUID.randomUUID().toString().take(8)}",
                userId = worker.userId,
                title = "Work Invitation from ${employer.companyName}",
                body = "Invited for ${job.title} (${job.district}, ₹${job.dailyWage}/day).",
                type = "INVITATION",
                createdAt = now
            )
        )
        logAudit(employer.userId, UserRoles.EMPLOYER, "WORK_INVITATION_SENT", "APPLICATION", invite.applicationId, "Invited ${worker.workerId} to ${job.jobId}")
        return Result.success(Unit)
    }

    suspend fun updateApplicationStatus(
        actorUserId: String,
        actorRole: String,
        application: JobApplicationEntity,
        newStatus: String
    ): Result<Unit> {
        val updated = application.copy(status = newStatus, updatedAt = System.currentTimeMillis())
        applicationDao.upsert(updated)
        val recipientUserId = if (actorRole == UserRoles.WORKER) application.employerUserId else application.workerUserId
        val title = when (newStatus) {
            "ACCEPTED" -> "${application.workerName} accepted your work invitation"
            "HIRED" -> "Congratulations! Hired by ${application.companyName}"
            "DECLINED" -> "Recruitment request declined for ${application.jobTitle}"
            else -> "Application update: $newStatus"
        }
        notificationDao.insert(
            NotificationEntity(
                notificationId = "ntf_${UUID.randomUUID().toString().take(8)}",
                userId = recipientUserId,
                title = title,
                body = "Job: ${application.jobTitle} (${application.companyName})",
                type = "JOB_ALERT",
                createdAt = System.currentTimeMillis()
            )
        )
        logAudit(actorUserId, actorRole, "APPLICATION_$newStatus", "APPLICATION", application.applicationId, "${application.jobTitle} -> $newStatus")
        return Result.success(Unit)
    }

    suspend fun sendMessage(
        sender: UserEntity,
        receiverUserId: String,
        receiverName: String,
        rawContent: String
    ): Result<Unit> {
        if (rawContent.isBlank()) return Result.failure(IllegalArgumentException("Message cannot be empty."))
        val safeContent = SecurityHelper.redactSensitiveDigitsInMessage(rawContent.trim())
        val threadKey = listOf(sender.id, receiverUserId).sorted().joinToString("_")
        val now = System.currentTimeMillis()
        messageDao.insertMessage(
            MessageEntity(
                messageId = "msg_${UUID.randomUUID().toString().take(8)}",
                threadKey = threadKey,
                senderUserId = sender.id,
                senderName = sender.fullName,
                senderRole = sender.role,
                receiverUserId = receiverUserId,
                receiverName = receiverName,
                content = safeContent,
                createdAt = now
            )
        )
        notificationDao.insert(
            NotificationEntity(
                notificationId = "ntf_${UUID.randomUUID().toString().take(8)}",
                userId = receiverUserId,
                title = "New message from ${sender.fullName}",
                body = safeContent.take(90),
                type = "SYSTEM",
                createdAt = now
            )
        )
        return Result.success(Unit)
    }

    suspend fun markNotificationsRead(userId: String) {
        notificationDao.markAllRead(userId)
    }

    suspend fun submitComplaint(
        reporter: UserEntity,
        targetName: String,
        category: String,
        description: String
    ): Result<Unit> {
        if (description.trim().length < 10) {
            return Result.failure(IllegalArgumentException("Please describe the issue (at least 10 characters)."))
        }
        val now = System.currentTimeMillis()
        val id = "cmp_${UUID.randomUUID().toString().take(8)}"
        complaintDao.upsert(
            ComplaintEntity(
                complaintId = id,
                reporterUserId = reporter.id,
                reporterName = reporter.fullName,
                reporterRole = reporter.role,
                targetName = targetName.trim().ifBlank { "General Platform Issue" },
                category = category,
                description = description.trim(),
                status = "OPEN",
                createdAt = now,
                updatedAt = now
            )
        )
        logAudit(reporter.id, reporter.role, "COMPLAINT_SUBMITTED", "COMPLAINT", id, "Category: $category")
        return Result.success(Unit)
    }

    // --- SUPER ADMIN RBAC ACTIONS ---

    private suspend fun requireSuperAdmin(actorUserId: String): UserEntity {
        val actor = userDao.getUserById(actorUserId)
            ?: throw SecurityException("Authentication required.")
        if (actor.role != UserRoles.SUPER_ADMIN) {
            logAudit(actorUserId, actor.role, "RBAC_VIOLATION", "ADMIN", actorUserId, "Blocked unauthorized admin action")
            throw SecurityException("Super Admin privileges required.")
        }
        return actor
    }

    suspend fun adminVerifyEmployer(
        adminUserId: String,
        employer: EmployerEntity,
        approve: Boolean,
        reason: String
    ): Result<Unit> {
        return try {
            requireSuperAdmin(adminUserId)
            val newStatus = if (approve) VerificationStatuses.VERIFIED else VerificationStatuses.REJECTED
            val now = System.currentTimeMillis()
            employerDao.upsertEmployer(
                employer.copy(
                    verificationStatus = newStatus,
                    rejectionReason = if (approve) "" else reason.trim(),
                    updatedAt = now
                )
            )
            notificationDao.insert(
                NotificationEntity(
                    notificationId = "ntf_${UUID.randomUUID().toString().take(8)}",
                    userId = employer.userId,
                    title = if (approve) "Company Verified by National Admin" else "Company Verification Requires Action",
                    body = if (approve) "${employer.companyName} can now publish job posts and invite workers." else "Reason: $reason",
                    type = "VERIFICATION",
                    createdAt = now
                )
            )
            logAudit(
                adminUserId,
                UserRoles.SUPER_ADMIN,
                if (approve) "EMPLOYER_VERIFIED" else "EMPLOYER_REJECTED",
                "EMPLOYER",
                employer.employerId,
                "${employer.companyName} -> $newStatus"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminVerifyWorker(
        adminUserId: String,
        worker: WorkerProfileEntity,
        approve: Boolean
    ): Result<Unit> {
        return try {
            requireSuperAdmin(adminUserId)
            val newStatus = if (approve) VerificationStatuses.VERIFIED else VerificationStatuses.REJECTED
            val now = System.currentTimeMillis()
            workerDao.upsertWorker(worker.copy(identityVerified = newStatus, updatedAt = now))
            notificationDao.insert(
                NotificationEntity(
                    notificationId = "ntf_${UUID.randomUUID().toString().take(8)}",
                    userId = worker.userId,
                    title = if (approve) "Digital Worker ID Verified" else "Verification Needs Re-check",
                    body = "Worker ID ${worker.workerId} verification status updated to $newStatus.",
                    type = "VERIFICATION",
                    createdAt = now
                )
            )
            logAudit(
                adminUserId,
                UserRoles.SUPER_ADMIN,
                "WORKER_VERIFICATION_$newStatus",
                "WORKER",
                worker.workerId,
                "Identity verification set to $newStatus (masked token only)"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminToggleWorkerSuspension(adminUserId: String, worker: WorkerProfileEntity): Result<Unit> {
        return try {
            requireSuperAdmin(adminUserId)
            val suspended = !worker.isSuspended
            workerDao.upsertWorker(worker.copy(isSuspended = suspended, updatedAt = System.currentTimeMillis()))
            logAudit(
                adminUserId,
                UserRoles.SUPER_ADMIN,
                if (suspended) "WORKER_SUSPENDED" else "WORKER_REACTIVATED",
                "WORKER",
                worker.workerId,
                "Toggled suspension for ${worker.fullName}"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminUpdateComplaint(
        adminUserId: String,
        complaint: ComplaintEntity,
        status: String,
        resolutionNote: String
    ): Result<Unit> {
        return try {
            requireSuperAdmin(adminUserId)
            val now = System.currentTimeMillis()
            complaintDao.upsert(
                complaint.copy(
                    status = status,
                    adminResolutionNote = resolutionNote.trim(),
                    updatedAt = now
                )
            )
            notificationDao.insert(
                NotificationEntity(
                    notificationId = "ntf_${UUID.randomUUID().toString().take(8)}",
                    userId = complaint.reporterUserId,
                    title = "Complaint Status: $status",
                    body = resolutionNote.ifBlank { "Your report (${complaint.category}) was updated by Super Admin." },
                    type = "COMPLAINT",
                    createdAt = now
                )
            )
            logAudit(adminUserId, UserRoles.SUPER_ADMIN, "COMPLAINT_$status", "COMPLAINT", complaint.complaintId, resolutionNote)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- PRIVACY, CONSENT, DATA EXPORT, ACCOUNT DELETION ---

    suspend fun updatePrivacySettings(
        user: UserEntity,
        sharePhone: Boolean,
        shareExactAddress: Boolean,
        jobAlertsEnabled: Boolean
    ) {
        val updated = user.copy(
            privacySharePhone = sharePhone,
            privacyShareExactAddress = shareExactAddress,
            jobAlertsEnabled = jobAlertsEnabled,
            updatedAt = System.currentTimeMillis()
        )
        userDao.updateUser(updated)
        logAudit(user.id, user.role, "PRIVACY_UPDATED", "USER", user.id, "sharePhone=$sharePhone, shareAddress=$shareExactAddress, alerts=$jobAlertsEnabled")
    }

    suspend fun exportUserDataJson(user: UserEntity): String {
        val worker = workerDao.getWorkerByUserId(user.id)
        val employer = employerDao.getEmployerByUserId(user.id)
        val messages = messageDao.getMessagesForUserOnce(user.id)

        val root = JSONObject().apply {
            put("platform", "ShramSetu All-India Labour Platform")
            put("exportedAtEpochMs", System.currentTimeMillis())
            put("account", JSONObject().apply {
                put("userId", user.id)
                put("role", user.role)
                put("fullName", user.fullName)
                put("mobileNumber", user.mobileNumber)
                put("preferredLanguage", user.preferredLanguage)
                put("consentAccepted", user.consentAccepted)
                put("consentTimestamp", user.consentTimestamp)
                put("privacySharePhone", user.privacySharePhone)
                put("privacyShareExactAddress", user.privacyShareExactAddress)
            })
            if (worker != null) {
                put("workerProfile", JSONObject().apply {
                    put("workerId", worker.workerId)
                    put("fatherName", worker.fatherName)
                    put("category", worker.category)
                    put("primaryTrade", worker.primaryTrade)
                    put("skills", worker.skillsCsv)
                    put("state", worker.state)
                    put("district", worker.district)
                    put("experienceYears", worker.experienceYears)
                    put("expectedDailyWage", worker.expectedDailyWage)
                    put("expectedMonthlySalary", worker.expectedMonthlySalary)
                    put("identityVerificationMethod", worker.identityVerificationMethod)
                    put("identityTokenMasked", worker.identityTokenMasked)
                    put("identityVerified", worker.identityVerified)
                })
            }
            if (employer != null) {
                put("employerProfile", JSONObject().apply {
                    put("employerId", employer.employerId)
                    put("companyName", employer.companyName)
                    put("companyType", employer.companyType)
                    put("gstOrCinNumber", employer.gstOrCinNumber)
                    put("verificationStatus", employer.verificationStatus)
                    put("state", employer.state)
                    put("district", employer.district)
                })
            }
            put("messagesCount", messages.size)
            put("messagesSample", JSONArray().apply {
                messages.take(25).forEach { msg ->
                    put(JSONObject().apply {
                        put("sender", msg.senderName)
                        put("receiver", msg.receiverName)
                        put("content", msg.content)
                        put("createdAt", msg.createdAt)
                    })
                }
            })
        }
        logAudit(user.id, user.role, "DATA_EXPORTED", "USER", user.id, "Exported DPDP personal data archive JSON")
        return root.toString(2)
    }

    suspend fun deleteAccount(user: UserEntity) {
        val now = System.currentTimeMillis()
        workerDao.deleteWorkerByUserId(user.id)
        employerDao.deleteEmployerByUserId(user.id)
        userDao.updateUser(
            user.copy(
                isDeleted = true,
                fullName = "Deleted Account",
                mobileNumber = "deleted_${user.id}",
                updatedAt = now
            )
        )
        logAudit(user.id, user.role, "ACCOUNT_DELETED", "USER", user.id, "User initiated permanent account deletion & profile purge")
        prefs.setActiveUserId(null)
    }

    private suspend fun logAudit(
        actorUserId: String,
        actorRole: String,
        action: String,
        targetType: String,
        targetId: String,
        details: String
    ) {
        auditDao.insert(
            AuditLogEntity(
                logId = "aud_${UUID.randomUUID().toString().take(8)}",
                actorUserId = actorUserId,
                actorRole = actorRole,
                action = action,
                targetType = targetType,
                targetId = targetId,
                details = details,
                timestamp = System.currentTimeMillis()
            )
        )
    }
}
