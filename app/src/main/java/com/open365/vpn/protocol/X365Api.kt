package com.open365.vpn.protocol

import android.util.Base64
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.UUID
import java.util.zip.GZIPInputStream
import java.util.zip.Inflater
import javax.crypto.Cipher

/**
 * 365VPN API 客户端。
 *
 * x-sig = device-info JSON 按 245 字节分块，逐块用服务端 RSA-2048 公钥做
 *         PKCS1 v1.5 加密，拼接后 base64。载荷不含时间戳/nonce，同设备可复用。
 * 登录  = POST {api}/v1/auth/login {email,password,reg:false} -> {access_token}
 * 配置  = GET  {api}/v1/app?flag=wassvpn (authorization: JWT)
 * proxy = base64(RSA-2048 块) —— 私钥逐块解密、拼接、zlib 解压 -> 节点 YAML。
 */
object X365Api {

    const val DEFAULT_API = "https://d2hh0svl8tdgyk.cloudfront.net"

    private const val APP_VERSION = "26.7.17"
    private const val CONFIG_VERSION = "25.11.20"
    private const val CORE_VERSION = "26.1.30"

    // ---- 内嵌密钥 ----

    private const val SERVER_PUBKEY_PEM = """
-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAyVJyVZWC/YVA0ziPR03J
eMcr5LJtcV2HgDf9SKOL7Xm9DQ7wn+M/0Wi5AV2uUnIdaM2+mZ2KeybfyfjcaWIZ
a5hbS9eeAoEMNG0BPs9Uh4YDNo67SM7SxwqXDQkZC0MdCGvGPUTbIQbuvmkbklOM
x8XWJqCWXqoDE/lXUIOBZbLNKqkN2MKeSjYOBex04i4vCrUjwvzGs0GxMgMLWp4T
IB8dGdTPLdXcveuCCztY9t+ouF1SAfkvbFXPo0wcTptteX8gEU3IjV4eXIsY7BHB
f9KUwjnK1TOZlvZA0mJMGgzSJGBfUuM0XupLaDuP0BPghBGmMZCf2s6Woq8+AAQw
0wIDAQAB
-----END PUBLIC KEY-----
"""

    private const val SERVER_PRIVKEY_PEM = """
-----BEGIN RSA PRIVATE KEY-----
MIIEpQIBAAKCAQEAyVJyVZWC/YVA0ziPR03JeMcr5LJtcV2HgDf9SKOL7Xm9DQ7w
n+M/0Wi5AV2uUnIdaM2+mZ2KeybfyfjcaWIZVjkMIEw+9kbCPWeqJShqYpgmet9n
yXNuTE4f5qoqDqZ8GQHt1OimEXsFozq5MCIhh2O+O6pU9lgV51Kj6zHOGilhwhya
jaX8C/Rr9bMz7UqAQl/Ydaa21qbfBXsDGq/2kyvwmpgbOkNb17jfGtlwlHFJGmLv
swVfTYRmIH9r8ll/LPs6AtttcUlv2fHtq7H9s+NeCTS7WUpr48mmdZbS1pu7Ki3b
i/mwxMxUe5dWaBM7kJt+V6Fy7HoHoTgpFUYGHQIDAQABAoIBACdxzG4BT7ttZtCb
pdLyJjXQTETQQsox6ZERJ0KarJlYP1a2JSYmh8P3UTw3xyZnLmTw3tXJPK85ZCJ1
7HWHX3B51+riFXn6TidqzrbeDs4Hgo6ThAm/4I35xp1SnM/nBax+qMMM3DDTmjyr
jkokfc5BmC2nh2MGyi/bDLvRs9CDDxho7ShjULZRgLcX+o3jkbvr1565n9vdDLZ7
jW3LlCgBAEDzjYkZRaehRAcoVVQwoMdbAw4tNpKFOI4d9Yo5AZfcqGXr867IsTiy
WPVbVM3rqQ9bn8zFGUvyHp6FP+52Mfqzk+mdWhOQib7GNcjoUTUx39fzqZ8sokv4
p9wiobECgYEA2g2mmemtiZE62ErcUSNtWMpJMZJ7nVETYKWtmqzOwVSdRqoYF0dy
nT+nb5bWu7VsijfbdEoKmP1ozn7TSy5KS6t+w0rAoU5ZJmkPY1DYoRTEYYQPrQlT
uSkQyNdEusHxDYSc+9+wjQR3JI432/3a4GCgDA8uvb+hKqSd+S/bkCUCgYEA7Ftp
imNs3egk92PPbTF5POQ5jPfRcRMzKrpuKTty+kyZhjkxpMbO+Zv3pODcDJK9oZub
maaPMzazyguWQ5ePK4ODxoFPoM8RBe/P20E7/Izt5vAh7sEbkfZQtDd1cXQm7SXb
3ToU7leKwvIg+YpTlnWridoWTwBxSBNmrvVTYJkCgYEAuErdSixkDWb/kxsCRllo
66hcYFdrvrRtajvdFGOFg4TeQIE2R0aNqjXIb9nOt2tIxzEae5iwiEl1MUGjl2ES
Tg4t3yTo1eyAEKSu6sPDs7D3oWuaTCcb6dy5YOYAItATydcRTxfqAeWKRQ0mTPqq
6QUBf++9E4ZI0t+63IcETTkCgYEAx/FMhtU080O+gcjdX/HKLcijJq09b/pd/ndX
WWoBCdxiwzj+1fWYgW9/Pus5OymnYV7RclmEKj7MOM80fllns9e9Ud9yDQcdz3fe
zguRQ0O0vPfGvMJ/ICrOeqWvpBouE89C/xJzQmyr5d4OJwrtSwqf4i2rUYl+Swqe
gsLllYECgYEAjMsc6Yf88Uomh9DC3d3P8pNtVN0Gvv70L9DEBWznR3nhTWujOgJH
432Y8yNnkTvNEscs33OIuyRXl1oAxBQ1Lj5zesTeM93LL/LoimWN8J5mgQT+5d2k
kNML7W+HWxfAoEj38qVsNrvMV5wE/yC9p+FV2Olh+nMiTyCNIzbhArs=
-----END RSA PRIVATE KEY-----
"""

    private val http = HttpTransport()

    // ---- 对外 API ----

    /** 登录，成功返回 access_token (JWT) */
    fun login(api: String, email: String, password: String, did: String): String {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
            .put("reg", false)
        val headers = baseHeaders(did, api)
        headers["x-forwarded-for"] = "192.0.2.1"
        val (code, data) = http.request("POST", "$api/v1/auth/login", body.toString(), headers)
        if (code != 200) throw ApiException("HTTP $code: ${String(data, Charsets.UTF_8).take(200)}")
        val token = JSONObject(String(data, Charsets.UTF_8)).optString("access_token")
        if (token.isEmpty()) throw ApiException("响应中无 access_token")
        return token
    }

    /** 抓取 /v1/app，返回解密后的节点 YAML 文本 */
    fun fetchConfigYaml(api: String, token: String, did: String, deviceName: String, screenW: Int, screenH: Int, osArch: String): String {
        val headers = baseHeaders(did, api, deviceName, screenW, screenH, osArch)
        headers["authorization"] = token
        val (code, data) = http.request("GET", "$api/v1/app?flag=wassvpn", null, headers)
        if (code != 200) throw ApiException("HTTP $code: ${String(data, Charsets.UTF_8).take(200)}")
        val proxy = JSONObject(String(data, Charsets.UTF_8)).optString("proxy")
        if (proxy.isEmpty()) throw ApiException("响应中无 proxy 字段")
        return decryptProxyField(proxy)
    }

    private fun baseHeaders(
        did: String,
        api: String,
        deviceName: String = "android",
        screenW: Int = 0,
        screenH: Int = 0,
        osArch: String = "arm64",
    ): MutableMap<String, String> = mutableMapOf(
        "user-agent" to "365VPN $APP_VERSION",
        "content-type" to "application/json",
        "accept-language" to "en",
        "content-language" to "en",
        "x-did" to did,
        "x-sig" to makeXSig(did, api, deviceName, screenW, screenH, osArch),
    )

    // ---- x-sig ----

    private fun makeXSig(did: String, api: String, deviceName: String, screenW: Int, screenH: Int, osArch: String): String {
        val dev = JSONObject()
        dev.put("app_name", "365VPN")
        dev.put("device_name", deviceName)
        dev.put("fit", 0)
        dev.put("os", "android")
        dev.put("os_version", android.os.Build.VERSION.RELEASE)
        dev.put("app_version", APP_VERSION)
        dev.put("os_arch", osArch)
        dev.put("client_ip", "0.0.0.0")
        dev.put("api", api)
        dev.put("proxy", false)
        dev.put("did", did)
        dev.put("dns", "tls://223.5.5.5")
        dev.put("config_version", CONFIG_VERSION)
        dev.put("core_version", CORE_VERSION)
        dev.put("screen_width", screenW.toString() + ".000000")
        dev.put("screen_height", screenH.toString() + ".000000")
        dev.put("language", "en")
        dev.put("fp", "")
        dev.put("fcm_token", "")
        dev.put("build_id", "")
        dev.put("is_emulator", false)
        dev.put("build_number", "")
        dev.put("package_name", "")
        val blob = dev.toString().toByteArray(Charsets.UTF_8)
        val pub = serverPublicKey()
        val out = ByteArrayOutputStream()
        var i = 0
        while (i < blob.size) {
            val end = minOf(i + 245, blob.size)
            out.write(rsaPublicEncrypt(pub, blob.copyOfRange(i, end)))
            i = end
        }
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    // ---- 解密 ----

    /** proxy 字段：base64 -> RSA-2048 逐 256 字节块私钥解密 -> zlib 解压 */
    private fun decryptProxyField(proxyB64: String): String {
        val data = Base64.decode(proxyB64, Base64.DEFAULT)
        val priv = serverPrivateKey()
        val plain = ByteArrayOutputStream()
        var i = 0
        while (i < data.size) {
            val end = i + 256
            plain.write(rsaPrivateDecrypt(priv, data.copyOfRange(i, end)))
            i = end
        }
        val inf = Inflater()
        inf.setInput(plain.toByteArray())
        val out = ByteArrayOutputStream()
        val buf = ByteArray(4096)
        while (!inf.finished()) {
            val n = inf.inflate(buf)
            if (n == 0) {
                if (inf.needsInput() || inf.needsDictionary()) break
            } else out.write(buf, 0, n)
        }
        inf.end()
        return String(out.toByteArray(), Charsets.UTF_8)
    }

    // ---- 节点解析 ----

    private val LINK_RE = Regex("""x365://[^\s"'<>]+""")

    /**
     * 从解密 YAML 提取节点：proxies: 段内，「    地区名:」为分节键，
     * 更深缩进的 - x365:// 行为节点。返回 (region, uri) 对。
     */
    fun extractNodes(yaml: String): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        var inProxies = false
        var region = ""
        for (raw in yaml.lines()) {
            val line = raw.trimEnd()
            when {
                line == "proxies:" -> { inProxies = true; continue }
                inProxies && line.isNotEmpty() && !line.startsWith(" ") -> {
                    inProxies = false // 顶层键，段结束
                }
                inProxies -> {
                    val section = Regex("^    ([^:]+):\\s*$").find(line)
                    if (section != null) { region = section.groupValues[1].trim(); continue }
                    val m = LINK_RE.find(line)
                    if (m != null) out.add(region to m.value)
                }
            }
        }
        return out
    }

    // ---- RSA ----

    private fun stripPem(pem: String): ByteArray =
        Base64.decode(
            pem.lineSequence()
                .filter { !it.startsWith("-----") && it.isNotBlank() }
                .joinToString(""),
            Base64.DEFAULT,
        )

    private fun serverPublicKey(): PublicKey {
        val spec = X509EncodedKeySpec(stripPem(SERVER_PUBKEY_PEM))
        return KeyFactory.getInstance("RSA").generatePublic(spec)
    }

    /** 内嵌密钥为 PKCS#1 格式 (BEGIN RSA PRIVATE KEY)，Android 只认 PKCS#8 -> 手动套壳 */
    private fun serverPrivateKey(): PrivateKey {
        val pkcs1 = stripPem(SERVER_PRIVKEY_PEM)
        val pkcs8 = wrapPkcs1AsPkcs8(pkcs1)
        return KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(pkcs8))
    }

    /** PKCS#8 = SEQ { int 0, SEQ { OID rsaEncryption, NULL }, OCTET STRING pkcs1 } */
    private fun wrapPkcs1AsPkcs8(pkcs1: ByteArray): ByteArray {
        val version = byteArrayOf(0x02, 0x01, 0x00)
        val algId = byteArrayOf(
            0x30, 0x0D,
            0x06, 0x09, 0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(),
            0x0D, 0x01, 0x01, 0x01,
            0x05, 0x00,
        )
        val octet = derWrap(0x04.toByte(), pkcs1)
        return derWrap(0x30.toByte(), concat(version, algId, octet))
    }

    private fun derWrap(tag: Byte, content: ByteArray): ByteArray {
        val len = content.size
        val header = when {
            len < 128 -> byteArrayOf(tag, len.toByte())
            len < 256 -> byteArrayOf(tag, 0x81.toByte(), len.toByte())
            else -> byteArrayOf(tag, 0x82.toByte(), (len shr 8).toByte(), (len and 0xff).toByte())
        }
        return header + content
    }

    private fun concat(vararg parts: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        parts.forEach { out.write(it) }
        return out.toByteArray()
    }

    private fun rsaPublicEncrypt(pub: PublicKey, data: ByteArray): ByteArray {
        val c = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        c.init(Cipher.ENCRYPT_MODE, pub)
        return c.doFinal(data)
    }

    private fun rsaPrivateDecrypt(priv: PrivateKey, data: ByteArray): ByteArray {
        val c = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        c.init(Cipher.DECRYPT_MODE, priv)
        return c.doFinal(data)
    }

    // ---- HTTP ----

    class ApiException(msg: String) : Exception(msg)

    private class HttpTransport {
        fun request(method: String, url: String, jsonBody: String?, headers: Map<String, String>): Pair<Int, ByteArray> {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 30_000
                requestMethod = method
                setRequestProperty("accept-encoding", "gzip")
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
                if (jsonBody != null) {
                    doOutput = true
                    outputStream.use { it.write(jsonBody.toByteArray(Charsets.UTF_8)) }
                }
            }
            val code = conn.responseCode
            val raw = try {
                conn.inputStream
            } catch (e: Exception) {
                conn.errorStream ?: ByteArrayInputStream(ByteArray(0))
            }.readBytes()
            conn.disconnect()
            val body = if ("gzip".equals(conn.contentEncoding, ignoreCase = true)) {
                GZIPInputStream(ByteArrayInputStream(raw)).readBytes()
            } else raw
            return code to body
        }
    }

    fun newDid(): String = UUID.randomUUID().toString()
}
