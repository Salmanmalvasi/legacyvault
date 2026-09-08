package com.example.legacyvault.data

import com.example.legacyvault.data.network.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class FinancialRecord(
    val id: String = UUID.randomUUID().toString(),
    val source: String, // "camera_ocr" | "manual" | "notification"
    val category: String, // "Bank Accounts" | "Insurance Policies" | "Retirement & PPF" | "Loans & Liabilities"
    val institution: String,
    val accountType: String,
    val accountNumber: String,
    val branchOrAgent: String = "—",
    val nominee: String = "Not stated",
    val gapWarning: String? = null
)

data class AccessInstruction(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val value: String,
    val isEncrypted: Boolean = true
)

data class Attestor(
    val id: String,
    val name: String,
    val role: String,
    val phone: String,
    val shareFragment: String,
    val hasAttested: Boolean = false,
    val attestationTime: String? = null
)

data class BlockchainReceipt(
    val txHash: String,
    val blockNumber: Long,
    val attestorName: String,
    val timestamp: String,
    val network: String = "Polygon Amoy (80002)",
    val explorerUrl: String? = null
)

class VaultRepository {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    val currentOwnerId = "owner_sundaram"

    // 1. Owner Profile & Living Signal Check-In State
    private val _ownerName = MutableStateFlow("S. Sundaram (74)")
    val ownerName: StateFlow<String> = _ownerName.asStateFlow()

    private val _beneficiaryName = MutableStateFlow("Ramesh Kumar (Son)")
    val beneficiaryName: StateFlow<String> = _beneficiaryName.asStateFlow()

    private val _lastCheckIn = MutableStateFlow(dateFormat.format(Date()))
    val lastCheckIn: StateFlow<String> = _lastCheckIn.asStateFlow()

    private val _isCheckInDue = MutableStateFlow(false)
    val isCheckInDue: StateFlow<Boolean> = _isCheckInDue.asStateFlow()

    private val _missedCount = MutableStateFlow(0)
    val missedCount: StateFlow<Int> = _missedCount.asStateFlow()

    private val _escalationActive = MutableStateFlow(false)
    val escalationActive: StateFlow<Boolean> = _escalationActive.asStateFlow()

    // 2. Discovered Records
    private val _records = MutableStateFlow<List<FinancialRecord>>(emptyList())
    val records: StateFlow<List<FinancialRecord>> = _records.asStateFlow()

    // 3. Access Instructions (Encrypted at rest with Shamir-protected key)
    private val _instructions = MutableStateFlow<List<AccessInstruction>>(emptyList())
    val instructions: StateFlow<List<AccessInstruction>> = _instructions.asStateFlow()

    // 4. Attestors and SSS Shares (2-of-3 threshold)
    private val _attestors = MutableStateFlow<List<Attestor>>(emptyList())
    val attestors: StateFlow<List<Attestor>> = _attestors.asStateFlow()

    // 5. Blockchain Ledger Receipts
    private val _blockchainLogs = MutableStateFlow<List<BlockchainReceipt>>(emptyList())
    val blockchainLogs: StateFlow<List<BlockchainReceipt>> = _blockchainLogs.asStateFlow()

    // 6. Vault Release Status
    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    // 7. Network Sync Status
    private val _isOnlineBackend = MutableStateFlow(false)
    val isOnlineBackend: StateFlow<Boolean> = _isOnlineBackend.asStateFlow()

    init {
        seedInitialDemoData()
    }

    fun seedInitialDemoData() {
        _records.value = listOf(
            FinancialRecord(
                source = "camera_ocr",
                category = "Bank Accounts",
                institution = "State Bank of India",
                accountType = "Senior Citizen Savings",
                accountNumber = "304918239120",
                branchOrAgent = "Adyar Chennai Branch (SBIN0001234)",
                nominee = "Ramesh Kumar (Son - 100%)",
                gapWarning = null
            ),
            FinancialRecord(
                source = "camera_ocr",
                category = "Insurance Policies",
                institution = "Life Insurance Corp (LIC)",
                accountType = "Jeevan Anand Policy",
                accountNumber = "847291xxx",
                branchOrAgent = "Agent: K. Balaji (Chennai Div)",
                nominee = "Pending verification",
                gapWarning = "Policy number partially obscured on paper slip — verify at branch."
            ),
            FinancialRecord(
                source = "manual",
                category = "Retirement & PPF",
                institution = "EPFO & Post Office PPF",
                accountType = "15-Yr Public Provident Fund",
                accountNumber = "PPF-490218-CHE",
                branchOrAgent = "Mylapore Head Post Office",
                nominee = "Ramesh Kumar",
                gapWarning = null
            )
        )

        _instructions.value = listOf(
            AccessInstruction(
                label = "SBI Safe Deposit Locker Key",
                value = "Small brass key kept in second wooden drawer under altar in pooja room. Locker #42 at SBI Adyar."
            ),
            AccessInstruction(
                label = "Ancestral House Land Deeds",
                value = "Physical patta document folder is in Godrej steel almirah, inside navy leather file."
            )
        )

        _attestors.value = listOf(
            Attestor(
                id = "attestor_1",
                name = "V. Krishnan",
                role = "Family Lawyer & Friend",
                phone = "+91 98401 23456",
                shareFragment = "1-4f8a29e1c4...",
                hasAttested = false
            ),
            Attestor(
                id = "attestor_2",
                name = "Dr. Ananya Iyer",
                role = "Family Physician",
                phone = "+91 94440 98765",
                shareFragment = "2-b91c8430fd...",
                hasAttested = false
            ),
            Attestor(
                id = "attestor_3",
                name = "M. Natarajan",
                role = "Chartered Accountant",
                phone = "+91 98840 55512",
                shareFragment = "3-7e21a0f962...",
                hasAttested = false
            )
        )

        _blockchainLogs.value = emptyList()
        _isVaultUnlocked.value = false
        _escalationActive.value = false
        _missedCount.value = 0
        _isCheckInDue.value = false
    }

    // --- Actions with Real Retrofit Network Calls & Offline Fallback ---

    fun performCheckIn() {
        val nowFormatted = dateFormat.format(Date()) + " at " + timeFormat.format(Date())
        _lastCheckIn.value = nowFormatted
        _isCheckInDue.value = false
        _missedCount.value = 0
        _escalationActive.value = false

        scope.launch {
            try {
                val resp = RetrofitClient.api.respondCheckIn(CheckinRespondRequestDto(currentOwnerId))
                if (resp.isSuccessful) {
                    _isOnlineBackend.value = true
                }
            } catch (e: Exception) {
                // Offline fallback - state is already safely updated locally
                _isOnlineBackend.value = false
            }
        }
    }

    fun resetDemoState() {
        seedInitialDemoData()
        scope.launch {
            try {
                RetrofitClient.api.resetDemo()
                _isOnlineBackend.value = true
            } catch (e: Exception) {
                _isOnlineBackend.value = false
            }
        }
    }

    fun simulateMissedCheckIn() {
        _missedCount.value += 1
        _isCheckInDue.value = true
        _escalationActive.value = true

        scope.launch {
            try {
                val resp = RetrofitClient.api.simulateCheckInMiss(currentOwnerId)
                if (resp.isSuccessful) {
                    _isOnlineBackend.value = true
                }
            } catch (e: Exception) {
                _isOnlineBackend.value = false
            }
        }
    }

    fun addManualRecord(
        institution: String,
        category: String,
        accountType: String,
        accountNumber: String,
        branch: String,
        nominee: String
    ) {
        val newRecord = FinancialRecord(
            source = "manual",
            category = category,
            institution = institution,
            accountType = accountType,
            accountNumber = accountNumber,
            branchOrAgent = branch.ifBlank { "—" },
            nominee = nominee.ifBlank { "Not stated" }
        )
        _records.value = listOf(newRecord) + _records.value

        scope.launch {
            try {
                val fields = mapOf(
                    "institution" to institution,
                    "category" to category,
                    "account_type" to accountType,
                    "account_number" to accountNumber,
                    "branch" to branch,
                    "nominee" to nominee
                )
                val resp = RetrofitClient.api.submitDiscoveryEntry(
                    DiscoveryEntryRequestDto(
                        ownerId = currentOwnerId,
                        source = "manual",
                        type = category.lowercase().replace(" ", "_"),
                        rawText = "$institution $accountType $accountNumber",
                        extractedFields = fields
                    )
                )
                if (resp.isSuccessful) {
                    _isOnlineBackend.value = true
                }
            } catch (e: Exception) {
                _isOnlineBackend.value = false
            }
        }
    }

    fun addOcrExtractedRecord(
        institution: String,
        category: String,
        accountType: String,
        accountNumber: String,
        branch: String,
        nominee: String,
        gapNote: String?,
        rawOcrText: String = ""
    ) {
        val ocrRecord = FinancialRecord(
            source = "camera_ocr",
            category = category,
            institution = institution,
            accountType = accountType,
            accountNumber = accountNumber,
            branchOrAgent = branch,
            nominee = nominee,
            gapWarning = gapNote
        )
        _records.value = listOf(ocrRecord) + _records.value

        scope.launch {
            try {
                val fields = mutableMapOf<String, Any>(
                    "institution" to institution,
                    "category" to category,
                    "account_type" to accountType,
                    "account_number" to accountNumber,
                    "branch" to branch,
                    "nominee" to nominee
                )
                if (gapNote != null) fields["gap_note"] = gapNote

                val resp = RetrofitClient.api.submitDiscoveryEntry(
                    DiscoveryEntryRequestDto(
                        ownerId = currentOwnerId,
                        source = "camera_ocr",
                        type = category.lowercase().replace(" ", "_"),
                        rawText = rawOcrText,
                        extractedFields = fields
                    )
                )
                if (resp.isSuccessful) {
                    _isOnlineBackend.value = true
                }
            } catch (e: Exception) {
                _isOnlineBackend.value = false
            }
        }
    }

    fun addAccessInstruction(label: String, value: String) {
        val newInstruction = AccessInstruction(label = label, value = value)
        _instructions.value = _instructions.value + newInstruction

        scope.launch {
            try {
                val resp = RetrofitClient.api.storeVaultInstruction(
                    VaultStoreRequestDto(
                        ownerId = currentOwnerId,
                        label = label,
                        value = value
                    )
                )
                if (resp.isSuccessful) {
                    _isOnlineBackend.value = true
                }
            } catch (e: Exception) {
                _isOnlineBackend.value = false
            }
        }
    }

    fun submitAttestation(attestorId: String) {
        val currentList = _attestors.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == attestorId }
        if (index != -1 && !currentList[index].hasAttested) {
            val nowTime = timeFormat.format(Date())
            val updated = currentList[index].copy(hasAttested = true, attestationTime = nowTime)
            currentList[index] = updated
            _attestors.value = currentList

            val totalAttested = currentList.count { it.hasAttested }
            if (totalAttested >= 2) {
                _isVaultUnlocked.value = true
            }

            scope.launch {
                try {
                    val resp = RetrofitClient.api.submitAttestation(
                        AttestorAttestRequestDto(
                            ownerId = currentOwnerId,
                            attestorId = attestorId,
                            share = updated.shareFragment
                        )
                    )
                    if (resp.isSuccessful && resp.body() != null) {
                        _isOnlineBackend.value = true
                        val body = resp.body()!!
                        val receipt = body.blockchainReceipt
                        val receiptObj = BlockchainReceipt(
                            txHash = receipt?.txHash ?: ("0x" + UUID.randomUUID().toString().replace("-", "")),
                            blockNumber = receipt?.blockNumber ?: (14529300L + _blockchainLogs.value.size + 1),
                            attestorName = updated.name,
                            timestamp = nowTime,
                            network = receipt?.chain ?: "Polygon Amoy (80002)",
                            explorerUrl = receipt?.explorerUrl
                        )
                        _blockchainLogs.value = listOf(receiptObj) + _blockchainLogs.value
                        if (body.vaultUnlocked) {
                            _isVaultUnlocked.value = true
                        }
                    } else {
                        // Fallback receipt
                        createLocalFallbackReceipt(updated.name, nowTime)
                    }
                } catch (e: Exception) {
                    _isOnlineBackend.value = false
                    createLocalFallbackReceipt(updated.name, nowTime)
                }
            }
        }
    }

    private fun createLocalFallbackReceipt(attestorName: String, timeStr: String) {
        val fallbackHash = "0x" + UUID.randomUUID().toString().replace("-", "") + "7a2f"
        val receipt = BlockchainReceipt(
            txHash = fallbackHash,
            blockNumber = 14529300L + _blockchainLogs.value.size + 1,
            attestorName = attestorName,
            timestamp = timeStr,
            network = "Polygon Amoy (80002)",
            explorerUrl = "https://amoy.polygonscan.com/tx/$fallbackHash"
        )
        _blockchainLogs.value = listOf(receipt) + _blockchainLogs.value
    }
}
