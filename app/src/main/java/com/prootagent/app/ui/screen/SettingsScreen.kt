package com.prootagent.app.ui.screen
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.prootagent.app.data.SettingsRepository
import com.prootagent.app.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    var url by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var updateSource by remember { mutableStateOf(1) }
    var themeMode by remember { mutableStateOf(0) }
    var rootfsSource by remember { mutableStateOf(0) }
    var osType by remember { mutableStateOf(0) }
    
    var sshHost by remember { mutableStateOf("127.0.0.1") }
    var sshPort by remember { mutableStateOf("8022") }
    var sshUser by remember { mutableStateOf("") }
    var sshPass by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        url = repo.apiUrl.first(); key = repo.apiKey.first(); model = repo.apiModel.first()
        updateSource = repo.updateSource.first(); themeMode = repo.themeMode.first()
        rootfsSource = repo.rootfsSource.first(); osType = repo.osType.first()
        sshHost = repo.sshHost.first(); sshPort = repo.sshPort.first().toString()
        sshUser = repo.sshUser.first(); sshPass = repo.sshPass.first()
    }

    val glass = LocalGlassColors.current

    Scaffold(
        containerColor = glass.bgStart,
        topBar = {
            TopAppBar(
                title = { Text("设置中心", color = glass.textPrimary) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, null, tint = glass.textPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = glass.bgStart)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(glass.bgStart, glass.bgEnd))).padding(padding).verticalScroll(scrollState).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            
            Text("大模型 API 配置", color = AccentBlue, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = glass.textPrimary, unfocusedTextColor = glass.textPrimary))
            OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("API Key") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = glass.textPrimary, unfocusedTextColor = glass.textPrimary))
            OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("模型 (如 gpt-4o)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = glass.textPrimary, unfocusedTextColor = glass.textPrimary))

            HorizontalDivider(color = glass.border)

            Text("Termux SSH 配置 (远程执行)", color = AccentTeal, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = sshHost, onValueChange = { sshHost = it }, label = { Text("Host (本机填 127.0.0.1)") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = glass.textPrimary, unfocusedTextColor = glass.textPrimary))
            OutlinedTextField(value = sshPort, onValueChange = { sshPort = it }, label = { Text("端口 (默认 8022)") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = glass.textPrimary, unfocusedTextColor = glass.textPrimary))
            OutlinedTextField(value = sshUser, onValueChange = { sshUser = it }, label = { Text("用户名 (Termux 输入 whoami 查看)") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = glass.textPrimary, unfocusedTextColor = glass.textPrimary))
            OutlinedTextField(value = sshPass, onValueChange = { sshPass = it }, label = { Text("密码 (Termux 输入 passwd 设置)") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = glass.textPrimary, unfocusedTextColor = glass.textPrimary))

            HorizontalDivider(color = glass.border)

            Text("外观主题", color = AccentBlue, style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(0 to "跟随系统", 1 to "深色", 2 to "浅色").forEach { (mode, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { themeMode = mode }) {
                        RadioButton(selected = themeMode == mode, onClick = { themeMode = mode }, colors = RadioButtonDefaults.colors(selectedColor = AccentBlue))
                        Text(label, color = glass.textPrimary)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { 
                    scope.launch { 
                        repo.saveConfig(url, key, model)
                        repo.saveUpdateSource(updateSource)
                        repo.saveThemeMode(themeMode)
                        repo.saveRootfsSource(rootfsSource)
                        repo.saveOsType(osType)
                        repo.saveSshConfig(sshHost, sshPort.toIntOrNull() ?: 8022, sshUser, sshPass)
                        navController.popBackStack() 
                    } 
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(12.dp)
            ) { Text("保存并应用", color = glass.bgStart) }
        }
    }
}
