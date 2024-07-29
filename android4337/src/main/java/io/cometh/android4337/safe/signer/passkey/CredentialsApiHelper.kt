package io.cometh.android4337.safe.signer.passkey

import android.content.Context
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import com.fasterxml.jackson.databind.ObjectMapper
import io.cometh.android4337.safe.signer.passkey.credentials.CreateCredentialResponse
import io.cometh.android4337.safe.signer.passkey.credentials.GetCredentialAuthenticationResponse
import io.cometh.android4337.utils.encodeBase64

class CredentialsApiHelper(
    private val context: Context,
    private val credentialManager: CredentialManager = CredentialManager.create(context)
) {

    /**
     * @throws GetCredentialException
     */
    suspend fun getCredential(rpId: String, challenge: ByteArray): GetCredentialAuthenticationResponse {
        val json = """
        {
          "challenge": "${challenge.encodeBase64()}",
          "timeout": 1800000,
          "userVerification": "required",
          "rpId": "$rpId"
        }
    """.trimIndent()

        val getPublicKeyCredentialOption = GetPublicKeyCredentialOption(json, null)
        val getCredentialRequest = GetCredentialRequest(
            listOf(
                getPublicKeyCredentialOption,
            )
        )
        val response = credentialManager.getCredential(context, getCredentialRequest)
        val responseJson = (response.credential as PublicKeyCredential).authenticationResponseJson
        return ObjectMapper().readValue(responseJson, GetCredentialAuthenticationResponse::class.java)
    }

    suspend fun createCredential(rpId: String, rpName: String, userId: String, userName: String, challenge: ByteArray): CreateCredentialResponse {
        val request = CreatePublicKeyCredentialRequest(
            requestJson = createJsonFor(
                rpId = rpId,
                rpName = rpName,
                userId = userId,
                userName = userName,
                challenge = challenge
            ),
            preferImmediatelyAvailableCredentials = true,
        )
        val response = credentialManager.createCredential(context, request) as CreatePublicKeyCredentialResponse
        val responseJson = ObjectMapper().readValue(response.registrationResponseJson, CreateCredentialResponse::class.java)
        return responseJson
    }

    fun createJsonFor(
        rpId: String,
        rpName: String,
        userId: String,
        userName: String,
        challenge: ByteArray
    ): String {
        return json
            .replace("<rpId>", rpId)
            .replace("<rpName>", rpName)
            .replace("<userId>", userId) // 64 bytes
            .replace("<userName>", userName)
            .replace("<userDisplayName>", userName)
            .replace("<challenge>", challenge.encodeBase64())
    }


    val json = """
    {
        "challenge": "<challenge>",
        "rp": {
            "id": "<rpId>",
            "name": "<rpName>"
        },
        "pubKeyCredParams": [
            {
                "type": "public-key",
                "alg": -7
            },
            {
                "type": "public-key",
                "alg": -257
            }
        ],
        "authenticatorSelection": {
            "authenticatorAttachment": "platform",
            "residentKey": "required"
        },
        "user": {
               "id": "<userId>",
               "name": "<userName>",
               "displayName": "<userDisplayName>"
        }
    }
""".trimIndent()


}