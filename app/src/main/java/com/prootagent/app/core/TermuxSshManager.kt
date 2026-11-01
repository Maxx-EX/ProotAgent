package com.prootagent.app.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.io.BufferedReader
import java.io.InputStreamReader

class TermuxSshManager {
    fun executeCommand(host: String, port: Int, user: String, pass: String, command: String): Flow<String> = flow {
        emit("🔗 正在连接 SSH ($host:$port)...")
        val ssh = SSHClient()
        try {
            // 使用官方提供的 PromiscuousVerifier 忽略指纹校验
            ssh.addHostKeyVerifier(PromiscuousVerifier())
            ssh.connect(host, port)
            try {
                ssh.authPassword(user, pass)
            } catch (e: Exception) {
                emit("❌ 认证失败，请检查用户名和密码")
                return@flow
            }
            
            emit("✅ 连接成功，执行命令...")
            val session = ssh.startSession()
            try {
                val cmd = session.exec(command)
                val reader = BufferedReader(InputStreamReader(cmd.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) { emit(line!!) }
                cmd.join()
                emit("✅ 退出码: ${cmd.exitStatus}")
            } catch (e: Exception) {
                emit("❌ 执行命令异常: ${e.message}")
            } finally {
                session.close()
            }
        } catch (e: Exception) {
            emit("❌ SSH 连接异常: ${e.message}")
        } finally {
            try { ssh.disconnect() } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)
}
