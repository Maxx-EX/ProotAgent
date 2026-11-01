package com.prootagent.app.core
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val hasUpdate: Boolean,
    val versionName: String = "",
    val changelog: String = "",
    val downloadUrl: String = ""
)

object UpdateManager {
    suspend fun checkUpdate(currentVersion: String, source: Int): AppUpdateInfo = withContext(Dispatchers.IO) {
        try {
            // 如果国内源可用 KKGitHub API，这里为了稳定暂时统一用官方 API 检查，下载时走代理
            val conn = URL("https://api.github.com/repos/Maxx-EX/ProotAgent/releases/latest").openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.setRequestProperty("User-Agent", "ProotAgent-App")
            
            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val latest = json.getString("tag_name").replace("v", "")
                val body = json.optString("body", "无更新日志")
                val assets = json.optJSONArray("assets")
                
                var rawDownloadUrl = ""
                if (assets != null && assets.length() > 0) {
                    for (i in 0 until assets.length()) {
                        val name = assets.getJSONObject(i).getString("name")
                        if (name.endsWith(".apk")) {
                            rawDownloadUrl = assets.getJSONObject(i).getString("browser_download_url")
                            break
                        }
                    }
                }

                if (latest != currentVersion && latest > currentVersion && rawDownloadUrl.isNotEmpty()) {
                    // 根据更新源处理下载链接
                    val finalUrl = if (source == 1) {
                        // 使用 ghproxy 等国内加速镜像
                        "https://mirror.ghproxy.com/$rawDownloadUrl"
                    } else {
                        rawDownloadUrl
                    }
                    return@withContext AppUpdateInfo(true, "v$latest", body, finalUrl)
                }
            }
            return@withContext AppUpdateInfo(false)
        } catch (e: Exception) {
            return@withContext AppUpdateInfo(false)
        }
    }
}
