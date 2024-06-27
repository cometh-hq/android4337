package io.cometh.android4337.safe

import io.cometh.android4337.ENTRY_POINT_ADDRESS
import io.cometh.android4337.UserOperation
import io.cometh.android4337.bundler.BundlerClient
import io.cometh.android4337.gasprice.UserOperationGasPriceProvider
import io.cometh.android4337.paymaster.PaymasterClient
import io.cometh.android4337.utils.toHex
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.web3j.protocol.Web3j
import org.web3j.tx.TransactionManager

class SafeAccountTest {

    @MockK
    lateinit var bundlerClient: BundlerClient

    @MockK
    lateinit var gasPriceProvider: UserOperationGasPriceProvider

    @MockK
    lateinit var web3j: Web3j

    @MockK
    lateinit var paymasterClient: PaymasterClient

    @MockK
    lateinit var transactionManager: TransactionManager

    val chainId = 11155111
    val entryPointAddress = ENTRY_POINT_ADDRESS

    lateinit var safeAccount1: SafeAccount
    lateinit var safeAccount2: SafeAccount

    val safeConfig = SafeConfig(
        "0x2dd68b007B46fBe91B9A7c3EDa5A7a1063cB5b47",
        "0xa581c4A4DB7175302464fF3C06380BC3270b4037",
        "0x29fcB43b46531BcA003ddC8FCB67FFE91900C762",
        "0x4e1DCf7AD4e460CfD30791CCC4F9c8a4f820ec67"
    )

    @Before
    fun before() {
        MockKAnnotations.init(this)
        safeAccount1 = SafeAccount.fromAddress(
            address = TestsData.account1SafeAddress,
            credentials = TestsData.account1Credentials,
            bundlerClient,
            gasPriceProvider,
            entryPointAddress,
            web3j,
            paymasterClient,
            chainId,
            safeConfig,
            web3jTransactionManager = transactionManager
        )
        safeAccount2 = SafeAccount.fromAddress(
            TestsData.account2SafeAddress,
            TestsData.account2Credentials,
            bundlerClient,
            gasPriceProvider,
            entryPointAddress,
            web3j,
            paymasterClient,
            11155111,
            safeConfig,
            web3jTransactionManager = transactionManager
        )
    }

    @Test
    fun createNewAccount() {
        every { transactionManager.sendCall(any(), any(), any()) } returns TestsData.proxyCode
        val safeAccount = SafeAccount.createNewAccount(
            credentials = TestsData.account1Credentials,
            bundlerClient,
            gasPriceProvider,
            entryPointAddress,
            web3j,
            paymasterClient,
            chainId,
            safeConfig,
            web3jTransactionManager = transactionManager
        )
        assertEquals(TestsData.account1SafeAddress, safeAccount.safeAddress)
    }

    @Test
    fun getInitCode() {
        val safeAccount2 = SafeAccount.fromAddress(
            TestsData.account2SafeAddress,
            TestsData.account2Credentials,
            bundlerClient,
            gasPriceProvider,
            entryPointAddress,
            web3j,
            paymasterClient,
            11155111,
            safeConfig,
            web3jTransactionManager = transactionManager
        )
        val initCode = safeAccount2.getInitCode().toHex()
        val expected =
            "0x4e1dcf7ad4e460cfd30791ccc4f9c8a4f820ec671688f0b900000000000000000000000029fcb43b46531bca003ddc8fcb67ffe91900c7620000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001e4b63e800d000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000010000000000000000000000002dd68b007b46fbe91b9a7c3eda5a7a1063cb5b470000000000000000000000000000000000000000000000000000000000000140000000000000000000000000a581c4a4db7175302464ff3c06380bc3270b403700000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000009d8a62f656a8d1615c1294fd71e9cfb3e4855a4f00000000000000000000000000000000000000000000000000000000000000648d0dc49f00000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000001000000000000000000000000a581c4a4db7175302464ff3c06380bc3270b40370000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
        assertEquals(expected, initCode)
    }

    @Test
    fun predictAddress() {
        every {
            transactionManager.sendCall(any(), any(), any())
        } returns TestsData.proxyCode
        val address = SafeAccount.predictAddress(
            TestsData.account1Credentials.address,
            transactionManager,
            safeConfig,
        )
        assertEquals(TestsData.account1SafeAddress, address)
    }

    @Test
    fun getOwners() {
        every {
            transactionManager.sendCall(
                any(),
                any(),
                any()
            )
        } returns "0x000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000000010000000000000000000000002f920a66c2f9760f6fe5f49b289322ddf60f9103"
        assertEquals("0x2f920a66c2f9760f6fe5f49b289322ddf60f9103", safeAccount1.getOwners()!!.first().value)
    }

    @Test
    fun signOperation() {
        val callData =
            "0x7bb374280000000000000000000000000338dcd5512ae8f3c481c33eb4b6eedf632d1d2f000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000406661abd00000000000000000000000000000000000000000000000000000000"
        val userOperation = UserOperation(
            sender = "0x2ff46f26638977ae8c88e205cca407a1a9725f0b",
            nonce = "0x00",
            initCode = "0x",
            callData = callData,
            callGasLimit = "0x1e8480",
            preVerificationGas = "0xea60",
            verificationGasLimit = "0x07a120",
            maxFeePerGas = "0x02ee7c55e2",
            maxPriorityFeePerGas = "0x1f2ecf7f",
            paymasterAndData = "0x"
        )

        val signature = safeAccount2.signOperation(userOperation, ENTRY_POINT_ADDRESS)
        val expected =
            "0x000000000000000000000000a5927f1a1d8783d9d7033abf5f1883582525a3558055b46a9425c5627a1a83d460d64f361379e3aa710d74b3c4763288598f373c866263c4a45394908c74a6d31c"
        assertEquals(expected, signature.toHex())
    }

    @Test(expected = IllegalArgumentException::class)
    fun fromAddress() {
        SafeAccount.fromAddress(
            "wrong_address",
            TestsData.account1Credentials,
            bundlerClient,
            gasPriceProvider,
            entryPointAddress,
            web3j,
            paymasterClient,
            chainId,
            safeConfig,
        )
    }


}