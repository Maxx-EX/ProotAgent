package com.prootagent.app.ui
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prootagent.app.BuildConfig
import com.prootagent.app.core.*
import com.prootagent.app.data.*
import com.prootagent.app.mcp.McpServer
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChatItem(
    val id: Long = System.currentTimeMillis(), val type: Type, val text: String = "",
    val isExpanded: Boolean = false, val details: MutableList<String> = mutableListOf()
) { enum class Type { USER, AI, PROGRESS } }

data class UiState(
    val messages: List<ChatItem> = emptyList(), val isGenerating: Boolean = false,
    val inputText: String = "", 
    val updateInfo: AppUpdateInfo? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0
)

class AgentViewModel(app: Application) : AndroidViewModel(app) {
    private val settingsRepo = SettingsRepository(app)
    private val prootManager = ProotManager(app.applicationInfo.nativeLibraryDir, app.filesDir)
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

    fun checkUpdate() {
        viewModelScope.launch {
            val source = settingsRepo.updateSource.first()
            val info = UpdateManager.checkUpdate(BuildConfig.VERSION_NAME, source)
            if (info.hasUpdate) _uiState.update { it.copy(updateInfo = info) }
        }
    }

    fun dismissUpdate() = _uiState.update { it.copy(updateInfo = null) }
    
    fun startDownload(context: android.content.Context) {
        val info = _uiState.value.updateInfo ?: return
        _uiState.update { it.copy(isDownloading = true, downloadProgress = 0) }
        viewModelScope.launch {
            val file = ApkDownloader.downloadApk(context, info.downloadUrl) { progress ->
                _uiState.update { it.copy(downloadProgress = progress) }
            }
            _uiState.update { it.copy(isDownloading = false) }
            if (file != null) {
                ApkDownloader.installApk(context, file)
            }
        }
    }

    fun updateInput(text: String) = _uiState.update { it.copy(inputText = text) }

    fun sendMessage() {
        val prompt = _uiState.value.inputText.trim()
        if (prompt.isEmpty() || _uiState.value.isGenerating) return
        val userMsg = ChatItem(type = ChatItem.Type.USER, text = prompt)
        val progressId = System.currentTimeMillis() + 1
        _uiState.update { it.copy(messages = it.messages + userMsg + ChatItem(id = progressId, type = ChatItem.Type.PROGRESS, text = "正在思考…"), inputText = "", isGenerating = true) }

        viewModelScope.launch {
            if (!rootfsManager.isInstalled()) {
                rootfsManager.downloadAndExtract().collect { (p, msg) -> updateProgressMsg(progressId, "准备环境", msg) }
            }
            agentRepo.runAgent(prompt).collect { event ->
                when (event) {
                    is AgentEvent.Text -> _uiState.update { state -> state.copy(messages = state.messages + ChatItem(type = ChatItem.Type.AI, text = event.text)) }
                    is AgentEvent.Progress -> updateProgressMsg(progressId, event.summary, event.log)
                    is AgentEvent.Error -> updateProgressMsg(progressId, "发生错误", event.message)
                }
            }
            _uiState.update { it.copy(isGenerating = false) }
        }
    }

    private fun updateProgressMsg(id: Long, summary: String, log: String) {
        _uiState.update { state ->
            val updated = state.messages.map { msg ->
                if (msg.id == id) msg.copy(text = summary, details = msg.details.toMutableList().apply { if (log.isNotEmpty()) add(log); if (size > 50) removeAt(0) })
                else msg
            }
            state.copy(messages = updated)
        }
    }
}
