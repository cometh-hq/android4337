package io.cometh.android4337.safe

import org.web3j.abi.datatypes.Address
import java.math.BigInteger

data class MultiSendTransaction(
    val op: Int,
    val to: Address,
    val value: BigInteger?,
    val data: ByteArray
) {
    init {
        require(op in 1..2)
    }
}