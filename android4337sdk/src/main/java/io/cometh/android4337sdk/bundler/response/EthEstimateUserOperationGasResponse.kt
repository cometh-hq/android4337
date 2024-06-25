package io.cometh.android4337sdk.bundler.response

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectReader
import io.cometh.android4337sdk.utils.hexStringToBigInt
import org.web3j.protocol.ObjectMapperFactory
import org.web3j.protocol.core.Response
import java.io.IOException

class EthEstimateUserOperationGasResponse : Response<UserOperationGasEstimation>() {

    // used by web3j to deserialize UserOperationGasEstimation
    @SuppressWarnings("unused")
    class ResponseDeserialiser : JsonDeserializer<UserOperationGasEstimation?>() {
        private val objectReader: ObjectReader = ObjectMapperFactory.getObjectReader()

        @Throws(IOException::class)
        override fun deserialize(
            jsonParser: JsonParser, deserializationContext: DeserializationContext
        ): UserOperationGasEstimation? {
            return if (jsonParser.currentToken != JsonToken.VALUE_NULL) {
                objectReader.readValue(jsonParser, UserOperationGasEstimation::class.java)
            } else null
        }
    }

}

data class UserOperationGasEstimation @JsonCreator constructor(
    @JsonProperty("preVerificationGas") val preVerificationGas: String,
    @JsonProperty("verificationGasLimit") val verificationGasLimit: String,
    @JsonProperty("callGasLimit") val callGasLimit: String,
) {
    fun getPreVerificationGas() = preVerificationGas.hexStringToBigInt()
    fun getVerificationGasLimit() = verificationGasLimit.hexStringToBigInt()
    fun getCallGasLimit() = callGasLimit.hexStringToBigInt()

}