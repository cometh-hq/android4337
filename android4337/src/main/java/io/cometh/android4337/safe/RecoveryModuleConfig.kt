package io.cometh.android4337.safe

import io.cometh.android4337.utils.requireHexAddress
import io.cometh.android4337.utils.requirePositive


data class RecoveryModuleConfig(
    val moduleFactoryAddress: String = "0x000000000000aDdB49795b0f9bA5BC298cDda236",
    val delayModuleAddress: String = "0xd54895B1121A2eE3f37b502F507631FA1331BED6",
    val recoveryCooldown: Int = 86400,
    val recoveryExpiration: Int = 604800,
) {
    init {
        moduleFactoryAddress.requireHexAddress()
        delayModuleAddress.requireHexAddress()
        recoveryCooldown.requirePositive()
        recoveryExpiration.requirePositive()
    }
}