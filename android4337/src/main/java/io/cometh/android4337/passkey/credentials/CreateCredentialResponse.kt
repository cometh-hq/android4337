package io.cometh.android4337.passkey.credentials

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

//{"rawId":"hWLZ-gI98By2z3fdrnqVeA","authenticatorAttachment":"platform","type":"public-key","id":"hWLZ-gI98By2z3fdrnqVeA","response":{"clientDataJSON":"eyJ0eXBlIjoid2ViYXV0aG4uY3JlYXRlIiwiY2hhbGxlbmdlIjoib0k4RHlaN3VnNjVzM2FZQnpjMEk0LTVTNi1qNzBkQWtCSTE5NEYyYXdwbyIsIm9yaWdpbiI6ImFuZHJvaWQ6YXBrLWtleS1oYXNoOnpRNEdTbDF5cGRZVC1VYk90WWQ3cGo1U1Nmc05lemRGc0Z4bU81aXExRWciLCJhbmRyb2lkUGFja2FnZU5hbWUiOiJuYy5zdGFydGFwcC5wYXNza2V5In0","attestationObject":"o2NmbXRkbm9uZWdhdHRTdG10oGhhdXRoRGF0YViUhxSe02D6T4-CHd4-hb2uT9qpcfEoMQaULLwv2jXyM0VdAAAAAOqbjWZNAR0hPOS2tIy1ddQAEIVi2foCPfActs933a56lXilAQIDJiABIVggFOnnbqXcCf7Vx1KPRy1FQzHrMcg-kS5-g8u0pmdxw9UiWCAv4jAT8BPjXht3jvAB7PL6KEQ49rtZc0J37-LDXMfDaA","transports":["internal","hybrid"],"authenticatorData":"hxSe02D6T4-CHd4-hb2uT9qpcfEoMQaULLwv2jXyM0VdAAAAAOqbjWZNAR0hPOS2tIy1ddQAEIVi2foCPfActs933a56lXilAQIDJiABIVggFOnnbqXcCf7Vx1KPRy1FQzHrMcg-kS5-g8u0pmdxw9UiWCAv4jAT8BPjXht3jvAB7PL6KEQ49rtZc0J37-LDXMfDaA","publicKeyAlgorithm":-7,"publicKey":"MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEFOnnbqXcCf7Vx1KPRy1FQzHrMcg-kS5-g8u0pmdxw9Uv4jAT8BPjXht3jvAB7PL6KEQ49rtZc0J37-LDXMfDaA"},"clientExtensionResults":{"credProps":{"rk":true}}}
data class CreateCredentialResponse @JsonCreator constructor(
    @JsonProperty("rawId") val rawId: String,
    @JsonProperty("authenticatorAttachment") val authenticatorAttachment: String,
    @JsonProperty("type") val type: String,
    @JsonProperty("id") val id: String,
    @JsonProperty("response") val response: CreateCredentialResponseContent,
    @JsonProperty("clientExtensionResults") val clientExtensionResults: Map<String, Any>,
)

