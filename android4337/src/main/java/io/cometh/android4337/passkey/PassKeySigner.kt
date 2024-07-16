package io.cometh.android4337.passkey


import android.content.Context
import androidx.credentials.CredentialManager
import io.cometh.android4337.passkey.credentials.GetCredentialAuthenticationResponse
import io.cometh.android4337.passkey.credentials.getPublicKeyCoordinates
import org.web3j.abi.DefaultFunctionEncoder
import org.web3j.abi.datatypes.DynamicBytes
import org.web3j.abi.datatypes.generated.StaticArray2
import org.web3j.abi.datatypes.generated.Uint256
import java.math.BigInteger
import java.security.SecureRandom

class PassKeySigner(
    private val rpId: String,
    private val context: Context,
    private val credentialManager: CredentialManager,
    private val credentialsApiHelper: CredentialsApiHelper = CredentialsApiHelper(context, credentialManager),
) {
    private var passKey: PassKey? = null

    fun importPasskey(publicKey: ByteArray) {
        val (x, y) = PassKeyUtils.publicKeyToXYCoordinates(publicKey)
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
        val (r, s) = PassKeyUtils.extractRSFromSignature(signatureDecoded)

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
