package io.cometh.android4337.safe

import io.cometh.android4337.utils.requireHex
import io.cometh.android4337.utils.requireHexAddress

data class SafeSignature(
    val signer: String,
    val data: String,
    val dynamic: Boolean
) {
    init {
        signer.requireHexAddress()
        data.requireHex()
    }
}
