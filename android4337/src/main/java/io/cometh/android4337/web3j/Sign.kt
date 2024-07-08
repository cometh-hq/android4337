package io.cometh.android4337.web3j

import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Sign
import org.web3j.crypto.Sign.SignatureData
import org.web3j.crypto.StructuredDataEncoder

object Sign {
    fun signTypedData(jsonData: String, keyPair: ECKeyPair): SignatureData {
        return Sign.signMessage(hashTypedData(jsonData), keyPair, false)
    }

    fun hashTypedData(jsonData: String): ByteArray {
        val dataEncoder = StructuredDataEncoder(jsonData)
        return dataEncoder.hashStructuredData()
    }
}
