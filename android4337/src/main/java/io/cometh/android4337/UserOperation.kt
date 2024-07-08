package io.cometh.android4337

import io.cometh.android4337.utils.hexToAddress
import io.cometh.android4337.utils.hexToBigInt
import io.cometh.android4337.utils.hexToByteArray
import io.cometh.android4337.utils.removeOx
import io.cometh.android4337.utils.requireHex
import io.cometh.android4337.utils.requireHexAddress
import io.cometh.android4337.web3j.AbiEncoder
import org.web3j.abi.datatypes.DynamicBytes
import org.web3j.abi.datatypes.generated.Uint128

data class UserOperation(
    val sender: String,
    val nonce: String,
    val factory: String? = null,
    val factoryData: String? = null,
    val callData: String,
    var callGasLimit: String,
    var verificationGasLimit: String,
    var preVerificationGas: String,
    var maxFeePerGas: String,
    var maxPriorityFeePerGas: String,
    var paymaster: String? = null,
    var paymasterData: String? = null,
    var paymasterVerificationGasLimit: String? = null,
    var paymasterPostOpGasLimit: String? = null,
    var signature: String? = null
) {
    init {
        sender.requireHex()
        nonce.requireHex()
        callData.requireHex()
        callGasLimit.requireHex()
        verificationGasLimit.requireHex()
        preVerificationGas.requireHex()
        maxFeePerGas.requireHex()
        maxPriorityFeePerGas.requireHex()
        factory?.requireHexAddress()
        factoryData?.requireHex()
        paymaster?.requireHexAddress()
        paymasterData?.requireHex()
        paymasterVerificationGasLimit?.requireHex()
        paymasterPostOpGasLimit?.requireHex()
    }
}

fun UserOperation.getInitCode(): String {
    if (factory == null || factoryData == null) return "0x"
    val initCode = "0x${factory.lowercase().removeOx()}${factoryData.removeOx()}"
    return initCode
}

fun UserOperation.getPaymasterAndData(): String {
    if (paymaster == null || paymasterData == null || paymasterVerificationGasLimit == null || paymasterPostOpGasLimit == null) return "0x"
    //["address", "uint128", "uint128", "bytes"]
    val encoded = AbiEncoder.encodePackedParameters(
        listOf(
            paymaster!!.hexToAddress(),
            Uint128(paymasterVerificationGasLimit!!.hexToBigInt()),
            Uint128(paymasterPostOpGasLimit!!.hexToBigInt()),
            DynamicBytes(paymasterData!!.hexToByteArray())
        )
    )
    return encoded
}

fun UserOperation.toMap(): Map<String, String> {
    val map = mutableMapOf(
        "sender" to sender,
        "nonce" to nonce,
        "callData" to callData,
        "callGasLimit" to callGasLimit,
        "verificationGasLimit" to verificationGasLimit,
        "preVerificationGas" to preVerificationGas,
        "maxFeePerGas" to maxFeePerGas,
        "maxPriorityFeePerGas" to maxPriorityFeePerGas,
    )
    if (this.factory != null) map["factory"] = this.factory
    if (this.factoryData != null) map["factoryData"] = this.factoryData
    if (this.signature != null) map["signature"] = this.signature!!
    if (this.paymaster != null) {
        map["paymaster"] = this.paymaster!!
        if (this.paymasterVerificationGasLimit != null) map["paymasterVerificationGasLimit"] = this.paymasterVerificationGasLimit!!
        if (this.paymasterPostOpGasLimit != null) map["paymasterPostOpGasLimit"] = this.paymasterPostOpGasLimit!!
        if (this.paymasterData != null) map["paymasterData"] = this.paymasterData!!
    }
    return map
}


