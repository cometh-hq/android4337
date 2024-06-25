package io.cometh.android4337sdk.web3j

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.web3j.protocol.ObjectMapperFactory
import org.web3j.protocol.core.Response
import org.web3j.utils.Numeric
import java.io.IOException
import java.math.BigInteger
import java.util.stream.Collectors


/** eth_feeHistory.  */
class EthFeeHistory : Response<EthFeeHistory.FeeHistory?>() {
    @JsonDeserialize(using = ResponseDeserialiser::class)
    override fun setResult(result: FeeHistory?) {
        super.setResult(result)
    }

    val feeHistory: FeeHistory?
        get() = result

    class FeeHistory {
        var oldestBlockRaw: String? = null
            private set
        var rewardRaw: List<List<String>>? = null
            private set
        var baseFeePerGasRaw: List<String>? = null
            private set
        var gasUsedRatio: List<Double>? = null

        constructor()

        constructor(
            oldestBlock: String?,
            reward: List<List<String>>?,
            baseFeePerGas: List<String>?,
            gasUsedRatio: List<Double>?
        ) {
            this.oldestBlockRaw = oldestBlock
            this.rewardRaw = reward
            this.baseFeePerGasRaw = baseFeePerGas
            this.gasUsedRatio = gasUsedRatio
        }

        fun getOldestBlock(): BigInteger {
            return Numeric.decodeQuantity(oldestBlockRaw)
        }

        fun setOldestBlock(oldestBlock: String?) {
            this.oldestBlockRaw = oldestBlock
        }

        fun getReward(): List<List<BigInteger>> {
            return rewardRaw!!.stream()
                .map { rewardPercentile: List<String> ->
                    rewardPercentile.stream()
                        .map { value: String? ->
                            Numeric.decodeQuantity(
                                value
                            )
                        }
                        .collect(Collectors.toList())
                }
                .collect(Collectors.toList())
        }

        fun setReward(reward: List<List<String>>?) {
            this.rewardRaw = reward
        }

        fun getBaseFeePerGas(): List<BigInteger> {
            return baseFeePerGasRaw!!.stream().map { value: String? ->
                Numeric.decodeQuantity(
                    value
                )
            }.collect(Collectors.toList())
        }

        fun setBaseFeePerGas(baseFeePerGas: List<String>?) {
            this.baseFeePerGasRaw = baseFeePerGas
        }

        override fun equals(o: Any?): Boolean {
            if (this === o) {
                return true
            }
            if (o !is FeeHistory) {
                return false
            }

            val feeHistory = o

            if (if (oldestBlockRaw != null
                ) oldestBlockRaw != feeHistory.oldestBlockRaw
                else feeHistory.oldestBlockRaw != null
            ) {
                return false
            }

            if (if (rewardRaw != null
                ) rewardRaw != feeHistory.rewardRaw
                else feeHistory.rewardRaw != null
            ) {
                return false
            }

            if (if (baseFeePerGasRaw != null
                ) baseFeePerGasRaw != feeHistory.baseFeePerGasRaw
                else feeHistory.baseFeePerGasRaw != null
            ) {
                return false
            }

            return if (gasUsedRatio != null
            ) gasUsedRatio == feeHistory.gasUsedRatio else feeHistory.gasUsedRatio == null
        }

        override fun hashCode(): Int {
            var result = if (oldestBlockRaw != null) oldestBlockRaw.hashCode() else 0
            result = 31 * result + (if (rewardRaw != null) rewardRaw.hashCode() else 0)
            result = (
                    31 * result
                            + (if (baseFeePerGasRaw != null
                    ) baseFeePerGasRaw.hashCode()
                    else 0))
            result = 31 * result + (if (gasUsedRatio != null) gasUsedRatio.hashCode() else 0)
            return result
        }
    }

    class ResponseDeserialiser : JsonDeserializer<FeeHistory?>() {
        private val objectReader: ObjectReader = ObjectMapperFactory.getObjectReader()

        @Throws(IOException::class)
        override fun deserialize(
            jsonParser: JsonParser, deserializationContext: DeserializationContext
        ): FeeHistory? {
            return if (jsonParser.currentToken != JsonToken.VALUE_NULL) {
                objectReader.readValue(jsonParser, FeeHistory::class.java)
            } else {
                null // null is wrapped by Optional in above getter
            }
        }
    }
}
