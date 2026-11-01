package com.prootagent.app.core

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class ProotManager(private val nativeLibDir: String) {
    private val prootBin = File(nativeLibDir, "libproot.so")

    fun executeCommand(command: String, rootfsPath: String): Flow<String> = flow {
        if (!prootBin.exists()) {
            emit("❌ 未找到 Proot 二进制: ${prootBin.absolutePath}")
            return@flow
        }
        val rootfsDir = File(rootfsPath)
        if (!rootfsDir.exists()) {
            emit("❌ 未找到 Rootfs 目录: $rootfsPath")
            return@flow
        }

        val cmd = listOf(
            prootBin.absolutePath,
            "--link2symlink", "-0",
            "-r", rootfsDir.absolutePath,
            "-b", "/sdcard", "-b", "/dev", "-b", "/proc", "-b", "/sys",
            "-b", "/etc/resolv.conf", // DNS 挂载
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
}
