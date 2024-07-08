package io.cometh.android4337.safe

import io.cometh.android4337.utils.hexToByteArray
import io.cometh.android4337.utils.toBytes
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.abi.datatypes.DynamicBytes
import org.web3j.abi.datatypes.DynamicStruct
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint176
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.utils.Numeric
import java.math.BigInteger

object Safe {
    fun getEnableModulesFunctionData(moduleAddresses: List<Address>): ByteArray {
        val inputParams = listOf(DynamicArray(Address::class.java, moduleAddresses))
        val outputParams = emptyList<TypeReference<*>>()
        val function = Function("enableModules", inputParams, outputParams)
        return FunctionEncoder.encode(function).hexToByteArray()
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
        return FunctionEncoder.encode(function).hexToByteArray()
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
        return FunctionEncoder.encode(function).hexToByteArray()
    }

    // multiSend
    fun getMultiSendFunctionData(
        safeModuleSetupAddress: Address,
        safeWebAuthnSharedSignerAddress: Address,
        enableModuleData: ByteArray,
        sharedSignerConfigureData: ByteArray
    ): ByteArray {
        val encodedMultiSendTxs = encodeMultiSendTransactions(
            listOf(
                MultiSendTransaction(1, safeModuleSetupAddress, BigInteger.ZERO, enableModuleData),
                MultiSendTransaction(1, safeWebAuthnSharedSignerAddress, BigInteger.ZERO, sharedSignerConfigureData)
            )
        )
        val inputParams = listOf(DynamicBytes(encodedMultiSendTxs))
        val outputParams = emptyList<TypeReference<*>>()
        val function = Function("multiSend", inputParams, outputParams)
        return FunctionEncoder.encode(function).hexToByteArray()
    }

    fun encodeMultiSendTransactions(transactions: List<MultiSendTransaction>): ByteArray {
        //'uint8', 'address', 'uint256', 'uint256', 'bytes'
        val result = transactions.map {
            val op = Numeric.toBytesPadded(it.op.toBigInteger(), 1)
            val to = it.to.toBytes()
            val value = Numeric.toBytesPadded(it.value, 32)
            val size = Numeric.toBytesPadded(it.data.size.toBigInteger(), 32)
            val data = it.data
            return@map op + to + value + size + data
        }
        return result.reduce { acc, bytes -> acc + bytes }
    }

    fun getSharedSignerConfigureCallData(
        x: BigInteger,
        y: BigInteger,
        verifiers: BigInteger
    ): ByteArray {
        val inputParams = listOf(
            DynamicStruct(Uint256(x), Uint256(y), Uint176(verifiers))
        )
        val outputParams = emptyList<TypeReference<*>>()
        val function = Function("configure", inputParams, outputParams)
        return FunctionEncoder.encode(function).hexToByteArray()
    }

}

data class MultiSendTransaction(
    val op: Int,
    val to: Address,
    val value: BigInteger?,
    val data: ByteArray
) {
    init {
        require(op in 1..2)
    }
}