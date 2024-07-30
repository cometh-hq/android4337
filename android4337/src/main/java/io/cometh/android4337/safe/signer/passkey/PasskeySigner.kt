package io.cometh.android4337.safe.signer.passkey


import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.credentials.exceptions.GetCredentialException
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
import io.cometh.android4337.utils.toHex
import io.cometh.android4337.web3j.AbiEncoder
import kotlinx.coroutines.runBlocking
import org.web3j.abi.DefaultFunctionEncoder
import org.web3j.abi.datatypes.DynamicBytes
import org.web3j.abi.datatypes.generated.StaticArray2
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.abi.datatypes.generated.Uint48
import java.math.BigInteger
import java.security.SecureRandom

class PasskeySigner(
    private val rpId: String,
    private val userName: String,
    private val context: Context,
    private val credentialsApiHelper: CredentialsApiHelper = CredentialsApiHelper(context),
    private val safeConfig: SafeConfig = SafeConfig.getDefaultConfig(),
    passkey: Passkey? = null
) : Signer {

    val DUMMY_AUTHENTICATOR_DATA = "0xfefefefefefefefefefefefefefefefefefefefefefefefefefefefefefefefefe04fefefefe"
    val DUMMY_CLIENT_DATA_FIELDS = """
        "origin":"http://safe.global"
        "padding":"This pads the clientDataJSON so that we can leave room for additional implementation specific fields for a more accurate 'preVerificationGas' estimate."
    """.trimIndent()

    private val prefs: SharedPreferences = context.getSharedPreferences("passkey-$rpId-$userName", Context.MODE_PRIVATE)

    private var _passkey: Passkey? = passkey

    init {
        require(rpId.isNotEmpty()) { "rpId must be set" }
        require(userName.isNotEmpty()) { "userName must be set" }
        if (_passkey == null && hasSavedPasskey()) loadPasskey()
    }


    private fun loadPasskey() {
        val x = prefs.getString("x", null)!!.hexToBigInt()
        val y = prefs.getString("y", null)!!.hexToBigInt()
        _passkey = Passkey(x, y)
    }

    private fun hasSavedPasskey(): Boolean {
        return prefs.contains("x") && prefs.contains("y")
    }

    /**
     * @throws CreateCredentialException
     */
    suspend fun createPasskey() {
        require(!hasSavedPasskey()) { "passkey already created for rpId=${rpId} and userName=${userName}" }
        require(_passkey == null) { "passkey already loaded" }
        require(userName.isNotEmpty()) { "userName must be set" }
        val createResponse = credentialsApiHelper.createCredential(
            rpId = rpId,
            rpName = "",
            userId = userName,
            userName = userName,
            challenge = SecureRandom().generateSeed(32)
        )
        val (x, y) = createResponse.response.getPublicKeyCoordinates()
        _passkey = Passkey(x, y)
        savePasskeyInPrefs()
    }

    private fun savePasskeyInPrefs() {
        prefs.edit {
            putString("x", _passkey!!.x.toHex())
            putString("y", _passkey!!.y.toHex())
        }
    }


    override fun sign(data: ByteArray): ByteArray {
        val authResponse = try {
            runBlocking { credentialsApiHelper.getCredential(rpId, data) }
        } catch (e: GetCredentialException) {
            throw SignerException("Failed to get credential", e)
        }
        val signatureDecoded = authResponse.response.getSignatureDecoded()
        val (r, s) = PasskeyUtils.extractRSFromSignature(signatureDecoded)

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
        requireNotNull(_passkey) { "PasskeySigner must have a pass key created or imported" }
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

    fun getPasskey(): Passkey? {
        return _passkey
    }
}

fun GetCredentialAuthenticationResponse.extractClientDataFields(): ByteArray? {
    val dataFields = String(response.getClientDataJSONDecoded())
    val matchResult = Regex("""^\{"type":"webauthn.get","challenge":"[A-Za-z0-9\-_]{43}",(.*)\}$""").find(dataFields) ?: return null
    val fields = matchResult.groupValues[1]
    return fields.toByteArray()
}