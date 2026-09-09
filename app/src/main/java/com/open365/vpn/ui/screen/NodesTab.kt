package com.open365.vpn.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.open365.vpn.ConnState
import com.open365.vpn.Traffic
import com.open365.vpn.ui.MainViewModel
import com.open365.vpn.ui.data.ExitInfo
import com.open365.vpn.ui.data.LogBus
import com.open365.vpn.ui.data.NetworkProbe
import com.open365.vpn.ui.model.CountryFlags
import com.open365.vpn.ui.model.NodeItem

/** 节点 Tab：仪表盘 + 节点列表 + 日志 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodesTab(vm: MainViewModel) {
    val nodes by vm.nodes.collectAsStateWithLifecycle()
    val connState by vm.connState.collectAsStateWithLifecycle()
    val traffic by vm.traffic.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val exitInfo by vm.exitInfo.collectAsStateWithLifecycle()
    val latencies by vm.latencies.collectAsStateWithLifecycle()
    val testing by vm.testing.collectAsStateWithLifecycle()
    val importExpanded by vm.importExpanded.collectAsStateWithLifecycle()
    val toast by vm.toast.collectAsStateWithLifecycle()
    val accountEmail by vm.accountEmail.collectAsStateWithLifecycle()
    val loginBusy by vm.loginBusy.collectAsStateWithLifecycle()
    val loginError by vm.loginError.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var deleteTarget by remember { mutableStateOf<NodeItem?>(null) }
    var editTarget by remember { mutableStateOf<NodeItem?>(null) }
    var showAccount by remember { mutableStateOf(false) }

    LaunchedEffect(toast) {
        toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.consumeToast()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = { Text("Open365VPN", fontWeight = FontWeight.SemiBold) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        // 全页唯一滚动容器：根除旧版 ListView 嵌套 ScrollView 的滚动冲突
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "status") {
                val connectedCountry = nodes.firstOrNull {
                    connState is ConnState.Connected && (connState as ConnState.Connected).label == it.label
                }?.countryCode
                StatusCard(
                    connState = connState,
                    traffic = traffic,
                    exitInfo = exitInfo,
                    countryFlag = CountryFlags.flagEmoji(connectedCountry.takeIf { it != null }),
                    onDisconnect = vm::disconnect,
                    onRefreshExit = vm::refreshExitInfo,
                )
            }

            item(key = "nodes-header") {
                SectionHeader(
                    title = "节点",
                    count = nodes.size,
                    expanded = importExpanded,
                    onToggle = { vm.setImportExpanded(!importExpanded) },
                    onAccount = { showAccount = true },
                )
            }

            if (importExpanded) {
                item(key = "import") {
                    ImportCard(onImport = vm::importNodes)
                }
            }

            itemsIndexed(nodes, key = { _, n -> n.uri }) { _, node ->
                NodeRow(
                    node = node,
                    connected = connState is ConnState.Connected &&
                        (connState as ConnState.Connected).label == node.label,
                    latencyMs = latencies[node.uri],
                    testing = testing.contains(node.uri),
                    onClick = { vm.connect(node) },
                    onLongClick = { deleteTarget = node },
                    onEdit = { editTarget = node },
                    onTest = { vm.testLatency(node) },
                )
            }

            item(key = "logs") {
                LogCard(logs)
            }
        }
    }

    deleteTarget?.let { node ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除节点") },
            text = { Text("确定删除「${node.label}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteNode(node)
                    deleteTarget = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            },
        )
    }

    if (showAccount) {
        AccountSheet(
            accountEmail = accountEmail,
            busy = loginBusy,
            error = loginError,
            onLogin = { email, pwd -> vm.login(email, pwd) },
            onRefresh = { vm.refreshNodes() },
            onLogout = {
                vm.logout()
                showAccount = false
            },
            onDismiss = {
                showAccount = false
                vm.clearLoginError()
            },
        )
    }

    editTarget?.let { node ->
        NodeEditSheet(
            node = node,
            onDismiss = { editTarget = null },
            onRename = { label ->
                vm.renameNode(node, label)
                editTarget = null
            },
            onDelete = {
                vm.deleteNode(node)
                editTarget = null
            },
        )
    }
}

/** 状态仪表盘卡：连接状态 + 流量 + 出口 IP/ASN */
@Composable
private fun StatusCard(
    connState: ConnState,
    traffic: Traffic?,
    exitInfo: ExitInfo?,
    countryFlag: String,
    onDisconnect: () -> Unit,
    onRefreshExit: () -> Unit,
) {
    val (label, statusText, statusColor) = when (val s = connState) {
        is ConnState.Connected -> Triple(s.label, "已连接", MaterialTheme.colorScheme.primary)
        is ConnState.Connecting -> Triple(s.label, "连接中…", MaterialTheme.colorScheme.tertiary)
        is ConnState.Failed -> Triple("", "连接失败：${s.message}", MaterialTheme.colorScheme.error)
        ConnState.Disconnected -> Triple("", "未连接", MaterialTheme.colorScheme.onSurfaceVariant)
    }
    val showFlag = connState is ConnState.Connected && countryFlag != "🌐"

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showFlag) {
                    Text(countryFlag, fontSize = 28.sp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = label.ifEmpty { "Open365VPN" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (connState is ConnState.Connecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = statusColor,
                    )
                    Spacer(Modifier.width(6.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor,
                )
            }

            if (traffic != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "↓ ${Traffic.format(traffic.rxBytes)}   ↑ ${Traffic.format(traffic.txBytes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }

            // 出口网络信息（IP / ASN / 归属地）
            if (connState is ConnState.Connected && exitInfo != null) {
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        InfoRow("出口 IP", exitInfo.ip)
                        InfoRow("归属地", exitInfo.country + exitInfo.countryCode.takeIf { it.isNotEmpty() }?.let { " ($it)" }.orEmpty())
                        InfoRow("ASN", exitInfo.asn)
                        if (exitInfo.org.isNotEmpty() && exitInfo.org != "—") {
                            InfoRow("运营商", exitInfo.org)
                        }
                    }
                }
            }

            if (connState is ConnState.Connected || connState is ConnState.Connecting) {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onDisconnect,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text("断开连接")
                }
            }
        }
    }
}

@Composable
private fun InfoRow(name: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            modifier = Modifier.width(56.dp),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 区块头：标题 + 节点数 + 添加按钮 */
@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAccount: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "$count 个",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onAccount) {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = "账号登录",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.Add,
                contentDescription = if (expanded) "收起导入区" else "添加节点",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** 导入区：粘贴 x365:// URI 批量导入 */
@Composable
private fun ImportCard(onImport: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(16.dp).animateContentSize()) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("x365:// URI（每行一个）") },
                minLines = 3,
                shape = MaterialTheme.shapes.medium,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onImport(text) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("导入")
            }
        }
    }
}

/** 节点行：国旗 + 名称 + 延迟，地址默认隐藏（眼睛切换），点击连接、长按删除 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NodeRow(
    node: NodeItem,
    connected: Boolean,
    latencyMs: Long?,
    testing: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: () -> Unit,
    onTest: () -> Unit,
) {
    var showAddress by remember { mutableStateOf(false) }
    val bg = if (connected) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = if (connected) MaterialTheme.colorScheme.onSecondaryContainer
    else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = MaterialTheme.shapes.large,
        color = bg,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 国旗 emoji 放在圆角底衬上
            Surface(
                shape = CircleShape,
                color = if (connected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Text(
                    text = CountryFlags.flagEmoji(node.countryCode),
                    fontSize = 22.sp,
                    modifier = Modifier.padding(6.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = node.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(8.dp))
                    // 延迟标记
                    when {
                        testing -> CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                        )
                        latencyMs != null -> LatencyBadge(latencyMs)
                    }
                }
                Text(
                    text = if (showAddress) "${node.server}:${node.port} · ${node.path}" else "点击连接 · 长按删除",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (connected) contentColor.copy(alpha = 0.75f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (connected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "当前连接",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            // 地址显隐切换
            IconButton(onClick = { showAddress = !showAddress }, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (showAddress) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (showAddress) "隐藏地址" else "显示地址",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(17.dp),
                )
            }
            // 测速
            IconButton(onClick = onTest, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Filled.Speed,
                    contentDescription = "测速",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(17.dp),
                )
            }
            // 编辑
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "编辑",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

@Composable
private fun LatencyBadge(ms: Long) {
    val color = when {
        ms < 300 -> MaterialTheme.colorScheme.primary
        ms < 800 -> Color(0xFFF5A623)
        else -> MaterialTheme.colorScheme.error
    }
    Surface(shape = CircleShape, color = color.copy(alpha = 0.14f)) {
        Text(
            text = "$ms ms",
            fontSize = 10.sp,
            color = color,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

/** 节点编辑底部弹层：重命名 + 协议详情 + 复制 URI + 删除 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NodeEditSheet(
    node: NodeItem,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(node.label) }
    var showSensitive by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    CountryFlags.flagEmoji(node.countryCode),
                    fontSize = 30.sp,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "编辑节点",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("名称") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )

            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    DetailRow("服务器", "${node.server}:${node.port.ifEmpty { "443" }}")
                    DetailRow("路径", node.path)
                    DetailRow("SNI", node.sni)
                    DetailRow("UUID", maskOr(node.uuid, showSensitive))
                    DetailRow("公钥 pbk", maskOr(node.pbk, showSensitive))
                    DetailRow("短 ID sid", maskOr(node.sid, showSensitive))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { showSensitive = !showSensitive }) {
                    Text(if (showSensitive) "隐藏密钥" else "显示密钥")
                }
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Open365VPN Node", node.uri))
                    Toast.makeText(context, "URI 已复制", Toast.LENGTH_SHORT).show()
                }) { Text("复制 URI") }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { onRename(name) },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large,
                ) { Text("保存") }
                androidx.compose.material3.OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large,
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("删除") }
            }
        }
    }
}

private fun maskOr(value: String, visible: Boolean): String {
    if (value.isEmpty()) return "—"
    return if (visible) value else {
        if (value.length <= 8) "••••••••"
        else value.take(4) + "••••" + value.takeLast(4)
    }
}

@Composable
private fun DetailRow(name: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(88.dp),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 日志卡：等宽字体、内部滚动、自动滚底 */
@Composable
private fun LogCard(logs: List<String>) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "连接日志",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Open365VPN Log", logs.joinToString("\n")))
                    Toast.makeText(context, "已复制 ${logs.size} 行日志", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = "复制日志",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(onClick = { LogBus.clear() }) {
                    Icon(
                        Icons.Filled.DeleteSweep,
                        contentDescription = "清空日志",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                val logState = rememberScrollState()
                LaunchedEffect(logs.size) {
                    if (logs.isNotEmpty()) logState.animateScrollTo(logState.maxValue)
                }
                Text(
                    text = if (logs.isEmpty()) "等待操作…" else logs.joinToString("\n"),
                    modifier = Modifier
                        .height(220.dp)
                        .verticalScroll(logState)
                        .padding(12.dp),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
