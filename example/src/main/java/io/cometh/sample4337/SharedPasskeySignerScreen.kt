package io.cometh.sample4337

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import io.cometh.android4337.SmartAccountException
import io.cometh.android4337.TransactionParams
import io.cometh.android4337.UserOperationReceiptPoller
import io.cometh.android4337.bundler.SimpleBundlerClient
import io.cometh.android4337.paymaster.PaymasterClient
import io.cometh.android4337.safe.SafeAccount
import io.cometh.android4337.safe.signer.passkey.PasskeySigner
import io.cometh.android4337.utils.hexToAddress
import io.cometh.android4337.utils.hexToByteArray
import io.cometh.android4337.utils.toHex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Convert

@Composable
fun SharedPasskeySignerScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var signUpResult by remember { mutableStateOf("") }
    var safeAddress by remember { mutableStateOf("") }
    var safeBalance by remember { mutableStateOf("") }
    var safeAccount: SafeAccount? by remember { mutableStateOf(null) }
    var multisendResult by remember { mutableStateOf("") }
    var messageResult by remember { mutableStateOf("") }

    val chainId = 84532
    val rpcService = HttpService("https://base-sepolia.g.alchemy.com/v2/UEwp8FtpdjcL5oekF6CjMzxe1D3768XU")
    val bundlerClient = SimpleBundlerClient(HttpService("https://bundler.cometh.io/$chainId/?apikey=Y3dZHg2cc2qOT9ukzvxZZ7jEloTqx5rx"))
    val credentials = Credentials.create("4bddaeef5fb283e847abf0bd480a771b7695d70f413b248dc56c0bb1bb4a0b86")
    val paymasterClient = PaymasterClient("https://paymaster.cometh.io/$chainId?apikey=Y3dZHg2cc2qOT9ukzvxZZ7jEloTqx5rx")

    val rpId = "sample4337.cometh.io"
    val userName = "my_user"

    LaunchedEffect(safeAddress) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                if (safeAddress.isNotEmpty()) {
                    Log.i("SignUpScreen", "ethGetBalance=$safeAddress")
                    val balance = Web3j.build(rpcService).ethGetBalance(safeAddress, DefaultBlockParameterName.LATEST).send().balance
                    // convert balance to eth
                    val ethBalance = Convert.fromWei(balance.toString(), Convert.Unit.ETHER)
                    safeBalance = "$ethBalance ETH"
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (PasskeySigner.hasSavedPasskey(context, rpId, userName)) {
            val passkeySigner = PasskeySigner.withSharedSigner(context, rpId, userName)
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    safeAccount = SafeAccount.createNewAccount(
                        bundlerClient = bundlerClient,
                        chainId = chainId,
                        web3Service = rpcService,
                        signer = passkeySigner,
                        paymasterClient = paymasterClient
                    )
                    signUpResult = """
                        Passkey Loaded ✅
                        x=${passkeySigner.passkey.x.toHex()}
                        y=${passkeySigner.passkey.y.toHex()}
                    """.trimIndent()
                    safeAddress = safeAccount!!.accountAddress
                    Log.i("SignUpScreen", "accountAddress=$safeAddress")
                    Log.i("SignUpScreen", signUpResult)
                }
            }
        }
    }


    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Passkey shared signer")
        Text(text = "PublicKey: ${credentials.address}", fontSize = 12.sp)
        Text(text = "Safe Address: $safeAddress", fontSize = 12.sp)
        Text(text = "Safe Balance: $safeBalance", fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            coroutineScope.launch {
                try {
                    val passkeySigner = PasskeySigner.withSharedSigner(context, rpId, userName)
                    val passkey = passkeySigner.passkey
                    safeAccount = withContext(Dispatchers.IO) {
                        return@withContext SafeAccount.createNewAccount(
                            bundlerClient = bundlerClient,
                            chainId = chainId,
                            web3Service = rpcService,
                            signer = passkeySigner,
                            paymasterClient = paymasterClient
                        )
                    }
                    signUpResult = """
                        Passkey Created ✅
                        x=${passkey.x.toHex()}
                        y=${passkey.y.toHex()}
                    """.trimIndent()
                    safeAddress = safeAccount!!.accountAddress

                    Log.i("SignUpScreen", signUpResult)
                } catch (e: CreateCredentialException) {
                    signUpResult = "❌ Create Credential Error: ${e.message}"
                    Log.e("SignUpScreen", "Error: ${e.message}", e)
                }

            }
        }) {
            Text(text = "Create passkey")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        val userOpHash = safeAccount!!.sendUserOperation(
                            to = "0x2f920a66C2f9760f6fE5F49b289322Ddf60f9103".hexToAddress(),
                            value = 0.toBigInteger(),
                            data = "0xaaaa".hexToByteArray()
                        )
                        signUpResult = """
                            User Operation Sent ✅
                            UserOpHash: $userOpHash
                        """.trimIndent()
                        val receipt = UserOperationReceiptPoller(bundlerClient).waitForReceipt(userOpHash)
                        signUpResult += """
                            User Operation Receipt ✅
                            Receipt: status=${receipt?.receipt?.status}
                        """.trimIndent()
                        Log.i("SignUpScreen", signUpResult)
                    } catch (e: SmartAccountException) {
                        signUpResult = when {
                            e is SmartAccountException.InvalidSignatureError -> {
                                "❌ Invalid Signature Error: ${e.message}"
                            }

                            e is SmartAccountException.SignerError && e.cause is GetCredentialException -> {
                                "❌ Get Credential Error: ${e.message}"
                            }

                            else -> "❌ Error: ${e.message}"
                        }
                        Log.e("SignUpScreen", "Error: ${e.message}", e)
                    }
                }
            }
        }) {
            Text(text = "Send User Operation")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = signUpResult, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        val userOpHash = safeAccount!!.sendUserOperation(
                            listOf(
                                TransactionParams(
                                    to = "0x2f920a66C2f9760f6fE5F49b289322Ddf60f9103",
                                    value = "0x0",
                                    data = "0xaaaa"
                                ),
                                TransactionParams(
                                    to = "0x2f920a66C2f9760f6fE5F49b289322Ddf60f9103",
                                    value = "0x0",
                                    data = "0xaaaa"
                                )
                            )
                        )
                        multisendResult = """
                            User Operation Sent ✅
                            UserOpHash: $userOpHash
                        """.trimIndent()
                        Log.i("SignUpScreen", multisendResult)
                    } catch (e: SmartAccountException) {
                        multisendResult = when {
                            e is SmartAccountException.InvalidSignatureError -> {
                                "❌ Invalid Signature Error: ${e.message}"
                            }

                            e is SmartAccountException.SignerError && e.cause is GetCredentialException -> {
                                "❌ Get Credential Error: ${e.message}"
                            }

                            else -> "❌ Error: ${e.message}"
                        }
                        Log.e("SignUpScreen", "Error: ${e.message}", e)
                    }
                }
            }
        }) {
            Text(text = "Send Multi User Operations")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = multisendResult, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        val message = "0xaaaa"
                        val signature = safeAccount!!.signMessage(message)
                        val signatureHex = signature.toHex()
                        Log.i("SignUpScreen", "signature=$signatureHex")
                        val isValidSignature = safeAccount!!.isValidSignature(message, signature)
                        messageResult = """
                            Message signed ✅
                            Signature: $signatureHex
                            isValidSignature: $isValidSignature
                        """.trimIndent()
                        Log.i("SignUpScreen", messageResult)
                    } catch (e: SmartAccountException) {
                        messageResult = "❌ Error: ${e.message}"
                        Log.e("SignUpScreen", "Error: ${e.message}", e)
                    }
                }
            }
        }) {
            Text(text = "Sign Message")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = messageResult, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))
    }
}
