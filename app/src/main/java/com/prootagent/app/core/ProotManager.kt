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
        
        val cmd = listOf(
            prootBin.absolutePath,
            "--link2symlink",
            "-0", // 模拟 root
            "-r", rootfsDir.absolutePath,
            "-b", "/sdcard",
            "-b", "/dev",
            "-b", "/proc",
            "-b", "/sys",
            // 关键：挂载宿主机的 DNS 配置，确保 Ubuntu 能上网
            "-b", "/etc/resolv.conf", 
            "/usr/bin/env", "-i",
            "HOME=/root",
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            "TERM=xterm-256color",
            "LANG=C.UTF-8",
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
