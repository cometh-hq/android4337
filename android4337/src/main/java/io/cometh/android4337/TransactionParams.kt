package io.cometh.android4337

import io.cometh.android4337.utils.requireHex
import io.cometh.android4337.utils.requireHexAddress

data class TransactionParams(
    val to: String,
    val value: String,
    val data: String
) {
    init {
        to.requireHexAddress()
        value.requireHex()
        data.requireHex()
    }
}