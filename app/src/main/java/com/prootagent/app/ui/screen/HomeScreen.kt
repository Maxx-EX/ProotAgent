package com.prootagent.app.ui.screen
import android.widget.Toast
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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
    val glass = LocalGlassColors.current
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(state.messages.size) { if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { if (state.workMode == 0) navController.navigate("env") }) {
                        Icon(Icons.Filled.Computer, null, tint = if (state.isEnvReady) AccentTeal else Color.Red, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (state.workMode == 1) "SSH 模式" else if (state.isEnvReady) "环境已就绪" else "未准备环境", color = if (state.isEnvReady) AccentTeal else Color.Red, fontSize = 14.sp)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.saveCurrentChat() }) { Icon(Icons.Filled.Save, null, tint = glass.textPrimary) }
                    IconButton(onClick = { navController.navigate("history") }) { Icon(Icons.Filled.History, null, tint = glass.textPrimary) }
                    IconButton(onClick = { navController.navigate("settings") }) { Icon(Icons.Filled.Settings, null, tint = glass.textPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = glass.bgStart.copy(alpha = 0.95f))
            )
        },
        bottomBar = {
            Column(Modifier.fillMaxWidth().background(glass.bgStart.copy(alpha = 0.95f))) {
                // 工作模式切换胶囊
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.Center) {
                    Text(
                        text = if (state.workMode == 0) "当前: 内置 Proot (点击切换)" else "当前: Termux SSH (点击切换)",
                        color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(glass.glassBg, RoundedCornerShape(50))
                            .clickable { viewModel.toggleWorkMode(if (state.workMode == 0) 1 else 0) }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.inputText, onValueChange = viewModel::updateInput,
                        enabled = !state.isGenerating,
                        modifier = Modifier.weight(1f), placeholder = { Text("输入指令…", color = glass.textSecondary, fontSize = 14.sp) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentBlue, unfocusedBorderColor = glass.border, focusedContainerColor = glass.glassBg, unfocusedContainerColor = glass.glassBg, focusedTextColor = glass.textPrimary, unfocusedTextColor = glass.textPrimary), maxLines = 3
                    )
                    Spacer(Modifier.width(8.dp))
                    
                    if (state.isGenerating) {
                        IconButton(onClick = viewModel::stopGeneration, modifier = Modifier.background(Color.Red, RoundedCornerShape(50)).size(44.dp)) {
                            Icon(Icons.Filled.Stop, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        IconButton(
                            onClick = { 
                                if (!state.isEnvReady && state.workMode == 0) { Toast.makeText(context, "请先准备环境", Toast.LENGTH_SHORT).show(); navController.navigate("env") }
                                else if (state.inputText.isNotBlank()) viewModel.sendMessage()
                            },
                            modifier = Modifier.background(AccentBlue, RoundedCornerShape(50)).size(44.dp)
                        ) { Icon(Icons.Filled.Send, null, tint = glass.bgStart, modifier = Modifier.size(20.dp)) }
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(glass.bgStart, glass.bgEnd))).padding(padding)) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
                items(state.messages, key = { it.id }) { msg ->
                    when (msg.type) {
                        ChatItem.Type.USER -> UserBubble(msg.text) { clipboard.setText(AnnotatedString(msg.text)) }
                        ChatItem.Type.AI -> AiBubble(msg.text) { clipboard.setText(AnnotatedString(msg.text)) }
                        ChatItem.Type.PROGRESS -> ProgressCard(msg)
                    }
                }
            }
        }
    }
}

@Composable
fun UserBubble(text: String, onCopy: () -> Unit) {
    val glass = LocalGlassColors.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Box(Modifier.widthIn(max = 280.dp).background(AccentBlue, RoundedCornerShape(16.dp, 2.dp, 16.dp, 16.dp)).clickable { onCopy() }.padding(12.dp)) {
            Text(text, color = glass.bgStart, fontSize = 14.sp)
        }
    }
}

@Composable
fun AiBubble(text: String, onCopy: () -> Unit) {
    val glass = LocalGlassColors.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(Modifier.widthIn(max = 320.dp).liquidGlass().clickable { onCopy() }.padding(12.dp)) {
            val parts = text.split("```")
            Column {
                parts.forEachIndexed { index, part ->
                    if (index % 2 == 1) {
                        Box(Modifier.fillMaxWidth().background(Color(0xFF1E1E2E), RoundedCornerShape(8.dp)).padding(8.dp)) {
                            Text(part.trim('\n'), color = Color(0xFFA6E3A1), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        }
                    } else {
                        if (part.isNotBlank()) Text(part, color = glass.textPrimary, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressCard(msg: ChatItem) {
    val glass = LocalGlassColors.current
    var expanded by remember { mutableStateOf(msg.isExpanded) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "arrow")
    Box(Modifier.fillMaxWidth().liquidGlass().clickable { expanded = !expanded }.padding(12.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⚙️ ${msg.text}", color = AccentTeal, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Icon(Icons.Filled.ExpandMore, null, tint = glass.textSecondary, modifier = Modifier.rotate(rotation).size(20.dp))
            }
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(color = glass.border)
                    Spacer(Modifier.height(8.dp))
                    msg.details.forEach { Text("> $it", color = AccentBlue, fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.padding(vertical = 1.dp)) }
                }
            }
        }
    }
}
