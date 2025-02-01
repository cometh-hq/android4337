package io.cometh.android4337.safe

import io.cometh.android4337.utils.hexToAddress
import org.junit.Assert
import org.junit.Test
import java.math.BigInteger

class DelayModuleTest {

    @Test
    fun predictAddress() {
        val expected = "0x9b24CE7A4d940920c479f26d9460F6195C1e86ab"
        val delayAddress = DelayModule.predictAddress(
            TestsData.account1SafeAddress.hexToAddress(),
            RecoveryModuleConfig()
        )
        Assert.assertEquals(expected, delayAddress)
    }

    @Test
    fun getSetTxNonceFunctionData() {
        val expected = "0x46ba23070000000000000000000000000000000000000000000000000000000000000001"
        val setTxNonceFunctionData = DelayModule.setTxNonceFunctionData(BigInteger.ONE)
        Assert.assertEquals(expected, setTxNonceFunctionData)
    }

    @Test
    fun getSetUpFunctionData() {
        val expected = "0xa4f9edbf000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000000a00000000000000000000000004bf81eef3911db0615297836a8ff351f5fe08c680000000000000000000000004bf81eef3911db0615297836a8ff351f5fe08c680000000000000000000000004bf81eef3911db0615297836a8ff351f5fe08c6800000000000000000000000000000000000000000000000000000000000151800000000000000000000000000000000000000000000000000000000000093a80"
        val setUpFunctionData = DelayModule.setUpFunctionData(
            RecoveryModuleConfig(),
            TestsData.account1SafeAddress.hexToAddress(),
        )
        Assert.assertEquals(expected, setUpFunctionData)
    }
}