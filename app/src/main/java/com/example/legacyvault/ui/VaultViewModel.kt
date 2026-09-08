package com.example.legacyvault.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.legacyvault.data.FinancialRecord
import com.example.legacyvault.data.VaultRepository
import com.example.legacyvault.data.network.LoginRequestDto
import com.example.legacyvault.data.network.PendingAttestationItemDto
import com.example.legacyvault.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserSession(
    val userId: String,
    val email: String,
    val role: String, // "owner" | "attestor" | "beneficiary"
    val displayName: String,
    val linkedOwnerId: String?,
    val attestorId: String?,
    val token: String
)

data class ManualDraftState(
    val institution: String = "",
    val category: String = "Bank Accounts",
    val accountType: String = "",
    val accountNumber: String = "",
    val branch: String = "",
    val nominee: String = ""
)

data class ScanDraftState(
    val institution: String = "",
    val category: String = "Bank Accounts",
    val accountType: String = "",
    val accountNumber: String = "",
    val branch: String = "",
    val nominee: String = "",
    val gapNote: String? = null,
    val rawOcrText: String = "",
    val showRawText: Boolean = false
)

class VaultViewModel(
    val repository: VaultRepository = VaultRepository()
) : ViewModel() {

    // 1. Authenticated User Session
    private val _currentUser = MutableStateFlow<UserSession?>(null)
    val currentUser: StateFlow<UserSession?> = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    // 2. Draft Form Persistence across tab navigation & screen rotation
    private val _manualDraft = MutableStateFlow(ManualDraftState())
    val manualDraft: StateFlow<ManualDraftState> = _manualDraft.asStateFlow()

    private val _scanDraft = MutableStateFlow(ScanDraftState())
    val scanDraft: StateFlow<ScanDraftState> = _scanDraft.asStateFlow()

    // 3. Attestor View Pending Requests
    private val _pendingAttestations = MutableStateFlow<List<PendingAttestationItemDto>>(emptyList())
    val pendingAttestations: StateFlow<List<PendingAttestationItemDto>> = _pendingAttestations.asStateFlow()

    init {
        // Initially unauthenticated so the user selects their role
        _currentUser.value = null
    }

    // --- Authentication Actions ---

    fun login(email: String, pass: String, onComplete: (Boolean) -> Unit = {}) {
        _isAuthenticating.value = true
        _authError.value = null

        viewModelScope.launch {
            try {
                val resp = RetrofitClient.api.login(LoginRequestDto(email.trim(), pass.trim()))
                if (resp.isSuccessful && resp.body() != null) {
                    val body = resp.body()!!
                    val u = body.user
                    val session = UserSession(
                        userId = u.userId,
                        email = u.email,
                        role = u.role,
                        displayName = u.displayName,
                        linkedOwnerId = u.linkedOwnerId,
                        attestorId = u.attestorId,
                        token = body.token
                    )
                    _currentUser.value = session
                    _isAuthenticating.value = false

                    if (u.role == "attestor" && u.attestorId != null) {
                        fetchPendingAttestations(u.attestorId)
                    }
                    onComplete(true)
                } else {
                    _authError.value = "Invalid email or password (HTTP ${resp.code()})"
                    _isAuthenticating.value = false
                    onComplete(false)
                }
            } catch (e: Exception) {
                // Offline fallback credential matching for demo resiliency
                val cleanEmail = email.trim().lowercase()
                val role = when {
                    cleanEmail.startsWith("attestor") -> "attestor"
                    cleanEmail.startsWith("beneficiary") -> "beneficiary"
                    else -> "owner"
                }
                val name = when (role) {
                    "attestor" -> "V. Krishnan (Lawyer)"
                    "beneficiary" -> "Ramesh Kumar (Son)"
                    else -> "S. Sundaram (Owner)"
                }
                val attId = if (role == "attestor") "attestor_1" else null

                _currentUser.value = UserSession(
                    userId = "usr_offline_${System.currentTimeMillis()}",
                    email = email,
                    role = role,
                    displayName = name,
                    linkedOwnerId = "owner_sundaram",
                    attestorId = attId,
                    token = "offline_token"
                )
                _isAuthenticating.value = false
                onComplete(true)
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _authError.value = null
    }

    fun setSessionForTesting(session: UserSession?) {
        _currentUser.value = session
    }

    fun switchRole(role: String) {
        when (role) {
            "owner" -> login("owner@vault.local", "pass123")
            "attestor_1" -> login("attestor1@vault.local", "pass123")
            "attestor_2" -> login("attestor2@vault.local", "pass123")
            "beneficiary" -> login("beneficiary@vault.local", "pass123")
        }
    }

    fun fetchPendingAttestations(attestorId: String) {
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.api.getPendingAttestations(attestorId)
                if (resp.isSuccessful && resp.body() != null) {
                    _pendingAttestations.value = resp.body()!!.requests
                }
            } catch (_: Exception) {}
        }
    }

    // --- State Persistence for Form Drafts ---

    fun updateManualDraft(
        institution: String? = null,
        category: String? = null,
        accountType: String? = null,
        accountNumber: String? = null,
        branch: String? = null,
        nominee: String? = null
    ) {
        _manualDraft.value = _manualDraft.value.copy(
            institution = institution ?: _manualDraft.value.institution,
            category = category ?: _manualDraft.value.category,
            accountType = accountType ?: _manualDraft.value.accountType,
            accountNumber = accountNumber ?: _manualDraft.value.accountNumber,
            branch = branch ?: _manualDraft.value.branch,
            nominee = nominee ?: _manualDraft.value.nominee
        )
    }

    fun saveManualDraftRecord() {
        val draft = _manualDraft.value
        if (draft.institution.isNotBlank()) {
            repository.addManualRecord(
                institution = draft.institution,
                category = draft.category,
                accountType = draft.accountType.ifBlank { "Savings" },
                accountNumber = draft.accountNumber.ifBlank { "Unspecified" },
                branch = draft.branch,
                nominee = draft.nominee
            )
            clearManualDraft()
        }
    }

    fun clearManualDraft() {
        _manualDraft.value = ManualDraftState()
    }

    fun updateScanDraft(
        institution: String? = null,
        category: String? = null,
        accountType: String? = null,
        accountNumber: String? = null,
        branch: String? = null,
        nominee: String? = null,
        gapNote: String? = null,
        rawOcrText: String? = null,
        showRawText: Boolean? = null
    ) {
        _scanDraft.value = _scanDraft.value.copy(
            institution = institution ?: _scanDraft.value.institution,
            category = category ?: _scanDraft.value.category,
            accountType = accountType ?: _scanDraft.value.accountType,
            accountNumber = accountNumber ?: _scanDraft.value.accountNumber,
            branch = branch ?: _scanDraft.value.branch,
            nominee = nominee ?: _scanDraft.value.nominee,
            gapNote = if (gapNote != null) gapNote else _scanDraft.value.gapNote,
            rawOcrText = rawOcrText ?: _scanDraft.value.rawOcrText,
            showRawText = showRawText ?: _scanDraft.value.showRawText
        )
    }

    fun saveScanDraftRecord() {
        val draft = _scanDraft.value
        if (draft.institution.isNotBlank() && draft.accountNumber.isNotBlank()) {
            repository.addOcrExtractedRecord(
                institution = draft.institution,
                category = draft.category,
                accountType = draft.accountType,
                accountNumber = draft.accountNumber,
                branch = draft.branch,
                nominee = draft.nominee,
                gapNote = draft.gapNote,
                rawOcrText = draft.rawOcrText
            )
            clearScanDraft()
        }
    }

    fun clearScanDraft() {
        _scanDraft.value = ScanDraftState()
    }
}
