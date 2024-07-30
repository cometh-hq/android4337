package io.cometh.android4337.safe.signer.eoa

import io.cometh.android4337.safe.signer.Signer
import io.cometh.android4337.utils.toByteArray
import org.web3j.crypto.Credentials
import org.web3j.crypto.Sign

class EOASigner(
    val credentials: Credentials
) : Signer {

    val ECDSA_DUMMY_SIGNATURE = "0xecececececececececececececececececececececececececececececececec"

    override fun sign(data: ByteArray): ByteArray {
        return Sign.signMessage(data, credentials.ecKeyPair, false).toByteArray()
    }

    override fun checkRequirements() {
        // nothing to do
    }

    override fun getDummySignature(): String {
        return ECDSA_DUMMY_SIGNATURE
    }

    fun getAddress(): String {
        return credentials.address
    }
}