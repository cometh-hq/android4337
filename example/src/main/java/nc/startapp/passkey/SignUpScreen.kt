package nc.startapp.passkey

import android.content.Context
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
import androidx.core.content.edit
import androidx.credentials.CredentialManager
import io.cometh.android4337.SmartAccountException
import io.cometh.android4337.bundler.SimpleBundlerClient
import io.cometh.android4337.passkey.PassKeySigner
import io.cometh.android4337.safe.SafeAccount
import io.cometh.android4337.utils.hexToAddress
import io.cometh.android4337.utils.hexToBigInt
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
fun SignUpScreen() {
    val context = LocalContext.current
    val credentialManager: CredentialManager by lazy { CredentialManager.create(context) }
    val coroutineScope = rememberCoroutineScope()
    var signUpResult by remember { mutableStateOf("") }
    var safeAddress by remember { mutableStateOf("") }
    var safeBalance by remember { mutableStateOf("") }
    var safeAccount: SafeAccount? by remember { mutableStateOf(null) }

    val chainId = 84532
    val rpcService = HttpService("https://base-sepolia.g.alchemy.com/v2/UEwp8FtpdjcL5oekF6CjMzxe1D3768XU")
    val bundlerClient = SimpleBundlerClient(HttpService("https://bundler.cometh.io/$chainId/?apikey=Y3dZHg2cc2qOT9ukzvxZZ7jEloTqx5rx"))
    val credentials = Credentials.create("4bddaeef5fb283e847abf0bd480a771b7695d70f413b248dc56c0bb1bb4a0b86")
//    val paymasterClient = PaymasterClient("https://paymaster.cometh.io/$chainId?apikey=Y3dZHg2cc2qOT9ukzvxZZ7jEloTqx5rx")
    val prefs = context.getSharedPreferences("passkey", Context.MODE_PRIVATE)

    val passKeySigner = PassKeySigner(
        rpId = "passkey.startapp.nc",
        context = context,
        credentialManager = credentialManager,
    )
    Log.i("SignUpScreen", "publicKey=${credentials.address}")

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
        val x = prefs.getString("x", null)
        val y = prefs.getString("y", null)
        if (x != null && y != null) {
            passKeySigner.importPassKey(x.hexToBigInt(), y.hexToBigInt())
            val passKey = passKeySigner.getPasskey()!!
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    safeAccount = SafeAccount.createNewAccount(
                        credentials = credentials,
                        bundlerClient = bundlerClient,
                        chainId = chainId,
                        web3Service = rpcService,
                        passKeySigner = passKeySigner,
//                        gasPriceProvider = RPCGasEstimator(rpcService),
//                        paymasterClient = paymasterClient
                    )
                    signUpResult = """
                        Passkey Loaded ✅
                        x=${passKey.x.toHex()}
                        y=${passKey.y.toHex()}
                    """.trimIndent()
                    safeAddress = safeAccount!!.accountAddress
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
        Text(text = "Sign Up with Passkey")
        Text(text = "PublicKey: ${credentials.address}", fontSize = 12.sp)
        Text(text = "Safe Address: $safeAddress", fontSize = 12.sp)
        Text(text = "Safe Balance: $safeBalance", fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            coroutineScope.launch {
                passKeySigner.createPasskey("alex")
                passKeySigner.getPasskey()?.let { passKey ->
                    safeAccount = withContext(Dispatchers.IO) {
                        return@withContext SafeAccount.createNewAccount(
                            credentials = credentials,
                            bundlerClient = bundlerClient,
                            chainId = chainId,
                            web3Service = rpcService,
                            passKeySigner = passKeySigner,
//                            paymasterClient = paymasterClient
                        )
                    }
                    signUpResult = """
                        Passkey Created ✅
                        x=${passKey.x.toHex()}
                        y=${passKey.y.toHex()}
                    """.trimIndent()
                    safeAddress = safeAccount!!.accountAddress

                    prefs.edit {
                        putString("x", passKey.x.toHex())
                        putString("y", passKey.y.toHex())
                    }

                    Log.i("SignUpScreen", signUpResult)
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
                        Log.i("SignUpScreen", signUpResult)
                    } catch (e: SmartAccountException) {
                        signUpResult = "❌ Error: ${e.message}"
                        Log.e("SignUpScreen", "Error: ${e.message}", e)
                    }
                }
            }
        }) {
            Text(text = "Send User Operation")
        }
//        Button(onClick = {
//            coroutineScope.launch {
//                signUpResult = ""
//                val request = CreatePublicKeyCredentialRequest(
//                    requestJson = createJsonFor("alex@startapp.nc"),
//                    preferImmediatelyAvailableCredentials = true,
//                )
//                val response = try {
//                    credentialManager.createCredential(context, request) as CreatePublicKeyCredentialResponse
//                } catch (e: Exception) {
//                    Log.e("SignUpScreen", "Error: ${e.message}", e)
//                    signUpResult = "Error: ${e.message}"
//                    return@launch
//                }
//                signUpResult = "Sign up done ✅"
//                Log.i("SignUpScreen", "response: ${response.registrationResponseJson}")
//                val responseJson = ObjectMapper().readValue(response.registrationResponseJson, CreateCredentialResponse::class.java)
//                Log.i("SignUpScreen", "publicKey=${responseJson.response.publicKey}")
//
//                // public key to x/y coordinates
//                val publicKey = responseJson.response.publicKey
//                val rawId = responseJson.rawId
//                val (x, y) = publicKeyToXYCoordinates(publicKey.decodeBase64())
//                Log.i("SignUpScreen", "x=$x, y=$y")
//            }
//        }) {
//            Text(text = "Sign Up")
//        }
//        Button(onClick = {
//            coroutineScope.launch {
//                try {
//                    val result = signWithPassKey(context, credentialManager, "0xaaaaaaaaaaaaaa".toByteArray())
//                    Log.i("SignUpScreen", "response: $result")
//                    // check emoji
//                    signUpResult = "Sign with passkey done ✅"
//                } catch (e: Exception) {
//                    Log.e("SignUpScreen", "Error: ${e.message}", e)
//                    signUpResult = "❌ Error: ${e.message}"
//                }
//            }
//        }) {
//            Text(text = "Sign With Passkey")
//        }
//        Spacer(modifier = Modifier.height(16.dp))
//        Button(onClick = {
//            coroutineScope.launch {
//                try {
//                    val result = signWithPassKey(context, credentialManager, "0xaaaaaaaaaaaaaa".toByteArray())
//                    Log.i("SignUpScreen", "response: $result")
//                    // check emoji
//                    signUpResult = "Sign with passkey done ✅"
//                } catch (e: Exception) {
//                    Log.e("SignUpScreen", "Error: ${e.message}", e)
//                    signUpResult = "❌ Error: ${e.message}"
//                }
//            }
//        }) {
//            Text(text = "Create with passkey")
//        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = signUpResult, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))
    }
}
