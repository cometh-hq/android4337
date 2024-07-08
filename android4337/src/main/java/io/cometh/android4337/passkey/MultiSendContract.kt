package io.cometh.android4337.passkey

import io.cometh.android4337.utils.requireHexAddress
import org.web3j.tx.TransactionManager

class MultiSendContract(
    private val transactionManager: TransactionManager,
    contractAddress: String,
) {

    init {
        contractAddress.requireHexAddress()
    }

    fun encodeMultiSend() {

    }
}