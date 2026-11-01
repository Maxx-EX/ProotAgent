package com.prootagent.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prootagent.app.ui.theme.Green
import com.prootagent.app.ui.theme.Navy700
import com.prootagent.app.ui.theme.Navy800
import com.prootagent.app.ui.theme.Navy900
import com.prootagent.app.ui.theme.Pink
import com.prootagent.app.ui.theme.SkyBlue
import com.prootagent.app.ui.theme.TextPrimary
import com.prootagent.app.ui.theme.TextSecondary

data class ProgressStep(
    val title: String,
    val status: String,
    val summary: String,
    val details: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var input by remember { mutableStateOf("") }

    val demoSteps = remember {
        listOf(
            ProgressStep(
                title = "解析指令",
                status = "done",
                summary = "识别为「下载 Python 并运行示例脚本」",
                details = listOf(
                    "intent: install_pkg + run_script",
                    "target: /root/workspace/demo.py",
                    "language: python3"
                )
            ),
            ProgressStep(
                title = "准备 Proot 环境",
                status = "running",
                summary = "挂载 rootfs，检查 /usr/bin/python3",
                details = listOf(
                    "proot --link2symlink -0 -r rootfs -b /sdcard:/host",
                    "which python3 → /usr/bin/python3",
                    "pip install requests (缓存命中)"
                )
            ),
            ProgressStep(
                title = "等待下一步指令",
                status = "idle",
                summary = "环境已就绪，可继续下发任务",
                details = listOf(
                    "rootfs: debian-bookworm-arm64",
                    "workspace: /root/workspace"
                )
            )
        )
    }

    Scaffold(
        containerColor = Navy900,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ProotAgent", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                        Text("Natural Language → Linux", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy900)
            )
        },
        bottomBar = {
            Surface(color = Navy800, tonalElevation = 8.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("告诉 Agent 你想做什么…", color = TextSecondary) },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SkyBlue,
                            unfocusedBorderColor = Navy700,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = SkyBlue
                        ),
                        maxLines = 3
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = { input = "" },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = SkyBlue,
                            contentColor = Navy900
                        )
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "发送")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item { WelcomeCard() }
            item {
                Text(
                    "执行进度",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            items(demoSteps) { step -> ProgressCard(step) }
        }
    }
}

@Composable
private fun WelcomeCard() {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Navy800)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Navy800, Navy700)))
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Terminal, contentDescription = null, tint = SkyBlue)
                    Spacer(Modifier.width(8.dp))
                    Text("准备就绪", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "用自然语言下指令，Agent 会在 Proot Linux 容器里执行，产物写入你指定的目录。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill("API: 未配置", Pink)
                    StatusPill("Rootfs: 待下载", SkyBlue)
                }
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.15f)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun ProgressCard(step: ProgressStep) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "arrow")

    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Navy800)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(step.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(Modifier.width(8.dp))
                        StatusPill(
                            when (step.status) {
                                "running" -> "运行中"
                                "done" -> "完成"
                                else -> "待命"
                            },
                            when (step.status) {
                                "running" -> SkyBlue
                                "done" -> Green
                                else -> TextSecondary
                            }
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(step.summary, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        Icons.Filled.ExpandMore,
                        contentDescription = "展开",
                        tint = TextSecondary,
                        modifier = Modifier.rotate(rotation)
                    )
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = Navy700)
                    Spacer(Modifier.height(12.dp))
                    Text("详情", style = MaterialTheme.typography.labelSmall, color = SkyBlue)
                    Spacer(Modifier.height(6.dp))
                    step.details.forEach { line ->
                        Text(
                            "› $line",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            ),
                            color = TextPrimary,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
