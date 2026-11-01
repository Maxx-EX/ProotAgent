package com.prootagent.app.ui.screen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.prootagent.app.ui.AgentViewModel
import com.prootagent.app.ui.ChatItem
import com.prootagent.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: AgentViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    // 修复智能转换问题：使用局部变量接收
    val updateInfo = state.updateInfo
    if (updateInfo != null) {
        AlertDialog(
            onDismissRequest = { if (!state.isDownloading) viewModel.dismissUpdate() },
            title = { Text("发现新版本 ${updateInfo.versionName}") },
            text = {
                Column {
                    Text(updateInfo.changelog, color = TextSecondary, fontSize = 12.sp, maxLines = 5)
                    if (state.isDownloading) {
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(progress = { state.downloadProgress / 100f }, modifier = Modifier.fillMaxWidth(), color = AccentBlue)
                        Spacer(Modifier.height(8.dp))
                        Text("正在下载: ${state.downloadProgress}%", color = AccentBlue)
                    }
                }
            },
            confirmButton = {
                if (!state.isDownloading) {
                    TextButton(onClick = { viewModel.startDownload(context) }) { Text("立即更新", color = AccentBlue) }
                }
            },
            dismissButton = {
                if (!state.isDownloading) {
                    TextButton(onClick = viewModel::dismissUpdate) { Text("稍后", color = TextSecondary) }
                }
            },
            containerColor = LiquidBgStart
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("ProotAgent", color = TextPrimary, fontWeight = FontWeight.Bold) },
                actions = { IconButton(onClick = { navController.navigate("settings") }) { Icon(Icons.Filled.Settings, contentDescription = "设置", tint = TextPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LiquidBgStart.copy(alpha = 0.8f))
            )
        },
        bottomBar = {
            Row(Modifier.fillMaxWidth().background(LiquidBgStart.copy(alpha = 0.9f)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.inputText, onValueChange = viewModel::updateInput,
                    modifier = Modifier.weight(1f), placeholder = { Text("输入指令执行…", color = TextSecondary) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue, unfocusedBorderColor = LiquidBorder,
                        focusedContainerColor = LiquidGlassBg, unfocusedContainerColor = LiquidGlassBg,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    ), maxLines = 3
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = viewModel::sendMessage, enabled = !state.isGenerating && state.inputText.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = AccentBlue, contentColor = LiquidBgStart)
                ) { Icon(Icons.Filled.Send, contentDescription = "发送") }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(LiquidBgStart, LiquidBgEnd))).padding(padding)) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
                items(state.messages, key = { it.id }) { msg ->
                    when (msg.type) {
                        ChatItem.Type.USER -> UserBubble(msg.text)
                        ChatItem.Type.AI -> AiBubble(msg.text)
                        ChatItem.Type.PROGRESS -> ProgressCard(msg)
                    }
                }
            }
        }
    }
}

@Composable
fun UserBubble(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Box(Modifier.widthIn(max = 280.dp).background(AccentBlue, RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)).padding(12.dp)) {
            Text(text, color = LiquidBgStart, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun AiBubble(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(Modifier.widthIn(max = 300.dp).liquidGlass().padding(12.dp)) {
            Text(text, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ProgressCard(msg: ChatItem) {
    var expanded by remember { mutableStateOf(msg.isExpanded) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "arrow")
    Box(Modifier.fillMaxWidth().liquidGlass().clickable { expanded = !expanded }.padding(12.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⚙️ ${msg.text}", color = AccentTeal, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = TextSecondary, modifier = Modifier.rotate(rotation))
            }
            Spacer(Modifier.height(4.dp))
            Text("共 ${msg.details.size} 条执行日志，点击查看详情", color = TextSecondary, fontSize = 12.sp)
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(color = LiquidBorder)
                    Spacer(Modifier.height(8.dp))
                    msg.details.forEach { Text("> $it", color = AccentBlue, fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp)) }
                }
            }
        }
    }
}
