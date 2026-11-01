package com.prootagent.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prootagent.app.BuildConfig
import com.prootagent.app.core.ProotManager
import com.prootagent.app.core.RootfsManager
import com.prootagent.app.core.UpdateManager
import com.prootagent.app.data.AgentEvent
import com.prootagent.app.data.AgentRepository
import com.prootagent.app.data.SettingsRepository
import com.prootagent.app.mcp.McpServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatItem(
    val id: Long = System.currentTimeMillis(),
    val type: Type,
    val text: String = "",
    val isExpanded: Boolean = false,
    val details: MutableList<String> = mutableListOf()
) {
    enum class Type { USER, AI, PROGRESS }
}

data class UiState(
    val messages: List<ChatItem> = emptyList(),
    val isGenerating: Boolean = false,
    val inputText: String = "",
    val updateMessage: String? = null
)

class AgentViewModel(app: Application) : AndroidViewModel(app) {
    private val settingsRepo = SettingsRepository(app)
    private val prootManager = ProotManager(app.nativeLibraryDir, app.filesDir)
    private val rootfsManager = RootfsManager(app.filesDir)
    private val agentRepo = AgentRepository(settingsRepo, prootManager)
    private val mcpServer = McpServer(8080, prootManager)

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        mcpServer.start()
        checkUpdate()
        _uiState.update { it.copy(messages = listOf(ChatItem(type = ChatItem.Type.AI, text = "你好！我是 ProotAgent，MAXX-EX 出品。\n请先在设置中配置 API，然后发送指令。"))) }
    }

    private fun checkUpdate() {
        viewModelScope.launch {
            val (hasUpdate, msg) = UpdateManager.checkUpdate(BuildConfig.VERSION_NAME)
            if (hasUpdate) _uiState.update { it.copy(updateMessage = msg) }
        }
    }

    fun dismissUpdate() {
        _uiState.update { it.copy(updateMessage = null) }
    }

    fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val prompt = _uiState.value.inputText.trim()
        if (prompt.isEmpty() || _uiState.value.isGenerating) return

        val userMsg = ChatItem(type = ChatItem.Type.USER, text = prompt)
        val progressId = System.currentTimeMillis() + 1
        val progressMsg = ChatItem(id = progressId, type = ChatItem.Type.PROGRESS, text = "正在思考…")
        
        _uiState.update { 
            it.copy(messages = it.messages + userMsg + progressMsg, inputText = "", isGenerating = true) 
        }

        viewModelScope.launch {
            // 检查并下载 Rootfs
            if (!rootfsManager.isInstalled()) {
                rootfsManager.downloadAndExtract().collect { (progress, msg) ->
                    updateProgressMsg(progressId, "准备环境", msg)
                }
            }

            agentRepo.runAgent(prompt).collect { event ->
                when (event) {
                    is AgentEvent.Text -> {
                        // 完成后把进度卡片替换为最终文本（或者保留进度卡片，这里选择追加 AI 文本）
                        _uiState.update { state ->
                            val newMsg = ChatItem(type = ChatItem.Type.AI, text = event.text)
                            state.copy(messages = state.messages + newMsg)
                        }
                    }
                    is AgentEvent.Progress -> {
                        updateProgressMsg(progressId, event.summary, event.log)
                    }
                    is AgentEvent.Error -> {
                        updateProgressMsg(progressId, "发生错误", event.message)
                    }
                }
            }
            _uiState.update { it.copy(isGenerating = false) }
        }
    }

    private fun updateProgressMsg(id: Long, summary: String, log: String) {
        _uiState.update { state ->
            val updated = state.messages.map { msg ->
                if (msg.id == id) {
                    val newDetails = msg.details.toMutableList().apply { 
                        if (log.isNotEmpty()) add(log)
                        if (size > 50) removeAt(0) // 限制日志长度防止 OOM
                    }
                    msg.copy(text = summary, details = newDetails)
                } else msg
            }
            state.copy(messages = updated)
        }
    }
}
