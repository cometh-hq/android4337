package nc.startapp.passkey

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import com.fasterxml.jackson.databind.ObjectMapper
import io.cometh.android4337.bundler.SimpleBundlerClient
import io.cometh.android4337.passkey.PassKeySigner
import io.cometh.android4337.passkey.credentials.CreateCredentialResponse
import kotlinx.coroutines.launch
import io.cometh.android4337.passkey.credentials.GetCredentialAuthenticationResponse
import io.cometh.android4337.passkey.publicKeyToXYCoordinates
import io.cometh.android4337.safe.Safe
import io.cometh.android4337.safe.SafeAccount
import io.cometh.android4337.utils.decodeBase64
import io.cometh.android4337.utils.toHex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.web3j.crypto.Credentials
import org.web3j.protocol.http.HttpService
import java.security.SecureRandom

@Composable
fun SignUpScreen() {
    val context = LocalContext.current
    val credentialManager: CredentialManager by lazy { CredentialManager.create(context) }
    val coroutineScope = rememberCoroutineScope()
    var signUpResult by remember { mutableStateOf("") }


    val rpcService = HttpService("https://base-sepolia.g.alchemy.com/v2/UEwp8FtpdjcL5oekF6CjMzxe1D3768XU")
    val bundlerClient = SimpleBundlerClient(HttpService("https://base-sepolia.bundler.cometh.io?apikey=Y3dZHg2cc2qOT9ukzvxZZ7jEloTqx5rx"))
    val privateKey = "4bddaeef5fb283e847abf0bd480a771b7695d70f413b248dc56c0bb1bb4a0b86"
    val credentials = Credentials.create(privateKey)

    val passKeySigner = PassKeySigner(
        rpId = "passkey.startapp.nc",
        context = context,
        credentialManager = credentialManager,
    )

    var safeAccount: SafeAccount? = null

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Sign Up with Passkey")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            coroutineScope.launch {
                passKeySigner.createPasskey("alex")
                passKeySigner.getPasskey()?.let { passKey ->
                    safeAccount = withContext(Dispatchers.IO) {
                        return@withContext SafeAccount.createNewAccount(
                            credentials = credentials,
                            bundlerClient = bundlerClient,
                            chainId = 11155111,
                            web3Service = rpcService,
                            passKeySigner = passKeySigner
                        )
                    }
                    signUpResult = """
                        Passkey Created ✅
                        x=${passKey.x.toHex()}
                        y=${passKey.y.toHex()}
                        Address: ${safeAccount!!.accountAddress}
                    """.trimIndent()
                }
            }
        }) {
            Text(text = "Create passkey")
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
        Text(text = signUpResult)
    }
}
