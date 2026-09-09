package com.open365.vpn.ui.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 进程级日志总线：UI / Go 回调 / VPN Service 三路日志统一汇入，
 * 取代旧版的 ACTION_LOG 广播（Activity 生命周期外也能安全写入）。
 * 上限 200 行，内部加时间戳。
 */
object LogBus {

    private const val MAX_LINES = 200
    private val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    @Synchronized
    fun post(line: String) {
        val ts = fmt.format(Date())
        _logs.update { current ->
            (current + "[$ts] $line").takeLast(MAX_LINES)
        }
    }

    @Synchronized
    fun clear() {
        _logs.value = emptyList()
    }
}
