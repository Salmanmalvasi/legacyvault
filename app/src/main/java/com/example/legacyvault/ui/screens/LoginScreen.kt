package com.example.legacyvault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.legacyvault.ui.VaultViewModel
import com.example.legacyvault.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: VaultViewModel,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("owner@vault.local") }
    var password by remember { mutableStateOf("pass123") }
    val authError by viewModel.authError.collectAsState()
    val isAuthenticating by viewModel.isAuthenticating.collectAsState()

    Scaffold(
        containerColor = WarmCream
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Brand Icon & Header
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SlateNavy,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Legacy Vault",
                        tint = GoldAccent,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Legacy Vault",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = DeepCharcoal
            )

            Text(
                text = "Dignified Inheritance & Cryptographic Estate Discovery",
                fontSize = 14.sp,
                color = DeepCharcoal.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Role Switcher for Live Demo Evaluation
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SELECT DEMO ROLE FOR EVALUATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateNavy,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickRoleChip(
                            label = "Owner",
                            subtitle = "S. Sundaram",
                            icon = Icons.Default.Person,
                            selected = email == "owner@vault.local",
                            modifier = Modifier.weight(1f)
                        ) {
                            email = "owner@vault.local"
                            password = "pass123"
                        }

                        QuickRoleChip(
                            label = "Lawyer",
                            subtitle = "Attestor 1",
                            icon = Icons.Default.Gavel,
                            selected = email == "attestor1@vault.local",
                            modifier = Modifier.weight(1f)
                        ) {
                            email = "attestor1@vault.local"
                            password = "pass123"
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickRoleChip(
                            label = "Doctor",
                            subtitle = "Attestor 2",
                            icon = Icons.Default.LocalHospital,
                            selected = email == "attestor2@vault.local",
                            modifier = Modifier.weight(1f)
                        ) {
                            email = "attestor2@vault.local"
                            password = "pass123"
                        }

                        QuickRoleChip(
                            label = "Beneficiary",
                            subtitle = "Ramesh (Son)",
                            icon = Icons.Default.FamilyRestroom,
                            selected = email == "beneficiary@vault.local",
                            modifier = Modifier.weight(1f)
                        ) {
                            email = "beneficiary@vault.local"
                            password = "pass123"
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Credentials Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Account Credentials",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepCharcoal
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = SlateNavy) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SlateNavy) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (authError != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = authError!!,
                            color = Terracotta,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            viewModel.login(email, password) { success ->
                                if (success) onLoginSuccess()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SlateNavy),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isAuthenticating
                    ) {
                        if (isAuthenticating) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Text("Sign In to Vault", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Security Policy Callout
            Surface(
                color = SlateNavyLight.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = SlateNavy,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Zero-Knowledge Role Boundary: Owners only view their own vault. Attestors cannot view unreleased assets. Beneficiaries unlock only upon 2-of-3 threshold consensus.",
                        fontSize = 12.sp,
                        color = DeepCharcoal.copy(alpha = 0.8f),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun QuickRoleChip(
    label: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) SlateNavy else WarmCream,
        shape = RoundedCornerShape(12.dp),
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD0C4B4)),
        modifier = modifier
            .clickable(onClick = onClick)
            .height(56.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) GoldAccent else SlateNavy,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color.White else DeepCharcoal
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = if (selected) Color.White.copy(alpha = 0.8f) else DeepCharcoal.copy(alpha = 0.6f),
                    maxLines = 1
                )
            }
        }
    }
}
