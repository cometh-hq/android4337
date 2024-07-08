package nc.startapp.passkey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import nc.startapp.passkey.theme.Android4337Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Android4337Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    Column(
//                        modifier = Modifier.padding(innerPadding)
//                    ) {
//                        Greeting(name = "Android")
//                        Owners()
//                    }
                    Surface {
                        SignUpScreen()
                    }
                }
            }
        }
    }
}


@Composable
fun Owners(
    mainViewModel: MainViewModel = viewModel()
) {
    Column {
        Button(onClick = {
            mainViewModel.fetchOwners()
        }) {
            Text("Fetch owners")
        }
        LazyColumn {
            items(mainViewModel.owners.value) { owner ->
                Text(owner)
            }
        }
        Button(onClick = {
            mainViewModel.sendUserOperation()
        }) {
            Text("Send UserOp")
        }
        Button(onClick = {
            mainViewModel.sendUserOpWithPaymaster()
        }) {
            Text("Send UserOp With Paymaster")
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Android4337Theme {
        Greeting("Android")
    }
}