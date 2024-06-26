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
import java.io.IOException

class EthGetUserOperationByHashResponse : Response<UserOperationByHash>() {
    @SuppressWarnings("unused")
    class ResponseDeserialiser : JsonDeserializer<UserOperationByHash?>() {
        private val objectReader: ObjectReader = ObjectMapperFactory.getObjectReader()

        @Throws(IOException::class)
        override fun deserialize(
            jsonParser: JsonParser, deserializationContext: DeserializationContext
        ): UserOperationByHash? {
            return if (jsonParser.currentToken != JsonToken.VALUE_NULL) {
                objectReader.readValue(jsonParser, UserOperationByHash::class.java)
            } else null
        }
    }
}

data class UserOperationByHash @JsonCreator constructor(
    @JsonProperty("userOperation") val userOperation: io.cometh.android4337.UserOperation,
    @JsonProperty("entryPoint") val entryPoint: String,
    @JsonProperty("transactionHash") val transactionHash: String,
    @JsonProperty("blockHash") val blockHash: String,
    @JsonProperty("blockNumber") val blockNumber: String,
)