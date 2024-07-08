package io.cometh.android4337.passkey

import io.cometh.android4337.utils.toHexNoPrefix
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
    fun buildSignatureBytes() {
        val signatureBytes = buildSignatureBytes(
            listOf(
                SafeSignature(
                    signer = "0xfD90FAd33ee8b58f32c00aceEad1358e4AFC23f9",
                    data = "0xaaaaaa",
                    dynamic = true
                )
            )
        )
        Assert.assertEquals(
            "0x000000000000000000000000fD90FAd33ee8b58f32c00aceEad1358e4AFC23f90000000000000000000000000000000000000000000000000000000000000041000000000000000000000000000000000000000000000000000000000000000003aaaaaa",
            signatureBytes
        )
    }

    @Test
    fun publicKeyToXYCoordinates() {
        val publicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEeZFfXkVoEtY2GkLSYvgSFD9Ryt6QE4b_8bP__AdJavVZhJt7oLAybNYIJ2RSH5qDGkmdITCScDxsDQeG7KZBGA"
        val (x, y) = publicKeyToXYCoordinates(decodeBase64Url(publicKey))
        Assert.assertEquals("0x79915f5e456812d6361a42d262f812143f51cade901386fff1b3fffc07496af5", x)
        Assert.assertEquals("0x59849b7ba0b0326cd6082764521f9a831a499d213092703c6c0d0786eca64118", y)
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