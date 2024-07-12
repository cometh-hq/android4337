package io.cometh.android4337.passkey


import android.content.Context
import androidx.credentials.CredentialManager
import io.cometh.android4337.passkey.credentials.GetCredentialAuthenticationResponse
import io.cometh.android4337.passkey.credentials.getPublicKeyCoordinates
import io.cometh.android4337.safe.SafeConfig
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.ASN1Sequence
import org.web3j.abi.DefaultFunctionEncoder
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

    fun importPassKey(x: BigInteger, y: BigInteger) {
        passKey = PassKey(x, y)
    }

    suspend fun createPasskey(userName: String, rpName: String = "", userId: String = "") {
        require(passKey == null) { "passkey already loaded" }
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

    suspend fun sign(dataToSign: ByteArray): String {
        val authResponse = credentialsApiHelper.getCredential(rpId, dataToSign)
        val signatureDecoded = authResponse.response.getSignatureDecoded()
        val (r, s) = extractRS(signatureDecoded)

        // bytes, bytes, uint256[2]
        val extractClientDataFields = authResponse.extractClientDataFields()
        val authenticatorDataDecoded = authResponse.response.getAuthenticatorDataDecoded()
        val passkeySignature = DefaultFunctionEncoder().encodeParameters(
            listOf(
                DynamicBytes(authenticatorDataDecoded),
                DynamicBytes(extractClientDataFields),
                StaticArray2(Uint256::class.java, Uint256(r), Uint256(s))
            )
        )
        return "0x$passkeySignature"

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
    val dataFields = String(response.getClientDataJSONDecoded())
    val matchResult = Regex("""^\{"type":"webauthn.get","challenge":"[A-Za-z0-9\-_]{43}",(.*)\}$""").find(dataFields)
    if (matchResult == null) {
        //TODO handle error
        throw NotImplementedError("//TODO handle error")
    }
    val fields = matchResult.groupValues[1]
    return fields.toByteArray()
}
