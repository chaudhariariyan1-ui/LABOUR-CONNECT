package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.domain.SecurityHelper
import com.example.domain.WorkerCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Database(
    entities = [
        UserEntity::class,
        WorkerProfileEntity::class,
        EmployerEntity::class,
        JobPostEntity::class,
        JobApplicationEntity::class,
        MessageEntity::class,
        NotificationEntity::class,
        ComplaintEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ShramSetuDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun workerDao(): WorkerDao
    abstract fun employerDao(): EmployerDao
    abstract fun jobPostDao(): JobPostDao
    abstract fun jobApplicationDao(): JobApplicationDao
    abstract fun messageDao(): MessageDao
    abstract fun notificationDao(): NotificationDao
    abstract fun complaintDao(): ComplaintDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: ShramSetuDatabase? = null

        fun getInstance(context: Context): ShramSetuDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShramSetuDatabase::class.java,
                    "shramsetu_workforce.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

object DatabaseSeeder {
    suspend fun seedIfEmpty(db: ShramSetuDatabase) = withContext(Dispatchers.IO) {
        if (db.workerDao().countWorkers() > 0) return@withContext

        val now = System.currentTimeMillis()
        val hour = 3_600_000L
        val day = 24 * hour

        fun makeUser(id: String, role: String, mobile: String, name: String, lang: String = "en"): UserEntity {
            val salt = SecurityHelper.generateSalt()
            val hash = SecurityHelper.hashSecret("1234", salt)
            return UserEntity(
                id = id,
                role = role,
                mobileNumber = mobile,
                passwordHash = hash,
                salt = salt,
                fullName = name,
                preferredLanguage = lang,
                consentAccepted = true,
                consentTimestamp = now - 5 * day,
                privacySharePhone = role == UserRoles.EMPLOYER,
                privacyShareExactAddress = false,
                createdAt = now - 5 * day,
                updatedAt = now - day
            )
        }

        // Core Demo Users (Worker, Employer, Super Admin)
        val demoWorkerUser = makeUser("usr_worker_demo", UserRoles.WORKER, "9876543210", "Rajesh Kumar Vishwakarma", "hi")
        val demoEmployerUser = makeUser("usr_emp_demo", UserRoles.EMPLOYER, "9820011223", "Vikramaditya Deshmukh")
        val superAdminUser = makeUser("usr_admin_root", UserRoles.SUPER_ADMIN, "9999000001", "National Registry Commissioner")

        val extraUsers = listOf(
            demoWorkerUser,
            demoEmployerUser,
            superAdminUser,
            makeUser("usr_w_2", UserRoles.WORKER, "9811223344", "Suresh Yadav"),
            makeUser("usr_w_3", UserRoles.WORKER, "9830044556", "Subhashis Mondal", "bn"),
            makeUser("usr_w_4", UserRoles.WORKER, "9848055667", "Venkatesh Rao", "te"),
            makeUser("usr_w_5", UserRoles.WORKER, "9840066778", "Karthikeyan Murugan", "ta"),
            makeUser("usr_w_6", UserRoles.WORKER, "9845077889", "Manjunath Gowda", "kn"),
            makeUser("usr_w_7", UserRoles.WORKER, "9825088990", "Maheshbhai Patel"),
            makeUser("usr_w_8", UserRoles.WORKER, "9829099001", "Bhanwar Singh Rathore"),
            makeUser("usr_w_9", UserRoles.WORKER, "9835011224", "Dinesh Paswan"),
            makeUser("usr_w_10", UserRoles.WORKER, "9810022335", "Imran Ansari"),
            makeUser("usr_emp_2", UserRoles.EMPLOYER, "9811133445", "Meenakshi Sundaram"),
            makeUser("usr_emp_3", UserRoles.EMPLOYER, "9831144556", "Anirban Chatterjee"),
            makeUser("usr_emp_4", UserRoles.EMPLOYER, "9871155667", "Harpreet Singh Gill")
        )
        extraUsers.forEach { db.userDao().upsertUser(it) }

        val workers = listOf(
            WorkerProfileEntity(
                workerId = "IND-MH-2026-104821",
                userId = "usr_worker_demo",
                fullName = "Rajesh Kumar Vishwakarma",
                fatherName = "Ramsevak Vishwakarma",
                dateOfBirth = "14/06/1993",
                gender = "Male",
                mobileNumber = "9876543210",
                whatsappNumber = "9876543210",
                state = "Maharashtra",
                district = "Pune",
                currentAddress = "Pimpri-Chinchwad Sector 12, Pune",
                permanentAddress = "Jaunpur Tehsil, Uttar Pradesh",
                category = WorkerCategory.SKILLED.key,
                primaryTrade = "Electrician",
                skillsCsv = "Electrician, Industrial Wiring, Panel Board, Solar Inverter",
                experienceYears = 7,
                expectedDailyWage = 950,
                expectedMonthlySalary = 26000,
                preferredLocation = "Pune, Mumbai, Thane",
                availabilityStatus = "AVAILABLE",
                mobileOtpVerified = true,
                identityVerificationMethod = "DIGILOCKER_CONSENT",
                identityTokenMasked = "DL-8F4A2B-XXXX-4921",
                identityVerified = VerificationStatuses.VERIFIED,
                createdAt = now - 10 * day,
                updatedAt = now - 1 * day
            ),
            WorkerProfileEntity(
                workerId = "IND-UP-2026-209314",
                userId = "usr_w_2",
                fullName = "Suresh Yadav",
                fatherName = "Brijesh Yadav",
                dateOfBirth = "03/02/1996",
                gender = "Male",
                mobileNumber = "9811223344",
                whatsappNumber = "9811223344",
                state = "Uttar Pradesh",
                district = "Noida",
                currentAddress = "Sector 62 Labour Colony, Noida",
                permanentAddress = "Azamgarh, Uttar Pradesh",
                category = WorkerCategory.UNSKILLED.key,
                primaryTrade = "Construction Helper",
                skillsCsv = "Construction Helper, Site Helper, Material Handling",
                experienceYears = 4,
                expectedDailyWage = 600,
                expectedMonthlySalary = 16500,
                preferredLocation = "Noida, Delhi NCR, Gurugram",
                availabilityStatus = "AVAILABLE",
                mobileOtpVerified = true,
                identityVerificationMethod = "ESHRAM_UAN",
                identityTokenMasked = "ES-3C19D4-XXXX-7740",
                identityVerified = VerificationStatuses.VERIFIED,
                createdAt = now - 8 * day,
                updatedAt = now - 2 * day
            ),
            WorkerProfileEntity(
                workerId = "IND-WB-2026-318452",
                userId = "usr_w_3",
                fullName = "Subhashis Mondal",
                fatherName = "Tapan Mondal",
                dateOfBirth = "21/09/1991",
                gender = "Male",
                mobileNumber = "9830044556",
                whatsappNumber = "9830044556",
                state = "West Bengal",
                district = "Kolkata",
                currentAddress = "Salt Lake Sector V, Kolkata",
                permanentAddress = "Bardhaman, West Bengal",
                category = WorkerCategory.SKILLED.key,
                primaryTrade = "Welder",
                skillsCsv = "Welder, Arc Welding, TIG Welding, Steel Fixer",
                experienceYears = 9,
                expectedDailyWage = 1050,
                expectedMonthlySalary = 29000,
                preferredLocation = "Kolkata, Howrah, Durgapur",
                availabilityStatus = "AVAILABLE",
                mobileOtpVerified = true,
                identityVerificationMethod = "VID_TOKEN",
                identityTokenMasked = "VID-XXXX-XXXX-8812",
                identityVerified = VerificationStatuses.VERIFIED,
                createdAt = now - 7 * day,
                updatedAt = now - 2 * day
            ),
            WorkerProfileEntity(
                workerId = "IND-TS-2026-412908",
                userId = "usr_w_4",
                fullName = "Venkatesh Rao",
                fatherName = "Narasimha Rao",
                dateOfBirth = "10/11/1994",
                gender = "Male",
                mobileNumber = "9848055667",
                whatsappNumber = "9848055667",
                state = "Telangana",
                district = "Hyderabad",
                currentAddress = "Kukatpally Housing Board, Hyderabad",
                permanentAddress = "Warangal, Telangana",
                category = WorkerCategory.SKILLED.key,
                primaryTrade = "AC Technician",
                skillsCsv = "AC Technician, HVAC Ducting, Chiller Maintenance",
                experienceYears = 6,
                expectedDailyWage = 1000,
                expectedMonthlySalary = 27500,
                preferredLocation = "Hyderabad, Rangareddy",
                availabilityStatus = "WORKING_NOTICE",
                mobileOtpVerified = true,
                identityVerificationMethod = "DIGILOCKER_CONSENT",
                identityTokenMasked = "DL-7A21E9-XXXX-1103",
                identityVerified = VerificationStatuses.VERIFIED,
                createdAt = now - 6 * day,
                updatedAt = now - 1 * day
            ),
            WorkerProfileEntity(
                workerId = "IND-TN-2026-509123",
                userId = "usr_w_5",
                fullName = "Karthikeyan Murugan",
                fatherName = "Murugan Vel",
                dateOfBirth = "18/04/1995",
                gender = "Male",
                mobileNumber = "9840066778",
                whatsappNumber = "9840066778",
                state = "Tamil Nadu",
                district = "Chennai",
                currentAddress = "Ambattur Industrial Estate, Chennai",
                permanentAddress = "Madurai, Tamil Nadu",
                category = WorkerCategory.SKILLED.key,
                primaryTrade = "Machine Operator",
                skillsCsv = "Machine Operator, CNC Lathe, Hydraulic Press",
                experienceYears = 5,
                expectedDailyWage = 900,
                expectedMonthlySalary = 25000,
                preferredLocation = "Chennai, Coimbatore",
                availabilityStatus = "AVAILABLE",
                mobileOtpVerified = true,
                identityVerificationMethod = "ESHRAM_UAN",
                identityTokenMasked = "ES-91B3F0-XXXX-5529",
                identityVerified = VerificationStatuses.VERIFIED,
                createdAt = now - 6 * day,
                updatedAt = now - 1 * day
            ),
            WorkerProfileEntity(
                workerId = "IND-KA-2026-614772",
                userId = "usr_w_6",
                fullName = "Manjunath Gowda",
                fatherName = "Siddappa Gowda",
                dateOfBirth = "05/08/1992",
                gender = "Male",
                mobileNumber = "9845077889",
                whatsappNumber = "9845077889",
                state = "Karnataka",
                district = "Bengaluru Urban",
                currentAddress = "Peenya Industrial Area, Bengaluru",
                permanentAddress = "Tumakuru, Karnataka",
                category = WorkerCategory.SKILLED.key,
                primaryTrade = "Plumber",
                skillsCsv = "Plumber, Pipe Fitter, Sanitary Fitting",
                experienceYears = 8,
                expectedDailyWage = 920,
                expectedMonthlySalary = 25500,
                preferredLocation = "Bengaluru Urban, Mysuru",
                availabilityStatus = "AVAILABLE",
                mobileOtpVerified = true,
                identityVerificationMethod = "VID_TOKEN",
                identityTokenMasked = "VID-XXXX-XXXX-6634",
                identityVerified = VerificationStatuses.PENDING,
                createdAt = now - 4 * day,
                updatedAt = now - 12 * hour
            ),
            WorkerProfileEntity(
                workerId = "IND-GJ-2026-718340",
                userId = "usr_w_7",
                fullName = "Maheshbhai Patel",
                fatherName = "Rameshbhai Patel",
                dateOfBirth = "28/01/1997",
                gender = "Male",
                mobileNumber = "9825088990",
                whatsappNumber = "9825088990",
                state = "Gujarat",
                district = "Surat",
                currentAddress = "Hazira Industrial Belt, Surat",
                permanentAddress = "Bhavnagar, Gujarat",
                category = WorkerCategory.UNSKILLED.key,
                primaryTrade = "Warehouse Helper",
                skillsCsv = "Warehouse Helper, Packaging Worker, Loading Worker",
                experienceYears = 3,
                expectedDailyWage = 580,
                expectedMonthlySalary = 15800,
                preferredLocation = "Surat, Bharuch, Vadodara",
                availabilityStatus = "AVAILABLE",
                mobileOtpVerified = true,
                identityVerificationMethod = "ESHRAM_UAN",
                identityTokenMasked = "ES-54D1A8-XXXX-9981",
                identityVerified = VerificationStatuses.VERIFIED,
                createdAt = now - 4 * day,
                updatedAt = now - 6 * hour
            ),
            WorkerProfileEntity(
                workerId = "IND-RJ-2026-829105",
                userId = "usr_w_8",
                fullName = "Bhanwar Singh Rathore",
                fatherName = "Karan Singh Rathore",
                dateOfBirth = "12/12/1989",
                gender = "Male",
                mobileNumber = "9829099001",
                whatsappNumber = "9829099001",
                state = "Rajasthan",
                district = "Jaipur",
                currentAddress = "Sitapura Industrial Area, Jaipur",
                permanentAddress = "Jodhpur, Rajasthan",
                category = WorkerCategory.SKILLED.key,
                primaryTrade = "Tiles Worker",
                skillsCsv = "Tiles Worker, Mason, Marble & Granite Polishing",
                experienceYears = 11,
                expectedDailyWage = 1100,
                expectedMonthlySalary = 30000,
                preferredLocation = "Jaipur, Udaipur, Gurugram",
                availabilityStatus = "AVAILABLE",
                mobileOtpVerified = true,
                identityVerificationMethod = "DIGILOCKER_CONSENT",
                identityTokenMasked = "DL-2B87C1-XXXX-3012",
                identityVerified = VerificationStatuses.VERIFIED,
                createdAt = now - 3 * day,
                updatedAt = now - 4 * hour
            ),
            WorkerProfileEntity(
                workerId = "IND-BR-2026-901244",
                userId = "usr_w_9",
                fullName = "Dinesh Paswan",
                fatherName = "Mahadev Paswan",
                dateOfBirth = "09/03/1998",
                gender = "Male",
                mobileNumber = "9835011224",
                whatsappNumber = "9835011224",
                state = "Bihar",
                district = "Patna",
                currentAddress = "Danapur Station Road, Patna",
                permanentAddress = "Darbhanga, Bihar",
                category = WorkerCategory.UNSKILLED.key,
                primaryTrade = "General Labour",
                skillsCsv = "General Labour, Loading Worker, Cleaning Worker",
                experienceYears = 2,
                expectedDailyWage = 550,
                expectedMonthlySalary = 14800,
                preferredLocation = "Patna, Delhi, Mumbai",
                availabilityStatus = "AVAILABLE",
                mobileOtpVerified = true,
                identityVerificationMethod = "VID_TOKEN",
                identityTokenMasked = "VID-XXXX-XXXX-2278",
                identityVerified = VerificationStatuses.PENDING,
                createdAt = now - 2 * day,
                updatedAt = now - 2 * hour
            ),
            WorkerProfileEntity(
                workerId = "IND-DL-2026-951402",
                userId = "usr_w_10",
                fullName = "Imran Ansari",
                fatherName = "Salim Ansari",
                dateOfBirth = "24/07/1990",
                gender = "Male",
                mobileNumber = "9810022335",
                whatsappNumber = "9810022335",
                state = "Delhi (NCT)",
                district = "South Delhi",
                currentAddress = "Okhla Phase II, New Delhi",
                permanentAddress = "Meerut, Uttar Pradesh",
                category = WorkerCategory.SKILLED.key,
                primaryTrade = "Shuttering Carpenter",
                skillsCsv = "Shuttering Carpenter, Carpenter, Steel Fixer",
                experienceYears = 10,
                expectedDailyWage = 1000,
                expectedMonthlySalary = 28000,
                preferredLocation = "New Delhi, Noida, Faridabad",
                availabilityStatus = "AVAILABLE",
                mobileOtpVerified = true,
                identityVerificationMethod = "DIGILOCKER_CONSENT",
                identityTokenMasked = "DL-4E11F6-XXXX-7410",
                identityVerified = VerificationStatuses.VERIFIED,
                createdAt = now - 2 * day,
                updatedAt = now - hour
            )
        )
        workers.forEach { db.workerDao().upsertWorker(it) }

        val employers = listOf(
            EmployerEntity(
                employerId = "emp_bharat_infra",
                userId = "usr_emp_demo",
                companyName = "Bharat InfraBuild Pvt Ltd",
                companyType = "Infrastructure Builder",
                gstOrCinNumber = "27AABCB4821K1Z5",
                registrationDocNumber = "MCA-CIN-U45200MH2018PTC310421",
                contactPerson = "Vikramaditya Deshmukh",
                officialEmail = "hiring@bharatinfrabuild.in",
                mobileNumber = "9820011223",
                state = "Maharashtra",
                district = "Pune",
                officeAddress = "Tower B, Magarpatta Cybercity, Pune 411028",
                verificationStatus = VerificationStatuses.VERIFIED,
                createdAt = now - 15 * day,
                updatedAt = now - 3 * day
            ),
            EmployerEntity(
                employerId = "emp_metro_works",
                userId = "usr_emp_2",
                companyName = "Dakshin Metro Engineering Corp",
                companyType = "Construction Company",
                gstOrCinNumber = "33AAACD9120M1Z2",
                registrationDocNumber = "MCA-CIN-U45309TN2016PLC112890",
                contactPerson = "Meenakshi Sundaram",
                officialEmail = "sitehr@dakshinmetro.in",
                mobileNumber = "9811133445",
                state = "Tamil Nadu",
                district = "Chennai",
                officeAddress = "Guindy Industrial Estate, Chennai 600032",
                verificationStatus = VerificationStatuses.VERIFIED,
                createdAt = now - 12 * day,
                updatedAt = now - 4 * day
            ),
            EmployerEntity(
                employerId = "emp_bengal_logistics",
                userId = "usr_emp_3",
                companyName = "Purba Logistics & Warehousing",
                companyType = "Warehouse & Logistics",
                gstOrCinNumber = "19AABCP7741R1Z9",
                registrationDocNumber = "WB-SHOP-EST-2021-88912",
                contactPerson = "Anirban Chatterjee",
                officialEmail = "ops@purbalogistics.in",
                mobileNumber = "9831144556",
                state = "West Bengal",
                district = "Kolkata",
                officeAddress = "Dankuni Freight Park, Kolkata 712311",
                verificationStatus = VerificationStatuses.PENDING,
                createdAt = now - 3 * day,
                updatedAt = now - day
            ),
            EmployerEntity(
                employerId = "emp_north_steel",
                userId = "usr_emp_4",
                companyName = "NavBharat Steel Fabricators",
                companyType = "Manufacturing & Factory",
                gstOrCinNumber = "07AAACN5512P1Z1",
                registrationDocNumber = "DEL-FAC-2019-44021",
                contactPerson = "Harpreet Singh Gill",
                officialEmail = "hr@navbharatsteel.in",
                mobileNumber = "9871155667",
                state = "Delhi (NCT)",
                district = "South Delhi",
                officeAddress = "Okhla Industrial Area Phase III, New Delhi",
                verificationStatus = VerificationStatuses.PENDING,
                createdAt = now - 2 * day,
                updatedAt = now - 6 * hour
            )
        )
        employers.forEach { db.employerDao().upsertEmployer(it) }

        val jobs = listOf(
            JobPostEntity(
                jobId = "job_101",
                employerId = "emp_bharat_infra",
                companyName = "Bharat InfraBuild Pvt Ltd",
                employerVerified = true,
                title = "High-Rise Commercial Site Electricians",
                category = WorkerCategory.SKILLED.key,
                tradeRequired = "Electrician",
                state = "Maharashtra",
                district = "Pune",
                workLocation = "Hinjewadi Phase 3 Tech Park",
                dailyWage = 980,
                monthlySalary = 27000,
                vacancies = 12,
                minExperienceYears = 3,
                description = "3-phase commercial wiring, LT/HT panel installation, and safety earthing for IT tower project. Accommodation & safety gear provided.",
                createdAt = now - 3 * day
            ),
            JobPostEntity(
                jobId = "job_102",
                employerId = "emp_bharat_infra",
                companyName = "Bharat InfraBuild Pvt Ltd",
                employerVerified = true,
                title = "Metro Viaduct Shuttering Carpenters",
                category = WorkerCategory.SKILLED.key,
                tradeRequired = "Shuttering Carpenter",
                state = "Maharashtra",
                district = "Mumbai",
                workLocation = "Thane-Wadala Corridor Site 4",
                dailyWage = 1050,
                monthlySalary = 29000,
                vacancies = 18,
                minExperienceYears = 4,
                description = "Heavy steel & plywood formwork assembly for elevated metro piers. Free PPE kit and PF/ESIC coverage.",
                createdAt = now - 2 * day
            ),
            JobPostEntity(
                jobId = "job_103",
                employerId = "emp_bharat_infra",
                companyName = "Bharat InfraBuild Pvt Ltd",
                employerVerified = true,
                title = "Construction Site Helpers (Day Shift)",
                category = WorkerCategory.UNSKILLED.key,
                tradeRequired = "Construction Helper",
                state = "Maharashtra",
                district = "Pune",
                workLocation = "Baner Smart Highway Stretch",
                dailyWage = 620,
                monthlySalary = 16800,
                vacancies = 30,
                minExperienceYears = 0,
                description = "Direct company payroll, weekly wage disbursement, clean drinking water & transit camp provided.",
                createdAt = now - 2 * day
            ),
            JobPostEntity(
                jobId = "job_104",
                employerId = "emp_metro_works",
                companyName = "Dakshin Metro Engineering Corp",
                employerVerified = true,
                title = "CNC & Heavy Machine Operators",
                category = WorkerCategory.SKILLED.key,
                tradeRequired = "Machine Operator",
                state = "Tamil Nadu",
                district = "Chennai",
                workLocation = "Sriperumbudur Precast Yard",
                dailyWage = 950,
                monthlySalary = 26000,
                vacancies = 8,
                minExperienceYears = 2,
                description = "Operate concrete batching & precast gantry equipment. Overtime paid as per statutory norms.",
                createdAt = now - 4 * day
            ),
            JobPostEntity(
                jobId = "job_105",
                employerId = "emp_metro_works",
                companyName = "Dakshin Metro Engineering Corp",
                employerVerified = true,
                title = "Arc & Structural Welders",
                category = WorkerCategory.SKILLED.key,
                tradeRequired = "Welder",
                state = "Tamil Nadu",
                district = "Coimbatore",
                workLocation = "L&T Bypass Fabrication Depot",
                dailyWage = 1020,
                monthlySalary = 28500,
                vacancies = 10,
                minExperienceYears = 3,
                description = "Structural girder welding & reinforcement cage assembly.",
                createdAt = now - day
            ),
            JobPostEntity(
                jobId = "job_106",
                employerId = "emp_bengal_logistics",
                companyName = "Purba Logistics & Warehousing",
                employerVerified = false,
                title = "Fulfillment Warehouse & Packaging Helpers",
                category = WorkerCategory.UNSKILLED.key,
                tradeRequired = "Warehouse Helper",
                state = "West Bengal",
                district = "Kolkata",
                workLocation = "Dankuni Freight Park",
                dailyWage = 590,
                monthlySalary = 16000,
                vacancies = 25,
                minExperienceYears = 0,
                description = "Sorting, palletizing, and barcode packaging inside modern ventilated warehouse.",
                createdAt = now - day
            ),
            JobPostEntity(
                jobId = "job_107",
                employerId = "emp_north_steel",
                companyName = "NavBharat Steel Fabricators",
                employerVerified = false,
                title = "Factory Loading & Material Workers",
                category = WorkerCategory.UNSKILLED.key,
                tradeRequired = "Loading Worker",
                state = "Delhi (NCT)",
                district = "South Delhi",
                workLocation = "Okhla Phase III",
                dailyWage = 640,
                monthlySalary = 17500,
                vacancies = 15,
                minExperienceYears = 1,
                description = "Loading finished steel sheets and assisting crane operators.",
                createdAt = now - 12 * hour
            )
        )
        jobs.forEach { db.jobPostDao().upsertJob(it) }

        // Seed Sample Invitation & Application
        db.jobApplicationDao().upsert(
            JobApplicationEntity(
                applicationId = "app_seed_1",
                jobId = "job_101",
                jobTitle = "High-Rise Commercial Site Electricians",
                companyName = "Bharat InfraBuild Pvt Ltd",
                workerId = "IND-MH-2026-104821",
                workerUserId = "usr_worker_demo",
                employerId = "emp_bharat_infra",
                employerUserId = "usr_emp_demo",
                workerName = "Rajesh Kumar Vishwakarma",
                workerTrade = "Electrician",
                workerLocation = "Pune, Maharashtra",
                status = "INVITED",
                type = "INVITATION",
                note = "We reviewed your verified Electrician profile (7 yrs exp). Please accept to schedule site onboarding at Hinjewadi.",
                createdAt = now - 10 * hour,
                updatedAt = now - 10 * hour
            )
        )
        db.jobApplicationDao().upsert(
            JobApplicationEntity(
                applicationId = "app_seed_2",
                jobId = "job_103",
                jobTitle = "Construction Site Helpers (Day Shift)",
                companyName = "Bharat InfraBuild Pvt Ltd",
                workerId = "IND-UP-2026-209314",
                workerUserId = "usr_w_2",
                employerId = "emp_bharat_infra",
                employerUserId = "usr_emp_demo",
                workerName = "Suresh Yadav",
                workerTrade = "Construction Helper",
                workerLocation = "Noida, Uttar Pradesh",
                status = "APPLIED",
                type = "APPLICATION",
                note = "Ready to relocate to Pune site immediately.",
                createdAt = now - 5 * hour,
                updatedAt = now - 5 * hour
            )
        )

        // Seed Messages
        db.messageDao().insertMessage(
            MessageEntity(
                messageId = "msg_seed_1",
                threadKey = "usr_emp_demo_usr_worker_demo",
                senderUserId = "usr_emp_demo",
                senderName = "Bharat InfraBuild Pvt Ltd",
                senderRole = UserRoles.EMPLOYER,
                receiverUserId = "usr_worker_demo",
                receiverName = "Rajesh Kumar Vishwakarma",
                content = "Namaste Rajesh ji, we have an immediate opening for Lead Site Electrician in Hinjewadi Phase 3 (₹980/day + safety gear & quarters). Are you available starting Monday?",
                createdAt = now - 8 * hour
            )
        )

        // Seed Notifications
        db.notificationDao().insert(
            NotificationEntity(
                notificationId = "notif_seed_1",
                userId = "usr_worker_demo",
                title = "New Work Invitation from Bharat InfraBuild",
                body = "You have been invited for High-Rise Commercial Site Electricians in Pune (₹980/day).",
                type = "INVITATION",
                createdAt = now - 10 * hour
            )
        )
        db.notificationDao().insert(
            NotificationEntity(
                notificationId = "notif_seed_2",
                userId = "usr_worker_demo",
                title = "Digital Worker ID Verified",
                body = "Your consent-based verification (DigiLocker Token) is complete. ID: IND-MH-2026-104821",
                type = "VERIFICATION",
                createdAt = now - 3 * day
            )
        )

        // Seed Complaints
        db.complaintDao().upsert(
            ComplaintEntity(
                complaintId = "cmp_seed_1",
                reporterUserId = "usr_w_2",
                reporterName = "Suresh Yadav",
                reporterRole = UserRoles.WORKER,
                targetName = "Unregistered Sub-Contractor (Noida Sector 62)",
                category = "WAGE_DISPUTE",
                description = "Pending 4 days overtime wage settlement from previous site contractor.",
                status = "IN_REVIEW",
                adminResolutionNote = "Assigned to UP Labour Welfare Desk for employer conciliation.",
                createdAt = now - 2 * day,
                updatedAt = now - day
            )
        )

        // Seed Audit Logs
        db.auditLogDao().insert(
            AuditLogEntity(
                logId = "aud_seed_1",
                actorUserId = "usr_admin_root",
                actorRole = UserRoles.SUPER_ADMIN,
                action = "EMPLOYER_VERIFIED",
                targetType = "EMPLOYER",
                targetId = "emp_bharat_infra",
                details = "Verified GST & MCA CIN documents for Bharat InfraBuild Pvt Ltd",
                timestamp = now - 3 * day
            )
        )
        db.auditLogDao().insert(
            AuditLogEntity(
                logId = "aud_seed_2",
                actorUserId = "usr_worker_demo",
                actorRole = UserRoles.WORKER,
                action = "WORKER_ID_ISSUED",
                targetType = "WORKER",
                targetId = "IND-MH-2026-104821",
                details = "Free registration completed with OTP & privacy-preserving DigiLocker token",
                timestamp = now - 10 * day
            )
        )
    }
}
