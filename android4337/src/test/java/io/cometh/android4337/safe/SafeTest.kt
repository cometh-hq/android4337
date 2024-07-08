package io.cometh.android4337.safe

import io.cometh.android4337.utils.hexToAddress
import org.junit.Test

class SafeTest {

    @Test
    fun getInitCode() {
        val enableModulesData = Safe.getEnableModulesFunctionData(
            listOf(
                "0x2dd68b007B46fBe91B9A7c3EDa5A7a1063cB5b47".hexToAddress(),
                "0xa581c4A4DB7175302464fF3C06380BC3270b4037".hexToAddress()
            )
        )
    }

}