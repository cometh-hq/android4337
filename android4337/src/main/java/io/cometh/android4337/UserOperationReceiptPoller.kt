package io.cometh.android4337

import io.cometh.android4337.bundler.BundlerClient
import io.cometh.android4337.bundler.response.UserOperationReceipt
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

class UserOperationReceiptPoller(
    private val bundlerClient: BundlerClient,
    private val timeoutInSeconds: Int = 30
) {
    suspend fun waitForReceipt(userOperationHash: String): UserOperationReceipt? {
        val result = withTimeoutOrNull(timeoutInSeconds.seconds) {
            val output: UserOperationReceipt
            while (true) {
                val resp = bundlerClient.ethGetUserOperationReceipt(userOperationHash).send()
                if (resp.result != null) {
                    output = resp.result
                    break
                }
                delay(1.seconds)
            }
            output
        }
        return result
    }
}