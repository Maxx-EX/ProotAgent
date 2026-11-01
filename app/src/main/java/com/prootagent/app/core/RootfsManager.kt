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

class RootfsManager(private val filesDir: File) {
    private val rootfsDir = File(filesDir, "rootfs")
    private val rootfsZip = File(filesDir, "rootfs.zip")
    private val defaultUrl = "https://dl-cdn.alpinelinux.org/alpine/v3.19/releases/aarch64/alpine-minirootfs-3.19.1-aarch64.tar.gz"

    fun isInstalled(): Boolean = File(rootfsDir, ".installed").exists()

    fun downloadAndExtract(): Flow<Pair<Int, String>> = flow {
        if (isInstalled()) { emit(Pair(100, "Rootfs 已就绪")); return@flow }
        rootfsDir.mkdirs()
        var downloaded = 0L
        try {
            val conn = URL(defaultUrl).openConnection() as HttpURLConnection
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
            output.close(); input.close()
            emit(Pair(100, "下载完成，准备解压"))
            File(rootfsDir, ".installed").createNewFile()
        } catch (e: Exception) { emit(Pair(-1, "下载失败: ${e.message}")) }
    }.flowOn(Dispatchers.IO)
}
