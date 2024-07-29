package io.cometh.android4337.safe.signer.passkey

import java.math.BigInteger

data class Passkey(
    val x: BigInteger,
    val y: BigInteger,
)