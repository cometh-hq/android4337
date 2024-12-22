package io.cometh.sample4337

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.cometh.sample4337.theme.Android4337Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var tabIndex by remember { mutableStateOf(0) }
            val tabs = listOf("Shared Signer", "Deployed Signer", "Connect Api")
            Android4337Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Surface(modifier = Modifier.padding(top = 32.dp)) {
                        TabRow(selectedTabIndex = tabIndex) {
                            tabs.forEachIndexed { index, title ->
                                Tab(text = { Text(title) },
                                    selected = tabIndex == index,
                                    onClick = { tabIndex = index }
                                )
                            }
                        }
                        when (tabIndex) {
                            0 -> SharedPasskeySignerScreen()
                            1 -> PasskeySignerScreen()
                            2 -> ConnectApiScreen()
                        }
                    }
                }
            }
        }
    }
}