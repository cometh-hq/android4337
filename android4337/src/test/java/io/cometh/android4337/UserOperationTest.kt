package io.cometh.android4337

import org.junit.Assert.assertEquals
import org.junit.Test

class UserOperationTest {

    @Test
    fun getInitCode() {
        UserOperation(
            sender = "0xcfe1e7242dF565f031e1D3F645169Dda9D1230d2",
            nonce = "0x0",
            factory = "0x4e1DCf7AD4e460CfD30791CCC4F9c8a4f820ec67",
            factoryData = "0x1688f0b900000000000000000000000029fcb43b46531bca003ddc8fcb67ffe91900c7620000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001e4b63e800d000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000010000000000000000000000002dd68b007b46fbe91b9a7c3eda5a7a1063cb5b470000000000000000000000000000000000000000000000000000000000000140000000000000000000000000a581c4a4db7175302464ff3c06380bc3270b403700000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000009d8a62f656a8d1615c1294fd71e9cfb3e4855a4f00000000000000000000000000000000000000000000000000000000000000648d0dc49f00000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000001000000000000000000000000a581c4a4db7175302464ff3c06380bc3270b40370000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
            callData = "0x",
            callGasLimit = "0x00",
            verificationGasLimit = "0x00",
            preVerificationGas = "0x00",
            maxFeePerGas = "0x00",
            maxPriorityFeePerGas = "0x00",
            paymasterData = "0x"
        ).let {
            assertEquals(
                "0x4e1dcf7ad4e460cfd30791ccc4f9c8a4f820ec671688f0b900000000000000000000000029fcb43b46531bca003ddc8fcb67ffe91900c7620000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001e4b63e800d000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000010000000000000000000000002dd68b007b46fbe91b9a7c3eda5a7a1063cb5b470000000000000000000000000000000000000000000000000000000000000140000000000000000000000000a581c4a4db7175302464ff3c06380bc3270b403700000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000009d8a62f656a8d1615c1294fd71e9cfb3e4855a4f00000000000000000000000000000000000000000000000000000000000000648d0dc49f00000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000001000000000000000000000000a581c4a4db7175302464ff3c06380bc3270b40370000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
                it.getInitCode()
            )
        }
    }

    @Test
    fun getPaymasterData() {
        UserOperation(
            sender = "0xcfe1e7242dF565f031e1D3F645169Dda9D1230d2",
            nonce = "0x0",
            callData = "0x",
            callGasLimit = "0x00",
            verificationGasLimit = "0x00",
            preVerificationGas = "0x00",
            maxFeePerGas = "0x00",
            maxPriorityFeePerGas = "0x00",
            paymaster = "0x4685d9587a7F72Da32dc323bfFF17627aa632C61",
            paymasterData = "0x00000000000000000000000000000000000000000000000000000000667d1421000000000000000000000000000000000000000000000000000000000000000026e7da98c314096d74cd7fb9d2e3bf074e20dd71f91ab6e9b7c0ad4d4ac057f15ad0d942b6880daddbf9d0ff9791c05ff64528f3428c3d4f3ee45cb5c12250081c",
            paymasterVerificationGasLimit = "0x4e09",
            paymasterPostOpGasLimit = "0x1",
        ).let {
            assertEquals(
                "0x4685d9587a7f72da32dc323bfff17627aa632c6100000000000000000000000000004e090000000000000000000000000000000100000000000000000000000000000000000000000000000000000000667d1421000000000000000000000000000000000000000000000000000000000000000026e7da98c314096d74cd7fb9d2e3bf074e20dd71f91ab6e9b7c0ad4d4ac057f15ad0d942b6880daddbf9d0ff9791c05ff64528f3428c3d4f3ee45cb5c12250081c",
                it.getPaymasterAndData()
            )
        }

    }
}