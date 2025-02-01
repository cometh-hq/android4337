package io.cometh.android4337.safe

import android.content.Context
import io.cometh.android4337.CustomHttpService
import io.cometh.android4337.EntryPointContract
import io.cometh.android4337.HttpResponseStub
import io.cometh.android4337.UserOperation
import io.cometh.android4337.bundler.BundlerClient
import io.cometh.android4337.bundler.SimpleBundlerClient
import io.cometh.android4337.gasprice.UserOperationGasPriceProvider
import io.cometh.android4337.mockBundlerEstimateUserOperation
import io.cometh.android4337.mockGasEstimateGetGasPrice
import io.cometh.android4337.mockGetNonce
import io.cometh.android4337.mockPaymasterSponsorUserOperation
import io.cometh.android4337.paymaster.PaymasterClient
import io.cometh.android4337.safe.signer.eoa.EOASigner
import io.cometh.android4337.safe.signer.passkey.Passkey
import io.cometh.android4337.safe.signer.passkey.PasskeySigner
import io.cometh.android4337.toInputStream
import io.cometh.android4337.utils.hexToAddress
import io.cometh.android4337.utils.hexToBigInt
import io.cometh.android4337.utils.toHex
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.web3j.tx.TransactionManager
import java.io.ByteArrayInputStream

class SafeAccountTest {

//    @MockK
//    lateinit var bundlerClient: BundlerClient

    @MockK
    lateinit var gasPriceProvider: UserOperationGasPriceProvider

    @MockK
    lateinit var httpResponseStub: HttpResponseStub

    @MockK
    lateinit var paymasterClient: PaymasterClient

    @MockK
    lateinit var transactionManager: TransactionManager

    val chainId = 11155111
    val entryPointAddress = EntryPointContract.ENTRY_POINT_ADDRESS_V7

    lateinit var safeAccount1: SafeAccount
    lateinit var safeAccount2: SafeAccount
    lateinit var web3Service: CustomHttpService
    lateinit var bundlerClient: BundlerClient

    @Before
    fun before() {
        MockKAnnotations.init(this)
        web3Service = CustomHttpService(httpResponseStub)
        bundlerClient = SimpleBundlerClient(web3Service)
        safeAccount1 = SafeAccount.fromAddress(
            address = TestsData.account1SafeAddress,
            EOASigner(TestsData.account1Credentials),
            bundlerClient,
            chainId,
            web3Service,
            paymasterClient = paymasterClient,
            gasPriceProvider = gasPriceProvider,
        )
        safeAccount2 = SafeAccount.fromAddress(
            TestsData.account2SafeAddress,
            EOASigner(TestsData.account2Credentials),
            bundlerClient,
            chainId,
            web3Service,
            paymasterClient = paymasterClient,
            gasPriceProvider = gasPriceProvider,
        )
    }

    @Test
    fun createNewAccount() {
        every { httpResponseStub.getResponse(any()) } returns """
            { "jsonrpc": "2.0", "id": 1, "result": "${TestsData.proxyCode}" }
        """.trimIndent().toInputStream()
        val safeAccount = SafeAccount.createNewAccount(
            EOASigner(TestsData.account1Credentials),
            bundlerClient,
            chainId,
            web3Service,
            paymasterClient = paymasterClient,
            gasPriceProvider = gasPriceProvider,
        )
        assertEquals(TestsData.account1SafeAddress, safeAccount.safeAddress)
    }

    @Test
    fun getFactoryData() {
        val factoryData = safeAccount2.getFactoryData()
        val expected =
            "0x1688f0b900000000000000000000000029fcb43b46531bca003ddc8fcb67ffe91900c7620000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001e4b63e800d000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000010000000000000000000000002dd68b007b46fbe91b9a7c3eda5a7a1063cb5b47000000000000000000000000000000000000000000000000000000000000014000000000000000000000000075cf11467937ce3f2f357ce24ffc3dbf8fd5c22600000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000009d8a62f656a8d1615c1294fd71e9cfb3e4855a4f00000000000000000000000000000000000000000000000000000000000000648d0dc49f0000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000100000000000000000000000075cf11467937ce3f2f357ce24ffc3dbf8fd5c2260000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
        assertEquals(expected, factoryData.toHex())
    }

    @Test
    fun predictAddress1() {
        every { httpResponseStub.getResponse(any()) } returns """
            { "jsonrpc": "2.0", "id": 1, "result": "${TestsData.proxyCode}" }
        """.trimIndent().toInputStream()
        val address = SafeAccount.predictAddress(
            EOASigner(TestsData.account1Credentials),
            web3Service
        )
        assertEquals(TestsData.account1SafeAddress, address)
    }

    @Test
    fun predictAddress2() {
        every { httpResponseStub.getResponse(any()) } returns """
            { "jsonrpc": "2.0", "id": 1, "result": "${TestsData.proxyCode}" }
        """.trimIndent().toInputStream()
        val address = SafeAccount.predictAddress(
            EOASigner(TestsData.account2Credentials),
            web3Service,
        )
        assertEquals(TestsData.account2SafeAddress, address)
    }

    @Test
    fun predictAddressWithPasskey() {
        every { httpResponseStub.getResponse(any()) } returns """
            { "jsonrpc": "2.0", "id": 1, "result": "${TestsData.proxyCode}" }
        """.trimIndent().toInputStream()
        val context = mockk<Context>()
        every { context.getSharedPreferences(any(), any()) } returns mockk()
        val address = runBlocking {
            SafeAccount.predictAddress(
                PasskeySigner.withSharedSigner(
                    rpId = "rpId",
                    userName = "userName",
                    context = context,
                    passkey = Passkey(
                        x = "0x9e5261b7f1e14fb9f3135053c093e4d95c8ea94fb6e761621f7c2cf13d36ccda".hexToBigInt(),
                        y = "0xe2190ee5f1ec2959e848c540f7f5d1c843bc45200158f46e6f984d258aae4b6e".hexToBigInt()
                    )
                ),
                web3Service,
            )
        }
        assertEquals("0xfF724471DcB34e42C715163B60A0881fAF7a9C96", address)
    }

    @Test
    fun getOwners() {
        val expected =
            "0x000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000000010000000000000000000000002f920a66c2f9760f6fe5f49b289322ddf60f9103"
        every { httpResponseStub.getResponse(any()) } returns """
            { "jsonrpc": "2.0", "id": 1, "result": "$expected" }
        """.trimIndent().toInputStream()
        every {
            transactionManager.sendCall(
                any(),
                any(),
                any()
            )
        } returns ""
        assertEquals("0x2f920a66c2f9760f6fe5f49b289322ddf60f9103", safeAccount1.getOwners()!!.first().value)
    }

    @Test
    fun signOperation() {
        val userOperation = UserOperation(
            sender = "0xcfe1e7242dF565f031e1D3F645169Dda9D1230d2",
            nonce = "0x00",
            callData = "0x7bb374280000000000000000000000000338dcd5512ae8f3c481c33eb4b6eedf632d1d2f000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000406661abd00000000000000000000000000000000000000000000000000000000",
            preVerificationGas = "0xea60",
            callGasLimit = "0x1e8480",
            verificationGasLimit = "0x07a120",
            maxFeePerGas = "0x02ee7c55e2",
            maxPriorityFeePerGas = "0x1f2ecf7f",
        )
        val signature = safeAccount2.signUserOperation(userOperation)
        val expected =
            "0x000000000000000000000000298adde4bafae7cf44a9bf2a1881a836716592c85ac5f6445e673647d6cc907e3af6d065c591f07173e83246ef649147b0034bf119da693c4025be55206e9db91c"
        assertEquals(expected, signature.toHex())
    }

    @Test
    fun signOperationWithPasskey() {
        val userOperation = UserOperation(
            sender = "0xcfe1e7242dF565f031e1D3F645169Dda9D1230d2",
            nonce = "0x00",
            callData = "0x7bb374280000000000000000000000000338dcd5512ae8f3c481c33eb4b6eedf632d1d2f000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000406661abd00000000000000000000000000000000000000000000000000000000",
            preVerificationGas = "0xea60",
            callGasLimit = "0x1e8480",
            verificationGasLimit = "0x07a120",
            maxFeePerGas = "0x02ee7c55e2",
            maxPriorityFeePerGas = "0x1f2ecf7f",
        )
        val signature = safeAccount2.signUserOperation(userOperation)
    }

    @Test(expected = IllegalArgumentException::class)
    fun fromAddress() {
        SafeAccount.fromAddress(
            "wrong_address",
            signer = EOASigner(TestsData.account1Credentials),
            bundlerClient = bundlerClient,
            entryPointAddress = entryPointAddress,
            web3Service = web3Service,
            chainId = chainId,
            gasPriceProvider = gasPriceProvider,
            paymasterClient = paymasterClient,
        )
    }

    @Test
    fun signMessage() {
        val message = "0xaaaa"
        val signature = safeAccount1.signMessage(message)
        val expected = "0x496d9eb4a63e929644c4b2812cb589b6c0f71f234e3799ba05893e8907f4367f40648de25b570a46559aa07927efa67a04e665652a19685121e870038003b30c1c"
        assertEquals(expected, signature.toHex())
    }

    @Test(expected = Error::class)
    fun enableRecoveryModule_alreadyEnabled() {
        every { httpResponseStub.getResponse(any()) } returns """
            { "jsonrpc": "2.0", "id": 1, "result": "0xabcd" }
        """.trimIndent().toInputStream()
        safeAccount1.enableRecoveryModule(
            guardianAddress = "0x2f920a66c2f9760f6fe5f49b289322ddf60f9103".hexToAddress(),
        )
    }

    @Test
    fun enableRecoveryModule() {
        val delayIsDeployedResp = """{ "jsonrpc": "2.0", "id": 1, "result": "0x" }""".toIS()
        val accountIsDeployedResp = """{ "jsonrpc": "2.0", "id": 1, "result": "0xaaaa" }""".toIS()
        every {
            httpResponseStub.getResponse(match { it.contains("eth_getCode") })
        } returns delayIsDeployedResp andThen accountIsDeployedResp
        mockGetNonce(httpResponseStub)
        mockBundlerEstimateUserOperation(httpResponseStub)
        mockGasEstimateGetGasPrice(gasPriceProvider)
        mockPaymasterSponsorUserOperation(paymasterClient)

        val sendUserOpResp = """{ "jsonrpc": "2.0", "id": 1, "result": "0x" }""".toIS()
        every {
            httpResponseStub.getResponse(match { it.contains("eth_sendUserOperation") })
        } returns sendUserOpResp

        safeAccount1.enableRecoveryModule(
            guardianAddress = "0x2f920a66c2f9760f6fe5f49b289322ddf60f9103".hexToAddress(),
        )

        verify {
            httpResponseStub.getResponse(
                """{
  "jsonrpc" : "2.0",
  "method" : "eth_sendUserOperation",
  "params" : [ {
    "sender" : "0x4bF81EEF3911db0615297836a8fF351f5Fe08c68",
    "nonce" : "0x03",
    "callData" : "0x7bb3742800000000000000000000000038869bf66a61cf6bdb996a6ae40d5853fd43b52600000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000080000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000003248d80ff0a000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000002cb00000000000000addb49795b0f9ba5bc298cdda23600000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000184f1ab873c000000000000000000000000d54895b1121a2ee3f37b502f507631fa1331bed600000000000000000000000000000000000000000000000000000000000000600000000000000000000000004bf81eef3911db0615297836a8ff351f5fe08c6800000000000000000000000000000000000000000000000000000000000000e4a4f9edbf000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000000a00000000000000000000000004bf81eef3911db0615297836a8ff351f5fe08c680000000000000000000000004bf81eef3911db0615297836a8ff351f5fe08c680000000000000000000000004bf81eef3911db0615297836a8ff351f5fe08c6800000000000000000000000000000000000000000000000000000000000151800000000000000000000000000000000000000000000000000000000000093a8000000000000000000000000000000000000000000000000000000000004bf81eef3911db0615297836a8ff351f5fe08c6800000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000024610b59250000000000000000000000009b24ce7a4d940920c479f26d9460f6195c1e86ab009b24ce7a4d940920c479f26d9460f6195c1e86ab00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000024610b59250000000000000000000000002f920a66c2f9760f6fe5f49b289322ddf60f910300000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
    "callGasLimit" : "0x163a2",
    "verificationGasLimit" : "0x1b247",
    "preVerificationGas" : "0xef1c",
    "maxFeePerGas" : "0x01e3fb094e",
    "maxPriorityFeePerGas" : "0x53cd81aa",
    "signature" : "0x000000000000000000000000e6e83e86c6f56595a8cd818957255e84aa98c1b22f4d5e324dd53e69a7198bd93bc150d1f93e47952116bf21cb5800c14edffdcbfaaf6cf3e468afa9afdfdd951b",
    "paymaster" : "0x4685d9587a7F72Da32dc323bfFF17627aa632C61",
    "paymasterVerificationGasLimit" : "0x4e09",
    "paymasterPostOpGasLimit" : "0x1",
    "paymasterData" : "0xDFF7FA1077Bce740a6a212b3995990682c0Ba66d000000000000000000000000000000000000000000000000000000006672ce7100000000000000000000000000000000000000000000000000000000000000000e499f53c85c53cd4f1444b807e380c6a01a412d7e1cfd24b6153debb97cbc986e6809dff8c005ed94c32bf1d5e722b9f40b909fc89d8982f2f99cb7a91b19f01c"
  }, "0x0000000071727De22E5E9d8BAf0edAc6f37da032" ],
  "id" : 4
}""".trimIndent().replace("\n", "").replace(" ", "")
            )
        }
    }

}

fun String.toIS(): ByteArrayInputStream {
    return trimIndent().toInputStream()
}