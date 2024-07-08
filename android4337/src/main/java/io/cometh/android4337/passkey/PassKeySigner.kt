package io.cometh.android4337.passkey


import android.content.Context
import androidx.credentials.CredentialManager
import io.cometh.android4337.passkey.credentials.GetCredentialAuthenticationResponse
import io.cometh.android4337.passkey.credentials.getPublicKeyCoordinates
import io.cometh.android4337.safe.SafeConfig
import io.cometh.android4337.utils.decodeBase64
import io.cometh.android4337.utils.hexToByteArray
import io.cometh.android4337.web3j.AbiEncoder
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.ASN1Sequence
import org.web3j.abi.datatypes.DynamicBytes
import org.web3j.abi.datatypes.generated.StaticArray2
import org.web3j.abi.datatypes.generated.Uint256
import java.math.BigInteger
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec

class PassKeySigner(
    private val rpId: String,
    private val context: Context,
    private val credentialManager: CredentialManager,
    private val safeConfig: SafeConfig = SafeConfig.createDefaultConfig(),
    private val credentialsApiHelper: CredentialsApiHelper = CredentialsApiHelper(context, credentialManager),
) {
    private var passKey: PassKey? = null

    fun importPasskey(publicKey: ByteArray) {
        val (x, y) = publicKeyToXYCoordinates(publicKey)
        passKey = PassKey(x, y)
    }

    suspend fun createPasskey(userName: String, rpName: String = "", userId: String = "") {
        require(userName.isNotEmpty()) { "userName must be set" }
        val createResponse = credentialsApiHelper.createCredential(
            rpId = rpId,
            rpName = rpName,
            userId = userId,
            userName = userName,
            challenge = SecureRandom().generateSeed(32)
        )
        val (x, y) = createResponse!!.response.getPublicKeyCoordinates()
        passKey = PassKey(x, y)
    }

    suspend fun sign(dataToSign: ByteArray): ByteArray {
        val authResponse = credentialsApiHelper.getCredential(rpId, dataToSign)
        val signatureDecoded = authResponse.response.signature.decodeBase64()
        val (r, s) = extractRS(signatureDecoded)

        // bytes, bytes, uint256[2]
        val passkeySignature = AbiEncoder.encodePackedParameters(
            listOf(
                DynamicBytes(authResponse.response.authenticatorData.decodeBase64()),
                DynamicBytes(authResponse.extractClientDataFields()),
                StaticArray2(Uint256::class.java, Uint256(r), Uint256(s))
            )
        )

        val signatureBytes = buildSignatureBytes(
            listOf(
                SafeSignature(
                    signer = safeConfig.safeWebAuthnSharedSignerAddress,
                    data = passkeySignature,
                    dynamic = true
                )
            )
        )
        return signatureBytes.hexToByteArray()
    }

    fun getPasskey(): PassKey? {
        return passKey
    }
}

fun extractRS(signature: ByteArray): Pair<BigInteger, BigInteger> {
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

data class SafeSignature(val signer: String, val data: String, val dynamic: Boolean)

fun buildSignatureBytes(signatures: List<SafeSignature>): String {
    val SIGNATURE_LENGTH_BYTES = 65
    val sortedSignatures = signatures.sortedBy { it.signer.lowercase() }

    var signatureBytes = "0x"
    var dynamicBytes = ""

    for (sig in sortedSignatures) {
        if (sig.dynamic) {
            /*
                A contract signature has a static part of 65 bytes and the dynamic part that needs to be appended
                at the end of signature bytes.
                The signature format is
                Signature type == 0
                Constant part: 65 bytes
                {32-bytes signature verifier}{32-bytes dynamic data position}{1-byte signature type}
                Dynamic part (solidity bytes): 32 bytes + signature data length
                {32-bytes signature length}{bytes signature data}
            */
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

fun publicKeyToXYCoordinates(publicKeyBytes: ByteArray): Pair<BigInteger, BigInteger> {
    val keyFactory = KeyFactory.getInstance("EC")
    val pubKeySpec = X509EncodedKeySpec(publicKeyBytes)
    val pubKey = keyFactory.generatePublic(pubKeySpec) as java.security.interfaces.ECPublicKey
    val ecPoint = pubKey.w
    val x = ecPoint.affineX
    val y = ecPoint.affineY
    return Pair(x, y)
}

fun GetCredentialAuthenticationResponse.extractClientDataFields(): ByteArray {
    val matchResult = Regex("""^\{"type":"webauthn.get","challenge":"[A-Za-z0-9\-_]{43}",(.*)\}$""").find(response.clientDataJSON)
    if (matchResult == null) {
        //TODO handle error
        throw NotImplementedError("//TODO handle error")
    }
    val fields = matchResult.groupValues[1]
    return fields.toByteArray()
}
