package com.open365.vpn

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.open365.vpn.ui.MainViewModel
import com.open365.vpn.ui.screen.AboutTab
import com.open365.vpn.ui.screen.NodesTab
import com.open365.vpn.ui.theme.X365Theme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    private val vpnPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            vm.onVpnPermissionResult(result.resultCode == Activity.RESULT_OK)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val theme by vm.theme.collectAsState()
            X365Theme(appTheme = theme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot(vm)
                }
                // 系统授权对话框请求由 ViewModel 发出，这里发起并回传结果
                val permissionIntent by vm.vpnPermissionRequest.collectAsState()
                LaunchedEffect(permissionIntent) {
                    permissionIntent?.let { vpnPermissionLauncher.launch(it) }
                }
            }
        }
    }
}

@Composable
private fun AppRoot(vm: MainViewModel) {
    val tab by vm.tab.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { vm.setTab(0) },
                    icon = {
                        Icon(
                            if (tab == 0) Icons.Filled.Language else Icons.Outlined.Language,
                            contentDescription = null,
                        )
                    },
                    label = { Text("节点") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { vm.setTab(1) },
                    icon = {
                        Icon(
                            if (tab == 1) Icons.Filled.Info else Icons.Outlined.Info,
                            contentDescription = null,
                        )
                    },
                    label = { Text("关于") },
                )
            }
        },
    ) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (tab) {
                0 -> NodesTab(vm)
                else -> AboutTab(vm)
            }
        }
    }
}
