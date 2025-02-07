package io.cometh.android4337.utils

import android.util.Base64
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign.SignatureData
import org.web3j.protocol.Web3j
import org.web3j.protocol.Web3jService
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.tx.ClientTransactionManager
import org.web3j.utils.Numeric
import java.math.BigInteger

fun String.isHex() = Regex("^0x[0-9A-Fa-f]*$").matches(this)
fun String.requireHex() = require(isHex()) { "Not a hex string: $this" }
fun String.requireHexAddress() = require(isHex() && length == 42) { "Not an address: $this" }
fun String.hexToAddress() = Address(160, this)
fun String.hexToBigInt(): BigInteger = requireHex().let { Numeric.toBigInt(this) }
fun String.hexToByteArray(): ByteArray = requireHex().let { Numeric.hexStringToByteArray(this) }
fun String.removeOx(): String = removePrefix("0x")

fun ByteArray.toHexNoPrefix(): String = Numeric.toHexStringNoPrefix(this)
fun ByteArray.toHex(): String = Numeric.toHexString(this)
fun ByteArray.toChecksumHex(): String = Keys.toChecksumAddress(toHex())
fun ByteArray.encodeBase64(): String {
    return Base64.encodeToString(this, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
}
fun String.decodeBase64(): ByteArray {
    return Base64.decode(this, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
}

fun SignatureData.toHexNoPrefix() = "${r.toHexNoPrefix()}${s.toHexNoPrefix()}${v.toHexNoPrefix()}"
fun SignatureData.toHex() = "0x${toHexNoPrefix()}"
fun SignatureData.toByteArray() = toHex().hexToByteArray()

fun BigInteger.toHexNoPrefix(): String = Numeric.toHexStringNoPrefix(this.toByteArray())
fun BigInteger.toHex(): String = Numeric.toHexString(this.toByteArray())

fun Address.toChecksumHex(): String = Keys.toChecksumAddress(this.toString())
fun Address.toChecksumHexNoPrefix(): String = Keys.toChecksumAddress(this.toString()).removeOx()
fun Address.toBytes(): ByteArray = Numeric.hexStringToByteArray(this.toString())

fun Function.encode(): String = FunctionEncoder.encode(this)

fun Int.requirePositive() = require(this > 0) { "Not positive: $this" }

fun Function.send(web3jService: Web3jService, contractAddress: Address): List<Type<Any>> {
    val data = FunctionEncoder.encode(this)
    val transactionManager = ClientTransactionManager(Web3j.build(web3jService), null)
    val value = transactionManager.sendCall(contractAddress.toChecksumHex(), data, DefaultBlockParameterName.LATEST)
    return FunctionReturnDecoder.decode(value, this.outputParameters)
}

fun Web3j.isDeployed(address: String): Boolean {
    address.requireHexAddress()
    val result = ethGetCode(address, DefaultBlockParameterName.LATEST).send()
    return !result.hasError() && result.code != "0x"
}
