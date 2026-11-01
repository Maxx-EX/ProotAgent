package com.prootagent.app.ui
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.prootagent.app.core.AgentForegroundService
import com.prootagent.app.data.SettingsRepository
import com.prootagent.app.ui.screen.*
import com.prootagent.app.ui.theme.ProotAgentTheme
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ -> startKeepAliveService() }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        checkPermissions()
        setContent {
            val context = this
            val repo = remember { SettingsRepository(context) }
            var themeMode by remember { mutableStateOf(0) }
            LaunchedEffect(Unit) { themeMode = repo.themeMode.first() }
            
            ProotAgentTheme(themeMode = themeMode) {
                AppNav()
            }
        }
    }

    private fun checkPermissions() {
        // 核心：强制申请所有文件管理权限，绕过分区存储
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }

        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (permissions.isNotEmpty()) requestPermissionLauncher.launch(permissions.toTypedArray()) else startKeepAliveService()
    }

    private fun startKeepAliveService() {
        ContextCompat.startForegroundService(this, Intent(this, AgentForegroundService::class.java))
    }
}

@Composable
fun AppNav() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("settings") { SettingsScreen(navController) }
        composable("env") { EnvironmentScreen(navController) }
        composable("history") { HistoryScreen(navController) }
    }
}
