package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.domain.SecurityHelper
import com.example.domain.WorkerCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ShramSetuPlatformTest {

    private lateinit var db: ShramSetuDatabase
    private lateinit var prefs: SessionPreferences
    private lateinit var repo: ShramSetuRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ShramSetuDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prefs = SessionPreferences(context)
        repo = ShramSetuRepository(db, prefs)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun privacyHelper_neverStoresRawAadhaarOrTwelveDigits() {
        val raw = "1234 5678 9012"
        val token = SecurityHelper.tokenizeIdentityReference("VID_TOKEN", raw)
        assertFalse(token.contains("12345678"))
        assertTrue(token.endsWith("9012"))

        val redacted = SecurityHelper.redactSensitiveDigitsInMessage("My id is 1234 5678 9012 call me")
        assertFalse(redacted.contains("5678"))
        assertTrue(redacted.contains("[ID REDACTED FOR PRIVACY]"))
    }

    @Test
    fun otpAndWorkerRegistrationAndAdminRbac_workEndToEnd() = runBlocking {
        repo.initializeDatabase()

        // 1. Mandatory OTP flow
        val otp = repo.requestOtp("9988776655").getOrThrow()
        assertEquals(6, otp.length)
        assertTrue(repo.verifyOtp("9988776655", "000000").isFailure)
        assertTrue(repo.verifyOtp("9988776655", otp).isSuccess)

        // 2. Register new Worker (Free)
        val worker = repo.registerWorker(
            WorkerRegistrationInput(
                fullName = "Amitabh Verma",
                fatherName = "Suresh Verma",
                dateOfBirth = "10/05/1994",
                gender = "Male",
                photoUri = "",
                mobileNumber = "9988776655",
                whatsappNumber = "9988776655",
                pin = "4321",
                state = "Karnataka",
                district = "Bengaluru Urban",
                currentAddress = "Whitefield",
                permanentAddress = "Whitefield",
                category = WorkerCategory.SKILLED.key,
                primaryTrade = "Electrician",
                skillsCsv = "Electrician, Wiring",
                experienceYears = 5,
                expectedDailyWage = 900,
                expectedMonthlySalary = 25000,
                preferredLocation = "Bengaluru Urban",
                availabilityStatus = "AVAILABLE",
                identityMethod = "DIGILOCKER_CONSENT",
                identityRawReference = "987654321234",
                consentIdentityVerify = true,
                consentTermsAndPrivacy = true,
                languageCode = "kn"
            )
        ).getOrThrow()

        assertTrue(worker.workerId.startsWith("IND-KA-2026-"))
        assertFalse(worker.identityTokenMasked.contains("98765432"))

        // 3. RBAC blocks worker from verifying an employer
        val pendingEmp = repo.observeAllEmployers().first().first { it.verificationStatus == VerificationStatuses.PENDING }
        val rbacFail = repo.adminVerifyEmployer(worker.userId, pendingEmp, true, "")
        assertTrue(rbacFail.isFailure)

        // 4. Super Admin verifies employer and employer posts a job
        val adminUser = repo.quickDemoLogin(UserRoles.SUPER_ADMIN).getOrThrow()
        assertTrue(repo.adminVerifyEmployer(adminUser.id, pendingEmp, true, "").isSuccess)
        val verifiedEmp = db.employerDao().getEmployerById(pendingEmp.employerId)!!
        assertEquals(VerificationStatuses.VERIFIED, verifiedEmp.verificationStatus)

        val job = repo.postJob(
            employer = verifiedEmp,
            title = "Bengaluru Site Electrician",
            category = WorkerCategory.SKILLED.key,
            tradeRequired = "Electrician",
            state = "Karnataka",
            district = "Bengaluru Urban",
            workLocation = "Electronic City",
            dailyWage = 950,
            monthlySalary = 26000,
            vacancies = 5,
            minExperienceYears = 2,
            description = "Industrial panel installation"
        ).getOrThrow()
        assertEquals("ACTIVE", job.status)

        // 5. Data Export contains worker profile
        val workerUser = db.userDao().getUserById(worker.userId)!!
        val json = repo.exportUserDataJson(workerUser)
        assertTrue(json.contains(worker.workerId))
        assertTrue(json.contains("Electrician"))
    }
}
