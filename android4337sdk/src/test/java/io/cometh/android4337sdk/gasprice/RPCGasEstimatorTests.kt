package io.cometh.android4337sdk.gasprice

import io.cometh.android4337sdk.web3j.EthFeeHistory
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.web3j.protocol.Web3jService
import org.web3j.protocol.core.Request

class RPCGasEstimatorTests {

    @MockK
    lateinit var web3jService: Web3jService

    @MockK
    lateinit var web3jPort: Web3jPort

    @Before
    fun before() {
        MockKAnnotations.init(this)
    }

    @Test
    fun getGasFeesWithBaseMultiplier100AndPriorityMultiplier100() {
        val rpcGasEstimator = RPCGasEstimator(
            web3jService,
            baseFeePercentMultiplier = 100L,
            priorityFeePercentMultiplier = 100L,
            web3jPort
        )

        val request = mockk<Request<Any, EthFeeHistory>>()
        val ethFeeHistory = EthFeeHistory().apply {
            result = EthFeeHistory.FeeHistory(
                "0x531c",
                listOf(listOf("0x155a4548"), listOf("0x59682f00"), listOf("0x3b9aca00"), listOf("0x59682f00"), listOf("0x59682f00")),
                listOf("0xc876a5b8", "0xc5b2834c", "0xbb3a432a", "0xc3f3bbac", "0xc6617c5e", "0xc816c3d2"),
                listOf(0.4448049, 0.2881665, 0.6863933666666666, 0.5495778666666666, 0.5344412666666667)
            )
        }
        every { request.send() } returns ethFeeHistory
        every { web3jPort.ethFeeHistory(any(), any(), any()) } returns request

        val gasPrice = rpcGasEstimator.getGasPrice()
        Assert.assertEquals(1171647502.toBigInteger(), gasPrice.maxPriorityFeePerGas)
        Assert.assertEquals(3356935122.toBigInteger() + gasPrice.maxPriorityFeePerGas, gasPrice.maxFeePerGas)

    }
}