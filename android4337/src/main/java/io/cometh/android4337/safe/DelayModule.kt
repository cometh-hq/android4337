package io.cometh.android4337.safe

import io.cometh.android4337.utils.encode
import io.cometh.android4337.utils.hexToByteArray
import io.cometh.android4337.utils.removeOx
import io.cometh.android4337.utils.send
import io.cometh.android4337.utils.toChecksumHexNoPrefix
import io.cometh.android4337.web3j.Create2
import org.web3j.abi.DefaultFunctionEncoder
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.abi.datatypes.DynamicBytes
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Hash
import org.web3j.protocol.Web3jService
import java.math.BigInteger


class DelayModule {
    companion object {

        fun predictAddress(
            safeAddress: Address,
            recoveryModuleConfig: RecoveryModuleConfig
        ): String {
            val initializer = setUpFunctionData(recoveryModuleConfig, safeAddress)
            val moduleAddress = recoveryModuleConfig.delayModuleAddress
            val initCodeHash = Hash.sha3("0x602d8060093d393df3363d3d373d3d3d363d73${moduleAddress.removeOx()}5af43d82803e903d91602b57fd5bf3")
            val salt = Hash.sha3("${Hash.sha3(initializer)}${safeAddress.toChecksumHexNoPrefix().padStart(64, '0')}")
            val moduleFactoryAddress = recoveryModuleConfig.moduleFactoryAddress
            return Create2.getCreate2Address(moduleFactoryAddress.removeOx(), salt, initCodeHash)
        }

        //  getModulesPaginated(address start, uint256 pageSize) returns (address[] memory array, address next) {
        fun getModulesPaginated(
            web3jService: Web3jService,
            contractAddress: Address,
            start: Address,
            pageSize: Long
        ): List<Address> {
            // getModulesPaginated(address start, uint256 pageSize) external view override returns (address[] memory array, address next)
            val inputParams = listOf(start, Uint256(pageSize))
            val outputParams = listOf(
                object : TypeReference<DynamicArray<Address>>() {},
                object : TypeReference<Address>() {}
            )
            val result = Function("getModulesPaginated", inputParams, outputParams)
                .send(web3jService, contractAddress)
            if (result.isEmpty()) return emptyList()
            return (result[0] as DynamicArray<Address>).value
        }

        // txNonce() returns (uint256)
        fun txNonce(web3jService: Web3jService, contractAddress: Address): BigInteger {
            return Function(
                "txNonce",
                emptyList(),
                listOf(object : TypeReference<Uint256>() {})
            )
                .send(web3jService, contractAddress)
                .let { (it[0] as Uint256).value }
        }

        fun queueNonce(web3jService: Web3jService, contractAddress: Address): BigInteger {
            return Function(
                "queueNonce",
                emptyList(),
                listOf(object : TypeReference<Uint256>() {})
            )
                .send(web3jService, contractAddress)
                .let { (it[0] as Uint256).value }
        }

        fun setTxNonceFunctionData(
            nonce: BigInteger
        ): String =
            Function("setTxNonce", listOf(Uint256(nonce)), emptyList<TypeReference<*>>()).encode()

        // setUp(bytes memory initParams)
        fun setUpFunctionData(recoveryModuleConfig: RecoveryModuleConfig, safeAddress: Address): String {
            val recoveryCooldown = recoveryModuleConfig.recoveryCooldown
            val recoveryExpiration = recoveryModuleConfig.recoveryExpiration
            val initParams = DefaultFunctionEncoder().encodeParameters(
                listOf(
                    safeAddress,
                    safeAddress,
                    safeAddress,
                    Uint256(recoveryCooldown.toBigInteger()),
                    Uint256(recoveryExpiration.toBigInteger())
                )
            )
            val inputParams = listOf(DynamicBytes("0x$initParams".hexToByteArray()))
            val outputParams = emptyList<TypeReference<*>>()
            return FunctionEncoder.encode(Function("setUp", inputParams, outputParams))
        }
    }
}
