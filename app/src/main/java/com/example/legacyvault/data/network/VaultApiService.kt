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

// --- Retrofit API Interface ---

interface VaultApiService {

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
    // 10.0.2.2 is Android Emulator's alias to host loopback localhost:8000
    @Volatile
    var baseUrl: String = "http://10.0.2.2:8000/"

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
