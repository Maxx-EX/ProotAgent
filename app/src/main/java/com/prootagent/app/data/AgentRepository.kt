package com.prootagent.app.data
import com.prootagent.app.core.ProotManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class AgentEvent {
    data class Text(val text: String) : AgentEvent()
    data class Progress(val summary: String, val log: String) : AgentEvent()
    data class Error(val message: String) : AgentEvent()
}

class AgentRepository(private val settings: SettingsRepository, private val prootManager: ProotManager) {
    private val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
    private val toolsJson = JSONArray().put(JSONObject().apply {
        put("type", "function"); put("function", JSONObject().apply {
            put("name", "execute_shell"); put("description", "Execute shell command inside Proot Linux.")
            put("parameters", JSONObject().apply {
                put("type", "object"); put("properties", JSONObject().put("command", JSONObject().apply { put("type", "string"); put("description", "Shell command") }))
                put("required", JSONArray().put("command"))
            })
        })
    })

    fun runAgent(prompt: String): Flow<AgentEvent> = flow {
        val url = settings.apiUrl.first().trimEnd('/') + "/chat/completions"
        val key = settings.apiKey.first()
        if (key.isBlank()) { emit(AgentEvent.Error("请先在设置中配置 API Key")); return@flow }

        var messages = JSONArray().apply { put(JSONObject().apply { put("role", "user"); put("content", prompt) }) }
        var loopCount = 0

        while (loopCount < 5) {
            val json = JSONObject().apply { put("model", settings.apiModel.first()); put("messages", messages); put("tools", toolsJson) }
            val request = Request.Builder().url(url).addHeader("Authorization", "Bearer $key").post(json.toString().toRequestBody("application/json".toMediaType())).build()

            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) { emit(AgentEvent.Error("API 错误: ${response.code}")); break }
                val message = JSONObject(response.body?.string() ?: break).getJSONArray("choices").getJSONObject(0).getJSONObject("message")
                messages.put(message)

                if (message.has("tool_calls")) {
                    val toolCalls = message.getJSONArray("tool_calls")
                    for (i in 0 until toolCalls.length()) {
                        val toolCall = toolCalls.getJSONObject(i)
                        val function = toolCall.getJSONObject("function")
                        if (function.getString("name") == "execute_shell") {
                            val command = JSONObject(function.getString("arguments")).getString("command")
                            emit(AgentEvent.Progress("执行命令", command))
                            val output = StringBuilder()
                            prootManager.executeCommand(command).collect { logLine ->
                                output.append(logLine).append("\n")
                                emit(AgentEvent.Progress("运行中", logLine))
                            }
                            messages.put(JSONObject().apply { put("role", "tool"); put("tool_call_id", toolCall.getString("id")); put("content", output.toString()) })
                        }
                    }
                    loopCount++
                } else {
                    emit(AgentEvent.Text(message.optString("content", "")))
                    break
                }
            } catch (e: Exception) { emit(AgentEvent.Error("网络异常: ${e.message}")); break }
        }
    }.flowOn(Dispatchers.IO)
}
