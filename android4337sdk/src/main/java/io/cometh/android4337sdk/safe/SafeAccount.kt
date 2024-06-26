package io.cometh.android4337sdk.safe

import androidx.annotation.WorkerThread
import io.cometh.android4337sdk.SmartAccount
import io.cometh.android4337sdk.SmartAccountException
import io.cometh.android4337sdk.UserOperation
import io.cometh.android4337sdk.bundler.BundlerClient
import io.cometh.android4337sdk.gasprice.UserOperationGasPriceProvider
import io.cometh.android4337sdk.paymaster.PaymasterClient
import io.cometh.android4337sdk.safe.SafeUtils.getEnableModulesFunctionData
import io.cometh.android4337sdk.safe.SafeUtils.getSetupFunctionData
import io.cometh.android4337sdk.utils.encode
import io.cometh.android4337sdk.utils.hexStringToBigInt
import io.cometh.android4337sdk.utils.hexStringToByteArray
import io.cometh.android4337sdk.utils.requireHexAddress
import io.cometh.android4337sdk.utils.toAddress
import io.cometh.android4337sdk.utils.toHex
import io.cometh.android4337sdk.utils.toHexNoPrefix
import io.cometh.android4337sdk.web3j.Create2
import io.cometh.android4337sdk.web3j.Sign
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
    web3j: Web3j,
    paymasterClient: PaymasterClient? = null,
    val safeAddress: String,
    private val chainId: Int,
    private val config: SafeConfig,
    transactionManager: TransactionManager = RawTransactionManager(web3j, credentials)
) : SmartAccount(
    credentials,
    bundlerClient,
    gasPriceProvider,
    entryPointAddress,
    web3j,
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
            gasPriceProvider: UserOperationGasPriceProvider,
            entryPointAddress: String,
            web3j: Web3j,
            paymasterClient: PaymasterClient? = null,
            chainId: Int,
            config: SafeConfig,
            web3jTransactionManager: TransactionManager = RawTransactionManager(web3j, credentials)
        ): SafeAccount {
            return SafeAccount(
                credentials,
                bundlerClient,
                gasPriceProvider,
                entryPointAddress,
                web3j,
                paymasterClient,
                address,
                chainId,
                config,
                web3jTransactionManager
            )
        }

        @WorkerThread
        @Throws(IOException::class, RuntimeException::class)
        fun createNewAccount(
            credentials: Credentials,
            bundlerClient: BundlerClient,
            gasPriceProvider: UserOperationGasPriceProvider,
            entryPointAddress: String,
            web3j: Web3j,
            paymasterClient: PaymasterClient? = null,
            chainId: Int,
            config: SafeConfig,
            web3jTransactionManager: TransactionManager = RawTransactionManager(web3j, credentials)
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
                web3j,
                paymasterClient,
                predictedAddress,
                chainId,
                config,
                web3jTransactionManager
            )
        }

        @WorkerThread
        @Throws(IOException::class, SmartAccountException::class)
        fun predictAddress(
            owner: String,
            web3jTransactionManager: TransactionManager,
            config: SafeConfig,
        ): String {
            owner.requireHexAddress()
            val nonce = BigInteger.ZERO
            val safeProxyContract = SafeProxyFactoryContract(web3jTransactionManager, config.safeProxyFactoryAddress)
            val proxyCreationCode = safeProxyContract.proxyCreationCode() ?: throw SmartAccountException("Failed to get proxy creation code")
            val enableModulesData = getEnableModulesFunctionData(listOf(config.erc4337ModuleAddress.toAddress()))
            val setupData = getSetupFunctionData(
                _owners = listOf(owner.toAddress()),
                _threshold = BigInteger.ONE,
                to = config.safeModuleSetupAddress.toAddress(),
                data = enableModulesData,
                fallbackHandler = config.erc4337ModuleAddress.toAddress(),
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
        val json = buildSafeJsonEip712(
            chainId,
            config.erc4337ModuleAddress,
            userOperation.sender,
            userOperation.nonce.toHex(),
            userOperation.initCode.toHex(),
            userOperation.callData.toHex(),
            userOperation.verificationGasLimit.toHex(),
            userOperation.callGasLimit.toHex(),
            userOperation.preVerificationGas.toHex(),
            userOperation.maxFeePerGas.toHex(),
            userOperation.maxPriorityFeePerGas.toHex(),
            userOperation.paymasterAndData.toHex(),
            validAfter.toHex(),
            validUntil.toHex(),
            entryPointAddress
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
            to, Uint256(value),
            DynamicBytes(data),
            Uint8(BigInteger.ZERO),
        )
        val outputParams = listOf(object : TypeReference<Uint256>() {})
        val callData = Function("executeUserOp", inputParams, outputParams).encode()
        return callData
    }

    override fun getInitCode(): ByteArray {
        val nonce = 0
        val enableModulesData = getEnableModulesFunctionData(listOf(config.erc4337ModuleAddress.toAddress()))
        val setupData = getSetupFunctionData(
            _owners = listOf(credentials.address.toAddress()),
            _threshold = BigInteger.ONE,
            to = config.safeModuleSetupAddress.toAddress(),
            data = enableModulesData,
            fallbackHandler = config.erc4337ModuleAddress.toAddress(),
            paymentToken = Address.DEFAULT,
            payment = BigInteger.ZERO,
            paymentReceiver = Address.DEFAULT
        )
        val createProxyWithNonceData = SafeUtils.getCreateProxyWithNonceFunctionData(
            _singleton = config.safeSingletonL2Address.toAddress(),
            initializer = setupData,
            saltNonce = nonce.toBigInteger()
        )
        val initCode = "${config.safeProxyFactoryAddress}${createProxyWithNonceData.toHexNoPrefix()}"
        return initCode.hexStringToByteArray()
    }

    private fun buildSafeJsonEip712(
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
                { "type":"uint256", "name":"callGasLimit" },
                { "type":"uint256", "name":"verificationGasLimit" },
                { "type":"uint256", "name":"preVerificationGas" },
                { "type":"uint256", "name":"maxFeePerGas" },
                { "type":"uint256", "name":"maxPriorityFeePerGas" },
                { "type":"bytes", "name":"paymasterAndData" },
                { "type":"uint48", "name":"validAfter" },
                { "type":"uint48", "name":"validUntil" },
                { "type":"address", "name":"entryPoint" }
             ]
          },
          "primaryType":"SafeOp",
          "domain":{ "chainId": "$chainId", "verifyingContract": "$verifyingContract" },
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