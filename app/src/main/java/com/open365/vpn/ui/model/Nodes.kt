package com.open365.vpn.ui.model

/**
 * 节点模型。countryCode 为 ISO 3166-1 alpha-2（大写），用于国旗 emoji 映射；
 * 由 URI path（/hk 等）或标签（香港等）推断，未知时为 null。
 */
data class NodeItem(
    val uri: String,
    val label: String,
    val server: String,
    val path: String,
    val countryCode: String?,
    val port: String = "",
    val uuid: String = "",
    val sni: String = "",
    val pbk: String = "",
    val sid: String = "",
)

/**
 * 国家/地区 → 国旗 emoji 映射。
 * 优先用 URI path 的 ISO 码（/hk → HK），标签中文名兜底；
 * 中文名映射来自 365VPN API 的 servers 元数据。
 */
object CountryFlags {

    private val LABEL_TO_ISO = mapOf(
        "独立IP" to "US", // API dediips 元数据：64.253.86.0 → US
        "香港" to "HK",
        "日本" to "JP",
        "新加坡" to "SG",
        "台湾" to "TW",
        "美国" to "US",
        "韩国" to "KR",
        "英国" to "GB",
        "德国" to "DE",
        "加拿大" to "CA",
        "澳大利亚" to "AU",
        "泰国" to "TH",
        "马来西亚" to "MY",
        "印度尼西亚" to "ID",
        "柬埔寨" to "KH",
        "阿联酋" to "AE",
        "俄罗斯" to "RU",
        "土耳其" to "TR",
        "西班牙" to "ES",
        "意大利" to "IT",
        "荷兰" to "NL",
        "芬兰" to "FI",
        "波兰" to "PL",
        "瑞士" to "CH",
        "奥地利" to "AT",
        "巴西" to "BR",
        "保加利亚" to "BG",
        "立陶宛" to "LT",
        "罗马尼亚" to "RO",
        "葡萄牙" to "PT",
    )

    /** ISO 3166-1 alpha-2 → Unicode 旗舰 emoji（Regional Indicator 符号对） */
    fun flagEmoji(iso: String?): String {
        if (iso == null || iso.length != 2) return "🌐"
        val base = 0x1F1E6 - 'A'.code
        val sb = StringBuilder(2)
        for (c in iso.uppercase()) {
            val code = c.code
            if (code !in 'A'.code..'Z'.code) return "🌐"
            sb.append(Character.toChars(base + code))
        }
        return sb.toString()
    }

    /** 从 URI path（如 "/hk"、"/duliip"）与标签（如 "香港"）推断 ISO 码 */
    fun isoFrom(path: String, label: String): String? {
        val code = path.trimStart('/').uppercase()
        if (code.length == 2 && code.all { it in 'A'..'Z' }) return code
        return LABEL_TO_ISO[label.trim()]
    }
}

/** 解析 x365:// URI，失败返回 null */
fun parseNode(uri: String): NodeItem? {
    if (!uri.startsWith("x365://")) return null
    fun param(name: String): String {
        if ("$name=" !in uri) return ""
        return uri.substringAfter("$name=").substringBefore("&").substringBefore("#")
    }

    val label = if ("#" in uri) uri.substringAfter("#").trim() else run {
        val p = param("path")
        p.trimStart('/')
    }
    val hostPort = uri.substringAfter("@").substringBefore("?").substringBefore("#")
    val server = hostPort.substringBefore(":")
    val port = if (":" in hostPort) hostPort.substringAfter(":") else ""
    val path = param("path").let { if (it.isEmpty()) "/" else "/$it" }

    return NodeItem(
        uri = uri,
        label = label,
        server = server,
        path = path,
        countryCode = CountryFlags.isoFrom(path, label),
        port = port,
        uuid = uri.substringAfter("x365://").substringBefore("@"),
        sni = param("sni").ifEmpty { param("host") },
        pbk = param("pbk"),
        sid = param("sid"),
    )
}

/** 按新 label 重建 URI 的 #fragment */
fun relabel(uri: String, newLabel: String): String {
    val base = if ("#" in uri) uri.substringBefore("#") else uri
    return "$base#$newLabel"
}
