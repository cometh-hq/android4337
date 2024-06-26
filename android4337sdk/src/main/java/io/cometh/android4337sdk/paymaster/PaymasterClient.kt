package io.cometh.android4337sdk.paymaster

import io.cometh.android4337sdk.UserOperation
import io.cometh.android4337sdk.bundler.DUMMY_SIGNATURE
import io.cometh.android4337sdk.toEncodedMap
import org.web3j.protocol.core.Request
import org.web3j.protocol.http.HttpService

class PaymasterClient(
    paymasterUrl: String
) {

    private val web3jService = HttpService(paymasterUrl)

    fun pmSponsorUserOperation(
        userOperation: UserOperation,
        entryPoint: String
    ): Request<Any, SponsorUserOperationResponse> {
        val data = userOperation.toEncodedMap().toMutableMap()
        data["signature"] = DUMMY_SIGNATURE
        return Request(
            "pm_sponsorUserOperation",
            listOf(data, entryPoint),
            web3jService,
            SponsorUserOperationResponse::class.java
        )
    }

    fun pmSupportedEntryPoints(): SupportedEntryPointsResponse {
        val resp = Request(
            "pm_supportedEntryPoints",
            emptyList<String>(),
            web3jService,
            SupportedEntryPointsResponse::class.java
        ).send()
        if (resp.hasError()) {
            throw RuntimeException("Error: ${resp.error?.message}")
        }
        return resp
    }
}