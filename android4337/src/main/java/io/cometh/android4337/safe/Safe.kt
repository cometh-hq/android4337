package io.cometh.android4337.safe

import io.cometh.android4337.safe.signer.passkey.PassKey
import io.cometh.android4337.utils.hexToBigInt
import io.cometh.android4337.utils.hexToByteArray
import io.cometh.android4337.utils.removeOx
import io.cometh.android4337.web3j.AbiEncoder
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.abi.datatypes.DynamicBytes
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.StaticStruct
import org.web3j.abi.datatypes.generated.Uint176
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.abi.datatypes.generated.Uint8
import org.web3j.utils.Numeric
import java.math.BigInteger

object Safe {

    fun getEnableModulesFunctionData(moduleAddresses: List<Address>): ByteArray {
        val inputParams = listOf(DynamicArray(Address::class.java, moduleAddresses))
        val outputParams = emptyList<TypeReference<*>>()
        val function = Function("enableModules", inputParams, outputParams)
        return FunctionEncoder.encode(function).hexToByteArray()
    }

    fun getSetupFunctionData(
        _owners: List<Address>,
        _threshold: BigInteger,
        to: Address,
        data: ByteArray,
        fallbackHandler: Address,
        paymentToken: Address,
        payment: BigInteger,
        paymentReceiver: Address
    ): ByteArray {
        val inputParams = listOf(
            DynamicArray(Address::class.java, _owners),
            Uint256(_threshold),
            to,
            DynamicBytes(data),
            fallbackHandler,
            paymentToken,
            Uint256(payment),
            paymentReceiver
        )
        val outputParams = emptyList<TypeReference<*>>()
        val function = Function("setup", inputParams, outputParams)
        return FunctionEncoder.encode(function).hexToByteArray()
    }

    fun getCreateProxyWithNonceFunctionData(
        _singleton: Address, initializer: ByteArray, saltNonce: BigInteger
    ): ByteArray {
        val inputParams = listOf(
            _singleton, DynamicBytes(initializer), Uint256(saltNonce)
        )
        val outputParams = emptyList<TypeReference<*>>()
        val function = Function("createProxyWithNonce", inputParams, outputParams)
        return FunctionEncoder.encode(function).hexToByteArray()
    }

    // multiSend
    fun getMultiSendFunctionData(
        safeModuleSetupAddress: Address,
        safeWebAuthnSharedSignerAddress: Address,
        enableModuleData: ByteArray,
        sharedSignerConfigureData: ByteArray
    ): ByteArray {
        val encodedMultiSendTxs = encodeMultiSendTransactions(
            listOf(
                MultiSendTransaction(1, safeModuleSetupAddress, BigInteger.ZERO, enableModuleData),
                MultiSendTransaction(1, safeWebAuthnSharedSignerAddress, BigInteger.ZERO, sharedSignerConfigureData)
            )
        )
        val inputParams = listOf(DynamicBytes(encodedMultiSendTxs))
        val outputParams = emptyList<TypeReference<*>>()
        val function = Function("multiSend", inputParams, outputParams)
        return FunctionEncoder.encode(function).hexToByteArray()
    }

    fun encodeMultiSendTransactions(transactions: List<MultiSendTransaction>): ByteArray {
        //'uint8', 'address', 'uint256', 'uint256', 'bytes'
        val result = transactions.map {
            val size = it.data.size.toBigInteger()
            AbiEncoder.encodePackedParameters(
                listOf(
                    Uint8(it.op.toBigInteger()), it.to, Uint256(it.value ?: BigInteger.ZERO), Uint256(size), DynamicBytes(it.data)
                )
            ).removeOx()
        }
        return "0x${result.joinToString("")}".hexToByteArray()
    }

    fun getSharedSignerConfigureCallData(
        x: BigInteger, y: BigInteger, verifiers: BigInteger
    ): ByteArray {
        val inputParams = listOf(
            StaticStruct(
                Uint256(x), Uint256(y), Uint176(verifiers)
            )
        )
        val outputParams = emptyList<TypeReference<*>>()
        val function = Function("configure", inputParams, outputParams)
        return FunctionEncoder.encode(function).hexToByteArray()
    }

    fun getSafeInitializer(
        owner: Address,
        config: SafeConfig
    ): ByteArray {
        return getSetupFunctionData(
            _owners = listOf(owner),
            _threshold = BigInteger.ONE,
            to = config.getSafeModuleSetupAddress(),
            data = getEnableModulesFunctionData(listOf(config.getSafe4337ModuleAddress())),
            fallbackHandler = config.getSafe4337ModuleAddress(),
            paymentToken = Address.DEFAULT,
            payment = BigInteger.ZERO,
            paymentReceiver = Address.DEFAULT
        )
    }

    fun getSafeInitializerWithPasskey(
        config: SafeConfig,
        passKey: PassKey
    ): ByteArray {
        return getSetupFunctionData(
            _owners = listOf(config.getSafeWebAuthnSharedSignerAddress()),
            _threshold = BigInteger.ONE,
            to = config.getSafeMultiSendAddress(),
            data = getMultiSendFunctionData(
                safeModuleSetupAddress = config.getSafeModuleSetupAddress(),
                safeWebAuthnSharedSignerAddress = config.getSafeWebAuthnSharedSignerAddress(),
                enableModuleData = getEnableModulesFunctionData(listOf(config.getSafe4337ModuleAddress())),
                sharedSignerConfigureData = getSharedSignerConfigureCallData(
                    x = passKey.x,
                    y = passKey.y,
                    verifiers = config.safeP256VerifierAddress.hexToBigInt()
                )
            ),
            fallbackHandler = config.getSafe4337ModuleAddress(),
            paymentToken = Address.DEFAULT,
            payment = BigInteger.ZERO,
            paymentReceiver = Address.DEFAULT
        )
    }

    fun buildSignatureBytes(signatures: List<SafeSignature>): String {
        val SIGNATURE_LENGTH_BYTES = 65
        val sortedSignatures = signatures.sortedBy { it.signer.lowercase() }

        var signatureBytes = "0x"
        var dynamicBytes = ""

        for (sig in sortedSignatures) {
            if (sig.dynamic) {
                val dynamicPartPosition = (sortedSignatures.size * SIGNATURE_LENGTH_BYTES + dynamicBytes.length / 2).toString(16).padStart(64, '0')
                val dynamicPartLength = (sig.data.slice(2 until sig.data.length).length / 2).toString(16).padStart(64, '0')
                val staticSignature = "${sig.signer.slice(2 until sig.signer.length).padStart(64, '0')}${dynamicPartPosition}00"
                val dynamicPartWithLength = "$dynamicPartLength${sig.data.slice(2 until sig.data.length)}"
                signatureBytes += staticSignature
                dynamicBytes += dynamicPartWithLength
            } else {
                signatureBytes += sig.data.slice(2 until sig.data.length)
            }
        }

        return signatureBytes + dynamicBytes
    }

    fun getSignatureBytes(
        authenticatorData: ByteArray,
        clientDataFields: String,
        r: BigInteger,
        s: BigInteger
    ): String {
        fun encodeUint256(x: BigInteger): String {
            return x.toString(16).padStart(64, '0')
        }

        fun byteSize(data: ByteArray): Int {
            return 32 * (Math.ceil(data.size / 32.0).toInt() + 1) // +1 is for the length parameter
        }

        fun encodeBytes(data: ByteArray): String {
            val lengthHex = encodeUint256(BigInteger.valueOf(data.size.toLong()))
            val dataHex = Numeric.toHexString(data).substring(2)
            return (lengthHex + dataHex).padEnd(byteSize(data) * 2, '0')
        }
        // authenticatorData starts after the first four words.
        val authenticatorDataOffset = 32 * 4
        // clientDataFields starts immediately after the authenticator data.
        val clientDataFieldsOffset = authenticatorDataOffset + byteSize(authenticatorData)

        // Convert clientDataFields to ByteArray
        val clientDataFieldsBytes = clientDataFields.toByteArray()

        return "0x${encodeUint256(BigInteger.valueOf(authenticatorDataOffset.toLong()))}" + "${encodeUint256(BigInteger.valueOf(clientDataFieldsOffset.toLong()))}" + "${
            encodeUint256(
                r
            )
        }${encodeUint256(s)}" + "${encodeBytes(authenticatorData)}${encodeBytes(clientDataFieldsBytes)}"
    }

}
