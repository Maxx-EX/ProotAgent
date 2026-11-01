package com.prootagent.app.core
import android.app.*
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.prootagent.app.App

class AgentForegroundService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null
    override fun onCreate() {
        super.onCreate()
        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ProotAgent::WakeLock").apply { acquire(10 * 60 * 1000L) }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1001, NotificationCompat.Builder(this, App.CHANNEL_AGENT).setContentTitle("ProotAgent 守护中").setSmallIcon(android.R.drawable.stat_sys_download).setOngoing(true).build())
        return START_STICKY
    }
    override fun onDestroy() { wakeLock?.release(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
