package io.cometh.android4337sdk

import androidx.annotation.WorkerThread
import io.cometh.android4337sdk.bundler.BundlerClient
import io.cometh.android4337sdk.gasprice.UserOperationGasPriceProvider
import io.cometh.android4337sdk.paymaster.PaymasterClient
import io.cometh.android4337sdk.utils.hexStringToByteArray
import io.cometh.android4337sdk.utils.requireHex
import io.cometh.android4337sdk.utils.requireHexAddress
import io.cometh.android4337sdk.utils.toAddress
import org.web3j.abi.datatypes.Address
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.tx.TransactionManager
import java.io.IOException
import java.math.BigInteger

val EMPTY_BYTE_ARRAY = byteArrayOf()

class SmartAccountException(message: String) : Exception(message)

abstract class SmartAccount(
    protected val credentials: Credentials,
    protected val bundlerClient: BundlerClient,
    protected val gasPriceProvider: UserOperationGasPriceProvider,
    protected val entryPointAddress: String,
    protected val web3: Web3j,
    protected val paymasterClient: PaymasterClient? = null,
    val accountAddress: String,
    protected val transactionManager: TransactionManager,
) {

    init {
        accountAddress.requireHexAddress()
        entryPointAddress.requireHexAddress()
    }

    fun isDeployed(): Boolean {
        val result = web3.ethGetCode(accountAddress, DefaultBlockParameterName.LATEST).send()
        return result.code != "0x"
    }

    @WorkerThread
    @Throws(SmartAccountException::class, IOException::class)
    fun sendTransaction(to: String, value: BigInteger, data: String): String {
        data.requireHex()
        to.requireHexAddress()

        val userOperation = prepareUserOperation(to, value, data).apply {
            signature = signOperation(this, entryPointAddress)
        }
        val result = bundlerClient.ethSendUserOperation(userOperation, entryPointAddress).send()
        if (result.hasError()) {
            throw SmartAccountException("Bundler cannot send user operation, code: ${result.error!!.code} ${result.error.message}")
        }
        return result.result
    }

    @WorkerThread
    fun prepareUserOperation(to: String, value: BigInteger, data: String): UserOperation {
        val nonce = getNonce()
        val callData = getCallData(to.toAddress(), value, data.hexStringToByteArray())
        val initCode = if (isDeployed()) EMPTY_BYTE_ARRAY else getInitCode()
        val userOperation = UserOperation(
            sender = accountAddress,
            nonce = nonce,
            initCode = initCode,
            callData = callData,
            callGasLimit = BigInteger.ZERO,
            verificationGasLimit = BigInteger.ZERO,
            preVerificationGas = BigInteger.ZERO,
            maxFeePerGas = BigInteger.ZERO,
            maxPriorityFeePerGas = BigInteger.ZERO,
            paymasterAndData = EMPTY_BYTE_ARRAY
        )

        gasPriceProvider.getGasPrice().let { gasPrice ->
            userOperation.apply {
                maxFeePerGas = gasPrice.maxFeePerGas
                maxPriorityFeePerGas = gasPrice.maxPriorityFeePerGas
            }
        }

        if (paymasterClient != null) {
            val resp = paymasterClient.pmSponsorUserOperation(userOperation, entryPointAddress).send()
            if (resp.hasError()) {
                throw SmartAccountException("Paymaster cannot sponsor user operation, code: ${resp.error!!.code} ${resp.error.message}")
            }
            userOperation.apply {
                val result = resp.result
                paymasterAndData = result.getPaymasterAndData()
                preVerificationGas = result.getPreVerificationGas()
                verificationGasLimit = result.getVerificationGasLimit()
                callGasLimit = result.getCallGasLimit()
            }
        } else {
            val resp = bundlerClient.ethEstimateUserOperationGas(userOperation, entryPointAddress).send()
            if (resp.hasError()) {
                throw SmartAccountException("Bundler cannot estimate user operation gas, code: ${resp.error!!.code} ${resp.error.message}")
            }
            val result = resp.result
            userOperation.apply {
                preVerificationGas = result.getPreVerificationGas()
                verificationGasLimit = result.getVerificationGasLimit()
                callGasLimit = result.getCallGasLimit()
            }
        }

        return userOperation
    }

    @WorkerThread
    @Throws(SmartAccountException::class, IOException::class)
    fun getNonce(): BigInteger {
        val entryPointContract = EntryPointContract(transactionManager, entryPointAddress)
        val nonce = entryPointContract.getNonce(accountAddress)
            ?: throw SmartAccountException("Entry point contract ($entryPointAddress) getNonce() returns 0x for account address ($accountAddress)")
        return nonce
    }

    abstract fun signOperation(userOperation: UserOperation, entryPointAddress: String): ByteArray
    abstract fun getCallData(to: Address, value: BigInteger, data: ByteArray): ByteArray
    abstract fun getInitCode(): ByteArray
}
