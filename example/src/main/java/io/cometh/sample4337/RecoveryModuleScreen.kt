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
import io.cometh.android4337.bundler.SimpleBundlerClient
import io.cometh.android4337.paymaster.PaymasterClient
import io.cometh.android4337.safe.SafeAccount
import io.cometh.android4337.safe.signer.passkey.PasskeySigner
import io.cometh.android4337.utils.hexToAddress
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
fun RecoveryModuleScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var signUpResult by remember { mutableStateOf("") }
    var safeAddress by remember { mutableStateOf("") }
    var safeBalance by remember { mutableStateOf("") }
    var safeAccount: SafeAccount? by remember { mutableStateOf(null) }
    var currentGuardianResult by remember { mutableStateOf("") }
    var isRecoveryStartedResult by remember { mutableStateOf("") }
    var cancelRecoveryResult by remember { mutableStateOf("") }
    var messageResult by remember { mutableStateOf("") }

    val chainId = 84532
    val rpcService = HttpService("https://base-sepolia.g.alchemy.com/v2/UEwp8FtpdjcL5oekF6CjMzxe1D3768XU")
    val bundlerClient = SimpleBundlerClient(HttpService("https://bundler.cometh.io/$chainId/?apikey=Y3dZHg2cc2qOT9ukzvxZZ7jEloTqx5rx"))
    val credentials = Credentials.create("4bddaeef5fb283e847abf0bd480a771b7695d70f413b248dc56c0bb1bb4a0b86")
    val paymasterClient = PaymasterClient("https://paymaster.cometh.io/$chainId?apikey=Y3dZHg2cc2qOT9ukzvxZZ7jEloTqx5rx")

    val rpId = "sample4337.cometh.io"
    val userName = "my_user"

    Log.i("SignUpScreen", "credentials.address=${credentials.address}")

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
                        recoveryAddress=${safeAccount!!.predictDelayModuleAddress()}
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
        Text(text = signUpResult, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = messageResult, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    val signature = safeAccount!!.enableRecoveryModule(
                        guardianAddress = "0x2f920a66c2f9760f6fe5f49b289322ddf60f9103".hexToAddress(),
                    )
                }
            }
        }) {
            Text(text = "Enable Recovery Module")
        }
        Spacer(modifier = Modifier.height(16.dp))
        // get_current_guardian
        Button(onClick = {
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    val guardianAddress = safeAccount!!.getCurrentGuardian(safeAccount!!.predictDelayModuleAddress().hexToAddress())
                    currentGuardianResult = "Current guardian: $guardianAddress"
                }
            }
        }) {
            Text(text = "Get current guardian")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = currentGuardianResult, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))
        // is_recovery_started
        Button(onClick = {
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    val isRecoveryStarted = safeAccount!!.isRecoveryStarted(safeAccount!!.predictDelayModuleAddress().hexToAddress())
                    isRecoveryStartedResult = "Is recovery started: ${if (isRecoveryStarted) "✅Yes" else "No"}"
                }
            }
        }) {
            Text(text = "Is recovery started ?")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = isRecoveryStartedResult, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))
        // cancel_recovery
        Button(onClick = {
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        val signature = safeAccount!!.cancelRecovery(safeAccount!!.predictDelayModuleAddress().hexToAddress())
                        cancelRecoveryResult = "Cancel recovery signature: $signature"
                    } catch (e: Exception) {
                        cancelRecoveryResult = "Cancel recovery failed: ${e.message}"
                    }
                }
            }
        }) {
            Text(text = "Cancel recovery")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = cancelRecoveryResult, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))
    }
}
