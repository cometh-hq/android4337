package io.cometh.android4337.passkey

import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.ASN1Sequence
import java.math.BigInteger
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec

object PassKeyUtils {

    fun publicKeyToXYCoordinates(publicKeyBytes: ByteArray): Pair<BigInteger, BigInteger> {
        val keyFactory = KeyFactory.getInstance("EC")
        val pubKeySpec = X509EncodedKeySpec(publicKeyBytes)
        val pubKey = keyFactory.generatePublic(pubKeySpec) as java.security.interfaces.ECPublicKey
        val ecPoint = pubKey.w
        val x = ecPoint.affineX
        val y = ecPoint.affineY
        return Pair(x, y)
    }

    fun extractRSFromSignature(signature: ByteArray): Pair<BigInteger, BigInteger> {
        ASN1InputStream(signature.inputStream()).use { asn1InputStream ->
            val asn1Sequence = asn1InputStream.readObject() as ASN1Sequence
            if (asn1Sequence.size() != 2) {
                throw IllegalArgumentException("Invalid ECDSA signature format")
            }
            val r = (asn1Sequence.getObjectAt(0) as ASN1Integer).value
            val s = (asn1Sequence.getObjectAt(1) as ASN1Integer).value
            return Pair(r, s)
        }
    }


}