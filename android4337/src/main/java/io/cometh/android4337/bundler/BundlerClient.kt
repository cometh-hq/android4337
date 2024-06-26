package io.cometh.android4337.bundler
import io.cometh.android4337.UserOperation
import io.cometh.android4337.bundler.response.EthEstimateUserOperationGasResponse
import io.cometh.android4337.bundler.response.EthGetUserOperationByHashResponse
import io.cometh.android4337.bundler.response.EthGetUserOperationReceiptResponse
import io.cometh.android4337.bundler.response.EthSendUserOperationResponse
import org.web3j.protocol.core.Request

interface BundlerClient {
    fun ethSendUserOperation(userOperation: UserOperation, entryPointAddress: String): Request<Any, EthSendUserOperationResponse>
    fun ethEstimateUserOperationGas(userOperation: UserOperation, entryPointAddress: String): Request<Any, EthEstimateUserOperationGasResponse>
    fun ethGetUserOperationReceipt(userOperationHash: String): Request<String, EthGetUserOperationReceiptResponse>
    fun ethGetUserOperationByHash(userOperationHash: String): Request<String, EthGetUserOperationByHashResponse>
}
