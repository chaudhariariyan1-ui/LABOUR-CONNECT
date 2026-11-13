package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.UserRoles
import com.example.domain.AppLanguage
import com.example.domain.LocalAppStrings
import com.example.ui.components.LegalDocumentDialog
import com.example.ui.theme.*

@Composable
fun AuthAndLandingScreen(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onLogin: (mobile: String, pin: String) -> Unit,
    onQuickDemoLogin: (role: String) -> Unit,
    onNavigateWorkerRegister: () -> Unit,
    onNavigateEmployerRegister: () -> Unit
) {
    val strings = LocalAppStrings.current
    var mobile by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    var showLegalDialog by remember { mutableStateOf<Boolean?>(null) } // true=terms, false=privacy

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Hero Header Band
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DeepNavyDark, NavyPrimary, RoyalBlue)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .align(Alignment.Center)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PureWhite.copy(alpha = 0.16f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Engineering,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = strings.appName,
                                style = MaterialTheme.typography.headlineSmall,
                                color = PureWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ALL-INDIA WORKFORCE PORTAL",
                                style = MaterialTheme.typography.labelSmall,
                                color = SkyBlueSoft
                            )
                        }
                    }

                    Surface(
                        color = VerifiedGreen,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "FREE FOR WORKERS",
                            style = MaterialTheme.typography.labelSmall,
                            color = PureWhite,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 6-Language Quick Selector Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppLanguage.entries.forEach { lang ->
                        val selected = lang == currentLanguage
                        FilterChip(
                            selected = selected,
                            onClick = { onLanguageSelected(lang) },
                            label = {
                                Text(
                                    text = lang.nativeName,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PureWhite,
                                selectedLabelColor = NavyPrimary,
                                containerColor = PureWhite.copy(alpha = 0.14f),
                                labelColor = PureWhite
                            ),
                            border = null,
                            modifier = Modifier.testTag("landing_lang_${lang.code}")
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Image(
                    painter = painterResource(id = R.drawable.img_hero_workforce),
                    contentDescription = "All-India skilled and unskilled workforce banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(164.dp)
                        .clip(RoundedCornerShape(16.dp))
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    text = strings.tagline,
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite
                )
                Text(
                    text = strings.freeRegistrationBadge,
                    style = MaterialTheme.typography.labelMedium,
                    color = SkyBlueSoft
                )
            }
        }

        // Main Content Column
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Registration Call-To-Action Cards
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "New Registration",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(
                        onClick = onNavigateWorkerRegister,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("btn_register_worker"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Badge, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.registerWorkerBtn)
                    }
                    OutlinedButton(
                        onClick = onNavigateEmployerRegister,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("btn_register_employer"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Business, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.registerEmployerBtn)
                    }
                }
            }

            // Mobile + Security PIN Login Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = strings.signInTitle,
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { input ->
                            mobile = input.filter { it.isDigit() }.take(10)
                        },
                        label = { Text(strings.mobileLabel) },
                        leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
                        prefix = { Text("+91 ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_mobile_input")
                    )
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { input ->
                            pin = input.filter { it.isDigit() }.take(6)
                        },
                        label = { Text(strings.pinLabel) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showPin = !showPin }) {
                                Icon(
                                    imageVector = if (showPin) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle PIN visibility"
                                )
                            }
                        },
                        visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_pin_input")
                    )
                    Button(
                        onClick = { onLogin(mobile, pin) },
                        enabled = mobile.length == 10 && pin.length >= 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("login_submit_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(strings.signInBtn)
                    }
                }
            }

            // Instant Role Portal Demo Switcher (Seeded DB Accounts)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = strings.quickDemoLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Text(
                        text = "Explore live seeded dashboards with full database operations (Default PIN: 1234).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { onQuickDemoLogin(UserRoles.WORKER) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("demo_worker_btn"),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(strings.roleWorker, maxLines = 1)
                        }
                        FilledTonalButton(
                            onClick = { onQuickDemoLogin(UserRoles.EMPLOYER) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("demo_employer_btn"),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(strings.roleEmployer, maxLines = 1)
                        }
                        FilledTonalButton(
                            onClick = { onQuickDemoLogin(UserRoles.SUPER_ADMIN) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("demo_admin_btn"),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(strings.roleAdmin, maxLines = 1)
                        }
                    }
                }
            }

            // Legal & Compliance Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(onClick = { showLegalDialog = true }) {
                    Text(strings.termsAndConditions)
                }
                Text("•", modifier = Modifier.align(Alignment.CenterVertically))
                TextButton(onClick = { showLegalDialog = false }) {
                    Text(strings.privacyPolicy)
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
