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
import io.cometh.android4337.SmartAccountException
import io.cometh.android4337.bundler.SimpleBundlerClient
import io.cometh.android4337.paymaster.PaymasterClient
import io.cometh.android4337.safe.SafeAccount
import io.cometh.android4337.safe.signer.SignerException
import io.cometh.android4337.safe.signer.eoa.EOASigner
import io.cometh.android4337.safe.signer.passkey.PasskeySigner
import io.cometh.android4337.utils.hexToAddress
import io.cometh.android4337.utils.hexToByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.web3j.crypto.Credentials
import org.web3j.protocol.http.HttpService


@Composable
fun PasskeySignerScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var signUpResult by remember { mutableStateOf("") }
    var safeAddress by remember { mutableStateOf("") }
    var safeBalance by remember { mutableStateOf("") }

    val chainId = 84532
    val rpcService = HttpService("https://base-sepolia.g.alchemy.com/v2/UEwp8FtpdjcL5oekF6CjMzxe1D3768XU")
    val bundlerClient = SimpleBundlerClient(HttpService("https://bundler.cometh.io/$chainId/?apikey=Y3dZHg2cc2qOT9ukzvxZZ7jEloTqx5rx"))
    val credentials = Credentials.create("4bddaeef5fb283e847abf0bd480a771b7695d70f413b248dc56c0bb1bb4a0b86")
    val paymasterClient = PaymasterClient("https://paymaster.cometh.io/$chainId?apikey=Y3dZHg2cc2qOT9ukzvxZZ7jEloTqx5rx")

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Deploy Passkey Signer")
        Text(text = "PublicKey: ${credentials.address}", fontSize = 12.sp)
        Text(text = "Safe Address: $safeAddress", fontSize = 12.sp)
        Text(text = "Safe Balance: $safeBalance", fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    signUpResult = "✅Signer and passkey created"
                    val safeAccount = SafeAccount.createNewAccount(
                        signer = EOASigner(credentials),
                        bundlerClient = bundlerClient,
                        chainId = chainId,
                        web3Service = rpcService,
                        paymasterClient = paymasterClient
                    )
                    safeAddress = safeAccount.accountAddress
//                    val firstUserOpHash = try {
//                        safeAccount.sendUserOperation(
//                            to = "0x2f920a66C2f9760f6fE5F49b289322Ddf60f9103".hexToAddress(),
//                            value = 0.toBigInteger(),
//                            data = "0xaaaa".hexToByteArray()
//                        )
//                    } catch (e: SmartAccountException) {
//                        signUpResult += "\n❌Error sending user operation: ${e.message}"
//                        return@withContext
//                    }
//                    signUpResult += "\n✅Safe account created: $safeAddress // $firstUserOpHash"
                    val signer = try {
                        PasskeySigner.withSigner(
                            context = context,
                            rpId = "sample4337.cometh.io",
                            userName = "single-signer",
                            web3jService = rpcService,
                        )
                    } catch (e: CreateCredentialException) {
                        signUpResult += "\n❌Error creating credentials: ${e.message}"
                        return@withContext
                    } catch (e: SignerException) {
                        signUpResult += "\n❌Error creating signer: ${e.message}"
                        return@withContext
                    }
                    Log.i("SignUpScreen", signUpResult)
                    val enableSignerUserOpHash = try {
                        safeAccount.deployAndEnablePasskeySigner(signer.passkey.x, signer.passkey.y)
                    } catch (e: SmartAccountException) {
                        signUpResult += "\n❌Error deploying and enabling signer: ${e.message}"
                        return@withContext
                    }
                    signUpResult += "\n✅Signer enabled: $enableSignerUserOpHash"
                    Log.i("SignUpScreen", signUpResult)
                    val safePassKeyAccount = SafeAccount.fromAddress(
                        address = safeAddress,
                        signer = signer,
                        bundlerClient = bundlerClient,
                        chainId = chainId,
                        web3Service = rpcService,
                        paymasterClient = paymasterClient
                    )

                    delay(4000)

                    val userOp = try {
                        safePassKeyAccount.sendUserOperation(
                            to = "0x2f920a66C2f9760f6fE5F49b289322Ddf60f9103".hexToAddress(),
                            value = 0.toBigInteger(),
                            data = "0xaaaa".hexToByteArray()
                        )
                    } catch (e: SmartAccountException) {
                        signUpResult += "\n❌Error sending user operation: ${e.message}"
                        return@withContext
                    }
                    signUpResult += "\n✅User operation sent: $userOp"
                    Log.i("SignUpScreen", signUpResult)
                }
            }
        }) {
            Text(text = "GO ")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = signUpResult, fontSize = 12.sp)
    }
}
