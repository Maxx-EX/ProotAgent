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
    val updateInfo: AppUpdateInfo? = null, val isEnvReady: Boolean = false, 
    val envStatus: String = "未准备", val activeRootfsPath: String = "",
    val historyList: List<HistoryRecord> = emptyList(),
    val workMode: Int = 0
)

class AgentViewModel(app: Application) : AndroidViewModel(app) {
    private val settingsRepo = SettingsRepository(app)
    private val prootManager = ProotManager(app.applicationInfo.nativeLibraryDir)
    private val sshManager = TermuxSshManager()
    private val rootfsManager = RootfsManager(app)
    private val historyRepo = ChatHistoryRepository(app)
    private val agentRepo = AgentRepository(settingsRepo, prootManager, sshManager)
    private val mcpServer = McpServer(8080, prootManager)

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        mcpServer.start()
        checkUpdate()
        viewModelScope.launch {
            val mode = settingsRepo.workMode.first()
            _uiState.update { it.copy(workMode = mode) }
            checkEnv()
        }
        loadHistory()
        _uiState.update { it.copy(messages = listOf(ChatItem(type = ChatItem.Type.AI, text = "你好！请确保已经把 Ubuntu 解压在 /storage/emulated/0/ai1 或直接放入压缩包，系统会自动识别。"))) }
    }

    fun toggleWorkMode(mode: Int) {
        _uiState.update { it.copy(workMode = mode) }
        viewModelScope.launch { settingsRepo.saveWorkMode(mode) }
        checkEnv()
    }

    fun checkEnv() {
        viewModelScope.launch {
            if (_uiState.value.workMode == 1) {
                _uiState.update { it.copy(isEnvReady = true, envStatus = "已连接 Termux SSH") }
            } else {
                val foundDir = prootManager.findRootfs() 
                val path = foundDir?.absolutePath ?: "/storage/emulated/0/ai1"
                val isExist = foundDir != null || java.io.File("/storage/emulated/0/ai1").exists()
                
                _uiState.update { it.copy(
                    isEnvReady = isExist, 
                    envStatus = if (isExist) "环境已就绪: $path" else "未准备",
                    activeRootfsPath = path
                ) }
            }
        }
    }

    fun installDeps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            val cmd = "apt update && apt install -y python3 python3-pip git curl build-essential || apk update && apk add python3 py3-pip git curl build-base"
            if (_uiState.value.workMode == 0) {
                prootManager.executeCommand(cmd, _uiState.value.activeRootfsPath).collect { _ -> }
            } else {
                val host = settingsRepo.sshHost.first()
                val port = settingsRepo.sshPort.first()
                val user = settingsRepo.sshUser.first()
                val pass = settingsRepo.sshPass.first()
                sshManager.executeCommand(host, port, user, pass, cmd).collect { _ -> }
            }
            _uiState.update { it.copy(isGenerating = false) }
        }
    }

    fun prepareEnvironment() {
        viewModelScope.launch {
            _uiState.update { it.copy(isEnvReady = false, envStatus = "正在准备...") }
            val source = settingsRepo.rootfsSource.first()
            val osType = settingsRepo.osType.first()
            rootfsManager.prepareEnvironment(source, osType).collect { (p, msg) ->
                _uiState.update { it.copy(envStatus = msg) }
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
        if (_uiState.value.isGenerating) return
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
            // 关键修复：提前获取 SSH 配置，避免在非 suspend Lambda 中调用挂起函数
            val host = settingsRepo.sshHost.first()
            val port = settingsRepo.sshPort.first()
            val user = settingsRepo.sshUser.first()
            val pass = settingsRepo.sshPass.first()

            val cmdExecutor: (String) -> Flow<String> = if (_uiState.value.workMode == 0) {
                { cmd -> prootManager.executeCommand(cmd, _uiState.value.activeRootfsPath) }
            } else {
                { cmd -> sshManager.executeCommand(host, port, user, pass, cmd) }
            }

            agentRepo.runAgent(prompt, cmdExecutor).collect { event ->
                when (event) {
                    is AgentEvent.Text -> _uiState.update { state ->
                        state.copy(messages = state.messages.map { if (it.id == aiMsgId) it.copy(text = it.text + event.text) else it })
                    }
                    is AgentEvent.Progress -> _uiState.update { state ->
                        state.copy(messages = state.messages.map { msg ->
                            if (msg.id == progressId) msg.copy(text = event.summary, details = msg.details.toMutableList().apply { if (event.log.isNotEmpty()) add(event.log); if (size > 30) removeAt(0) })
                            else msg
                        })
                    }
                    is AgentEvent.Error -> _uiState.update { state ->
                        state.copy(messages = state.messages.map { if (it.id == aiMsgId) it.copy(text = it.text + "\n[错误] ${event.message}") else it })
                    }
                    AgentEvent.Done -> { _uiState.update { it.copy(isGenerating = false) }; saveCurrentChat() }
                }
            }
        }
    }
}
