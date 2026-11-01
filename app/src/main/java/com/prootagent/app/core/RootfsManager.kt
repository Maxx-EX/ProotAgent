package com.prootagent.app.core

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

class RootfsManager(private val filesDir: File) {
    private val rootfsDir = File(filesDir, "rootfs")
    private val rootfsTar = File(filesDir, "rootfs.tar.gz")
    private val defaultUrl = "https://dl-cdn.alpinelinux.org/alpine/v3.19/releases/aarch64/alpine-minirootfs-3.19.1-aarch64.tar.gz"

    fun isInstalled(): Boolean = File(rootfsDir, ".installed").exists()
    
    fun deleteRootfs() {
        rootfsDir.deleteRecursively()
        rootfsTar.delete()
    }

    fun prepareEnvironment(): Flow<Pair<Int, String>> = flow {
        if (isInstalled()) {
            emit(Pair(100, "环境已就绪"))
            return@flow
        }

        rootfsDir.mkdirs()
        var downloaded = 0L

        // 1. 下载阶段
        try {
            val conn = URL(defaultUrl).openConnection() as HttpURLConnection
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
            }
            output.close()
            input.close()
        } catch (e: Exception) {
            emit(Pair(-1, "下载失败: ${e.message}"))
            return@flow
        }

        // 2. 解压阶段
        emit(Pair(50, "下载完成，正在解压..."))
        try {
            extractTarGz(rootfsTar, rootfsDir)
            File(rootfsDir, ".installed").createNewFile()
            rootfsTar.delete()
            emit(Pair(100, "环境准备完毕"))
        } catch (e: Exception) {
            emit(Pair(-1, "解压失败: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    private fun extractTarGz(tarGzFile: File, destDir: File) {
        GzipCompressorInputStream(BufferedInputStream(FileInputStream(tarGzFile))).use { gzipIn ->
            TarArchiveInputStream(gzipIn).use { tarIn ->
                var entry = tarIn.nextEntry
                while (entry != null) {
                    val outFile = File(destDir, entry.name)
                    // 防路径穿越攻击
                    if (!outFile.canonicalPath.startsWith(destDir.canonicalPath)) {
                        throw SecurityException("非法路径")
                    }
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            tarIn.copyTo(fos)
                        }
                    }
                    entry = tarIn.nextEntry
                }
            }
        }
    }
}
