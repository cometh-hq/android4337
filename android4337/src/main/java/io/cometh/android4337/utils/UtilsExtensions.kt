package io.cometh.android4337.utils

import android.os.Looper
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign.SignatureData
import org.web3j.utils.Numeric
import java.math.BigInteger

fun ByteArray.toHex(): String = Numeric.toHexString(this)
fun BigInteger.toHex(): String = Numeric.toHexString(this.toByteArray())
fun String.isHex() = Regex("^0x[0-9A-Fa-f]*$").matches(this)
fun String.requireHex() = require(isHex()) { "Not a hex string: $this" }
fun String.requireHexAddress() = require(isHex() && length == 42) { "Not an address: $this" }
fun String.hexStringToAddress() = Address(160, this)
fun ByteArray.toHexNoPrefix(): String = Numeric.toHexStringNoPrefix(this)
fun SignatureData.toHexNoPrefix() = "${r.toHexNoPrefix()}${s.toHexNoPrefix()}${v.toHexNoPrefix()}"
fun SignatureData.toHex() = "0x${toHexNoPrefix()}"
fun SignatureData.toByteArray() = toHex().hexStringToByteArray()
fun String.hexStringToBigInt(): BigInteger = requireHex().let { Numeric.toBigInt(this) }
fun String.hexStringToByteArray(): ByteArray = requireHex().let { Numeric.hexStringToByteArray(this) }
fun Function.encode(): ByteArray = FunctionEncoder.encode(this).hexStringToByteArray()
fun String.removeOx(): String = removePrefix("0x")
fun ByteArray.toChecksumHex(): String = Keys.toChecksumAddress(toHex())
fun BigInteger.toHexNoPrefix(): String = Numeric.toHexStringNoPrefix(this.toByteArray())
fun Address.toChecksumHex(): String = Keys.toChecksumAddress(this.toString())

fun checkNotMainThread(functionName: String? = null) = check(Looper.myLooper() != Looper.getMainLooper()) { "$functionName must not be executed on MainThread" }

