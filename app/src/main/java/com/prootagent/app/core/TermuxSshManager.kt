package com.prootagent.app.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.IOUtils
import net.schmizz.sshj.connection.channel.direct.Session

class TermuxSshManager {
    // 执行 SSH 命令
    fun executeCommand(host: String, port: Int, user: String, pass: String, command: String): Flow<String> = flow {
        emit("🔗 正在连接到 Termux SSH ($host:$port)...")
        val ssh = SSHClient()
        try {
            ssh.addHostKeyVerifier { _, _, _ -> true } // 忽略指纹校验
            ssh.connect(host, port)
            ssh.authPassword(user, pass)
            emit("✅ 连接成功，执行命令...")
            
            val session: Session = ssh.startSession()
            val cmd = session.exec(command)
            
            // 实时读取输出
            val reader = IOUtils.reader(cmd.inputStream)
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                emit(line!!)
            }
            cmd.join()
            emit("✅ 执行结束，退出码: ${cmd.exitStatus}")
        } catch (e: Exception) {
            emit("❌ SSH 执行异常: ${e.message}")
        } finally {
            try { ssh.disconnect() } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)
}
