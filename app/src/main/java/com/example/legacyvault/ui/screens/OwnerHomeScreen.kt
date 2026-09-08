package com.example.legacyvault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.legacyvault.data.VaultRepository
import com.example.legacyvault.ui.theme.*

@Composable
fun OwnerHomeScreen(
    repository: VaultRepository,
    onNavigateToScan: () -> Unit,
    onNavigateToManual: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToInstructions: () -> Unit,
    onNavigateToAttestors: () -> Unit
) {
    val ownerName by repository.ownerName.collectAsState()
    val lastCheckIn by repository.lastCheckIn.collectAsState()
    val isDue by repository.isCheckInDue.collectAsState()
    val escalationActive by repository.escalationActive.collectAsState()
    val records by repository.records.collectAsState()
    val instructions by repository.instructions.collectAsState()

    var showCheckInConfirmation by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmCream)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Top Header Row: Warm greeting on left, sleek compact demo pill on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Namaste,",
                    fontSize = 16.sp,
                    color = SlateGrey,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = ownerName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepCharcoal
                )
            }

            // Unobtrusive corner indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WarmAmberLight.copy(alpha = 0.9f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(WarmAmber)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "30s Demo",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarmAmberDark
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = { repository.resetDemoState() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Demo",
                        modifier = Modifier.size(16.dp),
                        tint = SlateGrey
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Reassuring Living Signal Check-In Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Your Living Signal",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateNavy
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (escalationActive) SoftRoseLight else ForestGreenLight
                    ) {
                        Text(
                            text = if (escalationActive) "Missed Check-in" else "Active & Safe",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (escalationActive) SoftRose else ForestGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (escalationActive)
                        "We missed your routine check-in. Tap below to confirm you are safe, or test escalation to trusted attestors."
                    else
                        "A simple tap lets your family know you are doing well without needing to make calls.",
                    fontSize = 14.sp,
                    color = SlateGrey,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Big Senior-Friendly Check-in Button
                Button(
                    onClick = {
                        repository.performCheckIn()
                        showCheckInConfirmation = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "I am Doing Well Today",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Last recorded: $lastCheckIn",
                    fontSize = 12.sp,
                    color = MutedGrey
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Simulation helper for Hackathon Demo
                TextButton(
                    onClick = { repository.simulateMissedCheckIn() }
                ) {
                    Text(
                        text = "Demo: Simulate Missed Check-In Alert",
                        fontSize = 12.sp,
                        color = WarmAmber
                    )
                }
            }
        }

        if (showCheckInConfirmation) {
            AlertDialog(
                onDismissRequest = { showCheckInConfirmation = false },
                title = { Text("Thank You, Sundaram ji!", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Your check-in has been logged. Your family and trusted attestors know you are safe and sound.",
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showCheckInConfirmation = false },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                    ) {
                        Text("Wonderful")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Estate Protection Overview
        Text(
            text = "Your Protected Estate",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = DeepCharcoal
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryBadge(
                modifier = Modifier.weight(1f),
                title = "Accounts & Policies",
                count = "${records.size}",
                icon = Icons.Default.AccountBalance,
                color = SlateNavy,
                onClick = onNavigateToInventory
            )
            SummaryBadge(
                modifier = Modifier.weight(1f),
                title = "Access Notes",
                count = "${instructions.size}",
                icon = Icons.Default.Key,
                color = HeritageTeal,
                onClick = onNavigateToInstructions
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Actions Grid
        Text(
            text = "Organize New Details",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = DeepCharcoal
        )

        Spacer(modifier = Modifier.height(12.dp))

        ActionCard(
            title = "Scan Paper Document",
            subtitle = "Camera reads passbooks, LIC slips & post office forms",
            icon = Icons.Default.CameraAlt,
            iconBg = SlateNavyLight,
            onClick = onNavigateToScan
        )

        Spacer(modifier = Modifier.height(10.dp))

        ActionCard(
            title = "Add Account Manually",
            subtitle = "Type bank details, PPF, or policy numbers yourself",
            icon = Icons.Default.Edit,
            iconBg = HeritageTeal,
            onClick = onNavigateToManual
        )

        Spacer(modifier = Modifier.height(10.dp))

        ActionCard(
            title = "Write Access Instruction",
            subtitle = "Store where keys, safe combos, or paper files are kept",
            icon = Icons.Default.Lock,
            iconBg = WarmAmber,
            onClick = onNavigateToInstructions
        )

        Spacer(modifier = Modifier.height(10.dp))

        ActionCard(
            title = "View Trusted Attestors & Release",
            subtitle = "See your 3 designated contacts & simulated release",
            icon = Icons.Default.Group,
            iconBg = IndigoModern,
            onClick = onNavigateToAttestors
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SummaryBadge(
    modifier: Modifier = Modifier,
    title: String,
    count: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = count, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DeepCharcoal)
            Text(text = title, fontSize = 12.sp, color = SlateGrey, lineHeight = 16.sp)
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DeepCharcoal)
                Text(text = subtitle, fontSize = 12.sp, color = SlateGrey, lineHeight = 16.sp)
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MutedGrey)
        }
    }
}
