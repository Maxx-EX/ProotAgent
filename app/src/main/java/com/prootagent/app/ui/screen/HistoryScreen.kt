package com.prootagent.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.prootagent.app.ui.AgentViewModel
import com.prootagent.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController, viewModel: AgentViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val glass = LocalGlassColors.current
    LaunchedEffect(Unit) { viewModel.loadHistory() }

    Scaffold(
        containerColor = glass.bgStart,
        topBar = {
            TopAppBar(
                title = { Text("对话记录", color = glass.textPrimary) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, null, tint = glass.textPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = glass.bgStart)
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(glass.bgStart, glass.bgEnd))).padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.historyList) { record ->
                Row(
                    Modifier.fillMaxWidth().liquidGlass().clickable { viewModel.loadChat(record); navController.popBackStack() }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(record.title, color = glass.textPrimary, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.deleteHistory(record.id) }) {
                        Icon(Icons.Filled.Delete, null, tint = AccentPurple)
                    }
                }
            }
        }
    }
}
