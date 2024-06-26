package io.cometh.android4337.web3j

import io.cometh.android4337.utils.toChecksumHex
import org.web3j.crypto.Hash
import org.web3j.utils.Numeric

object Create2 {
    fun getCreate2Address(sender: String, salt: String, initCodeHash: String): String {
        val senderBytes = Numeric.hexStringToByteArray(sender)
        val saltBytes = Numeric.hexStringToByteArray(salt)
        val initCodeHashBytes = Numeric.hexStringToByteArray(initCodeHash)
        val data = byteArrayOf(0xff.toByte()) + senderBytes + saltBytes + initCodeHashBytes
        val hash = Hash.sha3(data)
        val addressBytes = hash.copyOfRange(12, 32)
        return addressBytes.toChecksumHex()
    }
}