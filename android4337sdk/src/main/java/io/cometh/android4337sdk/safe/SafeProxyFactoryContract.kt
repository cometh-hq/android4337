package io.cometh.android4337sdk.safe

import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.DynamicBytes
import org.web3j.abi.datatypes.Function
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.tx.TransactionManager

class SafeProxyFactoryContract(
    private val transactionManager: TransactionManager,
    private val contractAddress: String
) {
    fun proxyCreationCode(): ByteArray? {
        val outputParams = listOf(object : TypeReference<DynamicBytes>() {})
        val function = Function("proxyCreationCode", mutableListOf(), outputParams)
        val encodedFunction = FunctionEncoder.encode(function)
        val value = transactionManager.sendCall(
            contractAddress,
            encodedFunction,
            DefaultBlockParameterName.LATEST
        )
        val result: List<DynamicBytes> = FunctionReturnDecoder.decode(value, function.outputParameters) as List<DynamicBytes>
        return result.firstOrNull()?.value
    }
}