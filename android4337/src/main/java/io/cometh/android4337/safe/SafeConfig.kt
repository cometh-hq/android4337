package io.cometh.android4337.safe

import io.cometh.android4337.utils.requireHexAddress

data class SafeConfig(
    val safeModuleSetupAddress: String,
    val erc4337ModuleAddress: String,
    val safeSingletonL2Address: String,
    val safeProxyFactoryAddress: String,
) {
    companion object {
        fun createDefaultConfig(): SafeConfig {
            return SafeConfig(
                "0x2dd68b007B46fBe91B9A7c3EDa5A7a1063cB5b47",
                "0x75cf11467937ce3F2f357CE24ffc3DBF8fD5c226",
                "0x29fcB43b46531BcA003ddC8FCB67FFE91900C762",
                "0x4e1DCf7AD4e460CfD30791CCC4F9c8a4f820ec67"
            )
        }
    }
    init {
        safeModuleSetupAddress.requireHexAddress()
        erc4337ModuleAddress.requireHexAddress()
        safeSingletonL2Address.requireHexAddress()
        safeProxyFactoryAddress.requireHexAddress()
    }
}

