package io.cometh.android4337.safe

import io.cometh.android4337.utils.hexStringToByteArray
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.abi.datatypes.DynamicBytes
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
import java.math.BigInteger

object SafeUtils {
    fun getEnableModulesFunctionData(moduleAddresses: List<Address>): ByteArray {
        val inputParams = listOf(DynamicArray(Address::class.java, moduleAddresses))
        val outputParams = emptyList<TypeReference<*>>()
        val function = Function("enableModules", inputParams, outputParams)
        return FunctionEncoder.encode(function).hexStringToByteArray()
    }

    fun getSetupFunctionData(
        _owners: List<Address>,
        _threshold: BigInteger,
        to: Address,
        data: ByteArray,
        fallbackHandler: Address,
        paymentToken: Address,
        payment: BigInteger,
        paymentReceiver: Address
    ): ByteArray {
        val inputParams = listOf(
            DynamicArray(Address::class.java, _owners),
            Uint256(_threshold),
            to,
            DynamicBytes(data),
            fallbackHandler,
            paymentToken,
            Uint256(payment),
            paymentReceiver
        )
        val outputParams = emptyList<TypeReference<*>>()
        val function = Function("setup", inputParams, outputParams)
        return FunctionEncoder.encode(function).hexStringToByteArray()
    }

    fun getCreateProxyWithNonceFunctionData(
        _singleton: Address,
        initializer: ByteArray,
        saltNonce: BigInteger
    ): ByteArray {
        val inputParams = listOf(
            _singleton,
            DynamicBytes(initializer),
            Uint256(saltNonce)
        )
        val outputParams = emptyList<TypeReference<*>>()
        val function = Function("createProxyWithNonce", inputParams, outputParams)
        return FunctionEncoder.encode(function).hexStringToByteArray()
    }

}