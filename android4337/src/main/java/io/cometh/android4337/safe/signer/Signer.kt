package io.cometh.android4337.safe.signer

interface Signer {
    fun sign(data: ByteArray): ByteArray
    fun checkRequirements()
    fun getDummySignature(): String
}