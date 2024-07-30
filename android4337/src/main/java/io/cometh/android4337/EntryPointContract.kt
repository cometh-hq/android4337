package io.cometh.android4337

import io.cometh.android4337.utils.requireHexAddress
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint192
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.protocol.Web3j
import org.web3j.protocol.Web3jService
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.tx.ClientTransactionManager
import java.io.IOException
import java.math.BigInteger


class EntryPointContract(
    private val web3jService: Web3jService,
    private val contractAddress: String = ENTRY_POINT_ADDRESS_V7
) {

    companion object {
        const val ENTRY_POINT_ADDRESS_V7 = "0x0000000071727De22E5E9d8BAf0edAc6f37da032"
    }

    init {
        contractAddress.requireHexAddress()
    }

    @Throws(IOException::class)
    fun getNonce(sender: Address): BigInteger? {
        val key = Uint192.DEFAULT
        val inputParams = listOf(sender, key)
        val outputParams = listOf(object : TypeReference<Uint256>() {})
        val function = Function("getNonce", inputParams, outputParams)
        val encodedFunction = FunctionEncoder.encode(function)
        val transactionManager = ClientTransactionManager(Web3j.build(web3jService), null)
        val value = transactionManager.sendCall(contractAddress, encodedFunction, DefaultBlockParameterName.LATEST)
        val result: List<Uint256> = FunctionReturnDecoder.decode(value, function.outputParameters) as List<Uint256>
        return result.firstOrNull()?.value
    }
}
