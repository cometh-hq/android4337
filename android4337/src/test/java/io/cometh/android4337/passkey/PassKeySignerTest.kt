package io.cometh.android4337.passkey

import android.content.Context
import io.cometh.android4337.safe.SafeConfig
import io.cometh.android4337.safe.signer.passkey.CredentialsApiHelper
import io.cometh.android4337.safe.signer.passkey.PassKeySigner
import io.cometh.android4337.safe.signer.passkey.PassKeyUtils
import io.cometh.android4337.safe.signer.passkey.credentials.GetCredentialAuthenticationResponse
import io.cometh.android4337.safe.signer.passkey.credentials.GetCredentialAuthenticationResponseContent
import io.cometh.android4337.utils.hexToByteArray
import io.cometh.android4337.utils.toHex
import io.cometh.android4337.utils.toHexNoPrefix
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import java.util.Base64


class PassKeySignerTest {


    @Test
    fun extractRS() {
        val signature = "MEQCIAZnp2j6bRUj49CFmhuHI_RKh_8puFto169kkI5mLsq8AiALHKJ9q5ogwIKKyxuA2GEyY-SAH5WIqpzoOno0T4FONQ"
        val signatureData = decodeBase64Url(signature)
        val (r, s) = PassKeyUtils.extractRSFromSignature(signatureData)
        Assert.assertEquals("0667a768fa6d1523e3d0859a1b8723f44a87ff29b85b68d7af64908e662ecabc", r.toHexNoPrefix())
        Assert.assertEquals("0b1ca27dab9a20c0828acb1b80d8613263e4801f9588aa9ce83a7a344f814e35", s.toHexNoPrefix())
    }

    @Test
    fun publicKeyToXYCoordinates() {
        val publicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEeZFfXkVoEtY2GkLSYvgSFD9Ryt6QE4b_8bP__AdJavVZhJt7oLAybNYIJ2RSH5qDGkmdITCScDxsDQeG7KZBGA"
        val (x, y) = PassKeyUtils.publicKeyToXYCoordinates(decodeBase64Url(publicKey))
        Assert.assertEquals("0x79915f5e456812d6361a42d262f812143f51cade901386fff1b3fffc07496af5", x.toHex())
        Assert.assertEquals("0x59849b7ba0b0326cd6082764521f9a831a499d213092703c6c0d0786eca64118", y.toHex())
    }

    @Test
    fun sign() {
        val credentialsApiHelper = mockk<CredentialsApiHelper>()
        val signer = PassKeySigner(
            rpId = "cometh",
            context = mockk<Context>(),
            credentialsApiHelper = credentialsApiHelper,
            safeConfig = SafeConfig.getDefaultConfig()
        )
        val clientDataJSON =
            "0x7b2274797065223a22776562617574686e2e676574222c226368616c6c656e6765223a22776d3951626c6f47494f6d2d46746b6e565363797a3741765434507335476a52376b65446667674c366649222c226f726967696e223a22687474703a2f2f6c6f63616c686f73743a35313733222c2263726f73734f726967696e223a66616c73657d"
        val signature =
            "0x3045022064b172917120427a045720e4d1f7cbabf7af950d17bfefb9e81a07772113c28f022100d82b4d7640fbda9de76fe79f272194e96a59ddddc937f3c7eea715650b1ac74a"
        val authenticatorData = "0x49960de5880e8c687434170f6476605b8fe4aeb9a28632c7995cf3ba831d97630500000000"

        // create a mock
        val contentMock = mockk<GetCredentialAuthenticationResponseContent>()
        every { contentMock.getClientDataJSONDecoded() } returns clientDataJSON.hexToByteArray()
        every { contentMock.getSignatureDecoded() } returns signature.hexToByteArray()
        every { contentMock.getAuthenticatorDataDecoded() } returns authenticatorData.hexToByteArray()

        coEvery { credentialsApiHelper.getCredential(any(), any()) }.returns(
            GetCredentialAuthenticationResponse(
                rawId = "",
                authenticatorAttachment = "",
                type = "",
                id = "",
                response = contentMock,
                clientExtensionResults = mapOf()
            )
        )
        val result = runBlocking { signer.sign("0xaaaa".hexToByteArray()) }
        Assert.assertEquals(
            "0x000000000000000000000000fd90fad33ee8b58f32c00aceead1358e4afc23f90000000000000000000000000000000000000000000000000000000000000041000000000000000000000000000000000000000000000000000000000000000140000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000e064b172917120427a045720e4d1f7cbabf7af950d17bfefb9e81a07772113c28fd82b4d7640fbda9de76fe79f272194e96a59ddddc937f3c7eea715650b1ac74a000000000000000000000000000000000000000000000000000000000000002549960de5880e8c687434170f6476605b8fe4aeb9a28632c7995cf3ba831d976305000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000034226f726967696e223a22687474703a2f2f6c6f63616c686f73743a35313733222c2263726f73734f726967696e223a66616c7365000000000000000000000000",
            result.toHex()
        )
    }
}

fun decodeBase64Url(base64Url: String): ByteArray {
    val base64 = base64Url.replace('-', '+').replace('_', '/')
    val padding = when (base64.length % 4) {
        2 -> "=="
        3 -> "="
        else -> ""
    }
    val base64WithPadding = base64 + padding
    return Base64.getDecoder().decode(base64WithPadding)
}

