package io.cometh.android4337.safe

import io.cometh.android4337.utils.hexToBigInt
import io.cometh.android4337.utils.hexToByteArray
import io.cometh.android4337.utils.toChecksumHex
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.DynamicBytes
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256

class DelayModuleFactory {
    companion object {
        // deployModule(address, bytes, uint256) returns (address)
        fun deployModuleFunctionData(
            singletonDelayAddress: Address,
            initializer: String,
            safeAddress: Address
        ): String {
            val inputParams = listOf(
                singletonDelayAddress,
                DynamicBytes(initializer.hexToByteArray()),
                Uint256(safeAddress.toChecksumHex().hexToBigInt())
            )
            val outputParams = listOf(TypeReference.create(Address::class.java))
            val function = Function("deployModule", inputParams, outputParams)
            return FunctionEncoder.encode(function)
        }
    }
}
