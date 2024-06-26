package io.cometh.android4337.gasprice

import java.math.BigInteger

data class GasPrice(
    val maxFeePerGas: BigInteger,
    val maxPriorityFeePerGas: BigInteger
)

