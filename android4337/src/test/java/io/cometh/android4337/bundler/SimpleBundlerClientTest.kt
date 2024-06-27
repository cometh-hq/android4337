package io.cometh.android4337.bundler

import io.cometh.android4337.ENTRY_POINT_ADDRESS
import io.cometh.android4337.UserOperation
import io.cometh.android4337.bundler.response.EthEstimateUserOperationGasResponse
import io.cometh.android4337.bundler.response.UserOperationGasEstimation
import io.cometh.android4337.utils.hexStringToBigInt
import io.cometh.android4337.utils.hexStringToByteArray
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.web3j.protocol.Web3jService
import java.math.BigInteger

class SimpleBundlerClientTest {

    @MockK
    lateinit var web3jService: Web3jService


    lateinit var bundlerClient: BundlerClient

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        bundlerClient = SimpleBundlerClient(web3jService)
    }

    @Test
    fun ethEstimateUserOperationGas() {
        val userOperation = UserOperation(
            sender = "0x2FF46F26638977AE8C88e205cCa407A1a9725F0B",
            nonce = "0x05".hexStringToBigInt(),
            initCode = "0x".hexStringToByteArray(),
            callData = "0x7bb374280000000000000000000000000338dcd5512ae8f3c481c33eb4b6eedf632d1d2f000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000406661abd00000000000000000000000000000000000000000000000000000000".hexStringToByteArray(),
            callGasLimit = "0x163a2".hexStringToBigInt(),
            preVerificationGas = "0xef1c".hexStringToBigInt(),
            verificationGasLimit = "0x1b247".hexStringToBigInt(),
            maxFeePerGas = "0x01e3fb094e".hexStringToBigInt(),
            maxPriorityFeePerGas = "0x53cd81aa".hexStringToBigInt(),
            paymasterAndData = "0x".hexStringToByteArray(),
            signature = "0x0000000000000000000000004232f7414022b3da2b1b3fc2d82d40a10eefc29c913c6801c1827dcb1c3735c8065234a4435ec0ca3a13786ecd683320661a5abb2b1dd2c2b3fc8dcf1473fcd81c".hexStringToByteArray()
        )

        val respData = EthEstimateUserOperationGasResponse().apply {
            setResult(
                UserOperationGasEstimation(
                    preVerificationGas = "0xEC2C",
                    verificationGasLimit = "0x45BCA",
                    callGasLimit = "0x2F44"
                )
            )
        }
        every { web3jService.send<EthEstimateUserOperationGasResponse>(any(), any()) } returns respData


        val resp = bundlerClient.ethEstimateUserOperationGas(
            userOperation,
            ENTRY_POINT_ADDRESS
        ).send()
        val result = resp.result
        assertEquals(BigInteger.valueOf(12100), result.getCallGasLimit())
        assertEquals(BigInteger.valueOf(60460), result.getPreVerificationGas())
        assertEquals(BigInteger.valueOf(285642), result.getVerificationGasLimit())
    }

    @Test
    fun ethGetUserOperationReceipt() {
        //TODO test ethGetUserOperationReceipt
    }

    @Test
    fun ethGetUserOperationByHash() {
        //TODO test ethGetUserOperationByHash
    }
}