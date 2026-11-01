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
import org.json.JSONArray
import org.json.JSONObject

data class ChatItem(
    val id: Long = System.currentTimeMillis(), val type: Type, val text: String = "",
    val isExpanded: Boolean = false, val details: MutableList<String> = mutableListOf()
) { enum class Type { USER, AI, PROGRESS } }

data class UiState(
    val messages: List<ChatItem> = emptyList(), val isGenerating: Boolean = false,
    val inputText: String = "", 
    val updateInfo: AppUpdateInfo? = null, val isDownloading: Boolean = false, val downloadProgress: Int = 0,
    val isEnvReady: Boolean = false, val envProgress: Int = 0, val envStatus: String = "未准备",
    val historyList: List<HistoryRecord> = emptyList()
)

class AgentViewModel(app: Application) : AndroidViewModel(app) {
    private val settingsRepo = SettingsRepository(app)
    private val prootManager = ProotManager(app.applicationInfo.nativeLibraryDir, app.filesDir)
    private val rootfsManager = RootfsManager(app.filesDir)
    private val historyRepo = ChatHistoryRepository(app)
    private val agentRepo = AgentRepository(settingsRepo, prootManager)
    private val mcpServer = McpServer(8080, prootManager)

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        mcpServer.start()
        checkUpdate()
        checkEnv()
        loadHistory()
        _uiState.update { it.copy(messages = listOf(ChatItem(type = ChatItem.Type.AI, text = "你好！我是 AndroidLinuxAI 容器调度 Agent。请先点击顶部进入环境管理准备 Linux 容器，然后输入指令。"))) }
    }

    fun checkEnv() {
        val installed = rootfsManager.isInstalled()
        _uiState.update { it.copy(isEnvReady = installed, envStatus = if (installed) "环境已就绪" else "未准备") }
    }

    fun prepareEnvironment() {
        viewModelScope.launch {
            _uiState.update { it.copy(isEnvReady = false, envStatus = "正在准备...") }
            rootfsManager.prepareEnvironment().collect { (p, msg) ->
                _uiState.update { it.copy(envProgress = p, envStatus = msg) }
            }
            checkEnv()
        }
    }

    fun loadHistory() {
        viewModelScope.launch { _uiState.update { it.copy(historyList = historyRepo.loadAll()) } }
    }

    fun saveCurrentChat() {
        if (_uiState.value.messages.isEmpty()) return
        val jsonArray = JSONArray()
        _uiState.value.messages.forEach {
            jsonArray.put(JSONObject().apply { put("type", it.type.name); put("text", it.text) })
        }
        val title = _uiState.value.messages.firstOrNull { it.type == ChatItem.Type.USER }?.text?.take(15) ?: "新对话"
        viewModelScope.launch { historyRepo.saveChat(System.currentTimeMillis(), title, jsonArray.toString()); loadHistory() }
    }

    fun loadChat(record: HistoryRecord) {
        try {
            val arr = JSONArray(record.content)
            val msgs = mutableListOf<ChatItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                msgs.add(ChatItem(type = ChatItem.Type.valueOf(obj.getString("type")), text = obj.getString("text")))
            }
            _uiState.update { it.copy(messages = msgs) }
        } catch (_: Exception) {}
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch { historyRepo.deleteChat(id); loadHistory() }
    }

    fun checkUpdate() {
        viewModelScope.launch {
            val source = settingsRepo.updateSource.first()
            val info = UpdateManager.checkUpdate(BuildConfig.VERSION_NAME, source)
            if (info.hasUpdate) _uiState.update { it.copy(updateInfo = info) }
        }
    }

    fun dismissUpdate() = _uiState.update { it.copy(updateInfo = null) }

    fun updateInput(text: String) = _uiState.update { it.copy(inputText = text) }

    fun sendMessage() {
        if (!_uiState.value.isEnvReady) return
        val prompt = _uiState.value.inputText.trim()
        if (prompt.isEmpty() || _uiState.value.isGenerating) return
        val userMsg = ChatItem(type = ChatItem.Type.USER, text = prompt)
        val progressId = System.currentTimeMillis() + 1
        _uiState.update { it.copy(messages = it.messages + userMsg + ChatItem(id = progressId, type = ChatItem.Type.PROGRESS, text = "正在思考…"), inputText = "", isGenerating = true) }

        viewModelScope.launch {
            agentRepo.runAgent(prompt).collect { event ->
                when (event) {
                    is AgentEvent.Text -> _uiState.update { state -> state.copy(messages = state.messages + ChatItem(type = ChatItem.Type.AI, text = event.text)) }
                    is AgentEvent.Progress -> updateProgressMsg(progressId, event.summary, event.log)
                    is AgentEvent.Error -> updateProgressMsg(progressId, "发生错误", event.message)
                }
            }
            _uiState.update { it.copy(isGenerating = false) }
            saveCurrentChat()
        }
    }

    private fun updateProgressMsg(id: Long, summary: String, log: String) {
        _uiState.update { state ->
            val updated = state.messages.map { msg ->
                if (msg.id == id) msg.copy(text = summary, details = msg.details.toMutableList().apply { if (log.isNotEmpty()) add(log); if (size > 30) removeAt(0) })
                else msg
            }
            state.copy(messages = updated)
        }
    }
}
