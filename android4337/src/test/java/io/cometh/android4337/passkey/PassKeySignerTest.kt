package io.cometh.android4337.passkey

import android.content.Context
import androidx.credentials.CredentialManager
import io.cometh.android4337.gasprice.UserOperationGasPriceProvider
import io.cometh.android4337.passkey.credentials.GetCredentialAuthenticationResponse
import io.cometh.android4337.passkey.credentials.GetCredentialAuthenticationResponseContent
import io.cometh.android4337.safe.Safe
import io.cometh.android4337.utils.encodeBase64
import io.cometh.android4337.utils.hexToByteArray
import io.cometh.android4337.utils.toHex
import io.cometh.android4337.utils.toHexNoPrefix
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
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
        val (r, s) = extractRS(signatureData)
        Assert.assertEquals("0667a768fa6d1523e3d0859a1b8723f44a87ff29b85b68d7af64908e662ecabc", r.toHexNoPrefix())
        Assert.assertEquals("0b1ca27dab9a20c0828acb1b80d8613263e4801f9588aa9ce83a7a344f814e35", s.toHexNoPrefix())
    }

    @Test
    fun publicKeyToXYCoordinates() {
        val publicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEeZFfXkVoEtY2GkLSYvgSFD9Ryt6QE4b_8bP__AdJavVZhJt7oLAybNYIJ2RSH5qDGkmdITCScDxsDQeG7KZBGA"
        val (x, y) = publicKeyToXYCoordinates(decodeBase64Url(publicKey))
        Assert.assertEquals("0x79915f5e456812d6361a42d262f812143f51cade901386fff1b3fffc07496af5", x.toHex())
        Assert.assertEquals("0x59849b7ba0b0326cd6082764521f9a831a499d213092703c6c0d0786eca64118", y.toHex())
    }

    @Test
    fun sign() {
        val credentialsApiHelper = mockk<CredentialsApiHelper>()
        val signer = PassKeySigner(
            rpId = "cometh",
            context = mockk<Context>(),
            credentialManager = mockk<CredentialManager>(),
            credentialsApiHelper = credentialsApiHelper
        )
        val clientDataJSON =
            "0x7b2274797065223a22776562617574686e2e676574222c226368616c6c656e6765223a225932414653364c316556687753416a6a5449436657617347624e41334b727856386942324a4f6837683349222c226f726967696e223a22687474703a2f2f6c6f63616c686f73743a35313734222c2263726f73734f726967696e223a66616c73652c226f746865725f6b6579735f63616e5f62655f61646465645f68657265223a22646f206e6f7420636f6d7061726520636c69656e74446174614a534f4e20616761696e737420612074656d706c6174652e205365652068747470733a2f2f676f6f2e676c2f796162506578227d"
        val signature =
            "0x30450221009e2b0fe92bc0efda5f6218ecac0d0a5f1aeb084cb3d2e9b19c9331cd5c1067360220105dae3a95ca9ff8c88daedb8345909d6c8ca38d7236b6a53ee76c9b08fcdbf8"
        val authenticatorData = "0x49960de5880e8c687434170f6476605b8fe4aeb9a28632c7995cf3ba831d97630500000007"

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
        val result = runBlocking {
            signer.sign(
                "0x819441c91f95e337941f16f6a10bac450aecacc4910028658ddf6895843c4611".hexToByteArray()
            )
        }
        Assert.assertEquals(
            "0x000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000e09e2b0fe92bc0efda5f6218ecac0d0a5f1aeb084cb3d2e9b19c9331cd5c106736105dae3a95ca9ff8c88daedb8345909d6c8ca38d7236b6a53ee76c9b08fcdbf8000000000000000000000000000000000000000000000000000000000000002549960de5880e8c687434170f6476605b8fe4aeb9a28632c7995cf3ba831d9763050000000700000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000a1226f726967696e223a22687474703a2f2f6c6f63616c686f73743a35313734222c2263726f73734f726967696e223a66616c73652c226f746865725f6b6579735f63616e5f62655f61646465645f68657265223a22646f206e6f7420636f6d7061726520636c69656e74446174614a534f4e20616761696e737420612074656d706c6174652e205365652068747470733a2f2f676f6f2e676c2f7961625065782200000000000000000000000000000000000000000000000000000000000000",
            result
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

fun encodeBase64Url(data: ByteArray): String {
    val base64 = Base64.getEncoder().encodeToString(data)
    return base64.replace('+', '-').replace('/', '_').replace("=", "")
}