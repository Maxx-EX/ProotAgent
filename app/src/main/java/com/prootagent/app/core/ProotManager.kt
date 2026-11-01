package com.prootagent.app.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.*
import java.util.zip.ZipFile

class ProotManager(private val nativeLibDir: String) {
    private val prootBin = File(nativeLibDir, "libproot.so")

    fun executeCommand(command: String, rootfsPath: String): Flow<String> = flow {
        if (!prootBin.exists()) {
            emit("❌ 未找到 Proot 二进制: ${prootBin.absolutePath}")
            return@flow
        }
        
        var rootfsDir = File(rootfsPath)
        
        // 智能检查：如果只有压缩包没有解压，尝试自动解压
        if (!File(rootfsDir, "bin/sh").exists()) {
            val tarFile = File(rootfsDir, "ubuntu-base-22.04-base-arm64.tar.gz")
            if (tarFile.exists()) {
                emit("📦 检测到压缩包，正在自动解压到当前目录...")
                try {
                    extractTarGz(tarFile, rootfsDir)
                    emit("✅ 解压完成！")
                } catch (e: Exception) {
                    emit("❌ 解压失败: ${e.message}")
                    return@flow
                }
            } else {
                emit("❌ 找不到 bin/sh，且未发现压缩包，请检查目录。")
                return@flow
            }
        }

        val cmd = listOf(
            prootBin.absolutePath,
            "--link2symlink", "-0",
            "-r", rootfsDir.absolutePath,
            "-b", "/sdcard", "-b", "/dev", "-b", "/proc", "-b", "/sys",
            "-b", "/etc/resolv.conf",
            "/usr/bin/env", "-i",
            "HOME=/root",
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            "TERM=xterm-256color", "LANG=C.UTF-8",
            "/bin/sh", "-c", command
        )

        try {
            val pb = ProcessBuilder(cmd)
            pb.redirectErrorStream(true)
            val process = pb.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) { emit(line!!) }
            emit("✅ 退出码: ${process.waitFor()}")
        } catch (e: Exception) {
            emit("❌ 运行异常: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

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
