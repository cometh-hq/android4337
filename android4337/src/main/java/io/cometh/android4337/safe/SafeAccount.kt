package io.cometh.android4337.safe

import androidx.annotation.WorkerThread
import io.cometh.android4337.EntryPointContract
import io.cometh.android4337.SmartAccount
import io.cometh.android4337.SmartAccountException
import io.cometh.android4337.UserOperation
import io.cometh.android4337.bundler.BundlerClient
import io.cometh.android4337.gasprice.RPCGasEstimator
import io.cometh.android4337.gasprice.UserOperationGasPriceProvider
import io.cometh.android4337.getInitCode
import io.cometh.android4337.getPaymasterAndData
import io.cometh.android4337.paymaster.PaymasterClient
import io.cometh.android4337.safe.SafeUtils.getEnableModulesFunctionData
import io.cometh.android4337.safe.SafeUtils.getSetupFunctionData
import io.cometh.android4337.utils.encode
import io.cometh.android4337.utils.hexStringToBigInt
import io.cometh.android4337.utils.hexStringToByteArray
import io.cometh.android4337.utils.requireHexAddress
import io.cometh.android4337.utils.hexStringToAddress
import io.cometh.android4337.utils.toHex
import io.cometh.android4337.utils.toHexNoPrefix
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
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.abi.datatypes.generated.Uint8
import org.web3j.crypto.Credentials
import org.web3j.crypto.Hash
import org.web3j.protocol.Service
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.TransactionManager
import org.web3j.utils.Numeric
import java.io.IOException
import java.math.BigInteger

class SafeAccount private constructor(
    credentials: Credentials,
    bundlerClient: BundlerClient,
    gasPriceProvider: UserOperationGasPriceProvider,
    entryPointAddress: String,
    web3Service: Service,
    val safeAddress: String,
    private val chainId: Int,
    private val config: SafeConfig,
    paymasterClient: PaymasterClient? = null,
    transactionManager: TransactionManager = RawTransactionManager(Web3j.build(web3Service), credentials)
) : SmartAccount(
    credentials,
    bundlerClient,
    gasPriceProvider,
    entryPointAddress,
    web3Service,
    paymasterClient,
    safeAddress,
    transactionManager
) {

    init {
        safeAddress.requireHexAddress()
    }

    companion object {
        fun fromAddress(
            address: String,
            credentials: Credentials,
            bundlerClient: BundlerClient,
            chainId: Int,
            web3Service: Service,
            config: SafeConfig = SafeConfig.createDefaultConfig(),
            entryPointAddress: String = EntryPointContract.ENTRY_POINT_ADDRESS_V7,
            paymasterClient: PaymasterClient? = null,
            gasPriceProvider: UserOperationGasPriceProvider = RPCGasEstimator(web3Service),
            web3jTransactionManager: TransactionManager = RawTransactionManager(Web3j.build(web3Service), credentials)
        ): SafeAccount {
            return SafeAccount(
                credentials,
                bundlerClient,
                gasPriceProvider,
                entryPointAddress,
                web3Service,
                address,
                chainId,
                config,
                paymasterClient,
                web3jTransactionManager
            )
        }

        @WorkerThread
        @Throws(IOException::class, RuntimeException::class)
        fun createNewAccount(
            credentials: Credentials,
            bundlerClient: BundlerClient,
            chainId: Int,
            web3Service: Service,
            config: SafeConfig = SafeConfig.createDefaultConfig(),
            entryPointAddress: String = EntryPointContract.ENTRY_POINT_ADDRESS_V7,
            paymasterClient: PaymasterClient? = null,
            gasPriceProvider: UserOperationGasPriceProvider = RPCGasEstimator(web3Service),
            web3jTransactionManager: TransactionManager = RawTransactionManager(Web3j.build(web3Service), credentials)
        ): SafeAccount {
            val predictedAddress = predictAddress(
                owner = credentials.address,
                web3jTransactionManager = web3jTransactionManager,
                config = config
            )
            return SafeAccount(
                credentials,
                bundlerClient,
                gasPriceProvider,
                entryPointAddress,
                web3Service,
                predictedAddress,
                chainId,
                config,
                paymasterClient,
                web3jTransactionManager
            )
        }

        @WorkerThread
        @Throws(IOException::class, SmartAccountException::class)
        fun predictAddress(
            owner: String,
            web3jTransactionManager: TransactionManager,
            config: SafeConfig = SafeConfig.createDefaultConfig()
        ): String {
            owner.requireHexAddress()
            val nonce = BigInteger.ZERO
            val safeProxyContract = SafeProxyFactoryContract(web3jTransactionManager, config.safeProxyFactoryAddress)
            val proxyCreationCode = safeProxyContract.proxyCreationCode() ?: throw SmartAccountException("Failed to get proxy creation code")
            val enableModulesData = getEnableModulesFunctionData(listOf(config.erc4337ModuleAddress.hexStringToAddress()))
            val setupData = getSetupFunctionData(
                _owners = listOf(owner.hexStringToAddress()),
                _threshold = BigInteger.ONE,
                to = config.safeModuleSetupAddress.hexStringToAddress(),
                data = enableModulesData,
                fallbackHandler = config.erc4337ModuleAddress.hexStringToAddress(),
                paymentToken = Address.DEFAULT,
                payment = BigInteger.ZERO,
                paymentReceiver = Address.DEFAULT
            )
            val keccak256Setup = Hash.sha3(setupData)
            val saltHash = "${keccak256Setup.toHexNoPrefix()}${Numeric.toBytesPadded(nonce, 32).toHexNoPrefix()}"
            val salt = Hash.sha3(saltHash)
            val safeSingletonL2AddressEncoded = Numeric.toBytesPadded(config.safeSingletonL2Address.hexStringToBigInt(), 32)
            val deploymentCode = "${proxyCreationCode.toHex()}${safeSingletonL2AddressEncoded.toHexNoPrefix()}"
            val keccak256DeploymentCode = Hash.sha3(deploymentCode)
            val proxyAddress = Create2.getCreate2Address(config.safeProxyFactoryAddress, salt, keccak256DeploymentCode)
            return proxyAddress
        }

    }

    override fun signOperation(
        userOperation: UserOperation,
        entryPointAddress: String
    ): ByteArray {
        val validAfter = BigInteger.ZERO
        val validUntil = BigInteger.ZERO
        val json = buildSafeJsonEip712V7(
            chainId = chainId,
            verifyingContract = config.erc4337ModuleAddress,
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
        val signatureData = Sign.signTypedData(json, credentials.ecKeyPair)
        val validAfterBytes = Numeric.toBytesPadded(validAfter, 6)
        val validUntilBytes = Numeric.toBytesPadded(validUntil, 6)
        val signature = "0x${validAfterBytes.toHexNoPrefix()}${validUntilBytes.toHexNoPrefix()}${signatureData.toHexNoPrefix()}"
        return signature.hexStringToByteArray()
    }

    @WorkerThread
    fun getOwners(): List<Address>? {
        val inputParams = emptyList<Type<*>>()
        val outputParams = listOf(object : TypeReference<DynamicArray<Address>>() {})
        val function = Function("getOwners", inputParams, outputParams)
        val encodedFunction = FunctionEncoder.encode(function)
        val value = transactionManager.sendCall(
            accountAddress,
            encodedFunction,
            DefaultBlockParameterName.LATEST
        )
        val result: List<DynamicArray<Address>> = FunctionReturnDecoder.decode(
            value,
            function.outputParameters
        ) as List<DynamicArray<Address>>
        return result.firstOrNull()?.value
    }

    override fun getCallData(to: Address, value: BigInteger, data: ByteArray): ByteArray {
        val inputParams = listOf(
            to,
            Uint256(value),
            DynamicBytes(data),
            Uint8(BigInteger.ZERO),
        )
        val outputParams = listOf(object : TypeReference<Uint256>() {})
        return Function("executeUserOp", inputParams, outputParams).encode()
    }

    override fun getFactoryAddress(): Address {
        return config.safeProxyFactoryAddress.hexStringToAddress()
    }

    override fun getFactoryData(): ByteArray {
        val nonce = 0
        val enableModulesData = getEnableModulesFunctionData(listOf(config.erc4337ModuleAddress.hexStringToAddress()))
        val setupData = getSetupFunctionData(
            _owners = listOf(credentials.address.hexStringToAddress()),
            _threshold = BigInteger.ONE,
            to = config.safeModuleSetupAddress.hexStringToAddress(),
            data = enableModulesData,
            fallbackHandler = config.erc4337ModuleAddress.hexStringToAddress(),
            paymentToken = Address.DEFAULT,
            payment = BigInteger.ZERO,
            paymentReceiver = Address.DEFAULT
        )
        val createProxyWithNonceData = SafeUtils.getCreateProxyWithNonceFunctionData(
            _singleton = config.safeSingletonL2Address.hexStringToAddress(),
            initializer = setupData,
            saltNonce = nonce.toBigInteger()
        )
        return createProxyWithNonceData
    }

    private fun buildSafeJsonEip712V7(
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


}