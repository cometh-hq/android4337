package io.cometh.android4337.web3j

import org.web3j.abi.datatypes.Type

object AbiEncoder {

    fun encodePackedParameters(parameters: List<Type<*>>, padAddress: Boolean = false): String {
        val result = StringBuilder()
        for (parameter in parameters) {
            result.append(TypeEncoder.encodePacked(parameter))
        }
        return "0x${result}"
    }

}