package io.cometh.android4337.web3j

import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Sign
import org.web3j.crypto.Sign.SignatureData
import org.web3j.crypto.StructuredDataEncoder

object Sign {
    fun signTypedData(jsonData: String, keyPair: ECKeyPair): SignatureData {
        val dataEncoder = StructuredDataEncoder(jsonData)
        val hashStructuredData = dataEncoder.hashStructuredData()
        return Sign.signMessage(hashStructuredData, keyPair, false)
    }
}
