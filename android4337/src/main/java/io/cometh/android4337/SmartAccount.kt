package io.cometh.android4337

import androidx.annotation.WorkerThread
import io.cometh.android4337.bundler.BundlerClient
import io.cometh.android4337.gasprice.UserOperationGasPriceProvider
import io.cometh.android4337.paymaster.PaymasterClient
import io.cometh.android4337.safe.signer.Signer
import io.cometh.android4337.utils.hexToAddress
import io.cometh.android4337.utils.requireHexAddress
import io.cometh.android4337.utils.toChecksumHex
import io.cometh.android4337.utils.toHex
import org.web3j.abi.datatypes.Address
import org.web3j.protocol.Service
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import java.io.IOException
import java.math.BigInteger

sealed class SmartAccountException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Error(message: String, cause: Throwable? = null) : SmartAccountException(message, cause)
    class InvalidSignatureError(message: String, cause: Throwable? = null) : SmartAccountException(message, cause)
    class UserOpGasEstimationError(message: String, cause: Throwable? = null) : SmartAccountException(message, cause)
    class PaymasterError(message: String, cause: Throwable? = null) : SmartAccountException(message, cause)
    class SignerError(message: String, cause: Throwable? = null) : SmartAccountException(message, cause)
    class PredictAddressError(message: String, cause: Throwable? = null) : SmartAccountException(message, cause)
}

abstract class SmartAccount(
    protected val signer: Signer,
    protected val bundlerClient: BundlerClient,
    protected val gasPriceProvider: UserOperationGasPriceProvider,
    protected val entryPointAddress: String,
    protected val web3Service: Service,
    protected val paymasterClient: PaymasterClient? = null,
    val accountAddress: String,
) {

    protected val web3 = Web3j.build(web3Service)

    init {
        accountAddress.requireHexAddress()
        entryPointAddress.requireHexAddress()
    }

    @WorkerThread
    @Throws(SmartAccountException::class, IOException::class)
    fun sendUserOperation(
        to: Address,
        value: BigInteger = BigInteger.ZERO,
        data: ByteArray,
        delegateCall: Boolean = false
    ): String {
        val userOperation = prepareUserOperation(to, value, data, delegateCall).apply {
            signature = signOperation(this).toHex()
        }
        val result = bundlerClient.ethSendUserOperation(userOperation, entryPointAddress).send()
        if (result.hasError()) {
            if (result.error!!.message.contains("AA24")) {
                throw SmartAccountException.InvalidSignatureError("Invalid signature: ${result.error!!.code} ${result.error.message}")
            }
            throw SmartAccountException.Error("Bundler cannot send user operation, code: ${result.error!!.code} ${result.error.message}")
        }
        return result.result
    }

    @WorkerThread
    @Throws(SmartAccountException::class, IOException::class)
    fun prepareUserOperation(to: Address, value: BigInteger, data: ByteArray, delegateCall: Boolean = false): UserOperation {
        val isDeployed = isDeployed()
        val userOperation = UserOperation(
            sender = accountAddress,
            nonce = getNonce().toHex(),
            factory = if (!isDeployed) getFactoryAddress().toChecksumHex() else null,
            factoryData = if (!isDeployed) getFactoryData().toHex() else null,
            callData = getCallData(to, value, data, delegateCall).toHex(),
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

        userOperation.signature = signer.getDummySignature()
        val estimateResp = bundlerClient.ethEstimateUserOperationGas(userOperation, entryPointAddress).send()
        userOperation.signature = null
        if (estimateResp.hasError()) {
            throw SmartAccountException.UserOpGasEstimationError("Bundler cannot estimate user operation gas, code: ${estimateResp.error!!.code} ${estimateResp.error.message}")
        }
        userOperation.apply {
            preVerificationGas = estimateResp.result.preVerificationGas
            verificationGasLimit = estimateResp.result.verificationGasLimit
            callGasLimit = estimateResp.result.callGasLimit
        }

        if (paymasterClient != null) {
            userOperation.signature = signer.getDummySignature()
            val resp =
                paymasterClient.pmSponsorUserOperation(userOperation, entryPointAddress).send()
            userOperation.signature = null
            if (resp.hasError()) {
                throw SmartAccountException.PaymasterError("Paymaster cannot sponsor user operation, code: ${resp.error!!.code} ${resp.error.message}")
            }
            userOperation.apply {
                paymaster = resp.result.paymaster
                paymasterData = resp.result.paymasterData
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
        val entryPointContract = EntryPointContract(web3Service, entryPointAddress)
        val nonce = entryPointContract.getNonce(accountAddress.hexToAddress())
            ?: throw SmartAccountException.Error("Entry point contract ($entryPointAddress) getNonce() returns 0x for account address ($accountAddress)")
        return nonce
    }

    @WorkerThread
    @Throws(SmartAccountException::class, IOException::class)
    fun isDeployed(): Boolean {
        val result = web3.ethGetCode(accountAddress, DefaultBlockParameterName.LATEST).send()
        if (result.hasError()) {
            throw SmartAccountException.Error("Cannot get code for account address ($accountAddress), code: ${result.error!!.code} ${result.error.message}")
        }
        return result.code != "0x"
    }

    abstract fun signOperation(userOperation: UserOperation): ByteArray
    abstract fun getFactoryAddress(): Address
    abstract fun getFactoryData(): ByteArray
    abstract fun addOwner(owner: Address): String
    abstract fun deployAndEnablePasskeySigner(x: BigInteger, y: BigInteger): String
    abstract fun getCallData(to: Address, value: BigInteger, data: ByteArray, delegateCall: Boolean): ByteArray
}
