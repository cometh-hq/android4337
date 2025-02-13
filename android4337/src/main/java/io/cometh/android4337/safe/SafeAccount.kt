package io.cometh.android4337.safe

import android.util.Log
import androidx.annotation.WorkerThread
import io.cometh.android4337.EntryPointContract
import io.cometh.android4337.SmartAccount
import io.cometh.android4337.SmartAccountException
import io.cometh.android4337.TransactionParams
import io.cometh.android4337.UserOperation
import io.cometh.android4337.bundler.BundlerClient
import io.cometh.android4337.gasprice.RPCGasEstimator
import io.cometh.android4337.gasprice.UserOperationGasPriceProvider
import io.cometh.android4337.getInitCode
import io.cometh.android4337.getPaymasterAndData
import io.cometh.android4337.paymaster.PaymasterClient
import io.cometh.android4337.safe.signer.Signer
import io.cometh.android4337.safe.signer.SignerException
import io.cometh.android4337.safe.signer.eoa.EOASigner
import io.cometh.android4337.safe.signer.passkey.PasskeySigner
import io.cometh.android4337.utils.encode
import io.cometh.android4337.utils.hexToAddress
import io.cometh.android4337.utils.hexToBigInt
import io.cometh.android4337.utils.hexToByteArray
import io.cometh.android4337.utils.isDeployed
import io.cometh.android4337.utils.requireHex
import io.cometh.android4337.utils.requireHexAddress
import io.cometh.android4337.utils.toChecksumHex
import io.cometh.android4337.utils.toHex
import io.cometh.android4337.web3j.AbiEncoder
import io.cometh.android4337.web3j.Create2
import io.cometh.android4337.web3j.Sign
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.abi.datatypes.DynamicBytes
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Bytes32
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.abi.datatypes.generated.Uint48
import org.web3j.abi.datatypes.generated.Uint8
import org.web3j.crypto.Hash
import org.web3j.protocol.Service
import org.web3j.protocol.Web3j
import org.web3j.protocol.Web3jService
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.tx.ClientTransactionManager
import org.web3j.tx.exceptions.ContractCallException
import java.io.IOException
import java.math.BigInteger

class SafeAccount private constructor(
    signer: Signer,
    val safeAddress: String,
    bundlerClient: BundlerClient,
    gasPriceProvider: UserOperationGasPriceProvider,
    entryPointAddress: String,
    web3Service: Service,
    private val chainId: Int,
    private val config: SafeConfig,
    paymasterClient: PaymasterClient? = null,
) : SmartAccount(
    signer,
    bundlerClient,
    gasPriceProvider,
    entryPointAddress,
    web3Service,
    paymasterClient,
    safeAddress
) {

    init {
        safeAddress.requireHexAddress()
    }

    companion object {
        fun fromAddress(
            address: String,
            signer: Signer,
            bundlerClient: BundlerClient,
            chainId: Int,
            web3Service: Service,
            config: SafeConfig = SafeConfig.getDefaultConfig(),
            entryPointAddress: String = EntryPointContract.ENTRY_POINT_ADDRESS_V7,
            paymasterClient: PaymasterClient? = null,
            gasPriceProvider: UserOperationGasPriceProvider = RPCGasEstimator(web3Service),
        ): SafeAccount {
            return SafeAccount(
                signer,
                address,
                bundlerClient,
                gasPriceProvider,
                entryPointAddress,
                web3Service,
                chainId,
                config,
                paymasterClient,
            )
        }

        @WorkerThread
        @Throws(IOException::class, RuntimeException::class)
        fun createNewAccount(
            signer: Signer,
            bundlerClient: BundlerClient,
            chainId: Int,
            web3Service: Service,
            config: SafeConfig = SafeConfig.getDefaultConfig(),
            entryPointAddress: String = EntryPointContract.ENTRY_POINT_ADDRESS_V7,
            paymasterClient: PaymasterClient? = null,
            gasPriceProvider: UserOperationGasPriceProvider = RPCGasEstimator(web3Service),
        ): SafeAccount {
            val predictedAddress = predictAddress(
                signer = signer,
                web3Service = web3Service,
                config = config,
            )
            return SafeAccount(
                signer,
                predictedAddress,
                bundlerClient,
                gasPriceProvider,
                entryPointAddress,
                web3Service,
                chainId,
                config,
                paymasterClient,
            )
        }

        @WorkerThread
        @Throws(IOException::class, SmartAccountException::class)
        fun predictAddress(
            signer: Signer,
            web3Service: Web3jService,
            config: SafeConfig = SafeConfig.getDefaultConfig(),
        ): String {
            val safeInitializer = when (signer) {
                is EOASigner -> Safe.getSafeInitializer(signer.getAddress().hexToAddress(), config)
                is PasskeySigner -> {
                    signer.passkey.let { passkey ->
                        Safe.getSafeInitializerWithPasskey(config, passkey)
                    }
                }

                else -> throw SmartAccountException.Error("Unsupported signer type")
            }
            val nonce = BigInteger.ZERO
            val keccak256Setup = Hash.sha3(safeInitializer)
            val saltHash = AbiEncoder.encodePackedParameters(listOf(Bytes32(keccak256Setup), Uint256(nonce)))
            val salt = Hash.sha3(saltHash)

            val safeProxyContract = SafeProxyFactoryContract(web3Service, config.safeProxyFactoryAddress)
            val proxyCreationCode = safeProxyContract.proxyCreationCode() ?: throw SmartAccountException.Error("Failed to get proxy creation code")
            val deploymentCode = AbiEncoder.encodePackedParameters(
                listOf(DynamicBytes(proxyCreationCode), Uint256(config.safeSingletonL2Address.hexToBigInt()))
            )
            val keccak256DeploymentCode = Hash.sha3(deploymentCode)
            val proxyAddress = Create2.getCreate2Address(config.safeProxyFactoryAddress, salt, keccak256DeploymentCode)
            return proxyAddress
        }

    }

    private val transactionManager = ClientTransactionManager(Web3j.build(web3Service), null)

    override fun signUserOperation(
        userOperation: UserOperation
    ): ByteArray {
        val validAfter = BigInteger.ZERO
        val validUntil = BigInteger.ZERO
        val json = buildSafeOperationJsonEip712V7(
            chainId = chainId,
            verifyingContract = config.safe4337ModuleAddress,
            sender = userOperation.sender,
            nonce = userOperation.nonce,
            initCode = userOperation.getInitCode(),
            callData = userOperation.callData,
            verificationGasLimit = userOperation.verificationGasLimit,
            callGasLimit = userOperation.callGasLimit,
            preVerificationGas = userOperation.preVerificationGas,
            maxFeePerGas = userOperation.maxFeePerGas,
            maxPriorityFeePerGas = userOperation.maxPriorityFeePerGas,
            paymasterAndData = userOperation.getPaymasterAndData(),
            validAfter = validAfter.toHex(),
            validUntil = validUntil.toHex(),
            entryPointAddress = entryPointAddress
        )

        val hashData = Sign.hashTypedData(json)
        val signatureData = try {
            signer.sign(hashData)
        } catch (e: SignerException) {
            throw SmartAccountException.SignerError("Failed to sign operation", e.cause)
        }
        val signature = AbiEncoder.encodePackedParameters(
            listOf(
                Uint48(validAfter),
                Uint48(validUntil),
                DynamicBytes(signatureData)
            )
        )
        return signature.hexToByteArray()
    }

    fun sendUserOperation(transactionParams: List<TransactionParams>): String {
        return Safe.getMultiSendFunctionData(
            transactionParams.map {
                MultiSendTransaction(
                    if (it.delegateCall) 1 else 0,
                    it.to.hexToAddress(),
                    it.value.hexToBigInt(),
                    it.data.hexToByteArray()
                )
            }
        ).let { multiSendData ->
            sendUserOperation(
                to = config.getSafeMultiSendAddress(),
                data = multiSendData,
                delegateCall = true
            )
        }
    }

    @WorkerThread
    fun getOwners(): List<Address>? {
        val inputParams = emptyList<Type<*>>()
        val outputParams = listOf(object : TypeReference<DynamicArray<Address>>() {})
        val function = Function("getOwners", inputParams, outputParams)
        val encodedFunction = FunctionEncoder.encode(function)
        val value = transactionManager.sendCall(
            accountAddress, encodedFunction, DefaultBlockParameterName.LATEST
        )
        val result: List<DynamicArray<Address>> = FunctionReturnDecoder.decode(
            value, function.outputParameters
        ) as List<DynamicArray<Address>>
        return result.firstOrNull()?.value
    }

    override fun getCallData(to: Address, value: BigInteger, data: ByteArray, delegateCall: Boolean): ByteArray {
        val inputParams = listOf(
            to,
            Uint256(value),
            DynamicBytes(data),
            Uint8(if (delegateCall) BigInteger.ONE else BigInteger.ZERO),
        )
        val outputParams = listOf(object : TypeReference<Uint256>() {})
        return Function("executeUserOp", inputParams, outputParams).encode().hexToByteArray()
    }

    override fun signMessage(message: String): ByteArray {
        message.requireHex()
        val json = buildSafeMessageEip712(
            chainId = chainId,
            verifyingContract = accountAddress,
            message = message
        )
        val hashData = Sign.hashTypedData(json)
        val signatureData = try {
            signer.sign(hashData)
        } catch (e: SignerException) {
            throw SmartAccountException.SignerError("Failed to sign message: ${message}", e.cause)
        }
        return signatureData
    }


    private val MAGIC_VALUE = "0x20c13b0b00000000000000000000000000000000000000000000000000000000"

    override fun isValidSignature(message: String, signature: ByteArray): Boolean {
        message.requireHex()
        if (message.length > 66) {
            throw SmartAccountException.Error("message must be 32 bytes max")
        }
        // function isValidSignature(bytes32 _dataHash, bytes calldata _signature)
        // pad message to 32 bytes (message starts with 0x)
        val inputParams = listOf(
            DynamicBytes(message.hexToByteArray()),
            DynamicBytes(signature)
        )
        val outputParams = listOf(object : TypeReference<Bytes32>() {})
        val function = Function("isValidSignature", inputParams, outputParams)
        val encodedFunction = FunctionEncoder.encode(function)
        val value = transactionManager.sendCall(
            accountAddress, encodedFunction, DefaultBlockParameterName.LATEST
        )
        val result: List<Bytes32> = FunctionReturnDecoder.decode(value, function.outputParameters) as List<Bytes32>
        return result.firstOrNull()?.value?.toHex() == MAGIC_VALUE

    }

    override fun getFactoryAddress(): Address {
        return config.safeProxyFactoryAddress.hexToAddress()
    }

    override fun getFactoryData(): ByteArray {
        val safeInitializer = when (signer) {
            is PasskeySigner -> {
                Safe.getSafeInitializerWithPasskey(
                    config = config,
                    passkey = signer.passkey
                )
            }

            is EOASigner -> Safe.getSafeInitializer(owner = signer.getAddress().hexToAddress(), config = config)
            else -> throw SmartAccountException.Error("Unsupported signer type")
        }
        val createProxyWithNonceData = Safe.getCreateProxyWithNonceFunctionData(
            _singleton = config.getSafeSingletonL2Address(),
            initializer = safeInitializer,
            saltNonce = 0.toBigInteger()
        )
        return createProxyWithNonceData
    }

    @WorkerThread
    @Throws(SmartAccountException::class, IOException::class)
    override fun addOwner(owner: Address): String {
        val userOperationHash = sendUserOperation(
            to = accountAddress.hexToAddress(),
            data = Safe.getAddOwnerWithThresholdFunctionData(
                owner = owner,
                _threshold = BigInteger.ONE
            )
        )
        return userOperationHash
    }

    @WorkerThread
    @Throws(SmartAccountException::class)
    override fun deployAndEnablePasskeySigner(x: BigInteger, y: BigInteger): String {
        val contract = SafeWebAuthnSignerFactoryContract(web3Service, contractAddress = config.getSafeWebauthnSignerFactoryAddress())
        val verifiers = config.safeP256VerifierAddress.hexToBigInt()
        val createSignerData = contract.createSignerFunction(x, y, verifiers).encode().hexToByteArray()
        val signerAddress = try {
            contract.getSigner(x, y, verifiers) ?: throw SmartAccountException.Error("Failed to get signer address")
        } catch (e: ContractCallException) {
            Log.e("SafeAccount", "Failed to get signer address", e)
            throw SmartAccountException.Error("Failed to get signer address", e)
        }
        val addOwnerData = Safe.getAddOwnerWithThresholdFunctionData(owner = signerAddress, _threshold = BigInteger.ONE)
        return Safe.getMultiSendFunctionData(
            listOf(
                MultiSendTransaction(
                    op = 0,
                    to = config.getSafeWebauthnSignerFactoryAddress(),
                    data = createSignerData
                ),
                MultiSendTransaction(
                    op = 0,
                    to = accountAddress.hexToAddress(),
                    data = addOwnerData
                )
            )
        ).let { multiSendData ->
            sendUserOperation(
                to = config.getSafeMultiSendAddress(),
                data = multiSendData,
                delegateCall = true
            )
        }

    }

    private fun buildSafeOperationJsonEip712V7(
        chainId: Int,
        verifyingContract: String,
        sender: String,
        nonce: String,
        initCode: String,
        callData: String,
        verificationGasLimit: String,
        callGasLimit: String,
        preVerificationGas: String,
        maxFeePerGas: String,
        maxPriorityFeePerGas: String,
        paymasterAndData: String,
        validAfter: String,
        validUntil: String,
        entryPointAddress: String
    ): String {
        val json = """
        {
          "types":{
             "EIP712Domain":[
                {"name":"chainId","type":"uint256"},
                {"name":"verifyingContract","type":"address"}
             ],
             "SafeOp":[
                { "type":"address", "name":"safe" },
                { "type":"uint256", "name":"nonce" },
                { "type":"bytes", "name":"initCode" },
                { "type":"bytes", "name":"callData" },
                { "type":"uint128", "name":"verificationGasLimit" },
                { "type":"uint128", "name":"callGasLimit" },
                { "type":"uint256", "name":"preVerificationGas" },
                { "type":"uint128", "name":"maxPriorityFeePerGas" },
                { "type":"uint128", "name":"maxFeePerGas" },
                { "type":"bytes", "name":"paymasterAndData" },
                { "type":"uint48", "name":"validAfter" },
                { "type":"uint48", "name":"validUntil" },
                { "type":"address", "name":"entryPoint" }
             ]
          },
          "primaryType":"SafeOp",
          "domain":{ "chainId": $chainId, "verifyingContract": "$verifyingContract" },
          "message":{
             "safe":"$sender",
             "nonce":"$nonce",
             "initCode":"$initCode",
             "callData":"$callData",
             "verificationGasLimit": "$verificationGasLimit",
             "callGasLimit": "$callGasLimit",
             "preVerificationGas": "$preVerificationGas",
             "maxFeePerGas": "$maxFeePerGas",
             "maxPriorityFeePerGas": "$maxPriorityFeePerGas",
             "paymasterAndData": "$paymasterAndData",
             "validAfter": "$validAfter",
             "validUntil": "$validUntil",
             "entryPoint": "$entryPointAddress"
          }
       }
    """.trimIndent()
        return json
    }

    // safe message for EIP712
    private fun buildSafeMessageEip712(
        chainId: Int,
        verifyingContract: String,
        message: String,
    ): String {
        val json = """
        {
          "types":{
             "EIP712Domain":[
                {"name":"chainId","type":"uint256"},
                {"name":"verifyingContract","type":"address"}
             ],
             "SafeMessage":[
                { "type":"bytes", "name":"message" }
             ]
          },
          "primaryType":"SafeMessage",
          "domain":{ "chainId": $chainId, "verifyingContract": "$verifyingContract" },
          "message":{
             "message":"$message"
          }
        }
        """.trimIndent()
        return json
    }

    fun predictDelayModuleAddress(recoveryModuleConfig: RecoveryModuleConfig = RecoveryModuleConfig()): String {
        return DelayModule.predictAddress(safeAddress.hexToAddress(), recoveryModuleConfig)
    }

    @WorkerThread
    @Throws(SmartAccountException::class, IOException::class)
    override fun enableRecovery(guardianAddress: Address, recoveryModuleConfig: RecoveryModuleConfig): String {
        val delayAddress = DelayModule.predictAddress(safeAddress.hexToAddress(), recoveryModuleConfig)
        val isDelayModuleDeployed = web3.isDeployed(delayAddress)
        if (isDelayModuleDeployed) {
            throw SmartAccountException.Error("Recovery module already enabled")
        }
        val initializer = DelayModule.setUpFunctionData(recoveryModuleConfig, safeAddress.hexToAddress())
        val transactionParams = listOf(
            TransactionParams(
                to = recoveryModuleConfig.moduleFactoryAddress,
                value = "0x0",
                data = DelayModuleFactory.deployModuleFunctionData(
                    recoveryModuleConfig.delayModuleAddress.hexToAddress(),
                    initializer,
                    safeAddress.hexToAddress()
                )
            ),
            TransactionParams(
                to = safeAddress,
                value = "0x0",
                data = Safe.enableModuleFunctionData(delayAddress.hexToAddress())
            ),
            TransactionParams(
                to = delayAddress,
                value = "0x0",
                data = Safe.enableModuleFunctionData(guardianAddress)
            )
        )
        return sendUserOperation(transactionParams)
    }

    @WorkerThread
    @Throws(IOException::class)
    override fun getCurrentGuardian(delayAddress: Address): Address? {
        val SENTINEL_MODULES = "0x0000000000000000000000000000000000000001"
        val modulesPaginated = DelayModule.getModulesPaginated(
            web3Service,
            contractAddress = delayAddress,
            start = SENTINEL_MODULES.hexToAddress(),
            pageSize = 1000
        )
        return modulesPaginated.getOrNull(0)
    }

    @WorkerThread
    @Throws(IOException::class)
    override fun isRecoveryStarted(delayAddress: Address): Boolean {
        val txNonce = DelayModule.txNonce(web3Service, delayAddress)
        val queueNonce = DelayModule.queueNonce(web3Service, delayAddress)
        return queueNonce > txNonce
    }

    @WorkerThread
    @Throws(SmartAccountException::class, IOException::class)
    override fun cancelRecovery(delayAddress: Address): String {
        val isDeployed = web3.isDeployed(delayAddress.toChecksumHex())
        if (!isDeployed) throw SmartAccountException.Error("Delay module not deployed")
        val isRecoveryStarted = isRecoveryStarted(delayAddress)
        if (!isRecoveryStarted) throw SmartAccountException.Error("There is no recovery started")
        val txNonce = DelayModule.txNonce(web3Service, delayAddress)
        return sendUserOperation(
            to = delayAddress,
            data = DelayModule.setTxNonceFunctionData(txNonce.plus(BigInteger.ONE)).hexToByteArray(),
        )
    }

}