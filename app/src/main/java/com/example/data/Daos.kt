package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId AND isDeleted = 0 LIMIT 1")
    fun observeUserById(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :userId AND isDeleted = 0 LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE mobileNumber = :mobile AND isDeleted = 0 LIMIT 1")
    suspend fun getUserByMobile(mobile: String): UserEntity?

    @Query("SELECT * FROM users WHERE role = :role AND isDeleted = 0 LIMIT 1")
    suspend fun getFirstUserByRole(role: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)
}

@Dao
interface WorkerDao {
    @Query("SELECT * FROM worker_profiles WHERE isSuspended = 0 ORDER BY updatedAt DESC")
    fun observeAllActiveWorkers(): Flow<List<WorkerProfileEntity>>

    @Query("SELECT * FROM worker_profiles ORDER BY createdAt DESC")
    fun observeAllWorkersAdmin(): Flow<List<WorkerProfileEntity>>

    @Query("SELECT * FROM worker_profiles WHERE userId = :userId LIMIT 1")
    fun observeWorkerByUserId(userId: String): Flow<WorkerProfileEntity?>

    @Query("SELECT * FROM worker_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getWorkerByUserId(userId: String): WorkerProfileEntity?

    @Query("SELECT * FROM worker_profiles WHERE workerId = :workerId LIMIT 1")
    suspend fun getWorkerById(workerId: String): WorkerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorker(worker: WorkerProfileEntity)

    @Query("DELETE FROM worker_profiles WHERE userId = :userId")
    suspend fun deleteWorkerByUserId(userId: String)

    @Query("SELECT COUNT(*) FROM worker_profiles")
    suspend fun countWorkers(): Int
}

@Dao
interface EmployerDao {
    @Query("SELECT * FROM employers ORDER BY createdAt DESC")
    fun observeAllEmployers(): Flow<List<EmployerEntity>>

    @Query("SELECT * FROM employers WHERE userId = :userId LIMIT 1")
    fun observeEmployerByUserId(userId: String): Flow<EmployerEntity?>

    @Query("SELECT * FROM employers WHERE userId = :userId LIMIT 1")
    suspend fun getEmployerByUserId(userId: String): EmployerEntity?

    @Query("SELECT * FROM employers WHERE employerId = :employerId LIMIT 1")
    suspend fun getEmployerById(employerId: String): EmployerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEmployer(employer: EmployerEntity)

    @Query("DELETE FROM employers WHERE userId = :userId")
    suspend fun deleteEmployerByUserId(userId: String)
}

@Dao
interface JobPostDao {
    @Query("SELECT * FROM job_posts WHERE status = 'ACTIVE' ORDER BY createdAt DESC")
    fun observeActiveJobs(): Flow<List<JobPostEntity>>

    @Query("SELECT * FROM job_posts ORDER BY createdAt DESC")
    fun observeAllJobs(): Flow<List<JobPostEntity>>

    @Query("SELECT * FROM job_posts WHERE employerId = :employerId ORDER BY createdAt DESC")
    fun observeJobsByEmployer(employerId: String): Flow<List<JobPostEntity>>

    @Query("SELECT * FROM job_posts WHERE jobId = :jobId LIMIT 1")
    suspend fun getJobById(jobId: String): JobPostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJob(job: JobPostEntity)
}

@Dao
interface JobApplicationDao {
    @Query("SELECT * FROM job_applications WHERE workerUserId = :workerUserId ORDER BY updatedAt DESC")
    fun observeByWorkerUser(workerUserId: String): Flow<List<JobApplicationEntity>>

    @Query("SELECT * FROM job_applications WHERE employerUserId = :employerUserId ORDER BY updatedAt DESC")
    fun observeByEmployerUser(employerUserId: String): Flow<List<JobApplicationEntity>>

    @Query("SELECT * FROM job_applications ORDER BY updatedAt DESC")
    fun observeAllApplications(): Flow<List<JobApplicationEntity>>

    @Query("SELECT * FROM job_applications WHERE applicationId = :id LIMIT 1")
    suspend fun getById(id: String): JobApplicationEntity?

    @Query("SELECT * FROM job_applications WHERE jobId = :jobId AND workerId = :workerId LIMIT 1")
    suspend fun findExisting(jobId: String, workerId: String): JobApplicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(application: JobApplicationEntity)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE senderUserId = :userId OR receiverUserId = :userId ORDER BY createdAt ASC")
    fun observeMessagesForUser(userId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE senderUserId = :userId OR receiverUserId = :userId ORDER BY createdAt DESC")
    suspend fun getMessagesForUserOnce(userId: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET isRead = 1 WHERE receiverUserId = :userId AND threadKey = :threadKey")
    suspend fun markThreadRead(userId: String, threadKey: String)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeForUser(userId: String): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllRead(userId: String)
}

@Dao
interface ComplaintDao {
    @Query("SELECT * FROM complaints ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ComplaintEntity>>

    @Query("SELECT * FROM complaints WHERE reporterUserId = :userId ORDER BY createdAt DESC")
    fun observeByReporter(userId: String): Flow<List<ComplaintEntity>>

    @Query("SELECT * FROM complaints WHERE complaintId = :id LIMIT 1")
    suspend fun getById(id: String): ComplaintEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(complaint: ComplaintEntity)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 200")
    fun observeRecent(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AuditLogEntity)
}
