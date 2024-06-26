package io.cometh.android4337

import io.cometh.android4337.bundler.BundlerClient
import io.cometh.android4337.bundler.response.EthEstimateUserOperationGasResponse
import io.cometh.android4337.bundler.response.UserOperationGasEstimation
import io.cometh.android4337.gasprice.GasPrice
import io.cometh.android4337.gasprice.UserOperationGasPriceProvider
import io.cometh.android4337.paymaster.PaymasterClient
import io.cometh.android4337.paymaster.SponsorUserOperation
import io.cometh.android4337.paymaster.SponsorUserOperationResponse
import io.cometh.android4337.safe.SafeAccount
import io.cometh.android4337.safe.SafeConfig
import io.cometh.android4337.safe.TestsData
import io.cometh.android4337.utils.hexStringToBigInt
import io.cometh.android4337.utils.toHex
import io.cometh.android4337.utils.toHexNoPrefix
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.methods.response.EthGetCode
import org.web3j.tx.TransactionManager
import java.math.BigInteger

class SmartAccountTest {

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
    val safeConfig = SafeConfig(
        "0x2dd68b007B46fBe91B9A7c3EDa5A7a1063cB5b47",
        "0xa581c4A4DB7175302464fF3C06380BC3270b4037",
        "0x29fcB43b46531BcA003ddC8FCB67FFE91900C762",
        "0x4e1DCf7AD4e460CfD30791CCC4F9c8a4f820ec67"
    )

    lateinit var safeAccountWithPaymaster: SafeAccount
    lateinit var safeAccountWithoutPaymaster: SafeAccount
    lateinit var safeAccount2: SafeAccount

    @Before
    fun before() {
        MockKAnnotations.init(this)
        safeAccountWithPaymaster = SafeAccount.fromAddress(
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
        safeAccountWithoutPaymaster = SafeAccount.fromAddress(
            address = TestsData.account2SafeAddress,
            credentials = TestsData.account2Credentials,
            bundlerClient,
            gasPriceProvider,
            entryPointAddress,
            web3j,
            null,
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
    fun getNonce() {
        every {
            transactionManager.sendCall(any(), any(), any())
        } returns "0x0000000000000000000000000000000000000000000000000000000000000003"
        assertEquals(BigInteger.valueOf(3), safeAccountWithPaymaster.getNonce())
    }

    @Test
    fun isDeployed() {
        val request = mockk<Request<String, EthGetCode>>()
        val ethGetCode = EthGetCode().apply { result = "0x1" }
        every { request.send() } returns ethGetCode
        every { web3j.ethGetCode(any(), any()) } returns request
        val isDeployed = safeAccountWithPaymaster.isDeployed()
        assertTrue(isDeployed)
    }

    @Test
    fun prepareUserOperationWithAccountDeployed() {
        // returns nonce 3
        mockGetNonce()
        mockEthGetCode("0x1")
        mockGasEstimateGetGasPrice()
        mockBundlerEstimateUserOperation()

        val userOperation = safeAccountWithoutPaymaster.prepareUserOperation(
            to = "0x0338Dcd5512ae8F3c481c33Eb4b6eEdF632D1d2f",
            value = BigInteger.ZERO,
            data = "0x06661abd"
        )

        assertEquals(safeAccountWithoutPaymaster.accountAddress, userOperation.sender)
        assertEquals(3.toBigInteger(), userOperation.nonce)
        assertEquals("0x", userOperation.initCode.toHex())
        assertEquals(
            "0x7bb374280000000000000000000000000338dcd5512ae8f3c481c33eb4b6eedf632d1d2f000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000406661abd00000000000000000000000000000000000000000000000000000000",
            userOperation.callData.toHex()
        )
        assertEquals(BigInteger.valueOf(12100), userOperation.callGasLimit)
        assertEquals(BigInteger.valueOf(285642), userOperation.verificationGasLimit)
        assertEquals(BigInteger.valueOf(60460), userOperation.preVerificationGas)
        assertEquals("0x01e3fb094e".hexStringToBigInt(), userOperation.maxFeePerGas)
        assertEquals("0x53cd81aa".hexStringToBigInt(), userOperation.maxPriorityFeePerGas)
        assertEquals("0x", userOperation.paymasterAndData.toHex())
        assertEquals("0xEC2C".hexStringToBigInt(), userOperation.preVerificationGas)
        assertEquals("0x45BCA".hexStringToBigInt(), userOperation.verificationGasLimit)
        assertEquals("0x2F44".hexStringToBigInt(), userOperation.callGasLimit)
    }

    @Test
    fun prepareUserOperationWithAccountDeployedAndNoData() {
        // returns nonce 3
        mockGetNonce()
        mockEthGetCode("0x1")
        mockGasEstimateGetGasPrice()
        mockBundlerEstimateUserOperation()

        val userOperation = safeAccountWithoutPaymaster.prepareUserOperation(
            to = "0xF64DA4EFa19b42ef2f897a3D533294b892e6d99E",
            value = BigInteger.ONE,
            data = "0x"
        )

        assertEquals(safeAccountWithoutPaymaster.accountAddress, userOperation.sender)
        assertEquals(3.toBigInteger(), userOperation.nonce)
        assertEquals("0x", userOperation.initCode.toHex())
        assertEquals(
            "0x7bb37428000000000000000000000000f64da4efa19b42ef2f897a3d533294b892e6d99e0000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
            userOperation.callData.toHex()
        )
        assertEquals(BigInteger.valueOf(12100), userOperation.callGasLimit)
        assertEquals(BigInteger.valueOf(285642), userOperation.verificationGasLimit)
        assertEquals(BigInteger.valueOf(60460), userOperation.preVerificationGas)
        assertEquals("0x01e3fb094e".hexStringToBigInt(), userOperation.maxFeePerGas)
        assertEquals("0x53cd81aa".hexStringToBigInt(), userOperation.maxPriorityFeePerGas)
        assertEquals("0x", userOperation.paymasterAndData.toHex())
        assertEquals("0xEC2C".hexStringToBigInt(), userOperation.preVerificationGas)
        assertEquals("0x45BCA".hexStringToBigInt(), userOperation.verificationGasLimit)
        assertEquals("0x2F44".hexStringToBigInt(), userOperation.callGasLimit)
    }

    @Test
    fun prepareUserOperationWithAccountNotDeployed() {
        mockGetNonce(3)
        mockEthGetCode("0x")
        mockGasEstimateGetGasPrice()
        mockBundlerEstimateUserOperation()

        val userOperation = safeAccountWithoutPaymaster.prepareUserOperation(
            to = "0xF64DA4EFa19b42ef2f897a3D533294b892e6d99E",
            value = BigInteger.ONE,
            data = "0x"
        )

        assertEquals(safeAccountWithoutPaymaster.accountAddress, userOperation.sender)
        assertEquals(3.toBigInteger(), userOperation.nonce)
        assertEquals(
            "0x4e1dcf7ad4e460cfd30791ccc4f9c8a4f820ec671688f0b900000000000000000000000029fcb43b46531bca003ddc8fcb67ffe91900c7620000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001e4b63e800d000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000010000000000000000000000002dd68b007b46fbe91b9a7c3eda5a7a1063cb5b470000000000000000000000000000000000000000000000000000000000000140000000000000000000000000a581c4a4db7175302464ff3c06380bc3270b403700000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000009d8a62f656a8d1615c1294fd71e9cfb3e4855a4f00000000000000000000000000000000000000000000000000000000000000648d0dc49f00000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000001000000000000000000000000a581c4a4db7175302464ff3c06380bc3270b40370000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
            userOperation.initCode.toHex()
        )
        assertEquals(
            "0x7bb37428000000000000000000000000f64da4efa19b42ef2f897a3d533294b892e6d99e0000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
            userOperation.callData.toHex()
        )
        assertEquals(BigInteger.valueOf(12100), userOperation.callGasLimit)
        assertEquals(BigInteger.valueOf(285642), userOperation.verificationGasLimit)
        assertEquals(BigInteger.valueOf(60460), userOperation.preVerificationGas)
        assertEquals("0x01e3fb094e".hexStringToBigInt(), userOperation.maxFeePerGas)
        assertEquals("0x53cd81aa".hexStringToBigInt(), userOperation.maxPriorityFeePerGas)
        assertEquals("0x", userOperation.paymasterAndData.toHex())
        assertEquals("0xEC2C".hexStringToBigInt(), userOperation.preVerificationGas)
        assertEquals("0x45BCA".hexStringToBigInt(), userOperation.verificationGasLimit)
        assertEquals("0x2F44".hexStringToBigInt(), userOperation.callGasLimit)
    }

    @Test
    fun prepareUserOperationWithPaymaster() {
        mockGetNonce(3)
        mockEthGetCode("0x1")
        mockGasEstimateGetGasPrice()
        mockBundlerEstimateUserOperation()
        mockPaymasterSponsorUserOperation()

        val userOperation = safeAccountWithPaymaster.prepareUserOperation(
            to = "0x0338Dcd5512ae8F3c481c33Eb4b6eEdF632D1d2f",
            value = BigInteger.ZERO,
            data = "0x06661abd"
        )

        assertEquals(safeAccountWithPaymaster.accountAddress, userOperation.sender)
        assertEquals(3.toBigInteger(), userOperation.nonce)
        assertEquals("0x", userOperation.initCode.toHex())
        assertEquals(
            "0x7bb374280000000000000000000000000338dcd5512ae8f3c481c33eb4b6eedf632d1d2f000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000406661abd00000000000000000000000000000000000000000000000000000000",
            userOperation.callData.toHex()
        )
        assertEquals("0x163a2".hexStringToBigInt(), userOperation.callGasLimit)
        assertEquals("0x1b247".hexStringToBigInt(), userOperation.verificationGasLimit)
        assertEquals("0xef1c".hexStringToBigInt(), userOperation.preVerificationGas)
        assertEquals("0x01e3fb094e".hexStringToBigInt(), userOperation.maxFeePerGas)
        assertEquals("0x53cd81aa".hexStringToBigInt(), userOperation.maxPriorityFeePerGas)
        assertEquals(
            "0xdff7fa1077bce740a6a212b3995990682c0ba66d000000000000000000000000000000000000000000000000000000006672ce7100000000000000000000000000000000000000000000000000000000000000000e499f53c85c53cd4f1444b807e380c6a01a412d7e1cfd24b6153debb97cbc986e6809dff8c005ed94c32bf1d5e722b9f40b909fc89d8982f2f99cb7a91b19f01c",
            userOperation.paymasterAndData.toHex()
        )
    }

    private fun mockGasEstimateGetGasPrice(
        maxFeePerGas: String = "0x01e3fb094e",
        maxPriorityFeePerGas: String = "0x53cd81aa"
    ) {
        every { gasPriceProvider.getGasPrice() } returns GasPrice(
            maxFeePerGas = maxFeePerGas.hexStringToBigInt(),
            maxPriorityFeePerGas = maxPriorityFeePerGas.hexStringToBigInt()
        )
    }

    private fun mockBundlerEstimateUserOperation(
        preVerificationGas: String = "0xEC2C",
        verificationGasLimit: String = "0x45BCA",
        callGasLimit: String = "0x2F44"
    ) {
        // bundler
        val estimateRq = mockk<Request<Any, EthEstimateUserOperationGasResponse>>()
        every { estimateRq.send() } returns EthEstimateUserOperationGasResponse().apply {
            result = UserOperationGasEstimation(
                preVerificationGas,
                verificationGasLimit,
                callGasLimit
            )
        }
        every { bundlerClient.ethEstimateUserOperationGas(any(), any()) } returns estimateRq
    }

    private fun mockGetNonce(nonce: Int = 3) {
        every {
            transactionManager.sendCall(entryPointAddress, any(), any())
        } returns "0x${nonce.toBigInteger().toHexNoPrefix().padStart(64, '0')}"
    }

    private fun mockEthGetCode(ethGetCodeResult: String) {
        val ethGetCodeRq = mockk<Request<String, EthGetCode>>()
        every { ethGetCodeRq.send() } returns EthGetCode().apply { result = ethGetCodeResult }
        every { web3j.ethGetCode(any(), any()) } returns ethGetCodeRq
    }

    private fun mockPaymasterSponsorUserOperation(
        paymasterAndData: String = "0xDFF7FA1077Bce740a6a212b3995990682c0Ba66d000000000000000000000000000000000000000000000000000000006672ce7100000000000000000000000000000000000000000000000000000000000000000e499f53c85c53cd4f1444b807e380c6a01a412d7e1cfd24b6153debb97cbc986e6809dff8c005ed94c32bf1d5e722b9f40b909fc89d8982f2f99cb7a91b19f01c",
        preVerificationGas: String = "0xef1c",
        verificationGasLimit: String = "0x1b247",
        callGasLimit: String = "0x163a2"
    ) {
        val sponsorRq = mockk<Request<Any, SponsorUserOperationResponse>>()
        every { sponsorRq.send() } returns SponsorUserOperationResponse().apply {
            result = SponsorUserOperation(
                paymasterAndData,
                preVerificationGas,
                verificationGasLimit,
                callGasLimit
            )
        }
        every { paymasterClient.pmSponsorUserOperation(any(), any()) } returns sponsorRq
    }


}

