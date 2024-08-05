package io.cometh.android4337.safe

import io.cometh.android4337.utils.encode
import io.cometh.android4337.utils.requireHexAddress
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint176
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.protocol.Web3j
import org.web3j.protocol.Web3jService
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.tx.ClientTransactionManager
import java.math.BigInteger

class SafeWebAuthnSignerFactoryContract(
    web3jService: Web3jService,
    private val contractAddress: String
) {

    init {
        contractAddress.requireHexAddress()
    }

    private val transactionManager = ClientTransactionManager(Web3j.build(web3jService), null)

    fun getSignerFunction(
        x: BigInteger,
        y: BigInteger,
        verifiers: BigInteger
    ): Function {
        val inputParams = listOf(Uint256(x), Uint256(y), Uint176(verifiers))
        val outputParams = listOf(object : TypeReference<Address>() {})
        return Function("getSigner", inputParams, outputParams)
    }

    fun getSigner(
        x: BigInteger,
        y: BigInteger,
        verifiers: BigInteger
    ): Address? {
        val result = getSignerFunction(x, y, verifiers).let { fn ->
            fn.encode().let { encodedFn ->
                val value = transactionManager.sendCall(
                    contractAddress,
                    encodedFn,
                    DefaultBlockParameterName.LATEST
                )
                FunctionReturnDecoder.decode(value, fn.outputParameters) as List<Address>
            }
        }
        return result.firstOrNull()
    }

    fun createSignerFunction(
        x: BigInteger,
        y: BigInteger,
        verifiers: BigInteger
    ): Function {
        val inputParams = listOf(Uint256(x), Uint256(y), Uint176(verifiers))
        val outputParams = listOf(object : TypeReference<Address>() {})
        return Function("createSigner", inputParams, outputParams)
    }

}
