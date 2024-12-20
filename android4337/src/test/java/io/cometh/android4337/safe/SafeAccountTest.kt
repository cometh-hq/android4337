package io.cometh.android4337.safe

import android.content.Context
import io.cometh.android4337.CustomHttpService
import io.cometh.android4337.EntryPointContract
import io.cometh.android4337.HttpResponseStub
import io.cometh.android4337.UserOperation
import io.cometh.android4337.bundler.BundlerClient
import io.cometh.android4337.gasprice.UserOperationGasPriceProvider
import io.cometh.android4337.paymaster.PaymasterClient
import io.cometh.android4337.safe.signer.eoa.EOASigner
import io.cometh.android4337.safe.signer.passkey.Passkey
import io.cometh.android4337.safe.signer.passkey.PasskeySigner
import io.cometh.android4337.toInputStream
import io.cometh.android4337.utils.hexToBigInt
import io.cometh.android4337.utils.toHex
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.web3j.tx.TransactionManager

class SafeAccountTest {

    @MockK
    lateinit var bundlerClient: BundlerClient

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

    @Before
    fun before() {
        MockKAnnotations.init(this)
        web3Service = CustomHttpService(httpResponseStub)
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
    fun getFactory() {

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

}