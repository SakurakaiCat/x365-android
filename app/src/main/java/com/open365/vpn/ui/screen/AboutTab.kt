package com.open365.vpn.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.open365.vpn.ui.MainViewModel

/** 关于 Tab：软件详情 + 主题选择 + 开源许可证 */
@Composable
fun AboutTab(vm: MainViewModel) {
    val theme by vm.theme.collectAsStateWithLifecycle()
    var showThemeSheet by remember { mutableStateOf(false) }
    var showLicense by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "app-info") { AppInfoCard() }
        item(key = "appearance") {
            SectionCard(title = "外观") {
                Surface(
                    onClick = { showThemeSheet = true },
                    shape = MaterialTheme.shapes.medium,
                    color = androidx.compose.ui.graphics.Color.Transparent,
                ) {
                    ListItem(
                        headlineContent = { Text("主题配色") },
                        supportingContent = { Text(theme.label) },
                        trailingContent = {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
            }
        }
        item(key = "license") {
            SectionCard(title = "开源信息") {
                LicenseRow(
                    name = "hev-socks5-tunnel",
                    license = "MIT License",
                    onClick = { showLicense = true },
                )
                LicenseRow(
                    name = "AndroidX · Jetpack Compose",
                    license = "Apache License 2.0",
                    onClick = { showLicense = true },
                )
            }
        }
    }

    if (showThemeSheet) {
        ThemeSheet(
            current = theme,
            onSelect = { vm.setTheme(it) },
            onDismiss = { showThemeSheet = false },
        )
    }
    if (showLicense) {
        LicenseSheet(onDismiss = { showLicense = false })
    }
}

@Composable
private fun AppInfoCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        ) {
            Text(
                "Open365VPN",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Open365VPN",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "version 1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "X365 协议 VPN 客户端\nReality TLS · SOCKS5 · tun2socks",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun LicenseRow(name: String, license: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = androidx.compose.ui.graphics.Color.Transparent,
    ) {
        ListItem(
            headlineContent = { Text(name, style = MaterialTheme.typography.bodyMedium) },
            supportingContent = { Text(license, style = MaterialTheme.typography.bodySmall) },
        )
    }
}
