package io.cometh.android4337.bundler.response

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectReader
import org.web3j.protocol.ObjectMapperFactory
import org.web3j.protocol.core.Response
import org.web3j.protocol.core.methods.response.Log
import org.web3j.protocol.core.methods.response.TransactionReceipt
import java.io.IOException

class EthGetUserOperationReceiptResponse : Response<UserOperationReceipt>() {
    @SuppressWarnings("unused")
    class ResponseDeserialiser : JsonDeserializer<UserOperationReceipt?>() {
        private val objectReader: ObjectReader = ObjectMapperFactory.getObjectReader()

        @Throws(IOException::class)
        override fun deserialize(
            jsonParser: JsonParser, deserializationContext: DeserializationContext
        ): UserOperationReceipt? {
            return if (jsonParser.currentToken != JsonToken.VALUE_NULL) {
                objectReader.readValue(jsonParser, UserOperationReceipt::class.java)
            } else null
        }
    }
}

data class UserOperationReceipt @JsonCreator constructor(
    @JsonProperty("userOpHash") val userOpHash: String,
    @JsonProperty("sender") val sender: String,
    @JsonProperty("nonce") val nonce: String,
    @JsonProperty("actualGasUsed") val actualGasUsed: String,
    @JsonProperty("actualGasCost") val actualGasCost: String,
    @JsonProperty("success") val success: String,
    @JsonProperty("paymaster") val paymaster: String?,
    @JsonProperty("receipt") val receipt: TransactionReceipt,
    @JsonProperty("logs") val logs: List<Log>,
)




