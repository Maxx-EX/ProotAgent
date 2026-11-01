package com.prootagent.app.ui.screen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.prootagent.app.ui.AgentViewModel
import com.prootagent.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvironmentScreen(navController: NavController, viewModel: AgentViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val glass = LocalGlassColors.current

    Scaffold(
        containerColor = glass.bgStart,
        topBar = {
            TopAppBar(
                title = { Text("工作环境管理", color = glass.textPrimary) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, null, tint = glass.textPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = glass.bgStart)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(glass.bgStart, glass.bgEnd))).padding(padding).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.fillMaxWidth().liquidGlass().padding(20.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (state.currentOsType == 1) "Ubuntu 22.04 LTS" else "Alpine Linux",
                        color = glass.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(state.envStatus, color = if (state.isEnvReady) AccentTeal else glass.textSecondary)
                    if (!state.isEnvReady && state.envProgress in 1..99) {
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(progress = { state.envProgress / 100f }, modifier = Modifier.fillMaxWidth(), color = AccentBlue)
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { if (!state.isEnvReady) viewModel.prepareEnvironment() },
                        enabled = !state.isEnvReady && state.envProgress == 0,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (state.isEnvReady) glass.glassBg else AccentBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(if (state.isEnvReady) "环境已就绪" else "一键准备 Rootfs") }
                }
            }
            
            if (state.isEnvReady) {
                Box(Modifier.fillMaxWidth().liquidGlass().padding(20.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("开发组件", color = AccentBlue, fontWeight = FontWeight.Bold)
                        Text(
                            if (state.currentOsType == 1) "安装 Python, Git 等 (apt)" else "安装 Python, Git 等 (apk)",
                            color = glass.textSecondary, fontSize = 12.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.installDeps() },
                            enabled = !state.isGenerating,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                        ) { Text(if (state.isGenerating) "正在安装中..." else "一键安装运行环境") }
                    }
                }
            }
        }
    }
}
