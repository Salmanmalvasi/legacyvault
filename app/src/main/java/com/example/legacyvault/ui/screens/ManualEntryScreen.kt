package com.example.legacyvault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.legacyvault.data.VaultRepository
import com.example.legacyvault.ui.ManualDraftState
import com.example.legacyvault.ui.VaultViewModel
import com.example.legacyvault.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreen(
    repository: VaultRepository,
    viewModel: VaultViewModel? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val vmDraftState = viewModel?.manualDraft?.collectAsState()
    var localDraft by remember { mutableStateOf(ManualDraftState()) }
    val draft = vmDraftState?.value ?: localDraft

    fun updateDraft(
        institution: String? = null,
        category: String? = null,
        accountType: String? = null,
        accountNumber: String? = null,
        branch: String? = null,
        nominee: String? = null
    ) {
        if (viewModel != null) {
            viewModel.updateManualDraft(institution, category, accountType, accountNumber, branch, nominee)
        } else {
            localDraft = localDraft.copy(
                institution = institution ?: localDraft.institution,
                category = category ?: localDraft.category,
                accountType = accountType ?: localDraft.accountType,
                accountNumber = accountNumber ?: localDraft.accountNumber,
                branch = branch ?: localDraft.branch,
                nominee = nominee ?: localDraft.nominee
            )
        }
    }

    val categories = listOf("Bank Accounts", "Insurance Policies", "Retirement & PPF", "Loans & Liabilities")
    var categoryExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Account Manually", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepCharcoal) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DeepCharcoal)
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
                text = "Type your details at your own pace. Entered data is saved persistently across screens.",
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
                    value = draft.category,
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
                                updateDraft(category = cat)
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
                value = draft.institution,
                onValueChange = { updateDraft(institution = it) },
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
                value = draft.accountType,
                onValueChange = { updateDraft(accountType = it) },
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
                value = draft.accountNumber,
                onValueChange = { updateDraft(accountNumber = it) },
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
                value = draft.branch,
                onValueChange = { updateDraft(branch = it) },
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
                value = draft.nominee,
                onValueChange = { updateDraft(nominee = it) },
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
                    if (draft.institution.isNotBlank()) {
                        if (viewModel != null) {
                            viewModel.saveManualDraftRecord()
                        } else {
                            repository.addManualRecord(
                                institution = draft.institution,
                                category = draft.category,
                                accountType = if (draft.accountType.isBlank()) "Savings" else draft.accountType,
                                accountNumber = if (draft.accountNumber.isBlank()) "Unspecified" else draft.accountNumber,
                                branch = draft.branch,
                                nominee = draft.nominee
                            )
                        }
                        onSaved()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SlateNavy),
                enabled = draft.institution.isNotBlank()
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save to My Protected Vault", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
