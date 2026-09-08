package com.example.legacyvault.data.network

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Request DTOs ---

data class DiscoveryEntryRequestDto(
    @SerializedName("owner_id") val ownerId: String,
    @SerializedName("source") val source: String,
    @SerializedName("type") val type: String,
    @SerializedName("raw_text") val rawText: String,
    @SerializedName("extracted_fields") val extractedFields: Map<String, Any>
)

data class VaultStoreRequestDto(
    @SerializedName("owner_id") val ownerId: String,
    @SerializedName("label") val label: String,
    @SerializedName("value") val value: String
)

data class CheckinRespondRequestDto(
    @SerializedName("owner_id") val ownerId: String
)

data class AttestorAttestRequestDto(
    @SerializedName("owner_id") val ownerId: String,
    @SerializedName("attestor_id") val attestorId: String,
    @SerializedName("share") val share: String? = null
)

// --- Response DTOs ---

data class StandardResponseDto(
    @SerializedName("status") val status: String?,
    @SerializedName("message") val message: String?
)

data class BlockchainReceiptDto(
    @SerializedName("tx_hash") val txHash: String?,
    @SerializedName("block_number") val blockNumber: Long?,
    @SerializedName("contract_address") val contractAddress: String?,
    @SerializedName("explorer_url") val explorerUrl: String?,
    @SerializedName("chain") val chain: String?
)

data class AttestorAttestResponseDto(
    @SerializedName("status") val status: String,
    @SerializedName("owner_id") val ownerId: String,
    @SerializedName("attestor_id") val attestorId: String,
    @SerializedName("total_attestations_received") val totalAttestations: Int,
    @SerializedName("threshold_required") val thresholdRequired: Int,
    @SerializedName("vault_unlocked") val vaultUnlocked: Boolean,
    @SerializedName("blockchain_receipt") val blockchainReceipt: BlockchainReceiptDto?
)

data class ReleaseStatusResponseDto(
    @SerializedName("owner_id") val ownerId: String,
    @SerializedName("beneficiary_id") val beneficiaryId: String,
    @SerializedName("total_attestations") val totalAttestations: Int,
    @SerializedName("threshold") val threshold: Int,
    @SerializedName("vault_released") val vaultReleased: Boolean
)

data class BlockchainLogEntryDto(
    @SerializedName("attestor_address") val attestorAddress: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("block_number") val blockNumber: Long,
    @SerializedName("tx_hash") val txHash: String,
    @SerializedName("explorer_url") val explorerUrl: String?,
    @SerializedName("chain") val chain: String
)

data class BlockchainLogsResponseDto(
    @SerializedName("owner_id") val ownerId: String,
    @SerializedName("chain") val chain: String,
    @SerializedName("logs") val logs: List<BlockchainLogEntryDto>
)

data class GenerateDocumentResponseDto(
    @SerializedName("status") val status: String,
    @SerializedName("owner_id") val ownerId: String,
    @SerializedName("beneficiary") val beneficiary: String,
    @SerializedName("pdf_download_url") val pdfDownloadUrl: String
)

// --- Auth DTOs ---

data class LoginRequestDto(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class UserDto(
    @SerializedName("user_id") val userId: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String, // "owner" | "attestor" | "beneficiary"
    @SerializedName("display_name") val displayName: String,
    @SerializedName("linked_owner_id") val linkedOwnerId: String?,
    @SerializedName("attestor_id") val attestorId: String?
)

data class LoginResponseDto(
    @SerializedName("status") val status: String,
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: UserDto
)

data class PendingAttestationItemDto(
    @SerializedName("attestor_id") val attestorId: String,
    @SerializedName("owner_id") val ownerId: String,
    @SerializedName("attestor_name") val attestorName: String,
    @SerializedName("assigned_share") val assignedShare: String,
    @SerializedName("has_attested") val hasAttested: Boolean,
    @SerializedName("owner_name") val ownerName: String,
    @SerializedName("missed_checkins") val missedCheckins: Int,
    @SerializedName("escalation_active") val escalationActive: Boolean,
    @SerializedName("vault_released") val vaultReleased: Boolean,
    @SerializedName("threshold_needed") val thresholdNeeded: String
)

data class PendingAttestationsResponseDto(
    @SerializedName("attestor_id") val attestorId: String,
    @SerializedName("total_requests") val totalRequests: Int,
    @SerializedName("requests") val requests: List<PendingAttestationItemDto>
)

data class AddAttestorRequestDto(
    @SerializedName("owner_id") val ownerId: String,
    @SerializedName("name") val name: String,
    @SerializedName("role") val role: String,
    @SerializedName("phone") val phone: String
)

data class AttestorItemDto(
    @SerializedName("attestor_id") val attestorId: String,
    @SerializedName("name") val name: String,
    @SerializedName("role") val role: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("assigned_share") val assignedShare: String? = null,
    @SerializedName("share_value") val shareValue: String? = null,
    @SerializedName("has_attested") val hasAttested: Boolean = false,
    @SerializedName("attestation_timestamp") val attestationTimestamp: Long? = null
)

data class AddAttestorResponseDto(
    @SerializedName("status") val status: String,
    @SerializedName("owner_id") val ownerId: String,
    @SerializedName("new_attestor_id") val newAttestorId: String,
    @SerializedName("new_attestor_name") val newAttestorName: String,
    @SerializedName("new_attestor_login_email") val newAttestorLoginEmail: String,
    @SerializedName("total_attestors") val totalAttestors: Int,
    @SerializedName("threshold") val threshold: Int,
    @SerializedName("explanation") val explanation: String,
    @SerializedName("all_attestors") val allAttestors: List<AttestorItemDto>
)

data class AttestorListResponseDto(
    @SerializedName("owner_id") val ownerId: String,
    @SerializedName("total_attestors") val totalAttestors: Int,
    @SerializedName("attestors") val attestors: List<AttestorItemDto>
)

data class CryptoShareInfoDto(
    @SerializedName("attestor_id") val attestorId: String,
    @SerializedName("name") val name: String,
    @SerializedName("role") val role: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("share_value") val shareValue: String,
    @SerializedName("share_display") val shareDisplay: String,
    @SerializedName("has_submitted") val hasSubmitted: Boolean,
    @SerializedName("submission_timestamp") val submissionTimestamp: Long?
)

data class CryptoSessionProofDto(
    @SerializedName("owner_id") val ownerId: String,
    @SerializedName("vault_released") val vaultReleased: Boolean,
    @SerializedName("cryptographic_scheme") val cryptographicScheme: String,
    @SerializedName("threshold") val threshold: Int,
    @SerializedName("total_shares") val totalShares: Int,
    @SerializedName("shares_submitted_count") val sharesSubmittedCount: Int,
    @SerializedName("original_master_key_truncated") val originalMasterKeyTruncated: String,
    @SerializedName("original_master_key_hash") val originalMasterKeyHash: String,
    @SerializedName("shares") val shares: List<CryptoShareInfoDto>,
    @SerializedName("reconstructed_key_truncated") val reconstructedKeyTruncated: String?,
    @SerializedName("reconstructed_key_hash") val reconstructedKeyHash: String?,
    @SerializedName("hash_match_confirmed") val hashMatchConfirmed: Boolean,
    @SerializedName("contract_address") val contractAddress: String,
    @SerializedName("polygonscan_url") val polygonscanUrl: String,
    @SerializedName("chain") val chain: String
)

// --- Retrofit API Interface ---

interface VaultApiService {

    @POST("/auth/login")
    suspend fun login(@Body req: LoginRequestDto): Response<LoginResponseDto>

    @GET("/attestor/pending")
    suspend fun getPendingAttestations(@Query("attestor_id") attestorId: String): Response<PendingAttestationsResponseDto>

    @POST("/discovery/entry")
    suspend fun submitDiscoveryEntry(@Body req: DiscoveryEntryRequestDto): Response<StandardResponseDto>

    @POST("/vault/store")
    suspend fun storeVaultInstruction(@Body req: VaultStoreRequestDto): Response<StandardResponseDto>

    @POST("/checkin/respond")
    suspend fun respondCheckIn(@Body req: CheckinRespondRequestDto): Response<StandardResponseDto>

    @POST("/checkin/simulate-miss")
    suspend fun simulateCheckInMiss(@Query("owner_id") ownerId: String): Response<StandardResponseDto>

    @POST("/attestor/attest")
    suspend fun submitAttestation(@Body req: AttestorAttestRequestDto): Response<AttestorAttestResponseDto>

    @POST("/attestor/add")
    suspend fun addAttestor(@Body req: AddAttestorRequestDto): Response<AddAttestorResponseDto>

    @GET("/attestor/list")
    suspend fun listAttestors(@Query("owner_id") ownerId: String): Response<AttestorListResponseDto>

    @GET("/crypto/session-proof")
    suspend fun getCryptoSessionProof(@Query("owner_id") ownerId: String): Response<CryptoSessionProofDto>

    @GET("/release/status")
    suspend fun getReleaseStatus(@Query("owner_id") ownerId: String): Response<ReleaseStatusResponseDto>

    @GET("/blockchain/logs")
    suspend fun getBlockchainLogs(@Query("owner_id") ownerId: String): Response<BlockchainLogsResponseDto>

    @POST("/release/generate-document")
    suspend fun generateDocument(@Query("owner_id") ownerId: String): Response<GenerateDocumentResponseDto>

    @POST("/demo/reset")
    suspend fun resetDemo(): Response<StandardResponseDto>
}


// --- Retrofit Client Provider ---

object RetrofitClient {
    private val isEmulator: Boolean
        get() = android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for")

    // On physical phone over USB reverse tunnel, 127.0.0.1 routes to host Mac:8000
    // On Android emulator, 10.0.2.2 routes to host Mac:8000
    @Volatile
    var baseUrl: String = if (isEmulator) "http://10.0.2.2:8000/" else "http://127.0.0.1:8000/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val api: VaultApiService by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VaultApiService::class.java)
    }
}
