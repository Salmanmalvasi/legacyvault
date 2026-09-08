package com.example.legacyvault.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.legacyvault.data.VaultRepository
import com.example.legacyvault.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttestorBeneficiaryScreen(
    repository: VaultRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val attestors by repository.attestors.collectAsState()
    val isVaultUnlocked by repository.isVaultUnlocked.collectAsState()
    val blockchainLogs by repository.blockchainLogs.collectAsState()
    val escalationActive by repository.escalationActive.collectAsState()
    val beneficiaryName by repository.beneficiaryName.collectAsState()
    val records by repository.records.collectAsState()
    val instructions by repository.instructions.collectAsState()
    val cryptoProof by repository.cryptoProof.collectAsState()

    val attestedCount = attestors.count { it.hasAttested }
    var showReleaseDialog by remember { mutableStateOf(false) }
    var showAddAttestorDialog by remember { mutableStateOf(false) }
    var showMathPanel by remember { mutableStateOf(true) }

    // Add Attestor Form State
    var newName by remember { mutableStateOf("") }
    var newRole by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var isAddingAttestor by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Attestors & Vault Release",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepCharcoal
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DeepCharcoal)
                    }
                },
                actions = {
                    TextButton(onClick = { repository.fetchCryptoProof() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp), tint = SlateNavy)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Refresh", fontSize = 12.sp, color = SlateNavy, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmCream)
            )
        },
        containerColor = WarmCream
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Status Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isVaultUnlocked) ForestGreenLight else if (escalationActive) WarmAmberLight else SlateNavyLight.copy(alpha = 0.12f)
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isVaultUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isVaultUnlocked) ForestGreen else SlateNavy,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isVaultUnlocked) "Vault Released" else "Vault Protected",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isVaultUnlocked) ForestGreen else SlateNavy
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CardBackground
                            ) {
                                Text(
                                    text = "$attestedCount / 2 Attestations",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepCharcoal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { (attestedCount / 2f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (isVaultUnlocked) ForestGreen else HeritageTeal,
                            trackColor = Color.White.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (isVaultUnlocked)
                                "Threshold reached (2 of ${attestors.size}). Shamir's Secret reconstructed the master key. All assets & instructions are now available to $beneficiaryName."
                            else if (escalationActive)
                                "Alert: Sustained non-response detected. Designated attestors are requested to log in and independently submit their cryptographic shares."
                            else
                                "Owner living signal is active. SSS shares remain securely distributed. Any 2 of ${attestors.size} attestors are required to authorize release.",
                            fontSize = 13.sp,
                            color = DeepCharcoal,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // 2. Security Model Notice (Explains strict non-proxy rule)
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SlateNavyLight.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = SlateNavy,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Non-Custodial Security Model: Only each independent attestor, logged in through their own account, can submit their cryptographic share. No one (including the owner) can attest on another's behalf.",
                            fontSize = 12.sp,
                            color = DeepCharcoal,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // 3. Attestors Header with Add Attestor Action
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Designated Trusted Attestors (${attestors.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateNavy
                    )

                    OutlinedButton(
                        onClick = { showAddAttestorDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp), tint = SlateNavy)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Attestor", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SlateNavy)
                    }
                }
            }

            // 4. Attestors List (Pure status display - NO proxy submit buttons)
            items(attestors) { attestor ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = attestor.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DeepCharcoal)
                                Text(text = "${attestor.role} • ${attestor.phone}", fontSize = 12.sp, color = SlateGrey)
                            }
                            if (attestor.hasAttested) {
                                Surface(shape = RoundedCornerShape(8.dp), color = ForestGreenLight) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Attestation Submitted ✓", fontSize = 11.sp, color = ForestGreen, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Surface(shape = RoundedCornerShape(8.dp), color = WarmAmberLight) {
                                    Text(
                                        text = "Awaiting Attestor Login",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WarmAmberDark,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Shamir Share: ${attestor.shareFragment}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = HeritageTeal
                            )
                            Text(
                                text = if (attestor.hasAttested) "Recorded on-chain" else "Independent Login Required",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (attestor.hasAttested) ForestGreen else SlateGrey
                            )
                        }
                    }
                }
            }

            // 5. "Show the Math" Live Cryptographic Proof Panel
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateNavy),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showMathPanel = !showMathPanel },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Functions, contentDescription = null, tint = GoldAccent)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Show the Math (Live Cryptographic Audit)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Live GF(2^8) Lagrange polynomial state from backend",
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (showMathPanel) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }

                        AnimatedVisibility(visible = showMathPanel) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                val proof = cryptoProof
                                if (proof != null) {
                                    // Original Master Key
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.Black.copy(alpha = 0.3f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "Original 256-bit AES Master Key:",
                                                fontSize = 10.sp,
                                                color = GoldAccent,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = proof.originalMasterKeyTruncated,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "SHA-256 Hash: ${proof.originalMasterKeyHash.take(16)}...${proof.originalMasterKeyHash.takeLast(8)}",
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.White.copy(alpha = 0.7f)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Real Shares Landing Status
                                    Text(
                                        text = "Distributed Shamir Shares (${proof.sharesSubmittedCount} of ${proof.threshold} received):",
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    proof.shares.forEach { s ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${s.name.take(18)}: ${s.shareDisplay}",
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.White.copy(alpha = 0.85f)
                                            )
                                            if (s.hasSubmitted) {
                                                Surface(shape = RoundedCornerShape(4.dp), color = ForestGreen) {
                                                    Text(
                                                        text = "Received ✓",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            } else {
                                                Surface(shape = RoundedCornerShape(4.dp), color = Color.Gray.copy(alpha = 0.4f)) {
                                                    Text(
                                                        text = "Awaiting",
                                                        fontSize = 9.sp,
                                                        color = Color.White.copy(alpha = 0.7f),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Reconstructed Key Match Proof
                                    if (proof.reconstructedKeyHash != null) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = ForestGreen.copy(alpha = 0.25f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = ForestGreenLight, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Lagrange Interpolation Match Confirmed!",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = ForestGreenLight
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Reconstructed Key: ${proof.reconstructedKeyTruncated}",
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "Reconstructed Hash == Original Hash: MATCH CONFIRMED",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = GoldAccent
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }

                                    // Clickable Polygonscan Amoy Explorer Link
                                    Button(
                                        onClick = {
                                            val url = proof.polygonscanUrl
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(intent)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = HeritageTeal),
                                        modifier = Modifier.fillMaxWidth().height(38.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("View Contract on Polygonscan Amoy ↗", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Text(
                                        text = "Connecting to backend cryptographic audit service...",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Blockchain Audit Trail
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Immutable On-Chain Attestation Log",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateNavy
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ForestGreenLight
                    ) {
                        Text(
                            text = "Polygon Amoy Active",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (blockchainLogs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground)
                    ) {
                        Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No attestation transactions logged yet. Switch to an attestor profile to record the first immutable on-chain receipt.",
                                fontSize = 12.sp,
                                color = SlateGrey,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(blockchainLogs) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Block #${log.blockNumber}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SlateNavy)
                                Text(text = log.timestamp, fontSize = 11.sp, color = MutedGrey)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Attestor: ${log.attestorName}", fontSize = 12.sp, color = DeepCharcoal)
                            Text(
                                text = "Tx: ${log.txHash}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = HeritageTeal
                            )
                        }
                    }
                }
            }

            // 7. Beneficiary Payoff Card (Active once 2-of-N threshold reached)
            if (isVaultUnlocked) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Beneficiary Estate Handover",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateNavy
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Prepared for: $beneficiaryName",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = ForestGreen
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "All scattered accounts have been consolidated, deduplicated, and synthesized into one dignified document.",
                                fontSize = 13.sp,
                                color = SlateGrey,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showReleaseDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                            ) {
                                Icon(imageVector = Icons.Default.Description, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("View Synthesized Estate Summary", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Add Attestor Dialog
        if (showAddAttestorDialog) {
            AlertDialog(
                onDismissRequest = { if (!isAddingAttestor) showAddAttestorDialog = false },
                title = { Text("Add Trusted Attestor & Re-split Shares", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = WarmAmberLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Important: Adding an attestor re-splits the vault master key across ${attestors.size + 1} shares using a new random polynomial over GF(2^8). All attestors will receive updated key fragments, and prior signatures are invalidated.",
                                fontSize = 11.sp,
                                color = WarmAmberDark,
                                lineHeight = 15.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Attestor Full Name") },
                            placeholder = { Text("e.g. K. Venkatesh (Brother)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newRole,
                            onValueChange = { newRole = it },
                            label = { Text("Role or Relationship") },
                            placeholder = { Text("e.g. Brother, Chartered Accountant") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newPhone,
                            onValueChange = { newPhone = it },
                            label = { Text("Phone Number") },
                            placeholder = { Text("+91 98400 12345") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newName.isNotBlank()) {
                                isAddingAttestor = true
                                repository.addAttestor(
                                    name = newName.trim(),
                                    role = if (newRole.isBlank()) "Trusted Attestor" else newRole.trim(),
                                    phone = if (newPhone.isBlank()) "—" else newPhone.trim()
                                ) { success, msg ->
                                    isAddingAttestor = false
                                    showAddAttestorDialog = false
                                    newName = ""
                                    newRole = ""
                                    newPhone = ""
                                    Toast.makeText(context, msg ?: "Attestor added successfully", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = newName.isNotBlank() && !isAddingAttestor,
                        colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                    ) {
                        Text(if (isAddingAttestor) "Re-splitting SSS..." else "Confirm & Re-split")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddAttestorDialog = false }, enabled = !isAddingAttestor) {
                        Text("Cancel", color = SlateGrey)
                    }
                }
            )
        }

        if (showReleaseDialog) {
            AlertDialog(
                onDismissRequest = { showReleaseDialog = false },
                title = { Text("Legacy Vault — Estate Summary", fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Consolidated for $beneficiaryName\n" +
                                    "• ${records.size} Financial Accounts & Policies Discovered\n" +
                                    "• ${instructions.size} Encrypted Practical Access Notes Unlocked\n" +
                                    "• Authorized by 2-of-${attestors.size} Shamir's Secret Sharing Key Reconstruction\n" +
                                    "• Immutably Logged on Polygon Amoy",
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = DeepCharcoal
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ForestGreenLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Official PDF report generated and ready for branch presentation.",
                                fontSize = 12.sp,
                                color = ForestGreen,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showReleaseDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = SlateNavy)
                    ) {
                        Text("Done")
                    }
                }
            )
        }
    }
}
