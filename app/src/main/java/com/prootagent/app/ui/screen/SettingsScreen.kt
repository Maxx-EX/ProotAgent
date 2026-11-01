package com.prootagent.app.ui.screen
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.sp
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
    var downloadUri by remember { mutableStateOf("") }
    var rootfsSource by remember { mutableStateOf(0) }
    var osType by remember { mutableStateOf(0) }

    val dirLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            downloadUri = it.toString()
        }
    }

    LaunchedEffect(Unit) {
        url = repo.apiUrl.first(); key = repo.apiKey.first(); model = repo.apiModel.first()
        updateSource = repo.updateSource.first(); themeMode = repo.themeMode.first()
        downloadUri = repo.downloadUri.first(); rootfsSource = repo.rootfsSource.first()
        osType = repo.osType.first()
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

            Text("外观主题", color = AccentBlue, style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(0 to "跟随系统", 1 to "深色", 2 to "浅色").forEach { (mode, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { themeMode = mode }) {
                        RadioButton(selected = themeMode == mode, onClick = { themeMode = mode }, colors = RadioButtonDefaults.colors(selectedColor = AccentBlue))
                        Text(label, color = glass.textPrimary)
                    }
                }
            }

            HorizontalDivider(color = glass.border)

            Text("工作空间目录", color = AccentBlue, style = MaterialTheme.typography.titleMedium)
            Text(if (downloadUri.isEmpty()) "当前使用私有目录" else "已自定义: $downloadUri", color = glass.textSecondary, fontSize = 12.sp, maxLines = 1)
            Button(onClick = { dirLauncher.launch(null) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)) {
                Text("选择外部存储目录 (SAF)", color = glass.bgStart)
            }

            HorizontalDivider(color = glass.border)

            Text("Linux 容器类型", color = AccentBlue, style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(0 to "Alpine (轻量)", 1 to "Ubuntu (完整)").forEach { (type, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { osType = type }) {
                        RadioButton(selected = osType == type, onClick = { osType = type }, colors = RadioButtonDefaults.colors(selectedColor = AccentTeal))
                        Text(label, color = glass.textPrimary)
                    }
                }
            }

            Text("镜像源设置", color = AccentBlue, style = MaterialTheme.typography.titleMedium)
            listOf(0 to "官方源 (海外)", 1 to "清华大学镜像 (TUNA)", 2 to "阿里云镜像").forEach { (src, label) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { rootfsSource = src }) {
                    RadioButton(selected = rootfsSource == src, onClick = { rootfsSource = src }, colors = RadioButtonDefaults.colors(selectedColor = AccentTeal))
                    Text(label, color = glass.textPrimary)
                }
            }

            HorizontalDivider(color = glass.border)

            Text("软件更新源", color = AccentBlue, style = MaterialTheme.typography.titleMedium)
            listOf(0 to "GitHub 官方", 1 to "国内加速镜像").forEach { (src, label) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { updateSource = src }) {
                    RadioButton(selected = updateSource == src, onClick = { updateSource = src }, colors = RadioButtonDefaults.colors(selectedColor = AccentBlue))
                    Text(label, color = glass.textPrimary)
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { 
                    scope.launch { 
                        repo.saveConfig(url, key, model)
                        repo.saveUpdateSource(updateSource)
                        repo.saveThemeMode(themeMode)
                        repo.saveDownloadUri(downloadUri)
                        repo.saveRootfsSource(rootfsSource)
                        repo.saveOsType(osType)
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
