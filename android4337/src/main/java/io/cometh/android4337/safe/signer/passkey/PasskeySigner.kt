package io.cometh.android4337.safe.signer.passkey


import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.credentials.exceptions.GetCredentialException
import io.cometh.android4337.safe.Safe
import io.cometh.android4337.safe.Safe.getSignatureBytes
import io.cometh.android4337.safe.SafeConfig
import io.cometh.android4337.safe.SafeSignature
import io.cometh.android4337.safe.SafeWebAuthnSignerFactoryContract
import io.cometh.android4337.safe.signer.Signer
import io.cometh.android4337.safe.signer.SignerException
import io.cometh.android4337.safe.signer.passkey.credentials.GetCredentialAuthenticationResponse
import io.cometh.android4337.safe.signer.passkey.credentials.getPublicKeyCoordinates
import io.cometh.android4337.utils.hexToBigInt
import io.cometh.android4337.utils.hexToByteArray
import io.cometh.android4337.utils.toChecksumHex
import io.cometh.android4337.utils.toHex
import io.cometh.android4337.web3j.AbiEncoder
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import okio.IOException
import org.web3j.abi.DefaultFunctionEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.DynamicBytes
import org.web3j.abi.datatypes.generated.StaticArray2
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.abi.datatypes.generated.Uint48
import org.web3j.protocol.Web3jService
import java.math.BigInteger
import java.security.SecureRandom

class PasskeySigner private constructor(
    private val rpId: String,
    private val userName: String,
    private val signerAddress: Address,
    val passkey: Passkey,
    private val credentialsApiHelper: CredentialsApiHelper,
) : Signer {

    val DUMMY_AUTHENTICATOR_DATA = "0xfefefefefefefefefefefefefefefefefefefefefefefefefefefefefefefefefe04fefefefe"
    val DUMMY_CLIENT_DATA_FIELDS = """
        "origin":"http://safe.global"
        "padding":"This pads the clientDataJSON so that we can leave room for additional implementation specific fields for a more accurate 'preVerificationGas' estimate."
    """.trimIndent()

    companion object {

        fun hasSavedPasskey(context: Context, rpId: String, userName: String): Boolean {
            return getSharedPrefs(rpId, userName, context).getOrCreatePasskey() != null
        }

        /**
         * @throws CreateCredentialException
         */
        suspend fun withSharedSigner(
            context: Context,
            rpId: String,
            userName: String,
            safeConfig: SafeConfig = SafeConfig.getDefaultConfig(),
            passkey: Passkey? = null,
            credentialsApiHelper: CredentialsApiHelper = CredentialsApiHelper(context),
        ): PasskeySigner {
            getOrCreatePasskey(passkey, rpId, userName, context, credentialsApiHelper).let {
                return PasskeySigner(rpId, userName, safeConfig.getSafeWebAuthnSharedSignerAddress(), it, credentialsApiHelper)
            }
        }

        suspend fun createPasskey(rpId: String, userName: String, credentialsApiHelper: CredentialsApiHelper): Passkey {
            val createResponse = credentialsApiHelper.createCredential(
                rpId = rpId,
                rpName = "",
                userId = userName,
                userName = userName,
                challenge = SecureRandom().generateSeed(32)
            )
            val (x, y) = createResponse.response.getPublicKeyCoordinates()
            return Passkey(x, y)
        }


        private fun getSharedPrefs(rpId: String, userName: String, context: Context): SharedPreferences {
            return context.getSharedPreferences("passkey-$rpId-$userName", Context.MODE_PRIVATE)
        }

        /**
         * @throws SignerException, CreateCredentialException
         */
        suspend fun withSigner(
            rpId: String,
            userName: String,
            context: Context,
            safeConfig: SafeConfig = SafeConfig.getDefaultConfig(),
            web3jService: Web3jService,
            credentialsApiHelper: CredentialsApiHelper = CredentialsApiHelper(context),
            passkey: Passkey? = null
        ): PasskeySigner {
            getOrCreatePasskey(passkey, rpId, userName, context, credentialsApiHelper).let {
                val factoryContract = SafeWebAuthnSignerFactoryContract(web3jService, safeConfig.getSafeWebauthnSignerFactoryAddress())
                val address = coroutineScope {
                    try {
                        factoryContract.getSigner(it.x, it.y, safeConfig.safeP256VerifierAddress.hexToBigInt())
                    } catch (e: IOException) {
                        throw SignerException("Failed to get signer address", e)
                    }
                } ?: throw SignerException("Failed to get signer address")
                return PasskeySigner(rpId, userName, address, it, credentialsApiHelper)
            }
        }

        private suspend fun getOrCreatePasskey(
            passkey: Passkey?,
            rpId: String,
            userName: String,
            context: Context,
            credentialsApiHelper: CredentialsApiHelper
        ): Passkey {
            var _passkey = passkey
            if (_passkey == null) {
                val sharedPrefs = getSharedPrefs(rpId, userName, context)
                _passkey = sharedPrefs.getOrCreatePasskey()
                if (_passkey == null) {
                    _passkey = createPasskey(rpId, userName, credentialsApiHelper)
                    sharedPrefs.savePasskey(_passkey)
                }
            }
            return _passkey
        }

    }

    init {
        require(rpId.isNotEmpty()) { "rpId must be set" }
        require(userName.isNotEmpty()) { "userName must be set" }
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
                    signer = signerAddress.toChecksumHex().lowercase(),
                    data = "0x$passkeySignature",
                    dynamic = true
                )
            )
        ).hexToByteArray()
    }

    override fun getDummySignature(): String {
        val signature = Safe.buildSignatureBytes(
            listOf(
                SafeSignature(
                    signer = signerAddress.toChecksumHex(),
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
}

fun GetCredentialAuthenticationResponse.extractClientDataFields(): ByteArray? {
    val dataFields = String(response.getClientDataJSONDecoded())
    val matchResult = Regex("""^\{"type":"webauthn.get","challenge":"[A-Za-z0-9\-_]{43}",(.*)\}$""").find(dataFields) ?: return null
    val fields = matchResult.groupValues[1]
    return fields.toByteArray()
}

private fun SharedPreferences.savePasskey(passkey: Passkey) {
    edit {
        putString("x", passkey.x.toHex())
        putString("y", passkey.y.toHex())
    }
}

private fun SharedPreferences.getOrCreatePasskey(): Passkey? {
    val x = getString("x", null)?.hexToBigInt()
    val y = getString("y", null)?.hexToBigInt()
    if (x == null || y == null) return null
    return Passkey(x, y)
}
