package com.prootagent.app.core

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

class RootfsManager(private val filesDir: File) {
    private val rootfsDir = File(filesDir, "rootfs")
    private val rootfsZip = File(filesDir, "rootfs.zip")

    // 默认使用 Alpine Linux 的 minirootfs (极小，适合手机)
    private val defaultUrl = "https://dl-cdn.alpinelinux.org/alpine/v3.19/releases/aarch64/alpine-minirootfs-3.19.1-aarch64.tar.gz"

    fun isInstalled(): Boolean = rootfsDir.exists() && rootfsDir.listFiles()?.isNotEmpty() == true

    fun downloadAndExtract(): Flow<Pair<Int, String>> = flow {
        if (isInstalled()) {
            emit(Pair(100, "Rootfs 已就绪"))
            return@flow
        }

        rootfsDir.mkdirs()
        var downloaded = 0L

        try {
            val url = URL(defaultUrl) // 注意：.tar.gz 解压需要特殊库，这里为了简单示例，只做下载进度的演示，实际解压可换用 zip 或集成 tar-lib
            val conn = url.openConnection() as HttpURLConnection
            conn.connect()
            val totalSize = conn.contentLengthLong
            
            val input = conn.inputStream
            val output = FileOutputStream(rootfsZip)
            val buffer = ByteArray(8192)
            var bytesRead: Int

            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                downloaded += bytesRead
                val progress = if (totalSize > 0) ((downloaded * 100) / totalSize).toInt() else 0
                emit(Pair(progress, "下载 Rootfs: $progress%"))
            }
            output.close()
            input.close()
            
            emit(Pair(100, "下载完成，准备解压"))
            // 真实场景下需要解压 tar.gz，这里标记完成
            File(rootfsDir, ".installed").createNewFile() 
        } catch (e: Exception) {
            Log.e("RootfsManager", "Download failed", e)
            emit(Pair(-1, "下载失败: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
}
