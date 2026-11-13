package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.domain.AppLanguage
import com.example.domain.LocalAppStrings
import com.example.domain.WorkerCategory
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun AdminDashboardScreen(
    user: UserEntity,
    currentLanguage: AppLanguage,
    workers: List<WorkerProfileEntity>,
    employers: List<EmployerEntity>,
    jobs: List<JobPostEntity>,
    applications: List<JobApplicationEntity>,
    complaints: List<ComplaintEntity>,
    auditLogs: List<AuditLogEntity>,
    onLanguageSelected: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    onVerifyEmployer: (EmployerEntity, Boolean, String) -> Unit,
    onVerifyWorker: (WorkerProfileEntity, Boolean) -> Unit,
    onToggleWorkerSuspension: (WorkerProfileEntity) -> Unit,
    onUpdateComplaint: (ComplaintEntity, String, String) -> Unit
) {
    val strings = LocalAppStrings.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val navItems = listOf(
        Triple(strings.navAnalytics, Icons.Default.Analytics, "admin_tab_analytics"),
        Triple(strings.navEmployers, Icons.Default.BusinessCenter, "admin_tab_employers"),
        Triple(strings.navWorkers, Icons.Default.Groups, "admin_tab_workers"),
        Triple(strings.navComplaints, Icons.Default.ReportProblem, "admin_tab_complaints"),
        Triple(strings.navAuditLogs, Icons.Default.Policy, "admin_tab_audit")
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 720.dp

        Scaffold(
            topBar = {
                ShramSetuTopBar(
                    title = "${strings.appName} • ${strings.roleAdmin}",
                    subtitle = "National Labour Governance & Analytics",
                    currentLanguage = currentLanguage,
                    unreadNotifications = complaints.count { it.status == "OPEN" } + employers.count { it.verificationStatus == VerificationStatuses.PENDING },
                    onLanguageSelected = onLanguageSelected,
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

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (selectedTab) {
                        0 -> AdminAnalyticsTab(
                            workers = workers,
                            employers = employers,
                            jobs = jobs,
                            applications = applications,
                            complaints = complaints
                        )
                        1 -> AdminEmployersTab(
                            employers = employers,
                            onVerifyEmployer = onVerifyEmployer
                        )
                        2 -> AdminWorkersTab(
                            workers = workers,
                            onVerifyWorker = onVerifyWorker,
                            onToggleSuspension = onToggleWorkerSuspension
                        )
                        3 -> AdminComplaintsTab(
                            complaints = complaints,
                            onUpdateComplaint = onUpdateComplaint
                        )
                        4 -> AdminAuditLogsTab(
                            auditLogs = auditLogs,
                            onLogout = onLogout
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminAnalyticsTab(
    workers: List<WorkerProfileEntity>,
    employers: List<EmployerEntity>,
    jobs: List<JobPostEntity>,
    applications: List<JobApplicationEntity>,
    complaints: List<ComplaintEntity>
) {
    val strings = LocalAppStrings.current

    val totalWorkers = workers.size
    val skilledCount = remember(workers) { workers.count { it.category == WorkerCategory.SKILLED.key } }
    val unskilledCount = remember(workers) { workers.count { it.category == WorkerCategory.UNSKILLED.key } }
    val verifiedWorkersCount = remember(workers) { workers.count { it.identityVerified == VerificationStatuses.VERIFIED } }
    val registeredCompanies = employers.size
    val pendingApplications = remember(applications) { applications.count { it.status == "APPLIED" || it.status == "INVITED" } }
    val activeJobPosts = remember(jobs) { jobs.count { it.status == "ACTIVE" } }
    val openComplaints = remember(complaints) { complaints.count { it.status == "OPEN" || it.status == "IN_REVIEW" } }

    val stateSummaries = remember(workers, employers, jobs) {
        val states = (workers.map { it.state } + employers.map { it.state } + jobs.map { it.state }).distinct()
        states.map { st ->
            val stWorkers = workers.filter { it.state == st }
            StateAnalyticsSummary(
                state = st,
                totalWorkers = stWorkers.size,
                skilledWorkers = stWorkers.count { it.category == WorkerCategory.SKILLED.key },
                unskilledWorkers = stWorkers.count { it.category == WorkerCategory.UNSKILLED.key },
                verifiedWorkers = stWorkers.count { it.identityVerified == VerificationStatuses.VERIFIED },
                registeredCompanies = employers.count { it.state == st },
                activeJobs = jobs.count { it.state == st && it.status == "ACTIVE" }
            )
        }.sortedByDescending { it.totalWorkers }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("National Workforce Command Center", style = MaterialTheme.typography.headlineSmall)
        }

        // KPI Grid (4 rows x 2 columns)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiCard(strings.totalWorkers, totalWorkers.toString(), Icons.Default.Groups, Modifier.weight(1f))
                    KpiCard(strings.verifiedWorkers, verifiedWorkersCount.toString(), Icons.Default.VerifiedUser, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiCard(strings.skilledWorkers, skilledCount.toString(), Icons.Default.Engineering, Modifier.weight(1f))
                    KpiCard(strings.unskilledWorkers, unskilledCount.toString(), Icons.Default.Handyman, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiCard(strings.registeredCompanies, registeredCompanies.toString(), Icons.Default.Business, Modifier.weight(1f))
                    KpiCard(strings.activeJobPosts, activeJobPosts.toString(), Icons.Default.Work, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiCard(strings.pendingApplications, pendingApplications.toString(), Icons.Default.PendingActions, Modifier.weight(1f))
                    KpiCard("Open Complaints", openComplaints.toString(), Icons.Default.ReportProblem, Modifier.weight(1f))
                }
            }
        }

        // State-wise Chart Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(strings.stateWiseAnalytics, style = MaterialTheme.typography.titleMedium)
                    val topStates = stateSummaries.take(6)
                    val maxVal = (topStates.maxOfOrNull { it.totalWorkers } ?: 1).coerceAtLeast(1)

                    topStates.forEach { row ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(row.state, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${row.totalWorkers} workers (${row.skilledWorkers}S / ${row.unskilledWorkers}U) • ${row.activeJobs} jobs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                            ) {
                                val bgColor = SlateSurfaceVariant
                                drawRoundRect(
                                    color = bgColor,
                                    size = size,
                                    cornerRadius = CornerRadius(8f, 8f)
                                )
                                val totalRatio = (row.totalWorkers.toFloat() / maxVal.toFloat()).coerceIn(0.05f, 1f)
                                val totalWidth = size.width * totalRatio
                                val skilledWidth = if (row.totalWorkers > 0) {
                                    totalWidth * (row.skilledWorkers.toFloat() / row.totalWorkers.toFloat())
                                } else 0f

                                drawRoundRect(
                                    color = CobaltAccent,
                                    size = Size(totalWidth, size.height),
                                    cornerRadius = CornerRadius(8f, 8f)
                                )
                                if (skilledWidth > 0f) {
                                    drawRoundRect(
                                        color = NavyPrimary,
                                        topLeft = Offset.Zero,
                                        size = Size(skilledWidth, size.height),
                                        cornerRadius = CornerRadius(8f, 8f)
                                    )
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = NavyPrimary, modifier = Modifier.size(10.dp), shape = RoundedCornerShape(2.dp)) {}
                            Spacer(Modifier.width(6.dp))
                            Text("Skilled", style = MaterialTheme.typography.labelSmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = CobaltAccent, modifier = Modifier.size(10.dp), shape = RoundedCornerShape(2.dp)) {}
                            Spacer(Modifier.width(6.dp))
                            Text("Unskilled", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // State-wise Detailed Table
        items(stateSummaries, key = { it.state }) { row ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(row.state, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Verified Workers: ${row.verifiedWorkers}/${row.totalWorkers}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "${row.registeredCompanies} Cos • ${row.activeJobs} Jobs",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
            Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun AdminEmployersTab(
    employers: List<EmployerEntity>,
    onVerifyEmployer: (EmployerEntity, Boolean, String) -> Unit
) {
    var statusFilter by remember { mutableStateOf("ALL") }
    var rejectTarget by remember { mutableStateOf<EmployerEntity?>(null) }

    val filtered = remember(employers, statusFilter) {
        if (statusFilter == "ALL") employers else employers.filter { it.verificationStatus == statusFilter }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ALL", VerificationStatuses.PENDING, VerificationStatuses.VERIFIED, VerificationStatuses.REJECTED).forEach { st ->
                    FilterChip(
                        selected = statusFilter == st,
                        onClick = { statusFilter = st },
                        label = { Text(st) }
                    )
                }
            }
        }

        items(filtered, key = { it.employerId }) { emp ->
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
                            Text(emp.companyName, style = MaterialTheme.typography.titleMedium)
                            Text("${emp.companyType} • ${emp.district}, ${emp.state}", style = MaterialTheme.typography.bodySmall)
                        }
                        VerificationPill(status = emp.verificationStatus)
                    }
                    DetailRow("GST / CIN", emp.gstOrCinNumber)
                    DetailRow("Reg Certificate", emp.registrationDocNumber)
                    DetailRow("Contact Person", "${emp.contactPerson} (${emp.officialEmail})")

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (emp.verificationStatus != VerificationStatuses.VERIFIED) {
                            Button(
                                onClick = { onVerifyEmployer(emp, true, "") },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("verify_emp_${emp.employerId}")
                            ) {
                                Text("Approve & Verify")
                            }
                        }
                        if (emp.verificationStatus != VerificationStatuses.REJECTED) {
                            OutlinedButton(
                                onClick = { rejectTarget = emp },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            ) {
                                Text("Reject")
                            }
                        }
                    }
                }
            }
        }
    }

    if (rejectTarget != null) {
        var reason by remember { mutableStateOf("GSTIN / Registration certificate mismatch.") }
        AlertDialog(
            onDismissRequest = { rejectTarget = null },
            title = { Text("Reject ${rejectTarget!!.companyName}") },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Rejection Reason") },
                    minLines = 2
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onVerifyEmployer(rejectTarget!!, false, reason)
                        rejectTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) { Text("Confirm Reject") }
            },
            dismissButton = { TextButton(onClick = { rejectTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AdminWorkersTab(
    workers: List<WorkerProfileEntity>,
    onVerifyWorker: (WorkerProfileEntity, Boolean) -> Unit,
    onToggleSuspension: (WorkerProfileEntity) -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(workers, search) {
        if (search.isBlank()) workers
        else workers.filter {
            it.fullName.contains(search, ignoreCase = true) ||
                it.workerId.contains(search, ignoreCase = true) ||
                it.primaryTrade.contains(search, ignoreCase = true) ||
                it.state.contains(search, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search by Worker ID, name, trade, or state...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Privacy Guard: Admins only view tokenized verification references (never full Aadhaar documents).",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        items(filtered, key = { it.workerId }) { w ->
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(w.fullName, style = MaterialTheme.typography.titleMedium)
                            Text("${w.workerId} • ${w.primaryTrade} (${w.category})", style = MaterialTheme.typography.labelSmall)
                        }
                        VerificationPill(status = w.identityVerified)
                    }
                    DetailRow("Location", "${w.district}, ${w.state}")
                    DetailRow("Provider Token", "${w.identityVerificationMethod} (${w.identityTokenMasked})")

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (w.identityVerified != VerificationStatuses.VERIFIED) {
                            Button(
                                onClick = { onVerifyWorker(w, true) },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            ) { Text("Verify ID") }
                        }
                        OutlinedButton(
                            onClick = { onToggleSuspension(w) },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) {
                            Text(if (w.isSuspended) "Reactivate" else "Suspend")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminComplaintsTab(
    complaints: List<ComplaintEntity>,
    onUpdateComplaint: (ComplaintEntity, String, String) -> Unit
) {
    var activeComplaint by remember { mutableStateOf<ComplaintEntity?>(null) }

    if (complaints.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            EmptyStateView(
                title = "No complaints reported",
                subtitle = "Worker and employer grievances will appear here for action."
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(complaints, key = { it.complaintId }) { cmp ->
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
                        Text(cmp.category, style = MaterialTheme.typography.titleMedium)
                        StatusPill(
                            text = cmp.status,
                            containerColor = if (cmp.status == "RESOLVED") VerifiedGreenBg else WarningAmberBg,
                            contentColor = if (cmp.status == "RESOLVED") VerifiedGreen else WarningAmber
                        )
                    }
                    Text("Reporter: ${cmp.reporterName} (${cmp.reporterRole})", style = MaterialTheme.typography.bodySmall)
                    Text("Against: ${cmp.targetName}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Text(cmp.description, style = MaterialTheme.typography.bodyMedium)
                    if (cmp.adminResolutionNote.isNotBlank()) {
                        Text("Resolution: ${cmp.adminResolutionNote}", style = MaterialTheme.typography.labelMedium, color = VerifiedGreen)
                    }
                    Button(
                        onClick = { activeComplaint = cmp },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Update / Resolve Report")
                    }
                }
            }
        }
    }

    if (activeComplaint != null) {
        var nextStatus by remember { mutableStateOf("RESOLVED") }
        var note by remember { mutableStateOf(activeComplaint!!.adminResolutionNote) }
        AlertDialog(
            onDismissRequest = { activeComplaint = null },
            title = { Text("Manage Complaint") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("IN_REVIEW", "RESOLVED", "DISMISSED").forEach { st ->
                            FilterChip(
                                selected = nextStatus == st,
                                onClick = { nextStatus = st },
                                label = { Text(st) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Official Resolution Note") },
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateComplaint(activeComplaint!!, nextStatus, note)
                    activeComplaint = null
                }) { Text("Save Update") }
            },
            dismissButton = { TextButton(onClick = { activeComplaint = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AdminAuditLogsTab(
    auditLogs: List<AuditLogEntity>,
    onLogout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Security & Compliance Audit Logs", style = MaterialTheme.typography.titleLarge)
                OutlinedButton(onClick = onLogout) { Text("Sign Out") }
            }
        }

        items(auditLogs, key = { it.logId }) { log ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(log.action, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(formatTimestamp(log.timestamp), style = MaterialTheme.typography.labelSmall)
                    }
                    Text("${log.actorRole} (${log.actorUserId}) • Target: ${log.targetType}/${log.targetId}", style = MaterialTheme.typography.labelSmall)
                    Text(log.details, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
