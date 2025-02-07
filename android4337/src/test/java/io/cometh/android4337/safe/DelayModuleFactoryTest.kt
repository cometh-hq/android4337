package io.cometh.android4337.safe

import io.cometh.android4337.utils.hexToAddress
import org.junit.Assert
import org.junit.Test

class DelayModuleFactoryTest {

    @Test
    fun getDeployModuleFunctionData() {
        val expected = "0xf1ab873c000000000000000000000000d54895b1121a2ee3f37b502f507631fa1331bed600000000000000000000000000000000000000000000000000000000000000600000000000000000000000004bf81eef3911db0615297836a8ff351f5fe08c680000000000000000000000000000000000000000000000000000000000000002aaaa000000000000000000000000000000000000000000000000000000000000"
        val config = RecoveryModuleConfig()
        DelayModuleFactory.deployModuleFunctionData(
            config.delayModuleAddress.hexToAddress(),
            "0xaaaa",
            TestsData.account1SafeAddress.hexToAddress()
        ).let {
            Assert.assertEquals(expected, it)
        }
    }
}