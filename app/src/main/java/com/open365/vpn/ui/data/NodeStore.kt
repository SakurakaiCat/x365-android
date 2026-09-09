package com.open365.vpn.ui.data

import android.content.Context
import com.open365.vpn.ui.model.NodeItem
import com.open365.vpn.ui.model.parseNode

/**
 * 节点持久化（SharedPreferences），沿用旧版 key "x365"/"nodes"，
 * 每行一个 x365:// URI，升级安装后旧数据无缝保留。
 */
object NodeStore {

    private const val PREFS = "x365"
    private const val KEY_NODES = "nodes"

    /**
     * 默认节点列表。开源版本不捆绑任何服务器凭证，
     * 用户需自行通过「导入」粘贴 x365:// URI 添加节点。
     */
    val DEFAULT_NODES = listOf<String>()

    fun load(context: Context): MutableList<NodeItem> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_NODES, "") ?: ""
        val nodes = mutableListOf<NodeItem>()
        if (saved.isNotEmpty()) {
            for (line in saved.split("\n")) {
                parseNode(line.trim())?.let { nodes.add(it) }
            }
        }
        if (nodes.isEmpty()) {
            for (uri in DEFAULT_NODES) {
                parseNode(uri)?.let { nodes.add(it) }
            }
            save(context, nodes)
        }
        return nodes
    }

    fun save(context: Context, nodes: List<NodeItem>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_NODES, nodes.joinToString("\n") { it.uri }).apply()
    }
}
