package io.cometh.android4337sdk

import io.cometh.android4337sdk.utils.toHex
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.DynamicBytes
import org.web3j.abi.datatypes.DynamicStruct
import org.web3j.abi.datatypes.generated.Uint256
import java.math.BigInteger

data class UserOperation(
    val sender: String,
    val nonce: BigInteger,
    val initCode: ByteArray,
    val callData: ByteArray,
    var callGasLimit: BigInteger,
    var verificationGasLimit: BigInteger,
    var preVerificationGas: BigInteger,
    var maxFeePerGas: BigInteger,
    var maxPriorityFeePerGas: BigInteger,
    var paymasterAndData: ByteArray,
    var signature: ByteArray? = null
) : DynamicStruct(
    Address(160, sender),
    Uint256(nonce),
    DynamicBytes(initCode),
    DynamicBytes(callData),
    Uint256(callGasLimit),
    Uint256(verificationGasLimit),
    Uint256(preVerificationGas),
    Uint256(maxFeePerGas),
    Uint256(maxPriorityFeePerGas),
    DynamicBytes(paymasterAndData),
    DynamicBytes(signature)
)

fun UserOperation.toEncodedMap(): Map<String, String> {
    val map = mutableMapOf(
        "sender" to this.sender,
        "nonce" to this.nonce.toHex(),
        "initCode" to this.initCode.toHex(),
        "callData" to this.callData.toHex(),
        "callGasLimit" to this.callGasLimit.toHex(),
        "verificationGasLimit" to this.verificationGasLimit.toHex(),
        "preVerificationGas" to this.preVerificationGas.toHex(),
        "maxFeePerGas" to this.maxFeePerGas.toHex(),
        "maxPriorityFeePerGas" to this.maxPriorityFeePerGas.toHex(),
        "paymasterAndData" to this.paymasterAndData.toHex(),
    )
    if (this.signature != null) map["signature"] = this.signature!!.toHex()
    return map
}
