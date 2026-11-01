package com.prootagent.app.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.*

class ProotManager(private val nativeLibDir: String) {
    private val prootBin = File(nativeLibDir, "libproot.so")
    private val loaderBin = File(nativeLibDir, "libproot_loader.so")

    fun findRootfs(): File? {
        val dirs = listOf(
            File("/storage/emulated/0/ai1"),
            File("/sdcard/ai1"),
            File("/data/data/com.prootagent.app/files/rootfs")
        )
        for (dir in dirs) {
            if (File(dir, "bin/sh").exists() || File(dir, "bin/bash").exists()) return dir
        }
        return null
    }

    fun executeCommand(command: String, rootfsPath: String): Flow<String> = flow {
        if (!prootBin.exists()) {
            emit("❌ 未找到 Proot 二进制: ${prootBin.absolutePath}"); return@flow
        }
        
        var rootfsDir = File(rootfsPath)
        
        // 自动解压逻辑
        if (!File(rootfsDir, "bin/sh").exists()) {
            val tarFile = File(rootfsDir, "ubuntu-base-22.04-base-arm64.tar.gz")
            if (tarFile.exists()) {
                emit("📦 检测到压缩包，正在解压到当前目录...")
                try {
                    GzipCompressorInputStream(BufferedInputStream(FileInputStream(tarFile))).use { gzipIn ->
                        TarArchiveInputStream(gzipIn).use { tarIn ->
                            var entry = tarIn.nextEntry
                            while (entry != null) {
                                val outFile = File(rootfsDir, entry.name)
                                if (entry.isDirectory) outFile.mkdirs()
                                else {
                                    outFile.parentFile?.mkdirs()
                                    FileOutputStream(outFile).use { fos -> tarIn.copyTo(fos) }
                                }
                                entry = tarIn.nextEntry
                            }
                        }
                    }
                    emit("✅ 解压完成！")
                } catch (e: Exception) {
                    emit("❌ 解压失败: ${e.message}"); return@flow
                }
            } else {
                emit("❌ 未找到 bin/sh，且无压缩包"); return@flow
            }
        }

        val cmd = listOf(
            prootBin.absolutePath, "--link2symlink", "-0", "-r", rootfsDir.absolutePath,
            "-b", "/sdcard", "-b", "/dev", "-b", "/proc", "-b", "/sys", "-b", "/etc/resolv.conf",
            "/usr/bin/env", "-i", "HOME=/root",
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            "/bin/sh", "-c", command
        )

        try {
            val pb = ProcessBuilder(cmd)
            pb.redirectErrorStream(true)
            
            // 注入环境变量
            val env = pb.environment()
            env["LD_LIBRARY_PATH"] = nativeLibDir
            
            // 如果存在 loader，则注入 PROOT_LOADER
            if (loaderBin.exists()) {
                env["PROOT_LOADER"] = loaderBin.absolutePath
            }
            
            val process = pb.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) { emit(line!!) }
            emit("✅ 退出码: ${process.waitFor()}")
        } catch (e: Exception) {
            emit("❌ 运行异常: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)
}
