package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.*
import com.example.domain.AppLanguage
import com.example.domain.LocalAppStrings
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShramSetuTopBar(
    title: String,
    subtitle: String? = null,
    currentLanguage: AppLanguage,
    unreadNotifications: Int = 0,
    onLanguageSelected: (AppLanguage) -> Unit,
    onNotificationsClick: (() -> Unit)? = null,
    onLogoutClick: (() -> Unit)? = null
) {
    var showLangMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        actions = {
            Box {
                TextButton(
                    onClick = { showLangMenu = true },
                    modifier = Modifier.testTag("language_selector_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Select language",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = currentLanguage.code.uppercase(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                DropdownMenu(
                    expanded = showLangMenu,
                    onDismissRequest = { showLangMenu = false }
                ) {
                    AppLanguage.entries.forEach { lang ->
                        DropdownMenuItem(
                            text = {
                                Text("${lang.nativeName} (${lang.englishName})")
                            },
                            leadingIcon = {
                                if (lang == currentLanguage) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            onClick = {
                                onLanguageSelected(lang)
                                showLangMenu = false
                            },
                            modifier = Modifier.testTag("lang_option_${lang.code}")
                        )
                    }
                }
            }

            if (onNotificationsClick != null) {
                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier.testTag("notifications_btn")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadNotifications > 0) {
                                Badge { Text(unreadNotifications.coerceAtMost(99).toString()) }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications"
                        )
                    }
                }
            }

            if (onLogoutClick != null) {
                IconButton(
                    onClick = onLogoutClick,
                    modifier = Modifier.testTag("logout_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Sign Out"
                    )
                }
            }
        }
    )
}

@Composable
fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(50),
        modifier = Modifier.heightIn(min = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun VerificationPill(status: String) {
    val strings = LocalAppStrings.current
    when (status) {
        VerificationStatuses.VERIFIED -> StatusPill(
            text = strings.verifiedLabel,
            containerColor = VerifiedGreenBg,
            contentColor = VerifiedGreen,
            icon = Icons.Default.Verified
        )
        VerificationStatuses.REJECTED -> StatusPill(
            text = "Rejected",
            containerColor = DangerRedBg,
            contentColor = DangerRed,
            icon = Icons.Default.ErrorOutline
        )
        else -> StatusPill(
            text = strings.pendingLabel,
            containerColor = WarningAmberBg,
            contentColor = WarningAmber,
            icon = Icons.Default.HourglassTop
        )
    }
}

@Composable
fun WorkerAvatar(
    fullName: String,
    photoUri: String,
    sizeDp: Int = 56
) {
    val initials = remember(fullName) {
        fullName.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { "W" }
    }
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (photoUri.isNotBlank()) {
            AsyncImage(
                model = Uri.parse(photoUri),
                contentDescription = "$fullName profile photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DigitalWorkerIdCard(
    worker: WorkerProfileEntity,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("digital_worker_id_card"),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // Header Band with generated blueprint texture
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(DeepNavyDark, NavyPrimary, RoyalBlue)
                        )
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_id_card_pattern),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(0.22f)
                )
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SHRAMSETU • NATIONAL WORKER ID",
                                style = MaterialTheme.typography.labelSmall,
                                color = SkyBlueSoft
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = worker.workerId,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = PureWhite
                            )
                        }
                        Surface(
                            color = PureWhite.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = worker.category,
                                style = MaterialTheme.typography.labelMedium,
                                color = PureWhite,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        WorkerAvatar(
                            fullName = worker.fullName,
                            photoUri = worker.photoUri,
                            sizeDp = 64
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = worker.fullName,
                                style = MaterialTheme.typography.titleLarge,
                                color = PureWhite
                            )
                            Text(
                                text = "${worker.primaryTrade} • ${worker.experienceYears} yrs exp",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SkyBlueSoft
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${worker.district}, ${worker.state}",
                                style = MaterialTheme.typography.bodySmall,
                                color = PureWhite.copy(alpha = 0.85f)
                            )
                        }
                        // Deterministic QR-style verification matrix rendered via Canvas
                        QrMatrixBadge(
                            seed = worker.workerId,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PureWhite)
                                .padding(4.dp)
                        )
                    }
                }
            }

            // Bottom Trust & Metadata Bar
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VerificationPill(status = worker.identityVerified)
                        if (worker.mobileOtpVerified) {
                            StatusPill(
                                text = "OTP Verified",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary,
                                icon = Icons.Default.PhonelinkLock
                            )
                        }
                    }
                    Text(
                        text = "₹${worker.expectedDailyWage}/day",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Skills & Trade",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = worker.skillsCsv,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Privacy Token",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = worker.identityTokenMasked,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QrMatrixBadge(seed: String, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val grid = 7
        val cellW = size.width / grid
        val cellH = size.height / grid
        val hash = seed.hashCode()
        for (r in 0 until grid) {
            for (c in 0 until grid) {
                val isCorner = (r < 2 && c < 2) || (r < 2 && c >= grid - 2) || (r >= grid - 2 && c < 2)
                val bit = ((hash ushr ((r * grid + c) % 31)) and 1) == 1
                if (isCorner || bit) {
                    drawRect(
                        color = NavyPrimary,
                        topLeft = Offset(c * cellW, r * cellH),
                        size = Size(cellW * 0.88f, cellH * 0.88f)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_jobs),
                contentDescription = title,
                modifier = Modifier
                    .height(140.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StateAndDistrictSelector(
    selectedState: String,
    selectedDistrict: String,
    includeAllIndiaOption: Boolean = false,
    onStateChanged: (String) -> Unit,
    onDistrictChanged: (String) -> Unit
) {
    var stateExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }

    val stateOptions = remember(includeAllIndiaOption) {
        if (includeAllIndiaOption) listOf("All India") + com.example.domain.IndiaLocations.stateNames
        else com.example.domain.IndiaLocations.stateNames
    }
    val districtOptions = remember(selectedState) {
        if (selectedState == "All India" || selectedState.isBlank()) listOf("All Districts")
        else if (includeAllIndiaOption) listOf("All Districts") + com.example.domain.IndiaLocations.districtsForState(selectedState)
        else com.example.domain.IndiaLocations.districtsForState(selectedState)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = stateExpanded,
            onExpandedChange = { stateExpanded = !stateExpanded },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = selectedState,
                onValueChange = {},
                readOnly = true,
                label = { Text("State / UT") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .testTag("state_dropdown"),
                singleLine = true
            )
            ExposedDropdownMenu(
                expanded = stateExpanded,
                onDismissRequest = { stateExpanded = false }
            ) {
                stateOptions.forEach { st ->
                    DropdownMenuItem(
                        text = { Text(st) },
                        onClick = {
                            onStateChanged(st)
                            val nextDistricts = if (st == "All India") listOf("All Districts")
                            else if (includeAllIndiaOption) listOf("All Districts")
                            else com.example.domain.IndiaLocations.districtsForState(st)
                            onDistrictChanged(nextDistricts.first())
                            stateExpanded = false
                        }
                    )
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = districtExpanded,
            onExpandedChange = { districtExpanded = !districtExpanded },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = selectedDistrict,
                onValueChange = {},
                readOnly = true,
                label = { Text("District") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .testTag("district_dropdown"),
                singleLine = true
            )
            ExposedDropdownMenu(
                expanded = districtExpanded,
                onDismissRequest = { districtExpanded = false }
            ) {
                districtOptions.forEach { dist ->
                    DropdownMenuItem(
                        text = { Text(dist) },
                        onClick = {
                            onDistrictChanged(dist)
                            districtExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationsDialog(
    notifications: List<NotificationEntity>,
    onMarkAllRead: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.notificationsTitle)
                if (notifications.any { !it.isRead }) {
                    TextButton(onClick = onMarkAllRead) {
                        Text("Mark all read")
                    }
                }
            }
        },
        text = {
            if (notifications.isEmpty()) {
                Text("No notifications yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notifications, key = { it.notificationId }) { notif ->
                        Surface(
                            color = if (notif.isRead) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(notif.title, style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(2.dp))
                                Text(notif.body, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = formatTimestamp(notif.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun MessagingHubView(
    currentUser: UserEntity,
    messages: List<MessageEntity>,
    onSendMessage: (receiverId: String, receiverName: String, content: String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Group messages into distinct conversation partners
    data class PartnerThread(
        val partnerId: String,
        val partnerName: String,
        val threadMessages: List<MessageEntity>
    )

    val threads = remember(messages, currentUser.id) {
        messages.groupBy { msg ->
            if (msg.senderUserId == currentUser.id) msg.receiverUserId to msg.receiverName
            else msg.senderUserId to msg.senderName
        }.map { (partner, list) ->
            PartnerThread(partner.first, partner.second, list.sortedBy { it.createdAt })
        }.sortedByDescending { it.threadMessages.lastOrNull()?.createdAt ?: 0L }
    }

    var selectedPartnerId by remember { mutableStateOf<String?>(null) }
    val activeThread = threads.firstOrNull { it.partnerId == selectedPartnerId }

    if (activeThread != null) {
        var draft by remember { mutableStateOf("") }
        Column(modifier = modifier.fillMaxSize()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedPartnerId = null }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to conversations")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(activeThread.partnerName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Privacy-Protected Messaging • Sensitive IDs Auto-Redacted",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(activeThread.threadMessages, key = { it.messageId }) { msg ->
                    val isMine = msg.senderUserId == currentUser.id
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.widthIn(max = 290.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(msg.content, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = formatTimestamp(msg.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Write a secure message...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_message_input"),
                    maxLines = 3
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = {
                        if (draft.isNotBlank()) {
                            onSendMessage(activeThread.partnerId, activeThread.partnerName, draft)
                            draft = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("send_chat_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message")
                }
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Privacy-Controlled Messaging: Phone numbers remain hidden unless consented. 12-digit ID numbers are automatically redacted.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            if (threads.isEmpty()) {
                EmptyStateView(
                    title = "No messages yet",
                    subtitle = "Conversations with verified employers or workers appear here."
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(threads, key = { it.partnerId }) { thread ->
                        val last = thread.threadMessages.last()
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPartnerId = thread.partnerId },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                WorkerAvatar(fullName = thread.partnerName, photoUri = "", sizeDp = 46)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(thread.partnerName, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        last.content,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = formatTimestamp(last.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComplaintFormDialog(
    defaultTarget: String = "",
    onSubmit: (target: String, category: String, description: String) -> Unit,
    onDismiss: () -> Unit
) {
    val categories = listOf(
        "WAGE_DISPUTE" to "Wage / Payment Dispute",
        "SAFETY_ISSUE" to "Site Safety Violation",
        "FAKE_JOB" to "Fake Job / Fraud Alert",
        "HARASSMENT" to "Harassment / Misconduct",
        "VERIFICATION_HELP" to "Verification Support",
        "OTHER" to "Other Grievance"
    )
    var target by remember { mutableStateOf(defaultTarget) }
    var selectedCategory by remember { mutableStateOf(categories.first().first) }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("File Complaint / Grievance") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Company / Contractor / Subject") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Category", style = MaterialTheme.typography.labelMedium)
                categories.forEach { (key, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCategory = key }
                    ) {
                        RadioButton(
                            selected = selectedCategory == key,
                            onClick = { selectedCategory = key }
                        )
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Describe issue in detail") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(target, selectedCategory, description) },
                enabled = description.trim().length >= 10
            ) {
                Text("Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun LegalDocumentDialog(
    isTerms: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isTerms) "Terms & Conditions" else "Privacy & DPDP Policy")
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = if (isTerms) {
                        """
                        1. Free Registration Guarantee: Worker registration on ShramSetu is 100% free across all Indian States and Union Territories. No employer or agent may charge a registration fee.
                        
                        2. Role-Based Conduct: Employers must be verified via valid GST/CIN/Labour registration before publishing live jobs or sending invitations.
                        
                        3. Fair Wages & Safety: Job posts must state transparent daily wages or monthly salaries compliant with applicable Indian Minimum Wages & occupational safety rules.
                        
                        4. Grievance Redressal: Any wage dispute, fake posting, or misconduct can be reported directly to the Super Admin Labour Welfare Desk.
                        """.trimIndent()
                    } else {
                        """
                        1. Data Minimization & Aadhaar Safety: ShramSetu never stores, displays, or transmits raw 12-digit Aadhaar numbers. Identity verification uses consent-based masked Virtual IDs (VID), e-Shram UAN references, or DigiLocker consent tokens.
                        
                        2. Purpose Limitation: Personal data is processed solely for workforce registration, Digital Worker ID generation, and consented job matching.
                        
                        3. Contact Masking: Worker mobile numbers and exact addresses remain hidden from employers unless explicitly enabled in Privacy Settings.
                        
                        4. User Rights (DPDP Compliance): You may inspect, export (JSON archive), withdraw consent, or permanently delete your account at any time.
                        """.trimIndent()
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Understood") }
        }
    )
}

@Composable
fun DataExportDialog(
    jsonContent: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var savedMessage by remember { mutableStateOf<String?>(null) }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(jsonContent.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                savedMessage = "Saved JSON archive to device!"
            }.onFailure {
                savedMessage = "Could not save file: ${it.localizedMessage}"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Personal Data Export (JSON)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (savedMessage != null) {
                    Text(savedMessage!!, color = VerifiedGreen, style = MaterialTheme.typography.labelLarge)
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                ) {
                    Text(
                        text = jsonContent,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { saveLauncher.launch("shramsetu_my_data.json") }) {
                Text("Save File")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(jsonContent))
                    savedMessage = "Copied JSON to clipboard."
                }) {
                    Text("Copy")
                }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}

fun formatTimestamp(epochMs: Long): String {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(Date(epochMs))
}
