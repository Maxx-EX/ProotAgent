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
    val messages: List<ChatItem> = emptyList(), 
    val isGenerating: Boolean = false, 
    val inputText: String = "", 
    val updateInfo: AppUpdateInfo? = null, val isDownloading: Boolean = false, val downloadProgress: Int = 0,
    val isEnvReady: Boolean = false, val envProgress: Int = 0, val envStatus: String = "检测中...",
    val historyList: List<HistoryRecord> = emptyList(),
    val currentOsType: Int = 0 // 0: Alpine, 1: Ubuntu
)

class AgentViewModel(app: Application) : AndroidViewModel(app) {
    private val settingsRepo = SettingsRepository(app)
    private val prootManager = ProotManager(app.applicationInfo.nativeLibraryDir, app.filesDir)
    private val rootfsManager = RootfsManager(app)
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
        _uiState.update { it.copy(messages = listOf(ChatItem(type = ChatItem.Type.AI, text = "你好！我是 ProotAgent。如果你已经提前放置了 Rootfs 文件，系统会自动识别并直接进入工作模式。如果没有，请点击顶部准备环境。"))) }
    }

    // 深度检查环境
    fun checkEnv() {
        viewModelScope.launch {
            val installed = rootfsManager.isInstalled()
            val osType = settingsRepo.osType.first()
            _uiState.update { it.copy(
                isEnvReady = installed, 
                envStatus = if (installed) "环境已就绪" else "未准备环境",
                currentOsType = osType
            ) }
        }
    }

    fun installDeps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            val osType = settingsRepo.osType.first()
            val cmd = if (osType == 1) {
                "apt update && apt install -y python3 python3-pip git curl build-essential"
            } else {
                "apk update && apk add python3 py3-pip nodejs npm git curl build-base"
            }
            prootManager.executeCommand(cmd).collect { _ -> }
            _uiState.update { it.copy(isGenerating = false) }
        }
    }

    fun prepareEnvironment() {
        viewModelScope.launch {
            _uiState.update { it.copy(isEnvReady = false, envStatus = "正在准备...") }
            val source = settingsRepo.rootfsSource.first()
            val osType = settingsRepo.osType.first()
            rootfsManager.prepareEnvironment(source, osType).collect { (p, msg) ->
                _uiState.update { it.copy(envProgress = p, envStatus = msg) }
            }
            checkEnv()
        }
    }

    fun stopGeneration() { agentRepo.stopGeneration(); _uiState.update { it.copy(isGenerating = false) } }
    fun loadHistory() { viewModelScope.launch { _uiState.update { it.copy(historyList = historyRepo.loadAll()) } } }

    fun saveCurrentChat() {
        if (_uiState.value.messages.isEmpty()) return
        val jsonArray = JSONArray()
        _uiState.value.messages.forEach { jsonArray.put(JSONObject().apply { put("type", it.type.name); put("text", it.text) }) }
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

    fun deleteHistory(id: Long) { viewModelScope.launch { historyRepo.deleteChat(id); loadHistory() } }

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
        // 发送前再次强制检查一次，防止用户中途自己放了文件进去没刷新
        checkEnv()
        if (!_uiState.value.isEnvReady || _uiState.value.isGenerating) return
        
        val prompt = _uiState.value.inputText.trim()
        if (prompt.isEmpty()) return
        
        val userMsg = ChatItem(type = ChatItem.Type.USER, text = prompt)
        val aiMsgId = System.currentTimeMillis() + 1
        val progressId = System.currentTimeMillis() + 2
        
        _uiState.update { it.copy(
            messages = it.messages + userMsg + ChatItem(id = aiMsgId, type = ChatItem.Type.AI, text = "") + ChatItem(id = progressId, type = ChatItem.Type.PROGRESS, text = "正在思考…"),
            inputText = "", isGenerating = true
        ) }

        viewModelScope.launch {
            agentRepo.runAgent(prompt).collect { event ->
                when (event) {
                    is AgentEvent.Text -> _uiState.update { state ->
                        val updated = state.messages.map { if (it.id == aiMsgId) it.copy(text = it.text + event.text) else it }
                        state.copy(messages = updated)
                    }
                    is AgentEvent.Progress -> _uiState.update { state ->
                        val updated = state.messages.map { msg ->
                            if (msg.id == progressId) msg.copy(text = event.summary, details = msg.details.toMutableList().apply { if (event.log.isNotEmpty()) add(event.log); if (size > 30) removeAt(0) })
                            else msg
                        }
                        state.copy(messages = updated)
                    }
                    is AgentEvent.Error -> _uiState.update { state ->
                        val updated = state.messages.map { if (it.id == aiMsgId) it.copy(text = it.text + "\n[错误] ${event.message}") else it }
                        state.copy(messages = updated)
                    }
                    AgentEvent.Done -> { _uiState.update { it.copy(isGenerating = false) }; saveCurrentChat() }
                }
            }
        }
    }
}
