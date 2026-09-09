package com.open365.vpn.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.open365.vpn.ui.theme.AppTheme
import com.open365.vpn.ui.theme.previewColors

/** 主题选择底部弹层 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSheet(
    current: AppTheme,
    onSelect: (AppTheme) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "主题配色",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )
            AppTheme.entries.forEach { theme ->
                val enabled = theme != AppTheme.DYNAMIC || android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
                Surface(
                    onClick = { if (enabled) onSelect(theme) },
                    shape = MaterialTheme.shapes.large,
                    color = if (theme == current) MaterialTheme.colorScheme.secondaryContainer
                    else Color.Transparent,
                ) {
                    ListItem(
                        leadingContent = {
                            Row {
                                theme.previewColors.forEach { c ->
                                    Surface(
                                        color = c,
                                        shape = CircleShape,
                                        modifier = Modifier.size(18.dp),
                                    ) {}
                                    Spacer(Modifier.width(4.dp))
                                }
                            }
                        },
                        headlineContent = {
                            Text(
                                theme.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (enabled) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            )
                        },
                        supportingContent = if (theme == AppTheme.DYNAMIC && !enabled) {
                            { Text("需要 Android 12 及以上", style = MaterialTheme.typography.bodySmall) }
                        } else null,
                        trailingContent = {
                            RadioButton(selected = theme == current, onClick = null, enabled = enabled)
                        },
                    )
                }
            }
        }
    }
}

/** 账号弹层：未登录 = 邮箱/密码登录表单；已登录 = 账号信息 + 刷新/退出 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSheet(
    accountEmail: String?,
    busy: Boolean,
    error: String?,
    onLogin: (String, String) -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "365VPN 账号",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
            if (accountEmail == null) {
                var email by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("邮箱") },
                    singleLine = true,
                    enabled = !busy,
                    shape = MaterialTheme.shapes.medium,
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("密码") },
                    singleLine = true,
                    enabled = !busy,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = MaterialTheme.shapes.medium,
                )
                if (error != null) {
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
                Button(
                    onClick = { onLogin(email, password) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    shape = MaterialTheme.shapes.large,
                ) {
                    if (busy) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (busy) "登录中…" else "登录并获取节点")
                }
                Text(
                    "凭据仅保存在本机，用于登录 365VPN API 拉取节点。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            } else {
                ListItem(
                    headlineContent = { Text(accountEmail) },
                    supportingContent = { Text("已登录") },
                    leadingContent = { Icon(Icons.Filled.AccountCircle, contentDescription = null) },
                )
                Button(
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    shape = MaterialTheme.shapes.large,
                ) {
                    if (busy) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (busy) "刷新中…" else "刷新节点")
                }
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text("退出登录")
                }
            }
        }
    }
}

/** 许可证文本弹层（MIT hev-socks5-tunnel / Apache-2.0 AndroidX） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("开源许可证", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

            LicenseBlock(
                title = "hev-socks5-tunnel — MIT License",
                body = MIT_TEXT,
            )
            LicenseBlock(
                title = "AndroidX / Jetpack Compose — Apache License 2.0",
                body = APACHE_NOTICE,
            )
        }
    }
}

@Composable
private fun LicenseBlock(title: String, body: String) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val MIT_TEXT = """
Copyright (c) 2022 hev

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
""".trimIndent()

private val APACHE_NOTICE = """
Copyright (c) The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
""".trimIndent()
