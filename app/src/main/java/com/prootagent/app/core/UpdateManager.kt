package com.prootagent.app.core
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateManager {
    suspend fun checkUpdate(currentVersion: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val conn = URL("https://api.github.com/repos/Maxx-EX/ProotAgent/releases/latest").openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val latest = json.getString("tag_name").replace("v", "")
                if (latest != currentVersion && latest > currentVersion) return@withContext Pair(true, "发现新版本: v$latest\n\n${json.optString("body", "")}")
            }
            return@withContext Pair(false, "已是最新版本")
        } catch (e: Exception) { return@withContext Pair(false, "检查失败: ${e.message}") }
    }
}
