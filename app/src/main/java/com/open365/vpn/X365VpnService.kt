package com.open365.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.open365.vpn.ui.data.LogBus
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 连接状态机，由 X365VpnService 驱动，UI 直接订阅 */
sealed interface ConnState {
    data object Disconnected : ConnState
    data class Connecting(val label: String) : ConnState
    data class Connected(val label: String) : ConnState
    data class Failed(val message: String) : ConnState
}

/** tun2socks 流量统计（字节） */
data class Traffic(val txBytes: Long, val rxBytes: Long) {
    companion object {
        fun format(bytes: Long): String = when {
            bytes >= 1 shl 30 -> "%.1f GB".format(bytes.toDouble() / (1 shl 30))
            bytes >= 1 shl 20 -> "%.1f MB".format(bytes.toDouble() / (1 shl 20))
            bytes >= 1 shl 10 -> "%.1f KB".format(bytes.toDouble() / (1 shl 10))
            else -> "$bytes B"
        }
    }
}

class X365VpnService : VpnService() {

    companion object {
        const val EXTRA_NODE_URI = "node_uri"
        const val EXTRA_NODE_LABEL = "node_label"
        private const val TAG = "X365Vpn"
        private const val VPN_MTU = 1500
        private const val VPN_ADDRESS = "172.16.0.1"
        private const val VPN_DNS = "8.8.8.8"
        private const val SOCKS_PORT = 10808
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "x365_vpn"

        /** 进程级连接状态，UI（同进程）直接订阅，无需广播 */
        val connState = MutableStateFlow<ConnState>(ConnState.Disconnected)
        val connStateFlow: StateFlow<ConnState> = connState.asStateFlow()

        /** 最近一次流量统计（字节），null = 暂无数据 */
        val traffic = MutableStateFlow<Traffic?>(null)
        val trafficFlow: StateFlow<Traffic?> = traffic.asStateFlow()

        // ── hev-socks5-tunnel JNI ──
        // 这些 native 方法由 libhev-socks5-tunnel.so 提供，JNI 包名为 com/open365/vpn。
        @JvmStatic
        private external fun TProxyStartService(configPath: String, fd: Int): Boolean

        @JvmStatic
        private external fun TProxyStopService(): Boolean

        @JvmStatic
        private external fun TProxyIsRunning(): Boolean

        @JvmStatic
        private external fun TProxyGetStats(): LongArray?

        init {
            System.loadLibrary("hev-socks5-tunnel")
        }
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var proxy: x365mobile.Proxy? = null
    private val running = AtomicBoolean(false)

    private fun log(msg: String) {
        Log.i(TAG, msg)
        LogBus.post(msg)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nodeUri = intent?.getStringExtra(EXTRA_NODE_URI)
        val nodeLabel = intent?.getStringExtra(EXTRA_NODE_LABEL) ?: "X365"

        if (nodeUri == null) {
            log("[VPN] 无节点 URI，停止服务")
            stopSelf()
            return START_NOT_STICKY
        }

        log("[VPN] 收到连接请求: $nodeLabel")
        connState.value = ConnState.Connecting(nodeLabel)
        startForeground(nodeLabel)

        Thread {
            try {
                // 切换节点：先完全停止旧的代理和隧道
                stopExisting()
                startProxy(nodeUri)
                startVpn(nodeLabel)
            } catch (e: Exception) {
                log("[VPN] 启动失败: ${e.message}")
                Log.e(TAG, "VPN start failed", e)
                connState.value = ConnState.Failed(e.message ?: "未知错误")
                stopSelf()
            }
        }.start()

        return START_STICKY
    }

    /**
     * 停止已有的代理和 hev-tunnel，释放端口和 TUN 接口。
     * 在切换节点时调用，避免 10808 端口被占用。
     */
    private fun stopExisting() {
        if (running.get()) {
            log("[VPN] 停止旧连接…")
            running.set(false)
            try { TProxyStopService() } catch (_: Exception) {}
            try { vpnInterface?.close() } catch (_: Exception) {}
            try { proxy?.stop() } catch (_: Exception) {}
            vpnInterface = null
            proxy = null
            // 等待旧资源完全释放
            Thread.sleep(500)
        }
    }

    private fun startProxy(nodeUri: String) {
        log("[Proxy] 初始化 Go SOCKS5 代理…")
        proxy = x365mobile.Proxy()
        val listenAddr = "127.0.0.1:$SOCKS_PORT"
        log("[Proxy] 启动 SOCKS5 监听于 $listenAddr")
        try {
            proxy!!.start(nodeUri, listenAddr)
            log("[Proxy] SOCKS5 已启动，path=${proxy!!.currentPath()}，server=${proxy!!.currentServer()}")
        } catch (e: Exception) {
            log("[Proxy] 启动失败: ${e.message}")
            throw e
        }
    }

    private fun startVpn(label: String) {
        log("[VPN] 配置 TUN 接口 (MTU=$VPN_MTU, addr=$VPN_ADDRESS)")

        val builder = Builder()
            .setMtu(VPN_MTU)
            .addAddress(VPN_ADDRESS, 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer(VPN_DNS)
            .setSession(label)

        // 保护自身应用的 socket 不走 VPN（防止回环）
        // 注意：Go SOCKS5 listener 在 startProxy 中已经建立，这里保护它
        builder.addDisallowedApplication(packageName)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
        }

        vpnInterface = builder.establish()
        if (vpnInterface == null) {
            log("[VPN] TUN 接口创建失败")
            connState.value = ConnState.Failed("TUN 接口创建失败")
            stopSelf()
            return
        }

        log("[VPN] TUN 接口已建立，fd=${vpnInterface!!.fd}")
        startHevTun2Socks(label)
    }

    /**
     * 使用 hev-socks5-tunnel（基于 lwIP 的用户态 TCP/IP 栈）将 TUN 接口
     * 的流量转发到本地 SOCKS5 代理。这替代了之前手写的、不完整的 tun2socks。
     */
    private fun startHevTun2Socks(label: String) {
        val configContent = buildString {
            appendLine("tunnel:")
            appendLine("  mtu: $VPN_MTU")
            appendLine("  ipv4: $VPN_ADDRESS")
            appendLine("socks5:")
            appendLine("  port: $SOCKS_PORT")
            appendLine("  address: 127.0.0.1")
            appendLine("  udp: 'udp'")
            appendLine("misc:")
            appendLine("  tcp-read-write-timeout: 300000")
            appendLine("  udp-read-write-timeout: 60000")
            appendLine("  log-level: info")
        }

        val configFile = File(filesDir, "hev-socks5-tunnel.yaml")
        configFile.writeText(configContent)
        log("[TUN] hev-socks5-tunnel 配置:\n$configContent")

        try {
            val ok = TProxyStartService(configFile.absolutePath, vpnInterface!!.fd)
            if (ok) {
                log("[TUN] hev-socks5-tunnel 已启动")
                running.set(true)
                connState.value = ConnState.Connected(label)
                traffic.value = null
                startStatsThread()
                updateNotificationConnected(label)
            } else {
                log("[TUN] hev-socks5-tunnel 启动失败")
                connState.value = ConnState.Failed("hev-socks5-tunnel 启动失败")
                stopSelf()
            }
        } catch (e: UnsatisfiedLinkError) {
            log("[TUN] hev-socks5-tunnel native 库未找到: ${e.message}")
            Log.e(TAG, "hev-socks5-tunnel native lib not found", e)
            connState.value = ConnState.Failed("native 库未找到: ${e.message}")
            stopSelf()
        } catch (e: Exception) {
            log("[TUN] hev-socks5-tunnel 异常: ${e.message}")
            Log.e(TAG, "hev-socks5-tunnel error", e)
            connState.value = ConnState.Failed(e.message ?: "tunnel 异常")
            stopSelf()
        }
    }

    /** 定期读取 tun2socks 流量统计并写入 flow，状态卡实时显示 */
    private fun startStatsThread() {
        Thread {
            while (running.get()) {
                Thread.sleep(5000)
                try {
                    val stats = TProxyGetStats()
                    if (stats != null && stats.size >= 4) {
                        val txBytes = stats[1]
                        val rxBytes = stats[3]
                        if (txBytes > 0 || rxBytes > 0) {
                            traffic.value = Traffic(txBytes, rxBytes)
                        }
                    }
                } catch (_: Exception) {}
            }
        }.start()
    }

    private fun startForeground(label: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Open365VPN", NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }

        val notification = buildNotification("X365 — $label", "正在连接…")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotificationConnected(label: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(
            NOTIFICATION_ID,
            buildNotification("X365 — $label", "已连接 via ${proxy?.currentPath() ?: "—"}")
        )
    }

    override fun onDestroy() {
        log("[VPN] 服务销毁，清理资源")
        running.set(false)
        // 用户主动断开时才回 DISCONNECTED；失败路径已置 FAILED，不要覆盖
        if (connState.value !is ConnState.Failed) {
            connState.value = ConnState.Disconnected
        }
        traffic.value = null
        try { TProxyStopService() } catch (_: Exception) {}
        try { vpnInterface?.close() } catch (_: Exception) {}
        try { proxy?.stop() } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onRevoke() {
        log("[VPN] VPN 被系统撤销")
        running.set(false)
        connState.value = ConnState.Disconnected
        stopSelf()
    }
}
