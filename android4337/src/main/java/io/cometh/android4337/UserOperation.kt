package io.cometh.android4337

import io.cometh.android4337.utils.requireHex

data class UserOperation(
    val sender: String,
    val nonce: String,
    val initCode: String,
    val callData: String,
    var callGasLimit: String,
    var verificationGasLimit: String,
    var preVerificationGas: String,
    var maxFeePerGas: String,
    var maxPriorityFeePerGas: String,
    var paymasterAndData: String,
    var signature: String? = null
) {
    init {
        sender.requireHex()
        nonce.requireHex()
        initCode.requireHex()
        callData.requireHex()
        callGasLimit.requireHex()
        verificationGasLimit.requireHex()
        preVerificationGas.requireHex()
        maxFeePerGas.requireHex()
        maxPriorityFeePerGas.requireHex()
        paymasterAndData.requireHex()
    }
}

fun UserOperation.toMap(): Map<String, String> {
    val map = mutableMapOf(
        "sender" to sender,
        "nonce" to nonce,
        "initCode" to initCode,
        "callData" to callData,
        "callGasLimit" to callGasLimit,
        "verificationGasLimit" to verificationGasLimit,
        "preVerificationGas" to preVerificationGas,
        "maxFeePerGas" to maxFeePerGas,
        "maxPriorityFeePerGas" to maxPriorityFeePerGas,
        "paymasterAndData" to paymasterAndData,
    )
    if (this.signature != null) map["signature"] = this.signature!!
    return map
}


