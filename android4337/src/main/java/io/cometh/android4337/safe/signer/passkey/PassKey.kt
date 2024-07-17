package io.cometh.android4337.safe.signer.passkey

import java.math.BigInteger

data class PassKey(
    val x: BigInteger,
    val y: BigInteger,
)