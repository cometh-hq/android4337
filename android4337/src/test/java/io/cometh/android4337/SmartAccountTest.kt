package io.cometh.android4337

import io.cometh.android4337.bundler.BundlerClient
import io.cometh.android4337.bundler.SimpleBundlerClient
import io.cometh.android4337.gasprice.GasPrice
import io.cometh.android4337.gasprice.UserOperationGasPriceProvider
import io.cometh.android4337.paymaster.PaymasterClient
import io.cometh.android4337.paymaster.SponsorUserOperation
import io.cometh.android4337.paymaster.SponsorUserOperationResponse
import io.cometh.android4337.safe.SafeAccount
import io.cometh.android4337.safe.TestsData
import io.cometh.android4337.utils.hexToBigInt
import io.cometh.android4337.utils.hexToByteArray
import io.cometh.android4337.utils.hexToAddress
import io.cometh.android4337.utils.toHexNoPrefix
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.web3j.protocol.core.Request
import org.web3j.tx.TransactionManager
import java.math.BigInteger

class SmartAccountTest {

    @MockK
    lateinit var gasPriceProvider: UserOperationGasPriceProvider

    @MockK
    lateinit var paymasterClient: PaymasterClient

    @MockK
    lateinit var transactionManager: TransactionManager

    @MockK
    lateinit var httpResponseStub: HttpResponseStub

    val chainId = 11155111
    val entryPointAddress = EntryPointContract.ENTRY_POINT_ADDRESS_V7

    lateinit var safeAccountWithPaymaster: SafeAccount
    lateinit var safeAccountWithoutPaymaster: SafeAccount
    lateinit var safeAccount2: SafeAccount
    lateinit var web3Service: CustomHttpService
    lateinit var bundlerClient: BundlerClient

    @Before
    fun before() {
        MockKAnnotations.init(this)
        web3Service = CustomHttpService(httpResponseStub)
        bundlerClient = SimpleBundlerClient(web3Service)

        safeAccountWithPaymaster = SafeAccount.fromAddress(
            address = TestsData.account1SafeAddress,
            credentials = TestsData.account1Credentials,
            bundlerClient = bundlerClient,
            entryPointAddress = entryPointAddress,
            web3Service = web3Service,
            chainId = chainId,
            gasPriceProvider = gasPriceProvider,
            paymasterClient = paymasterClient,
        )
        safeAccountWithoutPaymaster = SafeAccount.fromAddress(
            address = TestsData.account2SafeAddress,
            credentials = TestsData.account2Credentials,
            bundlerClient = bundlerClient,
            entryPointAddress = entryPointAddress,
            web3Service = web3Service,
            chainId = chainId,
            gasPriceProvider = gasPriceProvider,
        )
        safeAccount2 = SafeAccount.fromAddress(
            TestsData.account2SafeAddress,
            TestsData.account2Credentials,
            bundlerClient = bundlerClient,
            entryPointAddress = entryPointAddress,
            web3Service = web3Service,
            chainId = chainId,
            gasPriceProvider = gasPriceProvider,
            paymasterClient = paymasterClient,
        )
    }

    @Test
    fun getNonce() {
        every { httpResponseStub.getResponse(any()) } returns """
            { "jsonrpc": "2.0", "id": 1, "result": "0x0000000000000000000000000000000000000000000000000000000000000003" }
        """.trimIndent().toInputStream()
        assertEquals(BigInteger.valueOf(3), safeAccountWithPaymaster.getNonce())
    }

    @Test
    fun isDeployed() {
        every { httpResponseStub.getResponse(any()) } returns """
            { "jsonrpc": "2.0", "id": 1, "result": "0x1" }
        """.trimIndent().toInputStream()
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
            to = "0x0338Dcd5512ae8F3c481c33Eb4b6eEdF632D1d2f".hexToAddress(),
            value = BigInteger.ZERO,
            data = "0x06661abd".hexToByteArray()
        )

        assertEquals(safeAccountWithoutPaymaster.accountAddress, userOperation.sender)
        assertEquals("0x03", userOperation.nonce)
        assertNull(userOperation.factory)
        assertNull(userOperation.factoryData)
        assertEquals(
            "0x7bb374280000000000000000000000000338dcd5512ae8f3c481c33eb4b6eedf632d1d2f000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000406661abd00000000000000000000000000000000000000000000000000000000",
            userOperation.callData
        )
        assertEquals("0x01e3fb094e", userOperation.maxFeePerGas)
        assertEquals("0x53cd81aa", userOperation.maxPriorityFeePerGas)
        assertNull(userOperation.paymasterData)
        assertEquals("0xEC2C", userOperation.preVerificationGas)
        assertEquals("0x45BCA", userOperation.verificationGasLimit)
        assertEquals("0x2F44", userOperation.callGasLimit)
    }

    @Test
    fun prepareUserOperationWithAccountDeployedAndNoData() {
        // returns nonce 3
        mockGetNonce()
        mockEthGetCode("0x1")
        mockGasEstimateGetGasPrice()
        mockBundlerEstimateUserOperation()

        val userOperation = safeAccountWithoutPaymaster.prepareUserOperation(
            to = "0xF64DA4EFa19b42ef2f897a3D533294b892e6d99E".hexToAddress(),
            value = BigInteger.ONE,
            data = "0x".hexToByteArray()
        )

        assertEquals(safeAccountWithoutPaymaster.accountAddress, userOperation.sender)
        assertEquals("0x03", userOperation.nonce)
        assertNull(userOperation.factory)
        assertNull(userOperation.factoryData)
        assertEquals(
            "0x7bb37428000000000000000000000000f64da4efa19b42ef2f897a3d533294b892e6d99e0000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
            userOperation.callData
        )
        assertEquals("0x01e3fb094e", userOperation.maxFeePerGas)
        assertEquals("0x53cd81aa", userOperation.maxPriorityFeePerGas)
        assertNull(userOperation.paymasterData)
        assertEquals("0xEC2C", userOperation.preVerificationGas)
        assertEquals("0x45BCA", userOperation.verificationGasLimit)
        assertEquals("0x2F44", userOperation.callGasLimit)
        assertEquals(null, userOperation.signature)
    }

    @Test
    fun prepareUserOperationWithAccountNotDeployed() {
        mockGetNonce(3)
        mockEthGetCode("0x")
        mockGasEstimateGetGasPrice()
        mockBundlerEstimateUserOperation()

        val userOperation = safeAccountWithoutPaymaster.prepareUserOperation(
            to = "0xF64DA4EFa19b42ef2f897a3D533294b892e6d99E".hexToAddress(),
            value = BigInteger.ONE,
            data = "0x".hexToByteArray()
        )

        assertEquals(safeAccountWithoutPaymaster.accountAddress, userOperation.sender)
        assertEquals("0x03", userOperation.nonce)
        assertEquals("0x4e1DCf7AD4e460CfD30791CCC4F9c8a4f820ec67", userOperation.factory)
        assertEquals(
            "0x1688f0b900000000000000000000000029fcb43b46531bca003ddc8fcb67ffe91900c7620000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001e4b63e800d000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000010000000000000000000000002dd68b007b46fbe91b9a7c3eda5a7a1063cb5b47000000000000000000000000000000000000000000000000000000000000014000000000000000000000000075cf11467937ce3f2f357ce24ffc3dbf8fd5c22600000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000009d8a62f656a8d1615c1294fd71e9cfb3e4855a4f00000000000000000000000000000000000000000000000000000000000000648d0dc49f0000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000100000000000000000000000075cf11467937ce3f2f357ce24ffc3dbf8fd5c2260000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
            userOperation.factoryData
        )
        assertEquals(
            "0x7bb37428000000000000000000000000f64da4efa19b42ef2f897a3d533294b892e6d99e0000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
            userOperation.callData
        )
        assertEquals("0x01e3fb094e", userOperation.maxFeePerGas)
        assertEquals("0x53cd81aa", userOperation.maxPriorityFeePerGas)
        assertNull(userOperation.paymasterData)
        assertEquals("0xEC2C", userOperation.preVerificationGas)
        assertEquals("0x45BCA", userOperation.verificationGasLimit)
        assertEquals("0x2F44", userOperation.callGasLimit)
    }

    @Test
    fun prepareUserOperationWithPaymaster() {
        mockGetNonce(3)
        mockEthGetCode("0x1")
        mockGasEstimateGetGasPrice()
        mockBundlerEstimateUserOperation()
        mockPaymasterSponsorUserOperation()

        val userOperation = safeAccountWithPaymaster.prepareUserOperation(
            to = "0x0338Dcd5512ae8F3c481c33Eb4b6eEdF632D1d2f".hexToAddress(),
            value = BigInteger.ZERO,
            data = "0x06661abd".hexToByteArray()
        )

        assertEquals(safeAccountWithPaymaster.accountAddress, userOperation.sender)
        assertEquals("0x03", userOperation.nonce)
        assertNull(userOperation.factory)
        assertNull(userOperation.factoryData)
        assertEquals(
            "0x7bb374280000000000000000000000000338dcd5512ae8f3c481c33eb4b6eedf632d1d2f000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000406661abd00000000000000000000000000000000000000000000000000000000",
            userOperation.callData
        )
        assertEquals("0x163a2", userOperation.callGasLimit)
        assertEquals("0x1b247", userOperation.verificationGasLimit)
        assertEquals("0xef1c", userOperation.preVerificationGas)
        assertEquals("0x01e3fb094e", userOperation.maxFeePerGas)
        assertEquals("0x53cd81aa", userOperation.maxPriorityFeePerGas)
        assertEquals("0x4685d9587a7F72Da32dc323bfFF17627aa632C61", userOperation.paymaster)
        assertEquals("0x1", userOperation.paymasterPostOpGasLimit)
        assertEquals("0x4e09", userOperation.paymasterVerificationGasLimit)
        assertEquals(
            "0xDFF7FA1077Bce740a6a212b3995990682c0Ba66d000000000000000000000000000000000000000000000000000000006672ce7100000000000000000000000000000000000000000000000000000000000000000e499f53c85c53cd4f1444b807e380c6a01a412d7e1cfd24b6153debb97cbc986e6809dff8c005ed94c32bf1d5e722b9f40b909fc89d8982f2f99cb7a91b19f01c",
            userOperation.paymasterData
        )
    }

    private fun mockGasEstimateGetGasPrice(
        maxFeePerGas: String = "0x01e3fb094e",
        maxPriorityFeePerGas: String = "0x53cd81aa"
    ) {
        every { gasPriceProvider.getGasPrice() } returns GasPrice(
            maxFeePerGas = maxFeePerGas.hexToBigInt(),
            maxPriorityFeePerGas = maxPriorityFeePerGas.hexToBigInt()
        )
    }

    private fun mockBundlerEstimateUserOperation(
        preVerificationGas: String = "0xEC2C",
        verificationGasLimit: String = "0x45BCA",
        callGasLimit: String = "0x2F44"
    ) {

        every {
            httpResponseStub.getResponse(match { it.contains("eth_estimateUserOperationGas") })
        } returns """
            {
                "jsonrpc": "2.0",
                "id": 1,
                "result": {
                    "preVerificationGas": "$preVerificationGas",
                    "verificationGasLimit": "$verificationGasLimit",
                    "callGasLimit": "$callGasLimit"
                }
            }
        """.trimIndent().toInputStream()
    }

    private fun mockGetNonce(nonce: Int = 3) {
        every {
            httpResponseStub.getResponse(match { it.contains("eth_call") })
        } returns """
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "result": "0x${nonce.toBigInteger().toHexNoPrefix().padStart(64, '0')}"
                }
            """.trimIndent().toInputStream()
    }

    private fun mockEthGetCode(ethGetCodeResult: String) {
        every { httpResponseStub.getResponse(match { it.contains("eth_getCode") }) } returns """
            {
                "jsonrpc": "2.0",
                "id": 1,
                "result": "$ethGetCodeResult"
            }
        """.trimIndent().toInputStream()
    }

    private fun mockPaymasterSponsorUserOperation(
        paymaster: String = "0x4685d9587a7F72Da32dc323bfFF17627aa632C61",
        paymasterAndData: String = "0xDFF7FA1077Bce740a6a212b3995990682c0Ba66d000000000000000000000000000000000000000000000000000000006672ce7100000000000000000000000000000000000000000000000000000000000000000e499f53c85c53cd4f1444b807e380c6a01a412d7e1cfd24b6153debb97cbc986e6809dff8c005ed94c32bf1d5e722b9f40b909fc89d8982f2f99cb7a91b19f01c",
        paymasterVerificationGasLimit: String = "0x4e09",
        paymasterPostOpGasLimit: String = "0x1",
        preVerificationGas: String = "0xef1c",
        verificationGasLimit: String = "0x1b247",
        callGasLimit: String = "0x163a2",
    ) {
        val sponsorRq = mockk<Request<Any, SponsorUserOperationResponse>>()
        every { sponsorRq.send() } returns SponsorUserOperationResponse().apply {
            result = SponsorUserOperation(
                paymaster,
                paymasterAndData,
                paymasterVerificationGasLimit,
                paymasterPostOpGasLimit,
                preVerificationGas,
                verificationGasLimit,
                callGasLimit
            )
        }
        every { paymasterClient.pmSponsorUserOperation(any(), any()) } returns sponsorRq
    }


}

