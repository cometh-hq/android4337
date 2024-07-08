package io.cometh.android4337.passkey.credentials

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class GetCredentialAuthenticationResponseContent  @JsonCreator constructor(
    @JsonProperty("clientDataJSON") val clientDataJSON: String,
    @JsonProperty("authenticatorData") val authenticatorData: String,
    @JsonProperty("signature") val signature: String,
    @JsonProperty("userHandle") val userHandle: String,
)