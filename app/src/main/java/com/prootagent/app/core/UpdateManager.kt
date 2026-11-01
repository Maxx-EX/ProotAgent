package com.prootagent.app.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateManager {
    private const val API_URL = "https://api.github.com/repos/Maxx-EX/ProotAgent/releases/latest"

    suspend fun checkUpdate(currentVersion: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val conn = URL(API_URL).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            
            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val latestVersion = json.getString("tag_name").replace("v", "")
                val body = json.optString("body", "无更新日志")
                
                if (latestVersion != currentVersion && latestVersion > currentVersion) {
                    return@withContext Pair(true, "发现新版本: v$latestVersion\n\n$body")
                }
            }
            return@withContext Pair(false, "已是最新版本")
        } catch (e: Exception) {
            return@withContext Pair(false, "检查更新失败: ${e.message}")
        }
    }
}
