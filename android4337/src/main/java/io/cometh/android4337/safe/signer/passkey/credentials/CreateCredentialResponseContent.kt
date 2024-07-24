package io.cometh.android4337.safe.signer.passkey.credentials

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.cometh.android4337.safe.signer.passkey.PassKeyUtils
import io.cometh.android4337.utils.decodeBase64
import java.math.BigInteger

data class CreateCredentialResponseContent @JsonCreator constructor(
    @JsonProperty("clientDataJSON") val clientDataJSON: String,
    @JsonProperty("attestationObject") val attestationObject: String,
    @JsonProperty("transports") val transports: List<String>,
    @JsonProperty("authenticatorData") val authenticatorData: String,
    @JsonProperty("publicKeyAlgorithm") val publicKeyAlgorithm: Int,
    @JsonProperty("publicKey") val publicKey: String,
)

fun CreateCredentialResponseContent.getPublicKeyCoordinates(): Pair<BigInteger, BigInteger> {
    return PassKeyUtils.publicKeyToXYCoordinates(publicKey.decodeBase64())
}
