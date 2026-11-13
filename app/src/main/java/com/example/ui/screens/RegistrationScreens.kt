package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.EmployerRegistrationInput
import com.example.data.WorkerRegistrationInput
import com.example.domain.AppLanguage
import com.example.domain.IndiaLocations
import com.example.domain.LocalAppStrings
import com.example.domain.TradesCatalog
import com.example.domain.WorkerCategory
import com.example.ui.components.LegalDocumentDialog
import com.example.ui.components.StateAndDistrictSelector
import com.example.ui.components.StatusPill
import com.example.ui.components.WorkerAvatar
import com.example.ui.theme.VerifiedGreen
import com.example.ui.theme.VerifiedGreenBg

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WorkerRegistrationWizard(
    currentLanguage: AppLanguage,
    simulatedOtpBanner: String?,
    onRequestOtp: (mobile: String, onSent: () -> Unit) -> Unit,
    onVerifyOtp: (mobile: String, code: String, onVerified: () -> Unit) -> Unit,
    onSubmit: (WorkerRegistrationInput) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    var step by remember { mutableIntStateOf(1) }

    BackHandler {
        if (step > 1) step -= 1 else onBack()
    }

    // Step 1: Personal & Photo
    var fullName by remember { mutableStateOf("") }
    var fatherName by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("15/05/1995") }
    var gender by remember { mutableStateOf("Male") }
    var photoUri by remember { mutableStateOf("") }

    // Step 2: Mobile OTP & All-India Location
    var mobile by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var otpVerified by remember { mutableStateOf(false) }
    var whatsappSameAsMobile by remember { mutableStateOf(true) }
    var whatsappNumber by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("Maharashtra") }
    var district by remember { mutableStateOf("Pune") }
    var currentAddress by remember { mutableStateOf("") }
    var sameAsCurrentAddress by remember { mutableStateOf(true) }
    var permanentAddress by remember { mutableStateOf("") }

    // Step 3: Category, Trade, Skills, Wages & Availability
    var category by remember { mutableStateOf(WorkerCategory.SKILLED.key) }
    var primaryTrade by remember { mutableStateOf(TradesCatalog.skilledTrades.first()) }
    var extraSkills by remember { mutableStateOf("") }
    var experienceYears by remember { mutableStateOf("3") }
    var expectedDailyWage by remember { mutableStateOf("850") }
    var expectedMonthlySalary by remember { mutableStateOf("24000") }
    var preferredLocation by remember { mutableStateOf("Pune, Mumbai") }
    var availabilityStatus by remember { mutableStateOf("AVAILABLE") }

    // Step 4: Consent-based Identity Verification
    var identityMethod by remember { mutableStateOf("DIGILOCKER_CONSENT") }
    var identityReference by remember { mutableStateOf("") }
    var consentIdentityVerify by remember { mutableStateOf(false) }
    var consentTerms by remember { mutableStateOf(false) }
    var showLegalDialog by remember { mutableStateOf<Boolean?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUri = uri.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Free Worker Registration")
                        Text(
                            "Step $step of 4 • ₹0 Registration Fee",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (step > 1) step -= 1 else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LinearProgressIndicator(
                progress = { step / 4f },
                modifier = Modifier.fillMaxWidth()
            )

            if (simulatedOtpBanner != null) {
                Surface(
                    color = VerifiedGreenBg,
                    contentColor = VerifiedGreen,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Sms, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(simulatedOtpBanner, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            when (step) {
                1 -> {
                    Text("1. Personal Details & Photo", style = MaterialTheme.typography.titleLarge)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        WorkerAvatar(
                            fullName = fullName.ifBlank { "Worker" },
                            photoUri = photoUri,
                            sizeDp = 72
                        )
                        Column {
                            OutlinedButton(
                                onClick = {
                                    photoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.testTag("pick_worker_photo_btn")
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (photoUri.isBlank()) "Upload Profile Photo" else "Change Photo")
                            }
                            Text(
                                "Uses Android Photo Picker (no broad storage permission).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("reg_full_name")
                    )
                    OutlinedTextField(
                        value = fatherName,
                        onValueChange = { fatherName = it },
                        label = { Text("Father's / Guardian's Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("reg_father_name")
                    )
                    OutlinedTextField(
                        value = dob,
                        onValueChange = { dob = it },
                        label = { Text("Date of Birth (DD/MM/YYYY) *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("reg_dob")
                    )
                    Text("Gender", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Male", "Female", "Other").forEach { g ->
                            FilterChip(
                                selected = gender == g,
                                onClick = { gender = g },
                                label = { Text(g) }
                            )
                        }
                    }
                    Button(
                        onClick = { step = 2 },
                        enabled = fullName.trim().length >= 2 && fatherName.trim().length >= 2 && dob.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("reg_step1_next")
                    ) {
                        Text("Continue to Contact & Location")
                    }
                }

                2 -> {
                    Text("2. Mandatory Mobile OTP & Address", style = MaterialTheme.typography.titleLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = mobile,
                            onValueChange = {
                                mobile = it.filter { ch -> ch.isDigit() }.take(10)
                                otpVerified = false
                            },
                            label = { Text("Mobile Number *") },
                            prefix = { Text("+91 ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            enabled = !otpVerified,
                            modifier = Modifier.weight(1f).testTag("reg_mobile")
                        )
                        Button(
                            onClick = { onRequestOtp(mobile) { otpSent = true } },
                            enabled = mobile.length == 10 && !otpVerified,
                            modifier = Modifier.heightIn(min = 52.dp).testTag("reg_send_otp_btn")
                        ) {
                            Text(if (otpVerified) "Verified" else strings.sendOtp)
                        }
                    }

                    if (otpSent && !otpVerified) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = otpCode,
                                onValueChange = { otpCode = it.filter { ch -> ch.isDigit() }.take(6) },
                                label = { Text("Enter 6-Digit OTP") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("reg_otp_input")
                            )
                            Button(
                                onClick = { onVerifyOtp(mobile, otpCode) { otpVerified = true } },
                                enabled = otpCode.length == 6,
                                modifier = Modifier.heightIn(min = 52.dp).testTag("reg_verify_otp_btn")
                            ) {
                                Text(strings.verifyOtp)
                            }
                        }
                    }

                    if (otpVerified) {
                        StatusPill(
                            text = "Mobile OTP Verified",
                            containerColor = VerifiedGreenBg,
                            contentColor = VerifiedGreen,
                            icon = Icons.Default.CheckCircle
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = whatsappSameAsMobile,
                            onCheckedChange = { whatsappSameAsMobile = it }
                        )
                        Text("WhatsApp number same as Mobile")
                    }
                    if (!whatsappSameAsMobile) {
                        OutlinedTextField(
                            value = whatsappNumber,
                            onValueChange = { whatsappNumber = it.filter { ch -> ch.isDigit() }.take(10) },
                            label = { Text("WhatsApp Number") },
                            prefix = { Text("+91 ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(6) },
                        label = { Text("Create 4-to-6 Digit Security PIN *") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("reg_pin")
                    )

                    StateAndDistrictSelector(
                        selectedState = state,
                        selectedDistrict = district,
                        onStateChanged = { state = it },
                        onDistrictChanged = { district = it }
                    )

                    OutlinedTextField(
                        value = currentAddress,
                        onValueChange = { currentAddress = it },
                        label = { Text("Current Address *") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("reg_current_address")
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = sameAsCurrentAddress,
                            onCheckedChange = { sameAsCurrentAddress = it }
                        )
                        Text("Permanent address same as Current")
                    }
                    if (!sameAsCurrentAddress) {
                        OutlinedTextField(
                            value = permanentAddress,
                            onValueChange = { permanentAddress = it },
                            label = { Text("Permanent Address *") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Button(
                        onClick = { step = 3 },
                        enabled = otpVerified && pin.length >= 4 && currentAddress.trim().length >= 4,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("reg_step2_next")
                    ) {
                        Text("Continue to Trade & Wages")
                    }
                }

                3 -> {
                    Text("3. Category, Skills & Expected Wage", style = MaterialTheme.typography.titleLarge)
                    Text("Worker Category *", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = category == WorkerCategory.SKILLED.key,
                            onClick = {
                                category = WorkerCategory.SKILLED.key
                                primaryTrade = TradesCatalog.skilledTrades.first()
                            },
                            label = { Text(strings.skilledLabel) },
                            modifier = Modifier.testTag("cat_skilled_chip")
                        )
                        FilterChip(
                            selected = category == WorkerCategory.UNSKILLED.key,
                            onClick = {
                                category = WorkerCategory.UNSKILLED.key
                                primaryTrade = TradesCatalog.unskilledTrades.first()
                            },
                            label = { Text(strings.unskilledLabel) },
                            modifier = Modifier.testTag("cat_unskilled_chip")
                        )
                    }

                    Text("Select Primary Trade *", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TradesCatalog.tradesForCategory(category).forEach { trade ->
                            FilterChip(
                                selected = primaryTrade == trade,
                                onClick = { primaryTrade = trade },
                                label = { Text(trade) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = extraSkills,
                        onValueChange = { extraSkills = it },
                        label = { Text("Additional Skills (comma separated)") },
                        placeholder = { Text("e.g., Solar Wiring, Panel Testing") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = experienceYears,
                            onValueChange = { experienceYears = it.filter { ch -> ch.isDigit() }.take(2) },
                            label = { Text("Experience (Years)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = expectedDailyWage,
                            onValueChange = { expectedDailyWage = it.filter { ch -> ch.isDigit() }.take(5) },
                            label = { Text("Daily Wage (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = expectedMonthlySalary,
                        onValueChange = { expectedMonthlySalary = it.filter { ch -> ch.isDigit() }.take(6) },
                        label = { Text("Expected Monthly Salary (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = preferredLocation,
                        onValueChange = { preferredLocation = it },
                        label = { Text("Preferred Work Locations") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Availability Status", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "AVAILABLE" to "Available Now",
                            "WORKING_NOTICE" to "Notice Period",
                            "BUSY" to "Currently Busy"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = availabilityStatus == key,
                                onClick = { availabilityStatus = key },
                                label = { Text(label) }
                            )
                        }
                    }

                    Button(
                        onClick = { step = 4 },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("reg_step3_next")
                    ) {
                        Text("Continue to Consent & Verification")
                    }
                }

                4 -> {
                    Text("4. Privacy-Preserving Identity Verification", style = MaterialTheme.typography.titleLarge)
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("DPDP Act & UIDAI Privacy Compliant", style = MaterialTheme.typography.titleSmall)
                            }
                            Text(
                                "ShramSetu NEVER stores or exposes full Aadhaar numbers. Only a masked one-way token from your authorized verification provider (DigiLocker / e-Shram UAN / Virtual ID) is recorded.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    listOf(
                        "DIGILOCKER_CONSENT" to "DigiLocker Authorized Verification Token",
                        "ESHRAM_UAN" to "e-Shram Universal Account Number (UAN)",
                        "VID_TOKEN" to "16-Digit Virtual ID (VID) Reference"
                    ).forEach { (methodKey, methodLabel) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { identityMethod = methodKey }
                        ) {
                            RadioButton(
                                selected = identityMethod == methodKey,
                                onClick = { identityMethod = methodKey }
                            )
                            Text(methodLabel, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    OutlinedTextField(
                        value = identityReference,
                        onValueChange = { identityReference = it.take(18) },
                        label = { Text("Provider Reference / VID / UAN Last Digits *") },
                        placeholder = { Text("Will be tokenized & masked (e.g. DL-XXXX-4921)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("reg_identity_ref")
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = consentIdentityVerify,
                            onCheckedChange = { consentIdentityVerify = it },
                            modifier = Modifier.testTag("reg_consent_identity")
                        )
                        Text(
                            "I give explicit consent for privacy-preserving identity verification without storing full Aadhaar.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = consentTerms,
                            onCheckedChange = { consentTerms = it },
                            modifier = Modifier.testTag("reg_consent_terms")
                        )
                        Column {
                            Text(
                                "I accept ShramSetu Terms & Conditions and Privacy Policy.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row {
                                TextButton(onClick = { showLegalDialog = true }) { Text("Read Terms") }
                                TextButton(onClick = { showLegalDialog = false }) { Text("Read Privacy Policy") }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            onSubmit(
                                WorkerRegistrationInput(
                                    fullName = fullName,
                                    fatherName = fatherName,
                                    dateOfBirth = dob,
                                    gender = gender,
                                    photoUri = photoUri,
                                    mobileNumber = mobile,
                                    whatsappNumber = if (whatsappSameAsMobile) mobile else whatsappNumber,
                                    pin = pin,
                                    state = state,
                                    district = district,
                                    currentAddress = currentAddress,
                                    permanentAddress = if (sameAsCurrentAddress) currentAddress else permanentAddress,
                                    category = category,
                                    primaryTrade = primaryTrade,
                                    skillsCsv = listOf(primaryTrade, extraSkills)
                                        .filter { it.isNotBlank() }
                                        .joinToString(", "),
                                    experienceYears = experienceYears.toIntOrNull() ?: 0,
                                    expectedDailyWage = expectedDailyWage.toIntOrNull() ?: 600,
                                    expectedMonthlySalary = expectedMonthlySalary.toIntOrNull() ?: 18000,
                                    preferredLocation = preferredLocation,
                                    availabilityStatus = availabilityStatus,
                                    identityMethod = identityMethod,
                                    identityRawReference = identityReference,
                                    consentIdentityVerify = consentIdentityVerify,
                                    consentTermsAndPrivacy = consentTerms,
                                    languageCode = currentLanguage.code
                                )
                            )
                        },
                        enabled = identityReference.trim().length >= 4 && consentIdentityVerify && consentTerms,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp)
                            .testTag("reg_submit_worker_btn")
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Generate Free Digital Worker ID")
                    }
                }
            }
        }
    }

    if (showLegalDialog != null) {
        LegalDocumentDialog(
            isTerms = showLegalDialog == true,
            onDismiss = { showLegalDialog = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerRegistrationScreen(
    currentLanguage: AppLanguage,
    simulatedOtpBanner: String?,
    onRequestOtp: (mobile: String, onSent: () -> Unit) -> Unit,
    onVerifyOtp: (mobile: String, code: String, onVerified: () -> Unit) -> Unit,
    onSubmit: (EmployerRegistrationInput) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var companyName by remember { mutableStateOf("") }
    var companyType by remember { mutableStateOf(TradesCatalog.companyTypes.first()) }
    var typeExpanded by remember { mutableStateOf(false) }
    var gstOrCin by remember { mutableStateOf("") }
    var regDocNum by remember { mutableStateOf("") }
    var verificationDocUri by remember { mutableStateOf("") }
    var contactPerson by remember { mutableStateOf("") }
    var officialEmail by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var otpVerified by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("Maharashtra") }
    var district by remember { mutableStateOf("Pune") }
    var officeAddress by remember { mutableStateOf("") }
    var consentTerms by remember { mutableStateOf(false) }

    val docPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) verificationDocUri = uri.toString()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Employer / Company Registration") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (simulatedOtpBanner != null) {
                Surface(
                    color = VerifiedGreenBg,
                    contentColor = VerifiedGreen,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        simulatedOtpBanner,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it },
                label = { Text("Company / Contractor Legal Name *") },
                modifier = Modifier.fillMaxWidth().testTag("emp_reg_company")
            )

            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = !typeExpanded }
            ) {
                OutlinedTextField(
                    value = companyType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Industry / Company Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    TradesCatalog.companyTypes.forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t) },
                            onClick = {
                                companyType = t
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = gstOrCin,
                onValueChange = { gstOrCin = it.uppercase() },
                label = { Text("GSTIN / MCA CIN / Labour License No. *") },
                modifier = Modifier.fillMaxWidth().testTag("emp_reg_gst")
            )
            OutlinedTextField(
                value = regDocNum,
                onValueChange = { regDocNum = it.uppercase() },
                label = { Text("Company Registration Certificate ID *") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = { docPicker.launch(arrayOf("application/pdf", "image/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (verificationDocUri.isBlank()) "Attach Verification Document (PDF/Image)" else "Document Attached ✓")
            }

            OutlinedTextField(
                value = contactPerson,
                onValueChange = { contactPerson = it },
                label = { Text("Authorized HR / Contact Person *") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = officialEmail,
                onValueChange = { officialEmail = it },
                label = { Text("Official Company Email *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = mobile,
                    onValueChange = {
                        mobile = it.filter { ch -> ch.isDigit() }.take(10)
                        otpVerified = false
                    },
                    label = { Text("Mobile Number *") },
                    prefix = { Text("+91 ") },
                    enabled = !otpVerified,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { onRequestOtp(mobile) { otpSent = true } },
                    enabled = mobile.length == 10 && !otpVerified,
                    modifier = Modifier.heightIn(min = 52.dp)
                ) {
                    Text(if (otpVerified) "Verified" else "Send OTP")
                }
            }

            if (otpSent && !otpVerified) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { otpCode = it.filter { ch -> ch.isDigit() }.take(6) },
                        label = { Text("6-Digit OTP") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { onVerifyOtp(mobile, otpCode) { otpVerified = true } },
                        enabled = otpCode.length == 6,
                        modifier = Modifier.heightIn(min = 52.dp)
                    ) {
                        Text("Verify")
                    }
                }
            }

            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(6) },
                label = { Text("Create 4-to-6 Digit Security PIN *") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )

            StateAndDistrictSelector(
                selectedState = state,
                selectedDistrict = district,
                onStateChanged = { state = it },
                onDistrictChanged = { district = it }
            )

            OutlinedTextField(
                value = officeAddress,
                onValueChange = { officeAddress = it },
                label = { Text("Head / Site Office Address *") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = consentTerms, onCheckedChange = { consentTerms = it })
                Text(
                    "I confirm company details are authentic and agree to ShramSetu Fair Employment & Zero-Fee Worker Policy.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    onSubmit(
                        EmployerRegistrationInput(
                            companyName = companyName,
                            companyType = companyType,
                            gstOrCinNumber = gstOrCin,
                            registrationDocNumber = regDocNum,
                            verificationDocUri = verificationDocUri,
                            contactPerson = contactPerson,
                            officialEmail = officialEmail,
                            mobileNumber = mobile,
                            pin = pin,
                            state = state,
                            district = district,
                            officeAddress = officeAddress,
                            consentTermsAndPrivacy = consentTerms,
                            languageCode = currentLanguage.code
                        )
                    )
                },
                enabled = companyName.isNotBlank() && gstOrCin.length >= 6 && contactPerson.isNotBlank() &&
                    otpVerified && pin.length >= 4 && officeAddress.isNotBlank() && consentTerms,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("emp_reg_submit_btn")
            ) {
                Text("Submit Company for Verification")
            }
        }
    }
}
