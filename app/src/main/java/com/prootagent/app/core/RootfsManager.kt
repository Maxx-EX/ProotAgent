package com.prootagent.app.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

class RootfsManager(private val context: Context) {
    private val privateRootfsDir = File(context.filesDir, "rootfs")
    private val channelId = "download_channel"
    private val notificationId = 2001

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "环境下载", NotificationManager.IMPORTANCE_LOW)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    // 查找有效的 rootfs 路径
    fun getValidRootfsPath(): String? {
        // 1. 私有目录
        if (isRootfsValid(privateRootfsDir)) return privateRootfsDir.absolutePath
        // 2. 外部常见目录 (用户截图里的路径)
        val externalPaths = listOf(
            "/storage/emulated/0/ai1",
            "/storage/emulated/0/Download/ai1",
            "/storage/emulated/0/rootfs"
        )
        for (path in externalPaths) {
            if (isRootfsValid(File(path))) return path
        }
        return null
    }

    private fun isRootfsValid(dir: File): Boolean {
        if (!dir.exists()) return false
        val sh = File(dir, "bin/sh")
        val bash = File(dir, "bin/bash")
        return (sh.exists() && sh.length() > 0) || (bash.exists() && bash.length() > 0)
    }

    fun isInstalled(): Boolean = getValidRootfsPath() != null

    fun prepareEnvironment(sourceType: Int, osType: Int): Flow<Pair<Int, String>> = flow {
        if (isInstalled()) { emit(Pair(100, "环境已就绪")); return@flow }
        privateRootfsDir.mkdirs()
        val rootfsTar = File(context.filesDir, "rootfs.tar.gz")
        var downloaded = 0L

        val urlStr = when (osType) {
            0 -> when (sourceType) {
                1 -> "https://mirrors.tuna.tsinghua.edu.cn/alpine/v3.19/releases/aarch64/alpine-minirootfs-3.19.1-aarch64.tar.gz"
                else -> "https://dl-cdn.alpinelinux.org/alpine/v3.19/releases/aarch64/alpine-minirootfs-3.19.1-aarch64.tar.gz"
            }
            1 -> when (sourceType) {
                1 -> "https://mirrors.tuna.tsinghua.edu.cn/ubuntu-cdimage/ubuntu-base/releases/22.04/release/ubuntu-base-22.04-base-arm64.tar.gz"
                else -> "https://cdimage.ubuntu.com/ubuntu-base/releases/22.04/release/ubuntu-base-22.04-base-arm64.tar.gz"
            }
            else -> return@flow
        }

        try {
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.connect()
            val totalSize = conn.contentLengthLong
            val input = conn.inputStream
            val output = FileOutputStream(rootfsTar)
            val buffer = ByteArray(8192)
            var bytesRead: Int

            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                downloaded += bytesRead
                val progress = if (totalSize > 0) ((downloaded * 50) / totalSize).toInt() else 0
                emit(Pair(progress, "下载 Rootfs: $progress%"))
                updateNotification(progress, "正在下载系统环境...")
            }
            output.close(); input.close()
        } catch (e: Exception) {
            emit(Pair(-1, "下载失败: ${e.message}"))
            return@flow
        }

        emit(Pair(50, "下载完成，正在解压..."))
        try {
            extractTarGz(rootfsTar, privateRootfsDir)
            File(privateRootfsDir, "etc/resolv.conf").writeText("nameserver 8.8.8.8\nnameserver 114.114.114.114\n")
            rootfsTar.delete()
            emit(Pair(100, "环境准备完毕"))
        } catch (e: Exception) {
            emit(Pair(-1, "解压失败: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    private fun updateNotification(progress: Int, text: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("ProotAgent 环境准备")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(progress < 100)
            .build()
        nm.notify(notificationId, notification)
    }

    private fun extractTarGz(tarGzFile: File, destDir: File) {
        GzipCompressorInputStream(BufferedInputStream(FileInputStream(tarGzFile))).use { gzipIn ->
            TarArchiveInputStream(gzipIn).use { tarIn ->
                var entry = tarIn.nextEntry
                while (entry != null) {
                    val outFile = File(destDir, entry.name)
                    if (!outFile.canonicalPath.startsWith(destDir.canonicalPath)) throw SecurityException("非法路径")
                    if (entry.isDirectory) outFile.mkdirs()
                    else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos -> tarIn.copyTo(fos) }
                    }
                    entry = tarIn.nextEntry
                }
            }
        }
    }
}
