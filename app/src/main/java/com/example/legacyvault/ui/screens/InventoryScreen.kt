package com.example.legacyvault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.legacyvault.data.FinancialRecord
import com.example.legacyvault.data.VaultRepository
import com.example.legacyvault.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    repository: VaultRepository,
    onBack: () -> Unit,
    onAddNew: () -> Unit
) {
    val records by repository.records.collectAsState()

    // Group records by category
    val grouped = remember(records) {
        records.groupBy { it.category }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Protected Estate Inventory", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepCharcoal) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = DeepCharcoal)
                    }
                },
                actions = {
                    IconButton(onClick = onAddNew) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add New", tint = SlateNavy)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmCream)
            )
        },
        containerColor = WarmCream
    ) { innerPadding ->
        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No records in inventory yet. Tap + to add one.", color = SlateGrey)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                grouped.forEach { (category, items) ->
                    item {
                        Text(
                            text = "$category (${items.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateNavy,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                        )
                    }

                    items(items) { item ->
                        InventoryRecordCard(item)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun InventoryRecordCard(record: FinancialRecord) {
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
                Text(
                    text = record.institution,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepCharcoal,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (record.source == "camera_ocr") SlateNavyLight.copy(alpha = 0.15f) else HeritageTeal.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (record.source == "camera_ocr") "Scanned" else "Manual",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (record.source == "camera_ocr") SlateNavy else HeritageTeal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = record.accountType,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = SlateNavy
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Number: ", fontSize = 13.sp, color = SlateGrey)
                Text(text = record.accountNumber, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DeepCharcoal)
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Branch: ", fontSize = 13.sp, color = SlateGrey)
                Text(text = record.branchOrAgent, fontSize = 13.sp, color = DeepCharcoal)
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Nominee: ", fontSize = 13.sp, color = SlateGrey)
                Text(text = record.nominee, fontSize = 13.sp, color = DeepCharcoal)
            }

            if (record.gapWarning != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = WarmAmberLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = WarmAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = record.gapWarning,
                            fontSize = 11.sp,
                            color = DeepCharcoal,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}
