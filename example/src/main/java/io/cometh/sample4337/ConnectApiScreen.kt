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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.cometh.android4337.connect.ApiResult
import io.cometh.android4337.connect.ConnectApi
import io.cometh.android4337.connect.DeviceData
import kotlinx.coroutines.launch

@Composable
fun ConnectApiScreen() {
    val coroutineScope = rememberCoroutineScope()
    var isValidSignatureResult by remember { mutableStateOf("") }
    var messageResult by remember { mutableStateOf("") }

    val apiKey = "bnptvYrGQAqDTJOGpAUiMFAaw3QKjjeN"
    val walletAddress = "0x2AE4d78a1Ec1c9dd5B36fBb7d970Ac304049b9fA"

    val connectApi = ConnectApi(apiKey)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Connect API")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            coroutineScope.launch {
                val result = connectApi.isValidSignature(
                    walletAddress = walletAddress,
                    chainId = 84532,
                    message = "Hello, world!",
                    signature = "0x123456"
                )
                when (result) {
                    is ApiResult.Success -> {
                        isValidSignatureResult = """
                        IsValidSignature ✅
                        Result: ${result.data}
                    """.trimIndent()
                    }

                    is ApiResult.Error -> {
                        isValidSignatureResult = """
                        IsValidSignature ❌
                        Error: ${result.message} (Code: ${result.code})
                    """.trimIndent()
                    }
                }

                Log.i("ConnectApiScreen", isValidSignatureResult)
            }
        }) {
            Text(text = "IsValidSignature")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = isValidSignatureResult, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            coroutineScope.launch {
                connectApi.isValidSignature(
                    walletAddress = walletAddress,
                    chainId = 84532,
                    message = "Hello, world!",
                    signature = "0x123456"
                ).let {
                    when (it) {
                        is ApiResult.Success -> messageResult = "IsValidSignature ✅"
                        is ApiResult.Error -> messageResult = "IsValidSignature ❌"
                    }
                }
                connectApi.getPasskeySignersByWalletAddress(walletAddress).let {
                    when (it) {
                        is ApiResult.Success -> messageResult += """
                            |GetPasskeySignersByWalletAddress ✅
                            |Result: ${it.data}
                        """.trimMargin()

                        is ApiResult.Error -> messageResult += """
                            |GetPasskeySignersByWalletAddress ❌
                            |Error: ${it.message} (Code: ${it.code})
                        """.trimMargin()
                    }
                }

                connectApi.initWallet(
                    chainId = 84532,
                    smartAccountAddress = walletAddress,
                    initiatorAddress = "0x2f920a66c2f9760f6fe5f49b289322ddf60f9103",
                    publicKeyId = walletAddress
                ).let {
                    when (it) {
                        is ApiResult.Success -> messageResult += """
                            
                            |InitWallet ✅
                            |Result: ${it.data}
                        """.trimMargin()

                        is ApiResult.Error -> messageResult += """
                            
                            |InitWallet ❌
                            |Error: ${it.message} (Code: ${it.code})
                        """.trimMargin()
                    }
                }


                connectApi.createWebAuthnSigner(
                    chainId = 84532,
                    walletAddress = walletAddress,
                    publicKeyId = "0x123456",
                    publicKeyX = "0x123456",
                    publicKeyY = "0x123456",
                    deviceData = DeviceData(
                        deviceId = "123456",
                        deviceType = "Android",
                    ),
                    signerAddress = walletAddress,
                    isSharedWebAuthnSigner = true
                ).let {
                    when (it) {
                        is ApiResult.Success -> messageResult += """
                            
                            |CreateWebAuthnSigner ✅
                            |Result: ${it.data}
                        """.trimMargin()

                        is ApiResult.Error -> messageResult += """
                            
                            |CreateWebAuthnSigner ❌
                            |Error: ${it.message} (Code: ${it.code})
                        """.trimMargin()
                    }
                }

                connectApi.getPasskeySignersByWalletAddress(walletAddress).let {
                    when (it) {
                        is ApiResult.Success -> messageResult += """
                            
                            |GetPasskeySignersByWalletAddress ✅
                            |Result: ${it.data}
                        """.trimMargin()

                        is ApiResult.Error -> messageResult += """
                            
                            |GetPasskeySignersByWalletAddress ❌
                            |Error: ${it.message} (Code: ${it.code})
                        """.trimMargin()
                    }
                }

                Log.i("ConnectApiScreen", messageResult)
            }
        }) {
            Text(text = "GO")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = isValidSignatureResult, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))
    }
}
