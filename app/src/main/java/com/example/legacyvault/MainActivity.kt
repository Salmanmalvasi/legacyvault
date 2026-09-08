package com.example.legacyvault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.legacyvault.data.VaultRepository
import com.example.legacyvault.ui.screens.*
import com.example.legacyvault.ui.theme.*

enum class VaultScreen {
    HOME,
    SCAN,
    MANUAL_ENTRY,
    INVENTORY,
    INSTRUCTIONS,
    ATTESTORS
}

class MainActivity : ComponentActivity() {

    private val repository = VaultRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LegacyvaultTheme {
                LegacyVaultApp(repository)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegacyVaultApp(repository: VaultRepository) {
    var currentScreen by remember { mutableStateOf(VaultScreen.HOME) }
    val isVaultUnlocked by repository.isVaultUnlocked.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = CardBackground,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = currentScreen == VaultScreen.HOME,
                    onClick = { currentScreen = VaultScreen.HOME },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SlateNavy,
                        selectedTextColor = SlateNavy,
                        indicatorColor = SlateNavyLight.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    selected = currentScreen == VaultScreen.SCAN,
                    onClick = { currentScreen = VaultScreen.SCAN },
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Scan") },
                    label = { Text("Scan", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SlateNavy,
                        selectedTextColor = SlateNavy,
                        indicatorColor = SlateNavyLight.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    selected = currentScreen == VaultScreen.INVENTORY,
                    onClick = { currentScreen = VaultScreen.INVENTORY },
                    icon = { Icon(Icons.Default.AccountBalance, contentDescription = "Portfolio") },
                    label = { Text("Portfolio", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SlateNavy,
                        selectedTextColor = SlateNavy,
                        indicatorColor = SlateNavyLight.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    selected = currentScreen == VaultScreen.INSTRUCTIONS,
                    onClick = { currentScreen = VaultScreen.INSTRUCTIONS },
                    icon = { Icon(Icons.Default.Lock, contentDescription = "Access Notes") },
                    label = { Text("Notes", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SlateNavy,
                        selectedTextColor = SlateNavy,
                        indicatorColor = SlateNavyLight.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    selected = currentScreen == VaultScreen.ATTESTORS,
                    onClick = { currentScreen = VaultScreen.ATTESTORS },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (isVaultUnlocked) {
                                    Badge(containerColor = ForestGreen) { Text("✓") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = "Attestors")
                        }
                    },
                    label = { Text("Release", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SlateNavy,
                        selectedTextColor = SlateNavy,
                        indicatorColor = SlateNavyLight.copy(alpha = 0.2f)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                VaultScreen.HOME -> OwnerHomeScreen(
                    repository = repository,
                    onNavigateToScan = { currentScreen = VaultScreen.SCAN },
                    onNavigateToManual = { currentScreen = VaultScreen.MANUAL_ENTRY },
                    onNavigateToInventory = { currentScreen = VaultScreen.INVENTORY },
                    onNavigateToInstructions = { currentScreen = VaultScreen.INSTRUCTIONS },
                    onNavigateToAttestors = { currentScreen = VaultScreen.ATTESTORS }
                )
                VaultScreen.SCAN -> DiscoveryScanScreen(
                    repository = repository,
                    onBack = { currentScreen = VaultScreen.HOME },
                    onScanSaved = { currentScreen = VaultScreen.INVENTORY }
                )
                VaultScreen.MANUAL_ENTRY -> ManualEntryScreen(
                    repository = repository,
                    onBack = { currentScreen = VaultScreen.HOME },
                    onSaved = { currentScreen = VaultScreen.INVENTORY }
                )
                VaultScreen.INVENTORY -> InventoryScreen(
                    repository = repository,
                    onBack = { currentScreen = VaultScreen.HOME },
                    onAddNew = { currentScreen = VaultScreen.MANUAL_ENTRY }
                )
                VaultScreen.INSTRUCTIONS -> AccessInstructionsScreen(
                    repository = repository,
                    onBack = { currentScreen = VaultScreen.HOME }
                )
                VaultScreen.ATTESTORS -> AttestorBeneficiaryScreen(
                    repository = repository,
                    onBack = { currentScreen = VaultScreen.HOME }
                )
            }
        }
    }
}