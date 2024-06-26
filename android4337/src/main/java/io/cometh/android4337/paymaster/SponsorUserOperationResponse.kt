package io.cometh.android4337.paymaster

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectReader
import io.cometh.android4337.utils.hexStringToBigInt
import io.cometh.android4337.utils.hexStringToByteArray
import org.web3j.protocol.ObjectMapperFactory
import org.web3j.protocol.core.Response
import java.io.IOException

class SponsorUserOperationResponse : Response<SponsorUserOperation>() {
    @SuppressWarnings("unused")
    class ResponseDeserialiser : JsonDeserializer<SponsorUserOperation?>() {
        private val objectReader: ObjectReader = ObjectMapperFactory.getObjectReader()

        @Throws(IOException::class)
        override fun deserialize(
            jsonParser: JsonParser, deserializationContext: DeserializationContext
        ): SponsorUserOperation? {
            return if (jsonParser.currentToken != JsonToken.VALUE_NULL) {
                objectReader.readValue(jsonParser, SponsorUserOperation::class.java)
            } else {
                null // null is wrapped by Optional in above getter
            }
        }
    }
}

data class SponsorUserOperation @JsonCreator constructor(
    @JsonProperty("paymasterAndData") val paymasterAndData: String,
    @JsonProperty("preVerificationGas") val preVerificationGas: String,
    @JsonProperty("verificationGasLimit") val verificationGasLimit: String,
    @JsonProperty("callGasLimit") val callGasLimit: String,
) {
    fun getPaymasterAndData() = paymasterAndData.hexStringToByteArray()
    fun getPreVerificationGas() = preVerificationGas.hexStringToBigInt()
    fun getVerificationGasLimit() = verificationGasLimit.hexStringToBigInt()
    fun getCallGasLimit() = callGasLimit.hexStringToBigInt()
}

