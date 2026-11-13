package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.domain.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun WorkerDashboardScreen(
    user: UserEntity,
    worker: WorkerProfileEntity?,
    currentLanguage: AppLanguage,
    jobs: List<JobPostEntity>,
    applications: List<JobApplicationEntity>,
    messages: List<MessageEntity>,
    notifications: List<NotificationEntity>,
    complaints: List<ComplaintEntity>,
    onLanguageSelected: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    onApplyJob: (WorkerProfileEntity, JobPostEntity, String) -> Unit,
    onRespondApplication: (JobApplicationEntity, String) -> Unit,
    onUpdateWorker: (WorkerProfileEntity) -> Unit,
    onSendMessage: (receiverId: String, receiverName: String, content: String) -> Unit,
    onMarkNotificationsRead: () -> Unit,
    onSubmitComplaint: (target: String, category: String, description: String, onDone: () -> Unit) -> Unit,
    onUpdatePrivacy: (sharePhone: Boolean, shareAddress: Boolean, alerts: Boolean) -> Unit,
    onExportData: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val strings = LocalAppStrings.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showNotifications by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }
    var showComplaintDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showLegalDialog by remember { mutableStateOf<Boolean?>(null) }

    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }

    val navItems = listOf(
        Triple(strings.navJobs, Icons.Default.Work, "worker_tab_jobs"),
        Triple(strings.navDigitalId, Icons.Default.Badge, "worker_tab_id"),
        Triple(strings.navApplications, Icons.Default.AssignmentTurnedIn, "worker_tab_apps"),
        Triple(strings.navMessages, Icons.Default.Chat, "worker_tab_msgs"),
        Triple(strings.navProfile, Icons.Default.Person, "worker_tab_profile")
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 720.dp

        Scaffold(
            topBar = {
                ShramSetuTopBar(
                    title = strings.appName,
                    subtitle = worker?.workerId ?: user.fullName,
                    currentLanguage = currentLanguage,
                    unreadNotifications = unreadCount,
                    onLanguageSelected = onLanguageSelected,
                    onNotificationsClick = { showNotifications = true },
                    onLogoutClick = onLogout
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        navItems.forEachIndexed { idx, (label, icon, tag) ->
                            NavigationBarItem(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.testTag(tag)
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (useRail) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        navItems.forEachIndexed { idx, (label, icon, tag) ->
                            NavigationRailItem(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label) },
                                modifier = Modifier.testTag(tag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    if (worker == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        when (selectedTab) {
                            0 -> WorkerJobsTab(
                                worker = worker,
                                jobs = jobs,
                                applications = applications,
                                onApplyJob = onApplyJob
                            )
                            1 -> WorkerDigitalIdTab(
                                worker = worker,
                                onUpdateAvailability = { nextAvail ->
                                    onUpdateWorker(worker.copy(availabilityStatus = nextAvail))
                                },
                                onEditProfile = { showEditProfile = true }
                            )
                            2 -> WorkerRequestsTab(
                                applications = applications,
                                onRespond = onRespondApplication,
                                onMessageEmployer = { empUserId, companyName ->
                                    onSendMessage(empUserId, companyName, "Namaste, following up regarding the job opportunity.")
                                    selectedTab = 3
                                }
                            )
                            3 -> MessagingHubView(
                                currentUser = user,
                                messages = messages,
                                onSendMessage = onSendMessage
                            )
                            4 -> WorkerProfileAndSettingsTab(
                                user = user,
                                worker = worker,
                                complaints = complaints,
                                onEditProfile = { showEditProfile = true },
                                onUpdatePrivacy = onUpdatePrivacy,
                                onRaiseComplaint = { showComplaintDialog = true },
                                onExportData = onExportData,
                                onShowLegal = { isTerms -> showLegalDialog = isTerms },
                                onRequestDelete = { showDeleteConfirm = true },
                                onLogout = onLogout
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNotifications) {
        NotificationsDialog(
            notifications = notifications,
            onMarkAllRead = onMarkNotificationsRead,
            onDismiss = { showNotifications = false }
        )
    }

    if (showEditProfile && worker != null) {
        EditWorkerProfileDialog(
            worker = worker,
            onSave = { updated ->
                onUpdateWorker(updated)
                showEditProfile = false
            },
            onDismiss = { showEditProfile = false }
        )
    }

    if (showComplaintDialog) {
        ComplaintFormDialog(
            onSubmit = { target, cat, desc ->
                onSubmitComplaint(target, cat, desc) { showComplaintDialog = false }
            },
            onDismiss = { showComplaintDialog = false }
        )
    }

    if (showLegalDialog != null) {
        LegalDocumentDialog(
            isTerms = showLegalDialog == true,
            onDismiss = { showLegalDialog = null }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account Permanently?") },
            text = {
                Text("This permanently erases your Digital Worker ID, profile, and personal details from ShramSetu. This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Delete Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun WorkerJobsTab(
    worker: WorkerProfileEntity,
    jobs: List<JobPostEntity>,
    applications: List<JobApplicationEntity>,
    onApplyJob: (WorkerProfileEntity, JobPostEntity, String) -> Unit
) {
    val strings = LocalAppStrings.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedState by remember { mutableStateOf("All India") }
    var selectedDistrict by remember { mutableStateOf("All Districts") }
    var categoryFilter by remember { mutableStateOf("ALL") }
    var jobToApply by remember { mutableStateOf<JobPostEntity?>(null) }

    val appliedJobIds = remember(applications) { applications.map { it.jobId }.toSet() }

    val filteredJobs = remember(jobs, searchQuery, selectedState, selectedDistrict, categoryFilter) {
        jobs.filter { job ->
            val matchesQuery = searchQuery.isBlank() ||
                job.title.contains(searchQuery, ignoreCase = true) ||
                job.tradeRequired.contains(searchQuery, ignoreCase = true) ||
                job.companyName.contains(searchQuery, ignoreCase = true) ||
                job.workLocation.contains(searchQuery, ignoreCase = true)
            val matchesState = selectedState == "All India" || job.state.equals(selectedState, ignoreCase = true)
            val matchesDist = selectedDistrict == "All Districts" || job.district.equals(selectedDistrict, ignoreCase = true)
            val matchesCat = categoryFilter == "ALL" || job.category == categoryFilter
            matchesQuery && matchesState && matchesDist && matchesCat
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.searchJobsPlaceholder) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("worker_job_search_input")
            )
        }

        item {
            StateAndDistrictSelector(
                selectedState = selectedState,
                selectedDistrict = selectedDistrict,
                includeAllIndiaOption = true,
                onStateChanged = { selectedState = it },
                onDistrictChanged = { selectedDistrict = it }
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ALL" to "All Trades", "SKILLED" to strings.skilledLabel, "UNSKILLED" to strings.unskilledLabel)
                    .forEach { (key, label) ->
                        FilterChip(
                            selected = categoryFilter == key,
                            onClick = { categoryFilter = key },
                            label = { Text(label) }
                        )
                    }
            }
        }

        if (filteredJobs.isEmpty()) {
            item {
                EmptyStateView(
                    title = "No matching jobs in this area",
                    subtitle = "Try switching state to All India or clearing filters."
                )
            }
        } else {
            items(filteredJobs, key = { it.jobId }) { job ->
                val alreadyApplied = job.jobId in appliedJobIds
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(job.title, style = MaterialTheme.typography.titleMedium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        job.companyName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (job.employerVerified) {
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.Verified,
                                            contentDescription = "Verified Employer",
                                            tint = VerifiedGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            StatusPill(
                                text = job.category,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            "${job.tradeRequired} • ${job.workLocation}, ${job.district}, ${job.state}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            job.description,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "₹${job.dailyWage}/day • ₹${job.monthlySalary}/mo",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = VerifiedGreen
                                )
                                Text(
                                    "${job.vacancies} vacancies • Min ${job.minExperienceYears} yrs exp",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { jobToApply = job },
                                enabled = !alreadyApplied,
                                modifier = Modifier.heightIn(min = 48.dp).testTag("apply_job_${job.jobId}")
                            ) {
                                Text(if (alreadyApplied) strings.appliedLabel else strings.applyNow)
                            }
                        }
                    }
                }
            }
        }
    }

    if (jobToApply != null) {
        var note by remember { mutableStateOf("Available immediately. Experience: ${worker.experienceYears} years in ${worker.primaryTrade}.") }
        AlertDialog(
            onDismissRequest = { jobToApply = null },
            title = { Text("Apply: ${jobToApply!!.title}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${jobToApply!!.companyName} • ₹${jobToApply!!.dailyWage}/day", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note to Employer (Optional)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onApplyJob(worker, jobToApply!!, note)
                    jobToApply = null
                }) { Text("Confirm Application") }
            },
            dismissButton = {
                TextButton(onClick = { jobToApply = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun WorkerDigitalIdTab(
    worker: WorkerProfileEntity,
    onUpdateAvailability: (String) -> Unit,
    onEditProfile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DigitalWorkerIdCard(worker = worker)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Availability Status", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "AVAILABLE" to "Available",
                        "WORKING_NOTICE" to "Notice Period",
                        "BUSY" to "Busy"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = worker.availabilityStatus == key,
                            onClick = { onUpdateAvailability(key) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Registered Profile Details", style = MaterialTheme.typography.titleMedium)
                DetailRow("Father's Name", worker.fatherName)
                DetailRow("Date of Birth", worker.dateOfBirth)
                DetailRow("Gender", worker.gender)
                DetailRow("Preferred Location", worker.preferredLocation)
                DetailRow("Expected Monthly Salary", "₹${worker.expectedMonthlySalary}")
                DetailRow("Verification Method", worker.identityVerificationMethod)
                DetailRow("Masked Token", worker.identityTokenMasked)
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onEditProfile,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Edit Worker Profile")
                }
            }
        }
    }
}

@Composable
private fun WorkerRequestsTab(
    applications: List<JobApplicationEntity>,
    onRespond: (JobApplicationEntity, String) -> Unit,
    onMessageEmployer: (employerUserId: String, companyName: String) -> Unit
) {
    if (applications.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            EmptyStateView(
                title = "No applications or invitations yet",
                subtitle = "Apply to jobs in the Jobs tab or respond to direct invitations from verified companies."
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(applications, key = { it.applicationId }) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.jobTitle, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        StatusPill(
                            text = item.status,
                            containerColor = when (item.status) {
                                "HIRED", "ACCEPTED" -> VerifiedGreenBg
                                "DECLINED" -> DangerRedBg
                                "INVITED" -> MaterialTheme.colorScheme.primaryContainer
                                else -> WarningAmberBg
                            },
                            contentColor = when (item.status) {
                                "HIRED", "ACCEPTED" -> VerifiedGreen
                                "DECLINED" -> DangerRed
                                "INVITED" -> MaterialTheme.colorScheme.primary
                                else -> WarningAmber
                            }
                        )
                    }
                    Text("${item.companyName} • ${item.type}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    if (item.note.isNotBlank()) {
                        Text(item.note, style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (item.type == "INVITATION" && item.status == "INVITED") {
                            Button(
                                onClick = { onRespond(item, "ACCEPTED") },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text("Accept Invite")
                            }
                            OutlinedButton(
                                onClick = { onRespond(item, "DECLINED") },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text("Decline")
                            }
                        }
                        TextButton(
                            onClick = { onMessageEmployer(item.employerUserId, item.companyName) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Message")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkerProfileAndSettingsTab(
    user: UserEntity,
    worker: WorkerProfileEntity,
    complaints: List<ComplaintEntity>,
    onEditProfile: () -> Unit,
    onUpdatePrivacy: (Boolean, Boolean, Boolean) -> Unit,
    onRaiseComplaint: () -> Unit,
    onExportData: () -> Unit,
    onShowLegal: (Boolean) -> Unit,
    onRequestDelete: () -> Unit,
    onLogout: () -> Unit
) {
    val strings = LocalAppStrings.current
    var sharePhone by remember(user.privacySharePhone) { mutableStateOf(user.privacySharePhone) }
    var shareAddress by remember(user.privacyShareExactAddress) { mutableStateOf(user.privacyShareExactAddress) }
    var jobAlerts by remember(user.jobAlertsEnabled) { mutableStateOf(user.jobAlertsEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                WorkerAvatar(fullName = worker.fullName, photoUri = worker.photoUri, sizeDp = 56)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(worker.fullName, style = MaterialTheme.typography.titleMedium)
                    Text("${worker.primaryTrade} • ${worker.workerId}", style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onEditProfile) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit profile")
                }
            }
        }

        // Privacy & Consent Management
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(strings.privacySettings, style = MaterialTheme.typography.titleMedium)
                SwitchRow(
                    title = "Share Mobile with Verified Employers",
                    subtitle = "When OFF, employers see masked number (+91 ••••••${worker.mobileNumber.takeLast(4)})",
                    checked = sharePhone,
                    onCheckedChange = {
                        sharePhone = it
                        onUpdatePrivacy(sharePhone, shareAddress, jobAlerts)
                    }
                )
                SwitchRow(
                    title = "Share Exact Current Address",
                    subtitle = "When OFF, employers only see ${worker.district}, ${worker.state}",
                    checked = shareAddress,
                    onCheckedChange = {
                        shareAddress = it
                        onUpdatePrivacy(sharePhone, shareAddress, jobAlerts)
                    }
                )
                SwitchRow(
                    title = "Location-Based Job Notifications",
                    subtitle = "Receive matching trade & wage alerts",
                    checked = jobAlerts,
                    onCheckedChange = {
                        jobAlerts = it
                        onUpdatePrivacy(sharePhone, shareAddress, jobAlerts)
                    }
                )
            }
        }

        // Grievance & Support
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Complaints & Welfare Desk", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onRaiseComplaint) { Text(strings.raiseComplaint) }
                }
                if (complaints.isEmpty()) {
                    Text("No active complaints filed.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    complaints.forEach { cmp ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("${cmp.category} • ${cmp.status}", style = MaterialTheme.typography.labelLarge)
                                Text(cmp.description, style = MaterialTheme.typography.bodySmall)
                                if (cmp.adminResolutionNote.isNotBlank()) {
                                    Text("Admin Note: ${cmp.adminResolutionNote}", style = MaterialTheme.typography.labelSmall, color = VerifiedGreen)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Data Export, Legal & Account Deletion
        OutlinedButton(
            onClick = onExportData,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("export_data_btn")
        ) {
            Icon(Icons.Default.Download, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(strings.exportData)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onClick = { onShowLegal(true) }) { Text(strings.termsAndConditions) }
            TextButton(onClick = { onShowLegal(false) }) { Text(strings.privacyPolicy) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
                Text(strings.logout)
            }
            OutlinedButton(
                onClick = onRequestDelete,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                border = BorderStroke(1.dp, DangerRed),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("delete_account_btn")
            ) {
                Text(strings.deleteAccount)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditWorkerProfileDialog(
    worker: WorkerProfileEntity,
    onSave: (WorkerProfileEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var fullName by remember { mutableStateOf(worker.fullName) }
    var photoUri by remember { mutableStateOf(worker.photoUri) }
    var state by remember { mutableStateOf(worker.state) }
    var district by remember { mutableStateOf(worker.district) }
    var skillsCsv by remember { mutableStateOf(worker.skillsCsv) }
    var exp by remember { mutableStateOf(worker.experienceYears.toString()) }
    var dailyWage by remember { mutableStateOf(worker.expectedDailyWage.toString()) }
    var monthlySalary by remember { mutableStateOf(worker.expectedMonthlySalary.toString()) }
    var preferredLoc by remember { mutableStateOf(worker.preferredLocation) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) photoUri = uri.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Worker Profile") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                ) {
                    Text(if (photoUri.isBlank()) "Select Photo" else "Update Photo")
                }
                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") })
                StateAndDistrictSelector(
                    selectedState = state,
                    selectedDistrict = district,
                    onStateChanged = { state = it },
                    onDistrictChanged = { district = it }
                )
                OutlinedTextField(value = skillsCsv, onValueChange = { skillsCsv = it }, label = { Text("Skills") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = exp,
                        onValueChange = { exp = it.filter { c -> c.isDigit() } },
                        label = { Text("Exp (Yrs)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dailyWage,
                        onValueChange = { dailyWage = it.filter { c -> c.isDigit() } },
                        label = { Text("Daily ₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = monthlySalary,
                    onValueChange = { monthlySalary = it.filter { c -> c.isDigit() } },
                    label = { Text("Monthly Salary (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(value = preferredLoc, onValueChange = { preferredLoc = it }, label = { Text("Preferred Work Location") })
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    worker.copy(
                        fullName = fullName.trim().ifBlank { worker.fullName },
                        photoUri = photoUri,
                        state = state,
                        district = district,
                        skillsCsv = skillsCsv.trim(),
                        experienceYears = exp.toIntOrNull() ?: worker.experienceYears,
                        expectedDailyWage = dailyWage.toIntOrNull() ?: worker.expectedDailyWage,
                        expectedMonthlySalary = monthlySalary.toIntOrNull() ?: worker.expectedMonthlySalary,
                        preferredLocation = preferredLoc.trim()
                    )
                )
            }) { Text("Save Changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
