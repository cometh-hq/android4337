package io.cometh.android4337.safe

import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.DynamicBytes
import org.web3j.abi.datatypes.Function
import org.web3j.protocol.Web3j
import org.web3j.protocol.Web3jService
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.tx.ClientTransactionManager

class SafeProxyFactoryContract(
    web3jService: Web3jService,
    private val contractAddress: String
) {
    private val transactionManager = ClientTransactionManager(Web3j.build(web3jService), null)

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