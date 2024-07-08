package io.cometh.android4337.safe

import io.cometh.android4337.utils.hexToAddress
import io.cometh.android4337.utils.requireHexAddress
import org.web3j.abi.datatypes.Address

data class SafeConfig(
    val safeModuleSetupAddress: String,
    val erc4337ModuleAddress: String,
    val safeSingletonL2Address: String,
    val safeProxyFactoryAddress: String,
    val safeWebAuthnSharedSignerAddress: String,
    val safeMultiSendAddress: String,
    val safeP256VerifierAddress: String,
) {
    companion object {
        fun createDefaultConfig(): SafeConfig {
            return SafeConfig(
                "0x2dd68b007B46fBe91B9A7c3EDa5A7a1063cB5b47",
                "0x75cf11467937ce3F2f357CE24ffc3DBF8fD5c226",
                "0x29fcB43b46531BcA003ddC8FCB67FFE91900C762",
                "0x4e1DCf7AD4e460CfD30791CCC4F9c8a4f820ec67",
                "0xfD90FAd33ee8b58f32c00aceEad1358e4AFC23f9",
                "0x38869bf66a61cF6bDB996A6aE40D5853Fd43B526",
                "0x445a0683e494ea0c5AF3E83c5159fBE47Cf9e765"
            )
        }
    }
    init {
        safeModuleSetupAddress.requireHexAddress()
        erc4337ModuleAddress.requireHexAddress()
        safeSingletonL2Address.requireHexAddress()
        safeProxyFactoryAddress.requireHexAddress()
        safeP256VerifierAddress.requireHexAddress()
        safeMultiSendAddress.requireHexAddress()
        safeWebAuthnSharedSignerAddress.requireHexAddress()
    }

    fun getSafeModuleSetupAddress(): Address {
        return safeModuleSetupAddress.hexToAddress()
    }

    fun getErc4337ModuleAddress(): Address {
        return erc4337ModuleAddress.hexToAddress()
    }

    fun getSafeSingletonL2Address(): Address {
        return safeSingletonL2Address.hexToAddress()
    }

    fun getSafeProxyFactoryAddress(): Address {
        return safeProxyFactoryAddress.hexToAddress()
    }

    fun getSafeWebAuthnSharedSignerAddress(): Address {
        return safeWebAuthnSharedSignerAddress.hexToAddress()
    }

    fun getSafeMultiSendAddress(): Address {
        return safeMultiSendAddress.hexToAddress()
    }

    fun getSafeP256VerifierAddress(): Address {
        return safeP256VerifierAddress.hexToAddress()
    }

}

