package io.cometh.android4337

import org.web3j.protocol.Service
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * Create custom http service to be able to mock performIO and tests serialization/deserialization
 */
class CustomHttpService(val httpResponseStub: HttpResponseStub) : Service(true) {
    override fun close() {
    }

    override fun performIO(payload: String?): InputStream {
        return httpResponseStub.getResponse()
    }
}

class HttpResponseStub {
    fun getResponse(): InputStream {
        throw NotImplementedError()
    }
}

fun String.toInputStream() = ByteArrayInputStream(toByteArray(Charsets.UTF_8))