package com.prootagent.app.ui.screen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    var url by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var updateSource by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        url = repo.apiUrl.first()
        key = repo.apiKey.first()
        model = repo.apiModel.first()
        updateSource = repo.updateSource.first()
    }

    Scaffold(
        containerColor = LiquidBgStart,
        topBar = {
            TopAppBar(
                title = { Text("API 配置", color = TextPrimary) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = TextPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LiquidBgStart)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(LiquidBgStart, LiquidBgEnd))).padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("API Base URL") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentBlue, unfocusedBorderColor = LiquidBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedLabelColor = AccentBlue, unfocusedLabelColor = TextSecondary))
            OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("API Key") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentBlue, unfocusedBorderColor = LiquidBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedLabelColor = AccentBlue, unfocusedLabelColor = TextSecondary))
            OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("模型名称 (e.g. gpt-4o)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentBlue, unfocusedBorderColor = LiquidBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedLabelColor = AccentBlue, unfocusedLabelColor = TextSecondary))
            
            HorizontalDivider(color = LiquidBorder, modifier = Modifier.padding(vertical = 8.dp))
            
            Text("更新源设置", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = updateSource == 0, onClick = { updateSource = 0 }, colors = RadioButtonDefaults.colors(selectedColor = AccentBlue))
                Text("Github 官方 (推荐海外)", color = TextPrimary, modifier = Modifier.padding(start = 8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = updateSource == 1, onClick = { updateSource = 1 }, colors = RadioButtonDefaults.colors(selectedColor = AccentBlue))
                Text("国内加速镜像 (ghproxy)", color = TextPrimary, modifier = Modifier.padding(start = 8.dp))
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { 
                    scope.launch { 
                        repo.saveConfig(url, key, model)
                        repo.saveUpdateSource(updateSource)
                        navController.popBackStack() 
                    } 
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = LiquidBgStart),
                shape = RoundedCornerShape(12.dp)
            ) { Text("保存配置", style = MaterialTheme.typography.titleMedium) }
        }
    }
}
