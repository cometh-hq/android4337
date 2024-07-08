package io.cometh.android4337.passkey

import android.content.Context
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import com.fasterxml.jackson.databind.ObjectMapper
import io.cometh.android4337.passkey.credentials.CreateCredentialResponse
import io.cometh.android4337.passkey.credentials.GetCredentialAuthenticationResponse
import io.cometh.android4337.utils.encodeBase64

class CredentialsApiHelper(
    private val context: Context,
    private val credentialManager: CredentialManager,
) {

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
        // {"rawId":"SIsClfklbN4shqcF8T_U8A","authenticatorAttachment":"platform","type":"public-key","id":"SIsClfklbN4shqcF8T_U8A","response":{"clientDataJSON":"eyJ0eXBlIjoid2ViYXV0aG4uZ2V0IiwiY2hhbGxlbmdlIjoiTUhoaFlXRmhZV0ZoWVdGaFlXRmhZUSIsIm9yaWdpbiI6ImFuZHJvaWQ6YXBrLWtleS1oYXNoOnpRNEdTbDF5cGRZVC1VYk90WWQ3cGo1U1Nmc05lemRGc0Z4bU81aXExRWciLCJhbmRyb2lkUGFja2FnZU5hbWUiOiJuYy5zdGFydGFwcC5wYXNza2V5In0","authenticatorData":"hxSe02D6T4-CHd4-hb2uT9qpcfEoMQaULLwv2jXyM0UdAAAAAA","signature":"MEQCIAZnp2j6bRUj49CFmhuHI_RKh_8puFto169kkI5mLsq8AiALHKJ9q5ogwIKKyxuA2GEyY-SAH5WIqpzoOno0T4FONQ","userHandle":"JZfBAT24CGvl_LW1osiU6XgfNZjYPcNF9wazM6v4IQbvHzRRvjr6WWekJGaOGzg0Cvjwaa4MOSfWxbuT98eeGQ"},"clientExtensionResults":{}}
        val authResponse = ObjectMapper().readValue(responseJson, GetCredentialAuthenticationResponse::class.java)
        // {"type":"webauthn.get","challenge":"MHhhYWFhYWFhYWFhYWFhYQ","origin":"android:apk-key-hash:zQ4GSl1ypdYT-UbOtYd7pj5SSfsNezdFsFxmO5iq1Eg","androidPackageName":"nc.startapp.passkey"}
        return authResponse
    }

    suspend fun createCredential(rpId: String, rpName: String, userId: String, userName: String, challenge: ByteArray): CreateCredentialResponse? {
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