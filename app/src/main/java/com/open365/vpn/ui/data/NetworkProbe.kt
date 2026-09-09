package com.open365.vpn.ui.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import java.net.URLConnection

/** 出口网络信息（经已连接的本地 SOCKS5 代理查询） */
data class ExitInfo(
    val ip: String,
    val country: String,
    val countryCode: String,
    val asn: String,
    val org: String,
)

/**
 * 网络探测：
 * - 出口 IP/ASN：通过 127.0.0.1:10808 SOCKS5 访问 ip-api.com（走 VPN 隧道）
 * - 节点延迟：Go Proxy.TestConnect 计时（TCP 建立到 example.com 首字节）
 */
object NetworkProbe {

    private const val SOCKS_HOST = "127.0.0.1"
    private const val SOCKS_PORT = 10808

    val exitInfo = MutableStateFlow<ExitInfo?>(null)
    val exitInfoFlow: StateFlow<ExitInfo?> = exitInfo.asStateFlow()

    /** 最近一次测速结果：uri → 延迟毫秒 */
    val latencies = MutableStateFlow<Map<String, Long>>(emptyMap())
    val latenciesFlow: StateFlow<Map<String, Long>> = latencies.asStateFlow()

    private var lastQueryTime = 0L
    private const val MIN_QUERY_INTERVAL_MS = 15_000L

    fun clear() {
        exitInfo.value = null
        lastQueryTime = 0
    }

    /** 查询出口 IP/ASN（默认限频，force = true 跳过限频） */
    suspend fun queryExitInfo(force: Boolean = false): ExitInfo? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!force && now - lastQueryTime < MIN_QUERY_INTERVAL_MS) return@withContext exitInfo.value
        lastQueryTime = now
        try {
            val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(SOCKS_HOST, SOCKS_PORT))
            val conn = URL("http://ip-api.com/json/?fields=status,country,countryCode,query,as,asname").openConnection(proxy) as URLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            val body = conn.inputStream.use { input ->
                val buf = ByteArrayOutputStream()
                val chunk = ByteArray(2048)
                while (true) {
                    val n = input.read(chunk)
                    if (n < 0) break
                    buf.write(chunk, 0, n)
                }
                buf.toString("UTF-8")
            }
            val json = JSONObject(body)
            if (json.optString("status") != "success") return@withContext null
            val info = ExitInfo(
                ip = json.optString("query", "—"),
                country = json.optString("country", "—"),
                countryCode = json.optString("countryCode", ""),
                asn = json.optString("as", "—"),
                org = json.optString("asname", "—"),
            )
            exitInfo.value = info
            info
        } catch (e: Exception) {
            LogBus.post("[Probe] 出口信息查询失败: ${e.message}")
            null
        }
    }

    /** 通过指定节点测延迟，返回毫秒；失败返回 null */
    suspend fun measureLatency(nodeUri: String): Long? = withContext(Dispatchers.IO) {
        val proxy = try {
            x365mobile.Proxy()
        } catch (e: Throwable) {
            LogBus.post("[Probe] Go Proxy 初始化失败: ${e.message}")
            return@withContext null
        }
        try {
            val start = System.currentTimeMillis()
            val result = proxy.testConnect(nodeUri)
            val elapsed = System.currentTimeMillis() - start
            if (result.isBlank()) null else elapsed
        } catch (e: Throwable) {
            LogBus.post("[Probe] 测速失败: ${e.message}")
            null
        }
    }
}
