package io.cometh.android4337.safe

import io.cometh.android4337.utils.requireHexAddress

data class SafeConfig(
    val safeModuleSetupAddress: String,
    val erc4337ModuleAddress: String,
    val safeSingletonL2Address: String,
    val safeProxyFactoryAddress: String,
) {
    init {
        safeModuleSetupAddress.requireHexAddress()
        erc4337ModuleAddress.requireHexAddress()
        safeSingletonL2Address.requireHexAddress()
        safeProxyFactoryAddress.requireHexAddress()
    }
}