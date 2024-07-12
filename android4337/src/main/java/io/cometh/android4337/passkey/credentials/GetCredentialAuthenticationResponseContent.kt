package io.cometh.android4337.passkey.credentials

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.cometh.android4337.utils.decodeBase64

data class GetCredentialAuthenticationResponseContent @JsonCreator constructor(
    @JsonProperty("clientDataJSON") val clientDataJSON: String,
    @JsonProperty("authenticatorData") val authenticatorData: String,
    @JsonProperty("signature") val signature: String,
    @JsonProperty("userHandle") val userHandle: String,
) {
    fun getClientDataJSONDecoded(): ByteArray {
        return clientDataJSON.decodeBase64()
    }

    fun getAuthenticatorDataDecoded(): ByteArray {
        return authenticatorData.decodeBase64()
    }

    fun getSignatureDecoded(): ByteArray {
        return signature.decodeBase64()
    }

    fun getUserHandleDecoded(): ByteArray {
        return userHandle.decodeBase64()
    }
}

