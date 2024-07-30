package io.cometh.android4337.bundler

import io.cometh.android4337.CustomHttpService
import io.cometh.android4337.EntryPointContract
import io.cometh.android4337.HttpResponseStub
import io.cometh.android4337.UserOperation
import io.cometh.android4337.toInputStream
import io.cometh.android4337.utils.hexToBigInt
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SimpleBundlerClientTest {

    @MockK
    lateinit var httpResponseStub: HttpResponseStub

    lateinit var bundlerClient: BundlerClient

    val userOperation = UserOperation(
        sender = "0x2FF46F26638977AE8C88e205cCa407A1a9725F0B",
        nonce = "0x05",
        callData = "0x7bb374280000000000000000000000000338dcd5512ae8f3c481c33eb4b6eedf632d1d2f000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000406661abd00000000000000000000000000000000000000000000000000000000",
        callGasLimit = "0x163a2",
        preVerificationGas = "0xef1c",
        verificationGasLimit = "0x1b247",
        maxFeePerGas = "0x01e3fb094e",
        maxPriorityFeePerGas = "0x53cd81aa",
        paymasterData = "0x",
        signature = "0x0000000000000000000000004232f7414022b3da2b1b3fc2d82d40a10eefc29c913c6801c1827dcb1c3735c8065234a4435ec0ca3a13786ecd683320661a5abb2b1dd2c2b3fc8dcf1473fcd81c"
    )

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        bundlerClient = SimpleBundlerClient(CustomHttpService(httpResponseStub))
    }

    @Test
    fun ethEstimateUserOperationGas() {
        every { httpResponseStub.getResponse(any()) } returns """
            {
                "jsonrpc": "2.0",
                "id": 1,
                "result": {
                    "preVerificationGas": "0xEC2C",
                    "verificationGasLimit": "0x45BCA",
                    "callGasLimit": "0x2F44"
                }
            }
        """.trimIndent().toInputStream()

        val resp = bundlerClient.ethEstimateUserOperationGas(
            userOperation,
            EntryPointContract.ENTRY_POINT_ADDRESS_V7
        ).send()
        val result = resp.result
        assertEquals("0x2F44", result.callGasLimit)
        assertEquals("0xEC2C", result.preVerificationGas)
        assertEquals("0x45BCA", result.verificationGasLimit)
    }

    @Test
    fun ethGetUserOperationReceipt() {
        val userOpHash = "0xd74a51ddf9cba9a0f548ef179950e0c3f6cd8a48ed961aade3133d38f6382689"
        val sender = "0x11bfDEe1EA68BF738316cB56cD06323899f89292"
        val nonce = "0x0"
        val paymaster = "0x0000000000000000000000000000000000000000"
        val actualGasCost = "0x128e0b1eff87"
        val actualGasUsed = "0x187191"
        val success = "true"
        val logAddress = "0x0000000071727de22e5e9d8baf0edac6f37da032"
        val logTopic = "0x49628fd1471006c1482da88028e9ce4dbb080b815c9b0344d39e5a8e6ec1419f"
        val logData = "0xaaaa"
        val blockHash = "0x61d815a17cecf2cad8a270ffae66220935c8e355d1f3d61de20fcd3e304413be"
        val blockNumber = "0xca8037"
        val transactionHash = "0x70acbefb436e8708124609bb31464af90a97d49ec7722659cc2573e25594d072"
        val transactionIndex = "0xe"
        val logIndex = "0x3b"
        val removed = false

        val receiptTransactionHash = "0x70acbefb436e8708124609bb31464af90a97d49ec7722659cc2573e25594d072"
        val receiptTransactionIndex = "0xe"
        val receiptBlockHash = "0x61d815a17cecf2cad8a270ffae66220935c8e355d1f3d61de20fcd3e304413be"
        val receiptBlockNumber = "0xca8037"
        val receiptFrom = "0xf03ddbe5b9b4ddec66009d94dc5d33dd719f34e1"
        val receiptTo = "0x0000000071727de22e5e9d8baf0edac6f37da032"
        val receiptCumulativeGasUsed = "0x2e4c9a"
        val receiptGasUsed = "0xae32d"
        val receiptContractAddress = "null"
        val receiptLogAddress = "0x11bfdee1ea68bf738316cb56cd06323899f89292"
        val receiptLogTopics = "0xecdf3a3effea5783a3c4c2140e677577666428d44ed9d474a0b3a4c9943f8440"
        val receiptLogData = "0x"
        val receiptLogBlockHash = "0x61d815a17cecf2cad8a270ffae66220935c8e355d1f3d61de20fcd3e304413be"
        val receiptLogBlockNumber = "0xca8037"
        val receiptLogTransactionHash = "0x70acbefb436e8708124609bb31464af90a97d49ec7722659cc2573e25594d072"
        val receiptLogTransactionIndex = "0xe"
        val receiptLogLogIndex = "0x31"
        val receiptLogRemoved = false
        val receiptStatus = "0x1"
        val receiptLogsBloom = "0xbbbb"
        val receiptType = "0x2"
        val receiptEffectiveGasPrice = "0xab5e29"
        every { httpResponseStub.getResponse(any()) } returns """
{
	"jsonrpc": "2.0",
	"id": 1,
	"result": {
		"userOpHash": "$userOpHash",
		"sender": "$sender",
		"nonce": "$nonce",
		"paymaster": "$paymaster",
		"actualGasCost": "$actualGasCost",
		"actualGasUsed": "$actualGasUsed",
		"success": $success,
		"logs": [
			{
				"address": "$logAddress",
				"topics": [ "$logTopic" ],
				"data": "$logData",
				"blockHash": "$blockHash",
				"blockNumber": "$blockNumber",
				"transactionHash": "$transactionHash",
				"transactionIndex": "$transactionIndex",
				"logIndex": "$logIndex",
				"removed": $removed
			}
		],
		"receipt": {
			"transactionHash": "$receiptTransactionHash",
			"transactionIndex": "$receiptTransactionIndex",
			"blockHash": "$receiptBlockHash",
			"blockNumber": "$receiptBlockNumber",
			"from": "$receiptFrom",
			"to": "$receiptTo",
			"cumulativeGasUsed": "$receiptCumulativeGasUsed",
			"gasUsed": "$receiptGasUsed",
			"contractAddress": $receiptContractAddress,
			"logs": [
				{
					"address": "$receiptLogAddress",
					"topics": [ "$receiptLogTopics" ],
					"data": "$receiptLogData",
					"blockHash": "$receiptLogBlockHash",
					"blockNumber": "$receiptLogBlockNumber",
					"transactionHash": "$receiptLogTransactionHash",
					"transactionIndex": "$receiptLogTransactionIndex",
					"logIndex": "$receiptLogLogIndex",
					"removed": $receiptLogRemoved
				}
			],
			"status": "$receiptStatus",
			"logsBloom": "$receiptLogsBloom",
			"type": "$receiptType",
			"effectiveGasPrice": "$receiptEffectiveGasPrice"
		}
	}
}
        """.trimIndent().toInputStream()

        val resp = bundlerClient.ethGetUserOperationReceipt("0xaaaa").send()
        val result = resp.result
        assertEquals(userOpHash, result.userOpHash)
        assertEquals(sender, result.sender)
        assertEquals(nonce, result.nonce)
        assertEquals(paymaster, result.paymaster)
        assertEquals(actualGasCost, result.actualGasCost)
        assertEquals(actualGasUsed, result.actualGasUsed)
        assertEquals(success, result.success)
        assertEquals(logAddress, result.logs[0].address)
        assertEquals(logTopic, result.logs[0].topics[0])
        assertEquals(logData, result.logs[0].data)
        assertEquals(blockHash, result.logs[0].blockHash)
        assertEquals(blockNumber.hexToBigInt(), result.logs[0].blockNumber)
        assertEquals(transactionHash, result.logs[0].transactionHash)
        assertEquals(transactionIndex.hexToBigInt(), result.logs[0].transactionIndex)
        assertEquals(logIndex.hexToBigInt(), result.logs[0].logIndex)
        assertEquals(removed, result.logs[0].isRemoved)
        assertEquals(receiptTransactionHash, result.receipt.transactionHash)
        assertEquals(receiptTransactionIndex.hexToBigInt(), result.receipt.transactionIndex)
        assertEquals(receiptBlockHash, result.receipt.blockHash)
        assertEquals(receiptBlockNumber.hexToBigInt(), result.receipt.blockNumber)
        assertEquals(receiptFrom, result.receipt.from)
        assertEquals(receiptTo, result.receipt.to)
        assertEquals(receiptCumulativeGasUsed.hexToBigInt(), result.receipt.cumulativeGasUsed)
        assertEquals(receiptGasUsed.hexToBigInt(), result.receipt.gasUsed)
        assertEquals(null, result.receipt.contractAddress)
        assertEquals(receiptLogAddress, result.receipt.logs[0].address)
        assertEquals(receiptLogTopics, result.receipt.logs[0].topics[0])
        assertEquals(receiptLogData, result.receipt.logs[0].data)
        assertEquals(receiptLogBlockHash, result.receipt.logs[0].blockHash)
        assertEquals(receiptLogBlockNumber.hexToBigInt(), result.receipt.logs[0].blockNumber)
        assertEquals(receiptLogTransactionHash, result.receipt.logs[0].transactionHash)
        assertEquals(receiptLogTransactionIndex.hexToBigInt(), result.receipt.logs[0].transactionIndex)
        assertEquals(receiptLogLogIndex.hexToBigInt(), result.receipt.logs[0].logIndex)
        assertEquals(receiptLogRemoved, result.receipt.logs[0].isRemoved)
        assertEquals(receiptStatus, result.receipt.status)
        assertEquals(receiptLogsBloom, result.receipt.logsBloom)
        assertEquals(receiptType, result.receipt.type)
        assertEquals(receiptEffectiveGasPrice, result.receipt.effectiveGasPrice)

    }

    @Test
    fun ethGetUserOperationByHash() {
        val entryPoint = "0x0000000071727De22E5E9d8BAf0edAc6f37da032"
        val blockNumber = "0xb942ef"
        val blockHash = "0x19e3606eb9646f3795708afb8144773a7517f06460d17b8b18b92b2cb8a97898"
        val transactionHash = "0xc6c1d8030f6c05bc65f1e901b109be1bd4611fa1015d3b99004171bfaba51f6e"
        val sender = "0x4bf81eef3911db0615297836a8ff351f5fe08c68"
        val nonce = "0x0"
        val callData = "0x7bb37428000000000000000000000000f1dc86f621f85409bbea2f14e7e2971e73c282260"
        val callGasLimit = "0x1878f"
        val verificationGasLimit = "0x814ce"
        val preVerificationGas = "0xdb5c"
        val maxPriorityFeePerGas = "0xe5ddb9e"
        val maxFeePerGas = "0xe5ddde0"
        val factory = "0x4e1dcf7ad4e460cfd30791ccc4f9c8a4f820ec67"
        val factoryData = "0xbbbb"
        val paymaster = "0x6a6b7f6012ee5bef1cdf95df25e5045c7727c739"
        val paymasterVerificationGasLimit = "0x927c0"
        val paymasterPostOpGasLimit = "0x1"
        val paymasterData = "0xaaaa"
        val signature = "0xcccc"
        every { httpResponseStub.getResponse(any()) } returns """
            {
            	"jsonrpc": "2.0",
            	"id": 1,
            	"result": {
            		"userOperation": {
            			"sender": "$sender",
            			"nonce": "$nonce",
            			"callData": "$callData",
            			"callGasLimit": "$callGasLimit",
            			"verificationGasLimit": "$verificationGasLimit",
            			"preVerificationGas": "$preVerificationGas",
            			"maxPriorityFeePerGas": "$maxPriorityFeePerGas",
            			"maxFeePerGas": "$maxFeePerGas",
            			"factory": "$factory",
            			"factoryData": "$factoryData",
            			"paymaster": "$paymaster",
            			"paymasterVerificationGasLimit": "$paymasterVerificationGasLimit",
            			"paymasterPostOpGasLimit": "$paymasterPostOpGasLimit",
            			"paymasterData": "$paymasterData",
            			"signature": "$signature"
            		},
            		"entryPoint": "$entryPoint",
            		"blockNumber": "$blockNumber",
            		"blockHash": "$blockHash",
            		"transactionHash": "$transactionHash"
            	}
            }
        """.trimIndent().toInputStream()
        val resp =
            bundlerClient.ethGetUserOperationByHash("0xf2941038ed94b307399b31bff14ddf588bbe62df05bcf7cb712739168bde0305")
                .send()
        val result = resp.result
        assertEquals(sender, result.userOperation.sender)
        assertEquals(nonce, result.userOperation.nonce)
        assertEquals(callData, result.userOperation.callData)
        assertEquals(callGasLimit, result.userOperation.callGasLimit)
        assertEquals(verificationGasLimit, result.userOperation.verificationGasLimit)
        assertEquals(preVerificationGas, result.userOperation.preVerificationGas)
        assertEquals(maxPriorityFeePerGas, result.userOperation.maxPriorityFeePerGas)
        assertEquals(maxFeePerGas, result.userOperation.maxFeePerGas)
        assertEquals(factory, result.userOperation.factory)
        assertEquals(factoryData, result.userOperation.factoryData)
        assertEquals(paymaster, result.userOperation.paymaster)
        assertEquals(
            paymasterVerificationGasLimit,
            result.userOperation.paymasterVerificationGasLimit
        )
        assertEquals(paymasterPostOpGasLimit, result.userOperation.paymasterPostOpGasLimit)
        assertEquals(paymasterData, result.userOperation.paymasterData)
        assertEquals(signature, result.userOperation.signature)
        assertEquals(entryPoint, result.entryPoint)
        assertEquals(blockNumber, result.blockNumber)
        assertEquals(blockHash, result.blockHash)
        assertEquals(transactionHash, result.transactionHash)
    }

    @Test
    fun ethSupportedEntryPoints() {
        every { httpResponseStub.getResponse(any()) } returns """
            {
            	"jsonrpc": "2.0",
            	"id": 1,
            	"result": [
            		"0x5FF137D4b0FDCD49DcA30c7CF57E578a026d2789",
            		"0x0000000071727De22E5E9d8BAf0edAc6f37da032"
            	]
            }
        """.trimIndent().toInputStream()
        val resp = bundlerClient.ethSupportedEntryPoints().send()
        val result = resp.result
        assertEquals("0x5FF137D4b0FDCD49DcA30c7CF57E578a026d2789", result[0])
        assertEquals("0x0000000071727De22E5E9d8BAf0edAc6f37da032", result[1])
    }

    @Test
    fun ethSendUserOperation() {
        every { httpResponseStub.getResponse(any()) } returns """
            {
                "jsonrpc": "2.0",
                "id": 1,
                "result": "0xb38a2faf4b5c716eff634af472206f28574cd5104c69d97a315c3303ddb5fdbd"
            }
        """.trimIndent().toInputStream()

        bundlerClient.ethSendUserOperation(
            userOperation,
            EntryPointContract.ENTRY_POINT_ADDRESS_V7
        ).send()
    }
}
