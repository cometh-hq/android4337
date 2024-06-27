package io.cometh.android4337.bundler

import io.cometh.android4337.UserOperation
import io.cometh.android4337.bundler.response.EthEstimateUserOperationGasResponse
import io.cometh.android4337.bundler.response.EthGetUserOperationByHashResponse
import io.cometh.android4337.bundler.response.EthGetUserOperationReceiptResponse
import io.cometh.android4337.bundler.response.EthSendUserOperationResponse
import io.cometh.android4337.toEncodedMap
import io.cometh.android4337.utils.requireHexAddress
import org.web3j.protocol.Web3jService
import org.web3j.protocol.core.Request
import org.web3j.protocol.http.HttpService

val DUMMY_SIGNATURE =
    "0xfffffffffffffffffffffffffffffff0000000000000000000000000000000007aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1c"

class SimpleBundlerClient(private val web3jService: Web3jService) : BundlerClient {

    override fun ethSendUserOperation(userOperation: UserOperation, entryPointAddress: String): Request<Any, EthSendUserOperationResponse> {
        val data = userOperation.toEncodedMap()
        return Request(
            "eth_sendUserOperation",
            listOf(data, entryPointAddress),
            web3jService,
            EthSendUserOperationResponse::class.java
        )
    }

    override fun ethEstimateUserOperationGas(
        userOperation: UserOperation,
        entryPointAddress: String
    ): Request<Any, EthEstimateUserOperationGasResponse> {
        entryPointAddress.requireHexAddress()
        val data = userOperation.toEncodedMap().toMutableMap()
        data["signature"] = DUMMY_SIGNATURE
        return Request(
            "eth_estimateUserOperationGas",
            listOf(data, entryPointAddress),
            web3jService,
            EthEstimateUserOperationGasResponse::class.java
        )
    }

    override fun ethGetUserOperationReceipt(userOperationHash: String): Request<String, EthGetUserOperationReceiptResponse> {
        return Request(
            "eth_getUserOperationReceipt",
            listOf(userOperationHash),
            web3jService,
            EthGetUserOperationReceiptResponse::class.java
        )
    }

    override fun ethGetUserOperationByHash(userOperationHash: String): Request<String, EthGetUserOperationByHashResponse> {
        return Request(
            "eth_getUserOperationByHash",
            listOf(userOperationHash),
            web3jService,
            EthGetUserOperationByHashResponse::class.java
        )
    }
}

