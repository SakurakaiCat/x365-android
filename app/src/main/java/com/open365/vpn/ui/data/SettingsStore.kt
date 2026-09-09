package com.open365.vpn.ui.data

import android.content.Context
import com.open365.vpn.ui.theme.AppTheme

/** 应用设置持久化 */
object SettingsStore {

    private const val PREFS = "x365_settings"
    private const val KEY_THEME = "theme"
    private const val KEY_EMAIL = "account_email"
    private const val KEY_PASSWORD = "account_password"
    private const val KEY_DID = "account_did"
    private const val KEY_TOKEN = "account_token"
    /** 上次 API 拉到的节点 URI 集合，用于刷新时替换过期节点 */
    private const val KEY_API_NODES = "api_nodes"

    fun loadTheme(context: Context): AppTheme {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return AppTheme.fromId(prefs.getString(KEY_THEME, null))
    }

    fun saveTheme(context: Context, theme: AppTheme) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME, theme.id).apply()
    }

    // ---- 账号 ----

    data class Account(val email: String, val password: String, val did: String)

    fun loadAccount(context: Context): Account? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val email = prefs.getString(KEY_EMAIL, null) ?: return null
        val password = prefs.getString(KEY_PASSWORD, null) ?: return null
        var did = prefs.getString(KEY_DID, null)
        if (did.isNullOrEmpty()) {
            did = java.util.UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DID, did).apply()
        }
        return Account(email, password, did)
    }

    fun saveAccount(context: Context, account: Account) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_EMAIL, account.email)
            .putString(KEY_PASSWORD, account.password)
            .putString(KEY_DID, account.did)
            .apply()
    }

    fun clearAccount(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(KEY_EMAIL).remove(KEY_PASSWORD).remove(KEY_TOKEN).remove(KEY_API_NODES).apply()
    }

    fun loadToken(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TOKEN, null)

    fun saveToken(context: Context, token: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_TOKEN, token).apply()
    }

    fun loadApiNodeUris(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_API_NODES, emptySet())?.toSet() ?: emptySet()

    fun saveApiNodeUris(context: Context, uris: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putStringSet(KEY_API_NODES, uris).apply()
    }
}
