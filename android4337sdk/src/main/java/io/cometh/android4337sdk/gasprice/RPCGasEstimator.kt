package io.cometh.android4337sdk.gasprice

import io.cometh.android4337sdk.web3j.EthFeeHistory
import org.web3j.protocol.Web3jService
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.Request
import org.web3j.utils.Numeric
import java.math.BigInteger


class RPCGasEstimator(
    private val web3jService: Web3jService,
    private val baseFeePercentMultiplier: Long = 200,
    private val priorityFeePercentMultiplier: Long = 120,
    private val web3jPort: Web3jPort = Web3jPort(web3jService)
) : UserOperationGasPriceProvider {
    override fun getGasPrice(): GasPrice {
        val resp = web3jPort.ethFeeHistory(5, DefaultBlockParameterName.LATEST, listOf(40.0)).send()
        val feeHistory = resp.feeHistory ?: throw RuntimeException("Error: feeHistory is null")
        val latestBaseFeePerGas = feeHistory.getBaseFeePerGas().lastOrNull() ?: throw RuntimeException("Error: latestBaseFeePerGas is null")
        val adjustedMaxBaseFee = (latestBaseFeePerGas * BigInteger.valueOf(baseFeePercentMultiplier)) / BigInteger.valueOf(100)
        val priorityFeesPerBlock = feeHistory.getReward().mapNotNull { it.firstOrNull() }
        val priorityFeeMedian = priorityFeesPerBlock.fold(BigInteger.ZERO) { acc, x -> acc + x }
            .divideAndRemainder(BigInteger.valueOf(priorityFeesPerBlock.size.toLong())).first()
        val adjustedMaxPriorityFee = (priorityFeeMedian * BigInteger.valueOf(priorityFeePercentMultiplier)).divide(BigInteger.valueOf(100))
        return GasPrice(
            maxFeePerGas = adjustedMaxBaseFee + adjustedMaxPriorityFee,
            maxPriorityFeePerGas = adjustedMaxPriorityFee
        )
    }

}

class Web3jPort(private val web3jService: Web3jService) {
    fun ethFeeHistory(blockCount: Int, newestBlock: DefaultBlockParameter, rewardPercentiles: List<Double>): Request<Any, EthFeeHistory> {
        return Request(
            "eth_feeHistory", listOf(
                Numeric.encodeQuantity(
                    BigInteger.valueOf(blockCount.toLong()),
                ), newestBlock.value, rewardPercentiles
            ), web3jService, EthFeeHistory::class.java
        )
    }
}