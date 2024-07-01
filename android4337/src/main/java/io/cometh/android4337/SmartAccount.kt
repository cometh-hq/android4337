package io.cometh.android4337

import androidx.annotation.WorkerThread
import io.cometh.android4337.bundler.BundlerClient
import io.cometh.android4337.gasprice.UserOperationGasPriceProvider
import io.cometh.android4337.paymaster.PaymasterClient
import io.cometh.android4337.utils.requireHexAddress
import io.cometh.android4337.utils.toChecksumHex
import io.cometh.android4337.utils.toHex
import org.web3j.abi.datatypes.Address
import org.web3j.crypto.Credentials
import org.web3j.protocol.Service
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.tx.TransactionManager
import java.io.IOException
import java.math.BigInteger

class SmartAccountException(message: String) : Exception(message)

abstract class SmartAccount(
    protected val credentials: Credentials,
    protected val bundlerClient: BundlerClient,
    protected val gasPriceProvider: UserOperationGasPriceProvider,
    protected val entryPointAddress: String,
    protected val web3Service: Service,
    protected val paymasterClient: PaymasterClient? = null,
    val accountAddress: String,
    protected val transactionManager: TransactionManager,
) {

    protected val web3 = Web3j.build(web3Service)

    init {
        accountAddress.requireHexAddress()
        entryPointAddress.requireHexAddress()
    }

    @WorkerThread
    @Throws(SmartAccountException::class, IOException::class)
    fun sendUserOperation(to: Address, value: BigInteger, data: ByteArray): String {
        val userOperation = prepareUserOperation(to, value, data).apply {
            signature = signOperation(this, entryPointAddress).toHex()
        }
        val result = bundlerClient.ethSendUserOperation(userOperation, entryPointAddress).send()
        if (result.hasError()) {
            throw SmartAccountException("Bundler cannot send user operation, code: ${result.error!!.code} ${result.error.message}")
        }
        return result.result
    }

    @WorkerThread
    @Throws(SmartAccountException::class, IOException::class)
    fun prepareUserOperation(to: Address, value: BigInteger, data: ByteArray): UserOperation {
        val isDeployed = isDeployed()
        val userOperation = UserOperation(
            sender = accountAddress,
            nonce = getNonce().toHex(),
            factory = if (!isDeployed) getFactoryAddress().toChecksumHex() else null,
            factoryData = if (!isDeployed) getFactoryData().toHex() else null,
            callData = getCallData(to, value, data).toHex(),
            callGasLimit = "0x0",
            verificationGasLimit = "0x0",
            preVerificationGas = "0x0",
            maxFeePerGas = "0x0",
            maxPriorityFeePerGas = "0x0",
        )

        gasPriceProvider.getGasPrice().let { gasPrice ->
            userOperation.apply {
                maxFeePerGas = gasPrice.maxFeePerGas.toHex()
                maxPriorityFeePerGas = gasPrice.maxPriorityFeePerGas.toHex()
            }
        }

        val estimateResp = bundlerClient.ethEstimateUserOperationGas(userOperation, entryPointAddress).send()
        if (estimateResp.hasError()) {
            throw SmartAccountException("Bundler cannot estimate user operation gas, code: ${estimateResp.error!!.code} ${estimateResp.error.message}")
        }
        userOperation.apply {
            preVerificationGas = estimateResp.result.preVerificationGas
            verificationGasLimit = estimateResp.result.verificationGasLimit
            callGasLimit = estimateResp.result.callGasLimit
        }

        if (paymasterClient != null) {
            val resp = paymasterClient.pmSponsorUserOperation(userOperation, entryPointAddress).send()
            if (resp.hasError()) {
                throw SmartAccountException("Paymaster cannot sponsor user operation, code: ${resp.error!!.code} ${resp.error.message}")
            }
            userOperation.apply {
                paymaster = resp.result.paymaster
                paymasterAndData = resp.result.paymasterAndData
                paymasterVerificationGasLimit = resp.result.paymasterVerificationGasLimit
                paymasterPostOpGasLimit = resp.result.paymasterPostOpGasLimit
                preVerificationGas = resp.result.preVerificationGas
                verificationGasLimit = resp.result.verificationGasLimit
                callGasLimit = resp.result.callGasLimit
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

    @WorkerThread
    @Throws(SmartAccountException::class, IOException::class)
    fun isDeployed(): Boolean {
        val result = web3.ethGetCode(accountAddress, DefaultBlockParameterName.LATEST).send()
        if (result.hasError()) {
            throw SmartAccountException("Cannot get code for account address ($accountAddress), code: ${result.error!!.code} ${result.error.message}")
        }
        return result.code != "0x"
    }

    abstract fun signOperation(userOperation: UserOperation, entryPointAddress: String): ByteArray
    abstract fun getCallData(to: Address, value: BigInteger, data: ByteArray): ByteArray
    abstract fun getFactoryAddress(): Address
    abstract fun getFactoryData(): ByteArray
}
