package io.cometh.android4337.passkey.credentials

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class GetCredentialAuthenticationResponse @JsonCreator constructor(
    @JsonProperty("rawId") val rawId: String,
    @JsonProperty("authenticatorAttachment") val authenticatorAttachment: String,
    @JsonProperty("type") val type: String,
    @JsonProperty("id") val id: String,
    @JsonProperty("response") val response: GetCredentialAuthenticationResponseContent,
    @JsonProperty("clientExtensionResults") val clientExtensionResults: Map<String, String>,
)