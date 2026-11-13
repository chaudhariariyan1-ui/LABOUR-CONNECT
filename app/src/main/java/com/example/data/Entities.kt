package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

object UserRoles {
    const val WORKER = "WORKER"
    const val EMPLOYER = "EMPLOYER"
    const val SUPER_ADMIN = "SUPER_ADMIN"
}

object VerificationStatuses {
    const val PENDING = "PENDING"
    const val VERIFIED = "VERIFIED"
    const val REJECTED = "REJECTED"
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val role: String, // WORKER, EMPLOYER, SUPER_ADMIN
    val mobileNumber: String,
    val passwordHash: String,
    val salt: String,
    val fullName: String,
    val preferredLanguage: String = "en",
    val consentAccepted: Boolean = true,
    val consentTimestamp: Long = System.currentTimeMillis(),
    val privacySharePhone: Boolean = false,
    val privacyShareExactAddress: Boolean = false,
    val jobAlertsEnabled: Boolean = true,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "worker_profiles")
data class WorkerProfileEntity(
    @PrimaryKey val workerId: String, // e.g. IND-MH-2026-104821
    val userId: String,
    val fullName: String,
    val fatherName: String,
    val dateOfBirth: String,
    val gender: String,
    val photoUri: String = "",
    val mobileNumber: String,
    val whatsappNumber: String,
    val state: String,
    val district: String,
    val currentAddress: String,
    val permanentAddress: String,
    val category: String, // SKILLED or UNSKILLED
    val primaryTrade: String,
    val skillsCsv: String,
    val experienceYears: Int,
    val expectedDailyWage: Int,
    val expectedMonthlySalary: Int,
    val preferredLocation: String,
    val availabilityStatus: String, // AVAILABLE, WORKING_NOTICE, BUSY
    val mobileOtpVerified: Boolean = true,
    val identityVerificationMethod: String, // DIGILOCKER_CONSENT, ESHRAM_UAN, VID_TOKEN
    val identityTokenMasked: String, // Strictly masked e.g. VID-XXXX-XXXX-4921 (never raw Aadhaar)
    val identityVerified: String = VerificationStatuses.PENDING,
    val consentIdentityVerify: Boolean = true,
    val isSuspended: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "employers")
data class EmployerEntity(
    @PrimaryKey val employerId: String,
    val userId: String,
    val companyName: String,
    val companyType: String,
    val gstOrCinNumber: String,
    val registrationDocNumber: String,
    val verificationDocUri: String = "",
    val contactPerson: String,
    val officialEmail: String,
    val mobileNumber: String,
    val state: String,
    val district: String,
    val officeAddress: String,
    val verificationStatus: String = VerificationStatuses.PENDING,
    val rejectionReason: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "job_posts")
data class JobPostEntity(
    @PrimaryKey val jobId: String,
    val employerId: String,
    val companyName: String,
    val employerVerified: Boolean,
    val title: String,
    val category: String, // SKILLED or UNSKILLED
    val tradeRequired: String,
    val state: String,
    val district: String,
    val workLocation: String,
    val dailyWage: Int,
    val monthlySalary: Int,
    val vacancies: Int,
    val minExperienceYears: Int,
    val description: String,
    val status: String = "ACTIVE", // ACTIVE, PAUSED, CLOSED
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "job_applications")
data class JobApplicationEntity(
    @PrimaryKey val applicationId: String,
    val jobId: String,
    val jobTitle: String,
    val companyName: String,
    val workerId: String,
    val workerUserId: String,
    val employerId: String,
    val employerUserId: String,
    val workerName: String,
    val workerTrade: String,
    val workerLocation: String,
    val status: String, // APPLIED, INVITED, ACCEPTED, DECLINED, HIRED
    val type: String, // APPLICATION or INVITATION
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val threadKey: String, // sorted userId1_userId2
    val senderUserId: String,
    val senderName: String,
    val senderRole: String,
    val receiverUserId: String,
    val receiverName: String,
    val content: String,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val notificationId: String,
    val userId: String,
    val title: String,
    val body: String,
    val type: String, // JOB_ALERT, INVITATION, VERIFICATION, COMPLAINT, SYSTEM
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "complaints")
data class ComplaintEntity(
    @PrimaryKey val complaintId: String,
    val reporterUserId: String,
    val reporterName: String,
    val reporterRole: String,
    val targetName: String,
    val category: String, // WAGE_DISPUTE, SAFETY_ISSUE, FAKE_JOB, HARASSMENT, VERIFICATION_HELP, OTHER
    val description: String,
    val status: String = "OPEN", // OPEN, IN_REVIEW, RESOLVED, DISMISSED
    val adminResolutionNote: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val logId: String,
    val actorUserId: String,
    val actorRole: String,
    val action: String,
    val targetType: String,
    val targetId: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class StateAnalyticsSummary(
    val state: String,
    val totalWorkers: Int,
    val skilledWorkers: Int,
    val unskilledWorkers: Int,
    val verifiedWorkers: Int,
    val registeredCompanies: Int,
    val activeJobs: Int
)
