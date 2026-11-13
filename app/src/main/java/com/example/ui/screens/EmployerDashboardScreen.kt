package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
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
fun EmployerDashboardScreen(
    user: UserEntity,
    employer: EmployerEntity?,
    currentLanguage: AppLanguage,
    workers: List<WorkerProfileEntity>,
    employerJobs: List<JobPostEntity>,
    applications: List<JobApplicationEntity>,
    messages: List<MessageEntity>,
    notifications: List<NotificationEntity>,
    complaints: List<ComplaintEntity>,
    onLanguageSelected: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    onPostJob: (
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
    ) -> Unit,
    onToggleJobStatus: (JobPostEntity) -> Unit,
    onSendInvitation: (EmployerEntity, WorkerProfileEntity, JobPostEntity, String, () -> Unit) -> Unit,
    onUpdateApplicationStatus: (JobApplicationEntity, String) -> Unit,
    onResubmitDocs: (EmployerEntity, String, String, String) -> Unit,
    onSendMessage: (receiverId: String, receiverName: String, content: String) -> Unit,
    onMarkNotificationsRead: () -> Unit,
    onSubmitComplaint: (target: String, category: String, description: String, onDone: () -> Unit) -> Unit,
    onExportData: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val strings = LocalAppStrings.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showNotifications by remember { mutableStateOf(false) }
    var showPostJobDialog by remember { mutableStateOf(false) }
    var showComplaintDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showLegalDialog by remember { mutableStateOf<Boolean?>(null) }

    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }

    val navItems = listOf(
        Triple(strings.navFindWorkers, Icons.Default.PersonSearch, "emp_tab_workers"),
        Triple(strings.navMyJobs, Icons.Default.WorkOutline, "emp_tab_jobs"),
        Triple(strings.navRequests, Icons.Default.GroupAdd, "emp_tab_requests"),
        Triple(strings.navMessages, Icons.Default.Chat, "emp_tab_msgs"),
        Triple(strings.navCompany, Icons.Default.Business, "emp_tab_company")
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 720.dp

        Scaffold(
            topBar = {
                ShramSetuTopBar(
                    title = employer?.companyName ?: strings.roleEmployer,
                    subtitle = if (employer?.verificationStatus == VerificationStatuses.VERIFIED) "Verified Employer" else "Verification: ${employer?.verificationStatus ?: "Pending"}",
                    currentLanguage = currentLanguage,
                    unreadNotifications = unreadCount,
                    onLanguageSelected = onLanguageSelected,
                    onNotificationsClick = { showNotifications = true },
                    onLogoutClick = onLogout
                )
            },
            floatingActionButton = {
                if (selectedTab == 1 && employer != null) {
                    ExtendedFloatingActionButton(
                        onClick = { showPostJobDialog = true },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text(strings.postJob) },
                        modifier = Modifier.testTag("fab_post_job")
                    )
                }
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

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (employer == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        when (selectedTab) {
                            0 -> FindWorkersTab(
                                employer = employer,
                                workers = workers,
                                employerJobs = employerJobs.filter { it.status == "ACTIVE" },
                                onSendInvitation = onSendInvitation,
                                onStartChat = { w ->
                                    onSendMessage(
                                        w.userId,
                                        w.fullName,
                                        "Hello ${w.fullName}, ${employer.companyName} reviewed your ${w.primaryTrade} profile on ShramSetu."
                                    )
                                    selectedTab = 3
                                }
                            )
                            1 -> EmployerJobsTab(
                                employer = employer,
                                jobs = employerJobs,
                                onToggleStatus = onToggleJobStatus,
                                onOpenPostDialog = { showPostJobDialog = true }
                            )
                            2 -> EmployerRequestsTab(
                                applications = applications,
                                onUpdateStatus = onUpdateApplicationStatus,
                                onMessageWorker = { workerUserId, workerName ->
                                    onSendMessage(workerUserId, workerName, "Hello $workerName, regarding your recruitment status at ${employer.companyName}.")
                                    selectedTab = 3
                                }
                            )
                            3 -> MessagingHubView(
                                currentUser = user,
                                messages = messages,
                                onSendMessage = onSendMessage
                            )
                            4 -> EmployerCompanyTab(
                                employer = employer,
                                complaints = complaints,
                                onResubmitDocs = onResubmitDocs,
                                onRaiseComplaint = { showComplaintDialog = true },
                                onExportData = onExportData,
                                onShowLegal = { showLegalDialog = it },
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

    if (showPostJobDialog && employer != null) {
        PostJobDialog(
            employer = employer,
            onPost = { title, cat, trade, st, dist, loc, daily, monthly, vac, minExp, desc ->
                onPostJob(employer, title, cat, trade, st, dist, loc, daily, monthly, vac, minExp, desc) {
                    showPostJobDialog = false
                }
            },
            onDismiss = { showPostJobDialog = false }
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
        LegalDocumentDialog(isTerms = showLegalDialog == true, onDismiss = { showLegalDialog = null })
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Company Account?") },
            text = { Text("Your employer profile and credentials will be removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) { Text("Delete Account") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun FindWorkersTab(
    employer: EmployerEntity,
    workers: List<WorkerProfileEntity>,
    employerJobs: List<JobPostEntity>,
    onSendInvitation: (EmployerEntity, WorkerProfileEntity, JobPostEntity, String, () -> Unit) -> Unit,
    onStartChat: (WorkerProfileEntity) -> Unit
) {
    val strings = LocalAppStrings.current
    var query by remember { mutableStateOf("") }
    var selectedState by remember { mutableStateOf("All India") }
    var selectedDistrict by remember { mutableStateOf("All Districts") }
    var categoryFilter by remember { mutableStateOf("ALL") }
    var minExp by remember { mutableIntStateOf(0) }
    var availableOnly by remember { mutableStateOf(false) }

    var inviteTargetWorker by remember { mutableStateOf<WorkerProfileEntity?>(null) }

    val filtered = remember(workers, query, selectedState, selectedDistrict, categoryFilter, minExp, availableOnly) {
        workers.filter { w ->
            val qMatch = query.isBlank() ||
                w.primaryTrade.contains(query, ignoreCase = true) ||
                w.skillsCsv.contains(query, ignoreCase = true) ||
                w.fullName.contains(query, ignoreCase = true) ||
                w.district.contains(query, ignoreCase = true)
            val sMatch = selectedState == "All India" || w.state.equals(selectedState, ignoreCase = true)
            val dMatch = selectedDistrict == "All Districts" || w.district.equals(selectedDistrict, ignoreCase = true)
            val cMatch = categoryFilter == "ALL" || w.category == categoryFilter
            val eMatch = w.experienceYears >= minExp
            val aMatch = !availableOnly || w.availabilityStatus == "AVAILABLE"
            qMatch && sMatch && dMatch && cMatch && eMatch && aMatch
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (employer.verificationStatus != VerificationStatuses.VERIFIED) {
            item {
                Surface(
                    color = WarningAmberBg,
                    contentColor = WarningAmber,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Company verification is ${employer.verificationStatus}. Once verified by Super Admin, you can publish live jobs and send invitations.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(strings.searchWorkersPlaceholder) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("emp_search_workers_input")
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
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL" to "All", "SKILLED" to strings.skilledLabel, "UNSKILLED" to strings.unskilledLabel)
                        .forEach { (key, label) ->
                            FilterChip(
                                selected = categoryFilter == key,
                                onClick = { categoryFilter = key },
                                label = { Text(label) }
                            )
                        }
                    FilterChip(
                        selected = availableOnly,
                        onClick = { availableOnly = !availableOnly },
                        label = { Text(strings.availableLabel) }
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Min Experience:", style = MaterialTheme.typography.labelMedium)
                    listOf(0, 2, 5, 8).forEach { yr ->
                        FilterChip(
                            selected = minExp == yr,
                            onClick = { minExp = yr },
                            label = { Text(if (yr == 0) "Any" else "${yr}+ yrs") }
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateView(
                    title = "No workers match these filters",
                    subtitle = "Broaden the state, district, or experience filter."
                )
            }
        } else {
            items(filtered, key = { it.workerId }) { w ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            WorkerAvatar(fullName = w.fullName, photoUri = w.photoUri, sizeDp = 50)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(w.fullName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${w.primaryTrade} • ${w.experienceYears} yrs • ${w.district}, ${w.state}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            VerificationPill(status = w.identityVerified)
                        }

                        Text("Skills: ${w.skillsCsv}", style = MaterialTheme.typography.bodySmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Wage: ₹${w.expectedDailyWage}/day • ${w.availabilityStatus}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Contact: ${SecurityHelper.maskMobileNumber(w.mobileNumber, allowFull = false)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { inviteTargetWorker = w },
                                enabled = employer.verificationStatus == VerificationStatuses.VERIFIED && employerJobs.isNotEmpty(),
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.inviteToWork)
                            }
                            OutlinedButton(
                                onClick = { onStartChat(w) },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.sendMessage)
                            }
                        }
                    }
                }
            }
        }
    }

    if (inviteTargetWorker != null) {
        var selectedJob by remember { mutableStateOf(employerJobs.firstOrNull()) }
        var note by remember { mutableStateOf("We would like to invite you to join our project team.") }
        AlertDialog(
            onDismissRequest = { inviteTargetWorker = null },
            title = { Text("Invite ${inviteTargetWorker!!.fullName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select Active Job Post:", style = MaterialTheme.typography.labelLarge)
                    employerJobs.forEach { job ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedJob?.jobId == job.jobId,
                                onClick = { selectedJob = job }
                            )
                            Text("${job.title} (₹${job.dailyWage}/day)", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Invitation Message") },
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedJob?.let { job ->
                            onSendInvitation(employer, inviteTargetWorker!!, job, note) {
                                inviteTargetWorker = null
                            }
                        }
                    },
                    enabled = selectedJob != null
                ) { Text("Send Invitation") }
            },
            dismissButton = { TextButton(onClick = { inviteTargetWorker = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun EmployerJobsTab(
    employer: EmployerEntity,
    jobs: List<JobPostEntity>,
    onToggleStatus: (JobPostEntity) -> Unit,
    onOpenPostDialog: () -> Unit
) {
    if (jobs.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EmptyStateView(
                title = "No job opportunities posted yet",
                subtitle = "Verified employers can post skilled and unskilled openings across India."
            )
            Button(
                onClick = onOpenPostDialog,
                enabled = employer.verificationStatus == VerificationStatuses.VERIFIED,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Text("Post First Job Opening")
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(jobs, key = { it.jobId }) { job ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(job.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        StatusPill(
                            text = job.status,
                            containerColor = if (job.status == "ACTIVE") VerifiedGreenBg else WarningAmberBg,
                            contentColor = if (job.status == "ACTIVE") VerifiedGreen else WarningAmber
                        )
                    }
                    Text("${job.tradeRequired} (${job.category}) • ${job.district}, ${job.state}", style = MaterialTheme.typography.bodySmall)
                    Text("₹${job.dailyWage}/day • ${job.vacancies} vacancies", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    OutlinedButton(
                        onClick = { onToggleStatus(job) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(if (job.status == "ACTIVE") "Pause Listing" else "Re-activate Listing")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployerRequestsTab(
    applications: List<JobApplicationEntity>,
    onUpdateStatus: (JobApplicationEntity, String) -> Unit,
    onMessageWorker: (workerUserId: String, workerName: String) -> Unit
) {
    if (applications.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            EmptyStateView(
                title = "No recruitment requests yet",
                subtitle = "Applications from workers and your sent invitations appear here."
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(applications, key = { it.applicationId }) { app ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.workerName, style = MaterialTheme.typography.titleMedium)
                            Text("${app.workerTrade} • ${app.workerLocation}", style = MaterialTheme.typography.bodySmall)
                        }
                        StatusPill(
                            text = app.status,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text("Job: ${app.jobTitle} (${app.type})", style = MaterialTheme.typography.bodySmall)
                    if (app.note.isNotBlank()) Text(app.note, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (app.status == "APPLIED" || app.status == "ACCEPTED") {
                            Button(
                                onClick = { onUpdateStatus(app, "HIRED") },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) { Text("Hire Worker") }
                            OutlinedButton(
                                onClick = { onUpdateStatus(app, "DECLINED") },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) { Text("Decline") }
                        }
                        TextButton(
                            onClick = { onMessageWorker(app.workerUserId, app.workerName) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Message")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployerCompanyTab(
    employer: EmployerEntity,
    complaints: List<ComplaintEntity>,
    onResubmitDocs: (EmployerEntity, String, String, String) -> Unit,
    onRaiseComplaint: () -> Unit,
    onExportData: () -> Unit,
    onShowLegal: (Boolean) -> Unit,
    onRequestDelete: () -> Unit,
    onLogout: () -> Unit
) {
    val strings = LocalAppStrings.current
    var gst by remember(employer.gstOrCinNumber) { mutableStateOf(employer.gstOrCinNumber) }
    var docNum by remember(employer.registrationDocNumber) { mutableStateOf(employer.registrationDocNumber) }
    var docUri by remember(employer.verificationDocUri) { mutableStateOf(employer.verificationDocUri) }

    val docPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) docUri = uri.toString()
    }

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
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(employer.companyName, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    VerificationPill(status = employer.verificationStatus)
                }
                DetailRow("Type", employer.companyType)
                DetailRow("GSTIN / CIN", employer.gstOrCinNumber)
                DetailRow("Reg Certificate", employer.registrationDocNumber)
                DetailRow("Location", "${employer.district}, ${employer.state}")
                if (employer.rejectionReason.isNotBlank()) {
                    Text("Rejection Note: ${employer.rejectionReason}", color = DangerRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Company Verification Documents", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value = gst, onValueChange = { gst = it }, label = { Text("GSTIN / CIN") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = docNum, onValueChange = { docNum = it }, label = { Text("Registration Document No.") }, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = { docPicker.launch(arrayOf("application/pdf", "image/*")) }) {
                    Text(if (docUri.isBlank()) "Attach Verification Document" else "Document Attached ✓")
                }
                Button(
                    onClick = { onResubmitDocs(employer, gst, docNum, docUri) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text("Submit / Update Verification Docs")
                }
            }
        }

        OutlinedButton(onClick = onRaiseComplaint, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("${strings.raiseComplaint} (${complaints.size})")
        }
        OutlinedButton(onClick = onExportData, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(strings.exportData)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onClick = { onShowLegal(true) }) { Text(strings.termsAndConditions) }
            TextButton(onClick = { onShowLegal(false) }) { Text(strings.privacyPolicy) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onLogout, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text(strings.logout) }
            OutlinedButton(
                onClick = onRequestDelete,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                border = BorderStroke(1.dp, DangerRed),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) { Text(strings.deleteAccount) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostJobDialog(
    employer: EmployerEntity,
    onPost: (
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
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(WorkerCategory.SKILLED.key) }
    var trade by remember { mutableStateOf(TradesCatalog.skilledTrades.first()) }
    var tradeExpanded by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf(employer.state) }
    var district by remember { mutableStateOf(employer.district) }
    var workLocation by remember { mutableStateOf("") }
    var dailyWage by remember { mutableStateOf("900") }
    var monthlySalary by remember { mutableStateOf("25000") }
    var vacancies by remember { mutableStateOf("10") }
    var minExp by remember { mutableStateOf("2") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Post New Job Opportunity") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (employer.verificationStatus != VerificationStatuses.VERIFIED) {
                    Text(
                        "Only Super Admin verified employers can publish live jobs.",
                        color = DangerRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Job Title *") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = category == WorkerCategory.SKILLED.key,
                        onClick = {
                            category = WorkerCategory.SKILLED.key
                            trade = TradesCatalog.skilledTrades.first()
                        },
                        label = { Text("Skilled") }
                    )
                    FilterChip(
                        selected = category == WorkerCategory.UNSKILLED.key,
                        onClick = {
                            category = WorkerCategory.UNSKILLED.key
                            trade = TradesCatalog.unskilledTrades.first()
                        },
                        label = { Text("Unskilled") }
                    )
                }
                ExposedDropdownMenuBox(expanded = tradeExpanded, onExpandedChange = { tradeExpanded = !tradeExpanded }) {
                    OutlinedTextField(
                        value = trade,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Required Trade") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tradeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = tradeExpanded, onDismissRequest = { tradeExpanded = false }) {
                        TradesCatalog.tradesForCategory(category).forEach { t ->
                            DropdownMenuItem(text = { Text(t) }, onClick = {
                                trade = t
                                tradeExpanded = false
                            })
                        }
                    }
                }
                StateAndDistrictSelector(
                    selectedState = state,
                    selectedDistrict = district,
                    onStateChanged = { state = it },
                    onDistrictChanged = { district = it }
                )
                OutlinedTextField(value = workLocation, onValueChange = { workLocation = it }, label = { Text("Site / Work Location *") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dailyWage,
                        onValueChange = { dailyWage = it.filter { c -> c.isDigit() } },
                        label = { Text("Daily ₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = monthlySalary,
                        onValueChange = { monthlySalary = it.filter { c -> c.isDigit() } },
                        label = { Text("Monthly ₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = vacancies,
                        onValueChange = { vacancies = it.filter { c -> c.isDigit() } },
                        label = { Text("Vacancies") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minExp,
                        onValueChange = { minExp = it.filter { c -> c.isDigit() } },
                        label = { Text("Min Exp (Yrs)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Job Scope & Facilities *") },
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onPost(
                        title, category, trade, state, district, workLocation,
                        dailyWage.toIntOrNull() ?: 700,
                        monthlySalary.toIntOrNull() ?: 20000,
                        vacancies.toIntOrNull() ?: 1,
                        minExp.toIntOrNull() ?: 0,
                        description
                    )
                },
                enabled = title.isNotBlank() && workLocation.isNotBlank() && description.isNotBlank() &&
                    employer.verificationStatus == VerificationStatuses.VERIFIED
            ) { Text("Publish Job") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
