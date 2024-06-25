package io.cometh.android4337sdk.safe

import io.cometh.android4337sdk.utils.toAddress
import io.cometh.android4337sdk.utils.toHex
import org.junit.Test

class SafeUtilsTest {

    @Test
    fun getInitCode() {
        val enableModulesData = SafeUtils.getEnableModulesFunctionData(
            listOf(
                "0x2dd68b007B46fBe91B9A7c3EDa5A7a1063cB5b47".toAddress(),
                "0xa581c4A4DB7175302464fF3C06380BC3270b4037".toAddress()
            )
        )
    }

}