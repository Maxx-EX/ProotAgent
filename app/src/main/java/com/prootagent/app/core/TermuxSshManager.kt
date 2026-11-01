package com.prootagent.app.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import net.schmizz.sshj.DefaultConfig
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.Factory
import net.schmizz.sshj.transport.kex.KeyExchange
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.io.BufferedReader
import java.io.InputStreamReader

class TermuxSshManager {
    fun executeCommand(host: String, port: Int, user: String, pass: String, command: String): Flow<String> = flow {
        emit("🔗 正在连接 SSH ($host:$port)...")
        try {
            val config = DefaultConfig()
            // 复制一份可修改的算法列表，并移除 X25519 (curve25519)
            val kexFactories = ArrayList<Factory.Named<KeyExchange>>(config.keyExchangeFactories)
            kexFactories.removeIf { it.name.contains("curve25519", ignoreCase = true) }
            config.keyExchangeFactories = kexFactories

            val ssh = SSHClient(config)
            try {
                ssh.addHostKeyVerifier(PromiscuousVerifier())
                ssh.connect(host, port)
                ssh.authPassword(user, pass)
                
                emit("✅ 连接成功，执行命令...")
                val session = ssh.startSession()
                try {
                    val cmd = session.exec(command)
                    val reader = BufferedReader(InputStreamReader(cmd.inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        emit(line!!)
                    }
                    cmd.join()
                    emit("✅ 退出码: ${cmd.exitStatus}")
                } finally {
                    session.close()
                }
            } finally {
                try { ssh.disconnect() } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            emit("❌ SSH 异常: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)
}
