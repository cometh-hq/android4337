package io.cometh.android4337.connect

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor

data class DeviceData(val deviceType: String, val deviceId: String)
data class GetPasskeySignersByWalletAddressResponse(val success: Boolean = false, val webAuthnSigners: List<WebAuthnSigner> = emptyList())
data class WebAuthnSigner(
    val _id: String = "",
    val publicKeyId: String = "",
    val publicKeyX: String = "",
    val publicKeyY: String = "",
    val signerAddress: String = "",
    val isSharedWebAuthnSigner: Boolean = false
)

data class InitWalletRequest(
    val chainId: String,
    val walletAddress: String,
    val initiatorAddress: String,
    val publicKeyId: String? = null,
    val publicKeyX: String? = null,
    val publicKeyY: String? = null,
    val deviceData: DeviceData? = null
)

data class InitWalletResponse(val success: Boolean = false, val isNewWallet: Boolean = false)

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class CreateWebAuthnSignerRequest(
    val chainId: String,
    val walletAddress: String,
    val publicKeyId: String,
    val publicKeyX: String,
    val publicKeyY: String,
    val deviceData: DeviceData,
    val signerAddress: String,
    @get:JsonProperty("isSharedWebAuthnSigner") var isSharedWebAuthnSigner: Boolean
)

data class IsValidSignatureRequest(val chainId: String = "", val message: String = "", val signature: String = "")
data class IsValidSignatureResponse(val success: Boolean = false, val result: Boolean = false)

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
}

class ConnectApi(
    private val apiKey: String,
    private val baseUrl: String = "https://api.4337.cometh.io",
) {

    private val okClient: OkHttpClient = OkHttpClient.Builder().addInterceptor { chain ->
        val request = chain.request().newBuilder().addHeader("apiKey", apiKey).build()
        chain.proceed(request)
    }.addInterceptor(HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }).build()

    private val objectMapper: ObjectMapper = ObjectMapper().apply {
        this.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private suspend fun <T> executeRequest(
        request: Request, responseType: Class<T>
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        try {
            val response = okClient.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: return@withContext ApiResult.Error("Empty response body", response.code)
                val result = objectMapper.readValue(responseBody, responseType)
                ApiResult.Success(result)
            } else {
                val errorBody = response.body?.string()
                val errorMessage = if (errorBody != null) {
                    try {
                        objectMapper.readTree(errorBody).get("error").asText()
                    } catch (e: Exception) {
                        "Unknown error"
                    }
                } else {
                    "Unknown error"
                }
                ApiResult.Error(errorMessage, response.code)
            }
        } catch (e: Exception) {
            ApiResult.Error("Network error: ${e.message}")
        }
    }

    suspend fun initWallet(
        chainId: Int, smartAccountAddress: String, initiatorAddress: String, publicKeyId: String? = null, publicKeyX: String? = null, publicKeyY: String? = null, deviceData: DeviceData? = null
    ): ApiResult<InitWalletResponse> {
        val requestBody = objectMapper.writeValueAsString(
            InitWalletRequest(
                chainId = chainId.toString(),
                walletAddress = smartAccountAddress,
                initiatorAddress = initiatorAddress,
                publicKeyId = publicKeyId,
                publicKeyX = publicKeyX,
                publicKeyY = publicKeyY,
                deviceData = deviceData
            )
        ).toRequestBody("application/json".toMediaType())

        val request = Request.Builder().url("$baseUrl/wallet/init").post(requestBody).build()

        return executeRequest(request, InitWalletResponse::class.java)
    }

    suspend fun createWebAuthnSigner(
        chainId: Int, walletAddress: String, publicKeyId: String, publicKeyX: String, publicKeyY: String, deviceData: DeviceData, signerAddress: String, isSharedWebAuthnSigner: Boolean
    ): ApiResult<Unit> {
        val writeValueAsString = objectMapper.writeValueAsString(
            CreateWebAuthnSignerRequest(
                chainId = chainId.toString(),
                walletAddress = walletAddress,
                publicKeyId = publicKeyId,
                publicKeyX = publicKeyX,
                publicKeyY = publicKeyY,
                deviceData = deviceData,
                signerAddress = signerAddress,
                isSharedWebAuthnSigner = isSharedWebAuthnSigner
            )
        )
        val requestBody = writeValueAsString.toRequestBody("application/json".toMediaType())

        val request = Request.Builder().url("$baseUrl/webauthn-signer/create").post(requestBody).build()

        return executeRequest(request, Unit::class.java)
    }

    suspend fun getPasskeySignersByWalletAddress(walletAddress: String): ApiResult<List<WebAuthnSigner>> {
        val request = Request.Builder().url("$baseUrl/webauthn-signer/$walletAddress").get().build()

        return executeRequest(request, GetPasskeySignersByWalletAddressResponse::class.java).map { it.webAuthnSigners.toList() }
    }

    suspend fun isValidSignature(
        walletAddress: String, message: String, signature: String, chainId: Int
    ): ApiResult<Boolean> {
        val requestBody = objectMapper.writeValueAsString(
            IsValidSignatureRequest(
                chainId = chainId.toString(), message = message, signature = signature
            )
        ).toRequestBody("application/json".toMediaType())

        val request = Request.Builder().url("$baseUrl/wallet/is-valid-signature/$walletAddress").post(requestBody).build()

        return executeRequest(request, IsValidSignatureResponse::class.java).map { it.result }
    }
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Error -> this
}
