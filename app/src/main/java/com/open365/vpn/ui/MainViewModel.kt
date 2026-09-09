package com.open365.vpn.ui

import android.app.Application
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.open365.vpn.ConnState
import com.open365.vpn.Traffic
import com.open365.vpn.X365VpnService
import com.open365.vpn.ui.data.ExitInfo
import com.open365.vpn.ui.data.LogBus
import com.open365.vpn.ui.data.NetworkProbe
import com.open365.vpn.ui.data.NodeStore
import com.open365.vpn.ui.data.SettingsStore
import com.open365.vpn.protocol.X365Api
import com.open365.vpn.ui.model.NodeItem
import com.open365.vpn.ui.model.parseNode
import com.open365.vpn.ui.model.relabel
import com.open365.vpn.ui.theme.AppTheme
import go.Seq
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val connState: StateFlow<ConnState> = X365VpnService.connStateFlow
    val traffic: StateFlow<Traffic?> = X365VpnService.trafficFlow
    val logs: StateFlow<List<String>> = LogBus.logs
    val exitInfo: StateFlow<ExitInfo?> = NetworkProbe.exitInfoFlow
    val latencies: StateFlow<Map<String, Long>> = NetworkProbe.latenciesFlow

    private val _nodes = MutableStateFlow<List<NodeItem>>(emptyList())
    val nodes: StateFlow<List<NodeItem>> = _nodes.asStateFlow()

    private val _theme = MutableStateFlow(AppTheme.fromId(null))
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    /** 当前 Tab：0 = 节点，1 = 关于 */
    private val _tab = MutableStateFlow(0)
    val tab: StateFlow<Int> = _tab.asStateFlow()

    /** 导入区展开状态 */
    private val _importExpanded = MutableStateFlow(false)
    val importExpanded: StateFlow<Boolean> = _importExpanded.asStateFlow()

    /** 导入结果一次性提示 */
    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    /** 待授权连接的节点 */
    private var pendingNode: NodeItem? = null

    /** VPN 授权结果回调，由 MainActivity 的 registerForActivityResult 桥接进来 */
    private var permissionResult: ((Boolean) -> Unit)? = null

    /** 系统授权 Intent，非 null 时 Activity 应发起 startActivityForResult */
    val vpnPermissionRequest = MutableStateFlow<Intent?>(null)

    /** 正在测速的节点 uri 集合 */
    private val _testing = MutableStateFlow<Set<String>>(emptySet())
    val testing: StateFlow<Set<String>> = _testing.asStateFlow()

    // ---- 账号 / API 登录 ----

    /** 当前登录邮箱，null = 未登录 */
    private val _accountEmail = MutableStateFlow<String?>(null)
    val accountEmail: StateFlow<String?> = _accountEmail.asStateFlow()

    /** 登录/刷新中 */
    private val _loginBusy = MutableStateFlow(false)
    val loginBusy: StateFlow<Boolean> = _loginBusy.asStateFlow()

    /** 登录弹层错误信息（一次性） */
    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    init {
        // 注册 Go 侧 SOCKS5/隧道日志回调（gomobile 在 Go goroutine 中回调）
        try {
            Seq.setContext(app.applicationContext)
            x365mobile.X365mobile.setLogCallback(object : x365mobile.LogCallback {
                override fun onLog(msg: String?) {
                    LogBus.post("[Go] ${msg ?: ""}")
                }
            })
            LogBus.post("[UI] Go 日志回调已注册")
        } catch (e: Throwable) {
            LogBus.post("[UI] Go 日志回调注册失败: ${e.message}")
            android.util.Log.e("Open365VPN", "Go init failed", e)
        }
        _nodes.value = NodeStore.load(app)
        _theme.value = SettingsStore.loadTheme(app)
        _accountEmail.value = SettingsStore.loadAccount(app)?.email

        // 已存账号：启动后台自动刷新节点
        if (_accountEmail.value != null) {
            refreshNodes(silent = true)
        }

        // 已连接状态下打开 App：拉取出口信息
        viewModelScope.launch {
            X365VpnService.connStateFlow.collect { state ->
                if (state is ConnState.Connected) {
                    launch { NetworkProbe.queryExitInfo() }
                } else if (state is ConnState.Disconnected || state is ConnState.Failed) {
                    NetworkProbe.clear()
                }
            }
        }
    }

    fun consumeToast() {
        _toast.value = null
    }

    fun setTab(index: Int) {
        _tab.value = index
    }

    fun setTheme(theme: AppTheme) {
        _theme.value = theme
        SettingsStore.saveTheme(getApplication(), theme)
    }

    fun setImportExpanded(expanded: Boolean) {
        _importExpanded.value = expanded
    }

    /** 点击节点：无授权直接连接；需要授权则请求系统 VPN 授权对话框 */
    fun connect(node: NodeItem) {
        LogBus.post("[UI] 准备连接: ${node.label} (${node.server}${node.path})")
        val prepare = VpnService.prepare(getApplication())
        if (prepare == null) {
            startVpn(node)
        } else {
            pendingNode = node
            permissionResult = { granted ->
                permissionResult = null
                val target = pendingNode
                pendingNode = null
                if (granted && target != null) {
                    startVpn(target)
                } else {
                    LogBus.post("[UI] VPN 权限被拒绝")
                }
            }
            vpnPermissionRequest.value = prepare
        }
    }

    /** 由 Activity 调用：把系统授权结果传回挂起的 connect() */
    fun onVpnPermissionResult(granted: Boolean) {
        vpnPermissionRequest.value = null
        permissionResult?.invoke(granted)
    }

    private fun startVpn(node: NodeItem) {
        val intent = Intent(getApplication(), X365VpnService::class.java).apply {
            putExtra(X365VpnService.EXTRA_NODE_URI, node.uri)
            putExtra(X365VpnService.EXTRA_NODE_LABEL, node.label)
        }
        getApplication<Application>().startService(intent)
    }

    fun disconnect() {
        LogBus.post("[UI] 断开连接")
        getApplication<Application>().stopService(
            Intent(getApplication(), X365VpnService::class.java)
        )
    }

    /** 单节点测速：Go TestConnect 计时 */
    fun testLatency(node: NodeItem) {
        if (_testing.value.contains(node.uri)) return
        _testing.value = _testing.value + node.uri
        viewModelScope.launch {
            val ms = NetworkProbe.measureLatency(node.uri)
            if (ms != null) {
                NetworkProbe.latencies.value = NetworkProbe.latencies.value + (node.uri to ms)
                LogBus.post("[Probe] ${node.label} 延迟 ${ms}ms")
            } else {
                NetworkProbe.latencies.value = NetworkProbe.latencies.value - node.uri
            }
            _testing.value = _testing.value - node.uri
        }
    }

    /** 手动刷新出口信息 */
    fun refreshExitInfo() {
        viewModelScope.launch { NetworkProbe.queryExitInfo(force = true) }
    }

    fun importNodes(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            _toast.value = "请先粘贴 URI"
            return
        }
        var count = 0
        val current = _nodes.value.toMutableList()
        for (line in trimmed.lines()) {
            val uri = line.trim()
            if (!uri.startsWith("x365://")) continue
            val parsed = parseNode(uri) ?: continue
            if (current.any { it.uri == parsed.uri }) continue
            current.add(parsed)
            count++
        }
        if (count > 0) {
            _nodes.value = current
            NodeStore.save(getApplication(), current)
            _toast.value = "导入 $count 个节点"
            LogBus.post("[UI] 导入 $count 个节点，当前共 ${current.size} 个")
            _importExpanded.value = false
        } else {
            _toast.value = "未找到有效 x365:// URI"
        }
    }

    fun deleteNode(node: NodeItem) {
        val current = _nodes.value.filterNot { it.uri == node.uri }
        _nodes.value = current
        NodeStore.save(getApplication(), current)
        LogBus.post("[UI] 已删除节点: ${node.label}")
    }

    /** 重命名节点（更新 label 并重建 URI fragment） */
    fun renameNode(node: NodeItem, newLabel: String) {
        val label = newLabel.trim()
        if (label.isEmpty()) {
            _toast.value = "名称不能为空"
            return
        }
        val newUri = relabel(node.uri, label)
        val current = _nodes.value.map {
            if (it.uri == node.uri) parseNode(newUri) ?: it else it
        }
        _nodes.value = current
        NodeStore.save(getApplication(), current)
        LogBus.post("[UI] 节点已重命名: ${node.label} → $label")
    }

    // ---- 账号 / API ----

    fun clearLoginError() { _loginError.value = null }

    /** 登录：验证账号、保存凭据，并立即拉取节点 */
    fun login(email: String, password: String) {
        val e = email.trim()
        if (e.isEmpty() || password.isEmpty()) {
            _loginError.value = "请输入邮箱和密码"
            return
        }
        if (_loginBusy.value) return
        _loginBusy.value = true
        viewModelScope.launch {
            try {
                val account = SettingsStore.Account(e, password, X365Api.newDid())
                fetchNodes(account)
                SettingsStore.saveAccount(getApplication(), account)
                _accountEmail.value = e
                _loginError.value = null
                LogBus.post("[API] 登录成功: $e")
            } catch (ex: Exception) {
                _loginError.value = ex.message ?: "登录失败"
                LogBus.post("[API] 登录失败: ${ex.message}")
            } finally {
                _loginBusy.value = false
            }
        }
    }

    /** 用已存凭据刷新节点（复用 token，失败自动重登） */
    fun refreshNodes(silent: Boolean = false) {
        if (_loginBusy.value) return
        _loginBusy.value = true
        viewModelScope.launch {
            try {
                val account = SettingsStore.loadAccount(getApplication())
                    ?: throw X365Api.ApiException("未登录")
                fetchNodes(account)
                if (!silent) _toast.value = "节点已刷新"
            } catch (ex: Exception) {
                if (!silent) _toast.value = "刷新失败: ${ex.message}"
                LogBus.post("[API] 刷新节点失败: ${ex.message}")
            } finally {
                _loginBusy.value = false
            }
        }
    }

    /** 退出登录：清除凭据并移除 API 来源节点 */
    fun logout() {
        val app = getApplication<Application>()
        val apiUris = SettingsStore.loadApiNodeUris(app)
        val current = _nodes.value.filterNot { it.uri in apiUris }
        _nodes.value = current
        NodeStore.save(app, current)
        SettingsStore.clearAccount(app)
        _accountEmail.value = null
        LogBus.post("[API] 已退出登录，移除 ${apiUris.size} 个 API 节点")
    }

    /** 登录 → /v1/app → 解密配置 → 同步节点列表（IO 线程） */
    private suspend fun fetchNodes(account: SettingsStore.Account) = withContext(Dispatchers.IO) {
        val app = getApplication<Application>()
        val metrics = app.resources.displayMetrics
        // 复用 token，失败则重新登录
        var token = SettingsStore.loadToken(app)
        val yaml = try {
            if (token.isNullOrEmpty()) throw X365Api.ApiException("no token")
            X365Api.fetchConfigYaml(
                X365Api.DEFAULT_API, token, account.did,
                android.os.Build.MODEL, metrics.widthPixels, metrics.heightPixels,
                osArch = "arm64",
            )
        } catch (first: Exception) {
            token = X365Api.login(X365Api.DEFAULT_API, account.email, account.password, account.did)
            SettingsStore.saveToken(app, token)
            X365Api.fetchConfigYaml(
                X365Api.DEFAULT_API, token, account.did,
                android.os.Build.MODEL, metrics.widthPixels, metrics.heightPixels,
                osArch = "arm64",
            )
        }
        val found = X365Api.extractNodes(yaml)
        if (found.isEmpty()) throw X365Api.ApiException("配置中没有节点")

        // 同步：移除失效 API 节点，加入/更新新节点
        val oldApiUris = SettingsStore.loadApiNodeUris(app)
        val newUris = mutableSetOf<String>()
        val merged = _nodes.value.filterNot { it.uri in oldApiUris }.toMutableList()
        for ((region, uri) in found) {
            // 独立 IP 段的 YAML 键就是 IP 本身，显示为「独立IP」
            val label = when {
                region.isEmpty() -> "节点"
                region.first().isDigit() -> "独立IP"
                else -> region
            }
            val labeled = relabel(uri, label)
            val item = parseNode(labeled) ?: continue
            newUris.add(item.uri)
            if (merged.none { it.uri == item.uri }) merged.add(item)
        }
        _nodes.value = merged
        NodeStore.save(app, merged)
        SettingsStore.saveApiNodeUris(app, newUris)
        LogBus.post("[API] 节点同步完成: ${found.size} 个")
    }
}
