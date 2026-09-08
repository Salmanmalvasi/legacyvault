package com.example.legacyvault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.legacyvault.data.VaultRepository
import com.example.legacyvault.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreen(
    repository: VaultRepository,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    var institution by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Bank Accounts") }
    var accountType by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var nominee by remember { mutableStateOf("") }

    val categories = listOf("Bank Accounts", "Insurance Policies", "Retirement & PPF", "Loans & Liabilities")
    var categoryExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Account Manually", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepCharcoal) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = DeepCharcoal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmCream)
            )
        },
        containerColor = WarmCream
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = "Type your details at your own pace. A family member can also help enter these numbers.",
                fontSize = 14.sp,
                color = SlateGrey,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Category Picker
            Text(text = "What type of account is this?", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DeepCharcoal)
            Spacer(modifier = Modifier.height(6.dp))

            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardBackground,
                        unfocusedContainerColor = CardBackground
                    )
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat, fontSize = 15.sp) },
                            onClick = {
                                selectedCategory = cat
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Institution Name
            Text(text = "Bank or Institution Name", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DeepCharcoal)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = institution,
                onValueChange = { institution = it },
                placeholder = { Text("e.g. Canara Bank, Post Office, LIC", color = MutedGrey) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Plan / Account Type
            Text(text = "Plan or Account Type", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DeepCharcoal)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = accountType,
                onValueChange = { accountType = it },
                placeholder = { Text("e.g. Pension Savings, Fixed Deposit, Term Life", color = MutedGrey) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Account / Policy Number
            Text(text = "Account or Policy Number", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DeepCharcoal)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = accountNumber,
                onValueChange = { accountNumber = it },
                placeholder = { Text("e.g. 10293847561", color = MutedGrey) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Branch / Location
            Text(text = "Branch or City", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DeepCharcoal)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = branch,
                onValueChange = { branch = it },
                placeholder = { Text("e.g. T. Nagar Branch, Chennai", color = MutedGrey) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Nominee
            Text(text = "Nominee Name (if known)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DeepCharcoal)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = nominee,
                onValueChange = { nominee = it },
                placeholder = { Text("e.g. Ramesh Kumar (Son)", color = MutedGrey) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground
                )
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    if (institution.isNotBlank()) {
                        repository.addManualRecord(
                            institution = institution,
                            category = selectedCategory,
                            accountType = accountType.ifBlank { "Savings" },
                            accountNumber = accountNumber.ifBlank { "Unspecified" },
                            branch = branch,
                            nominee = nominee
                        )
                        onSaved()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SlateNavy),
                enabled = institution.isNotBlank()
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save to My Protected Vault", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
