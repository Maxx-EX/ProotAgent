package com.prootagent.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // 修复 SSH 加密算法缺失
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.insertProviderAt(BouncyCastleProvider(), 1)
        
        // 创建前台服务通知渠道
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_AGENT,
                "Agent 运行状态",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示 Agent 任务运行进度"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_AGENT = "agent_runtime"
    }
}
