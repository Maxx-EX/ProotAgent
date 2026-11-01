package com.prootagent.app.core
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class ProotManager(private val nativeLibDir: String, private val filesDir: File) {
    private val prootBin = File(nativeLibDir, "libproot.so")
    private val rootfsDir = File(filesDir, "rootfs")

    fun executeCommand(command: String): Flow<String> = flow {
        if (!prootBin.exists()) {
            emit("❌ 未找到 Proot 二进制: ${prootBin.absolutePath}")
            return@flow
        }
        emit("⚙️ 启动 Proot...")
        val cmd = listOf(prootBin.absolutePath, "--link2symlink", "-0", "-r", rootfsDir.absolutePath, "-b", "/sdcard", "-b", "/dev", "-b", "/proc", "-b", "/sys", "/usr/bin/env", "-i", "HOME=/root", "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin", "/bin/sh", "-c", command)
        try {
            val pb = ProcessBuilder(cmd)
            pb.redirectErrorStream(true)
            val process = pb.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) { emit(line!!) }
            emit("✅ 退出码: ${process.waitFor()}")
        } catch (e: Exception) { emit("❌ 异常: ${e.message}") }
    }.flowOn(Dispatchers.IO)
}
