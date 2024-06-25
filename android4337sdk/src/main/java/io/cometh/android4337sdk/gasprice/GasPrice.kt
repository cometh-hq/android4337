package io.cometh.android4337sdk.gasprice

import java.math.BigInteger

data class GasPrice(
    val maxFeePerGas: BigInteger,
    val maxPriorityFeePerGas: BigInteger
)

