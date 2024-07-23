package io.cometh.android4337.safe.signer.passkey


import android.content.Context
import io.cometh.android4337.safe.Safe
import io.cometh.android4337.safe.Safe.getSignatureBytes
import io.cometh.android4337.safe.SafeConfig
import io.cometh.android4337.safe.SafeSignature
import io.cometh.android4337.safe.signer.Signer
import io.cometh.android4337.safe.signer.SignerException
import io.cometh.android4337.safe.signer.passkey.credentials.GetCredentialAuthenticationResponse
import io.cometh.android4337.safe.signer.passkey.credentials.getPublicKeyCoordinates
import io.cometh.android4337.utils.hexToBigInt
import io.cometh.android4337.utils.hexToByteArray
import io.cometh.android4337.web3j.AbiEncoder
import kotlinx.coroutines.runBlocking
import org.web3j.abi.DefaultFunctionEncoder
import org.web3j.abi.datatypes.DynamicBytes
import org.web3j.abi.datatypes.generated.StaticArray2
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.abi.datatypes.generated.Uint48
import java.math.BigInteger
import java.security.SecureRandom

class PassKeySigner(
    private val rpId: String,
    private val context: Context,
    private val credentialsApiHelper: CredentialsApiHelper = CredentialsApiHelper(context),
    private val safeConfig: SafeConfig = SafeConfig.createDefaultConfig(),
) : Signer {

    val DUMMY_AUTHENTICATOR_DATA = "0xfefefefefefefefefefefefefefefefefefefefefefefefefefefefefefefefefe04fefefefe"
    val DUMMY_CLIENT_DATA_FIELDS = """
        "origin":"http://safe.global"
        "padding":"This pads the clientDataJSON so that we can leave room for additional implementation specific fields for a more accurate 'preVerificationGas' estimate."
    """.trimIndent()

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


    override fun sign(data: ByteArray): ByteArray {
        val authResponse = runBlocking { credentialsApiHelper.getCredential(rpId, data) }
        val signatureDecoded = authResponse.response.getSignatureDecoded()
        val (r, s) = PassKeyUtils.extractRSFromSignature(signatureDecoded)

        // bytes, bytes, uint256[2]
        val extractClientDataFields = authResponse.extractClientDataFields() ?: throw SignerException("Failed to extract client data fields")
        val authenticatorDataDecoded = authResponse.response.getAuthenticatorDataDecoded()
        val passkeySignature = DefaultFunctionEncoder().encodeParameters(
            listOf(
                DynamicBytes(authenticatorDataDecoded),
                DynamicBytes(extractClientDataFields),
                StaticArray2(Uint256::class.java, Uint256(r), Uint256(s))
            )
        )
        return Safe.buildSignatureBytes(
            listOf(
                SafeSignature(
                    signer = safeConfig.safeWebAuthnSharedSignerAddress.lowercase(),
                    data = "0x$passkeySignature",
                    dynamic = true
                )
            )
        ).hexToByteArray()
    }

    override fun checkRequirements() {
        requireNotNull(passKey) { "PassKeySigner must have a pass key created or imported" }
    }

    override fun getDummySignature(): String {
        val signature = Safe.buildSignatureBytes(
            listOf(
                SafeSignature(
                    signer = safeConfig.safeWebAuthnSharedSignerAddress,
                    data = getSignatureBytes(
                        DUMMY_AUTHENTICATOR_DATA.toByteArray(),
                        DUMMY_CLIENT_DATA_FIELDS,
                        "0xecececececececececececececececececececececececececececececececec".hexToBigInt(),
                        "0xd5ad5ad5ad5ad5ad5ad5ad5ad5ad5ad5ad5ad5ad5ad5ad5ad5ad5ad5ad5ad5af".hexToBigInt()
                    ),
                    dynamic = true
                )
            )
        )
        // uint48, uint48, bytes
        return AbiEncoder.encodePackedParameters(
            listOf(
                Uint48(BigInteger.ZERO),
                Uint48(BigInteger.ZERO),
                DynamicBytes(signature.hexToByteArray()),
            )
        )
    }

    fun getPasskey(): PassKey? {
        return passKey
    }
}

fun GetCredentialAuthenticationResponse.extractClientDataFields(): ByteArray? {
    val dataFields = String(response.getClientDataJSONDecoded())
    val matchResult = Regex("""^\{"type":"webauthn.get","challenge":"[A-Za-z0-9\-_]{43}",(.*)\}$""").find(dataFields) ?: return null
    val fields = matchResult.groupValues[1]
    return fields.toByteArray()
}
