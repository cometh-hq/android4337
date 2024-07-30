package io.cometh.android4337.bundler

import io.cometh.android4337.CustomHttpService
import io.cometh.android4337.EntryPointContract
import io.cometh.android4337.HttpResponseStub
import io.cometh.android4337.UserOperation
import io.cometh.android4337.toInputStream
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SimpleBundlerClientTest {

    @MockK
    lateinit var httpResponseStub: HttpResponseStub

    lateinit var bundlerClient: BundlerClient

    val userOperation = UserOperation(
        sender = "0x2FF46F26638977AE8C88e205cCa407A1a9725F0B",
        nonce = "0x05",
        callData = "0x7bb374280000000000000000000000000338dcd5512ae8f3c481c33eb4b6eedf632d1d2f000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000406661abd00000000000000000000000000000000000000000000000000000000",
        callGasLimit = "0x163a2",
        preVerificationGas = "0xef1c",
        verificationGasLimit = "0x1b247",
        maxFeePerGas = "0x01e3fb094e",
        maxPriorityFeePerGas = "0x53cd81aa",
        paymasterData = "0x",
        signature = "0x0000000000000000000000004232f7414022b3da2b1b3fc2d82d40a10eefc29c913c6801c1827dcb1c3735c8065234a4435ec0ca3a13786ecd683320661a5abb2b1dd2c2b3fc8dcf1473fcd81c"
    )

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        bundlerClient = SimpleBundlerClient(CustomHttpService(httpResponseStub))
    }

    @Test
    fun ethEstimateUserOperationGas() {
        every { httpResponseStub.getResponse(any()) } returns """
            {
                "jsonrpc": "2.0",
                "id": 1,
                "result": {
                    "preVerificationGas": "0xEC2C",
                    "verificationGasLimit": "0x45BCA",
                    "callGasLimit": "0x2F44"
                }
            }
        """.trimIndent().toInputStream()

        val resp = bundlerClient.ethEstimateUserOperationGas(
            userOperation,
            EntryPointContract.ENTRY_POINT_ADDRESS_V7
        ).send()
        val result = resp.result
        assertEquals("0x2F44", result.callGasLimit)
        assertEquals("0xEC2C", result.preVerificationGas)
        assertEquals("0x45BCA", result.verificationGasLimit)
    }

    @Test
    fun ethGetUserOperationReceipt() {
        //TODO test ethGetUserOperationReceipt
    }

    @Test
    fun ethGetUserOperationByHash() {
        //TODO test ethGetUserOperationByHash
    }

    @Test
    fun ethSupportedEntryPoints() {
        every { httpResponseStub.getResponse(any()) } returns """
            {
            	"jsonrpc": "2.0",
            	"id": 1,
            	"result": [
            		"0x5FF137D4b0FDCD49DcA30c7CF57E578a026d2789",
            		"0x0000000071727De22E5E9d8BAf0edAc6f37da032"
            	]
            }
        """.trimIndent().toInputStream()
        val resp = bundlerClient.ethSupportedEntryPoints().send()
        val result = resp.result
        assertEquals("0x5FF137D4b0FDCD49DcA30c7CF57E578a026d2789", result[0])
        assertEquals("0x0000000071727De22E5E9d8BAf0edAc6f37da032", result[1])
    }

    @Test
    fun ethSendUserOperation() {
        every { httpResponseStub.getResponse(any()) } returns """
            {
                "jsonrpc": "2.0",
                "id": 1,
                "result": "0xb38a2faf4b5c716eff634af472206f28574cd5104c69d97a315c3303ddb5fdbd"
            }
        """.trimIndent().toInputStream()

        bundlerClient.ethSendUserOperation(
            userOperation,
            EntryPointContract.ENTRY_POINT_ADDRESS_V7
        ).send()
    }
}
