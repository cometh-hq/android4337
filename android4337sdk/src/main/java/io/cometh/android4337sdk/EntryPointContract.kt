package io.cometh.android4337sdk

import io.cometh.android4337sdk.utils.requireHexAddress
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint192
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.tx.TransactionManager
import java.io.IOException
import java.math.BigInteger

val ENTRY_POINT_ADDRESS = "0x5FF137D4b0FDCD49DcA30c7CF57E578a026d2789"

class EntryPointContract(
    private val transactionManager: TransactionManager,
    private val contractAddress: String = ENTRY_POINT_ADDRESS
) {
    init {
        contractAddress.requireHexAddress()
    }

    @Throws(IOException::class)
    fun getNonce(address: String): BigInteger? {
        val key = Uint192.DEFAULT
        val inputParams = listOf(Address(address), key)
        val outputParams = listOf(object : TypeReference<Uint256>() {})
        val function = Function("getNonce", inputParams, outputParams)
        val encodedFunction = FunctionEncoder.encode(function)
        val value = transactionManager.sendCall(contractAddress, encodedFunction, DefaultBlockParameterName.LATEST)
        val result: List<Uint256> = FunctionReturnDecoder.decode(value, function.outputParameters) as List<Uint256>
        return result.firstOrNull()?.value
    }
}
