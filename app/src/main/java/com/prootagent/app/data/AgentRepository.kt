package com.prootagent.app.data

import com.prootagent.app.core.ProotManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.first
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

class AgentRepository(
    private val settings: SettingsRepository,
    private val prootManager: ProotManager
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val toolsJson = JSONArray().put(JSONObject().apply {
        put("type", "function")
        put("function", JSONObject().apply {
            put("name", "execute_shell")
            put("description", "Execute shell command inside the Proot Linux container.")
            put("parameters", JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().put("command", JSONObject().apply {
                    put("type", "string")
                    put("description", "The shell command to execute.")
                }))
                put("required", JSONArray().put("command"))
            })
        })
    })

    fun runAgent(prompt: String): Flow<AgentEvent> = flow {
        val url = settings.apiUrl.first().trimEnd('/') + "/chat/completions"
        val key = settings.apiKey.first()
        val model = settings.apiModel.first()

        if (key.isBlank()) {
            emit(AgentEvent.Error("请先在设置中配置 API Key"))
            return@flow
        }

        var messages = JSONArray().apply {
            put(JSONObject().apply { put("role", "user"); put("content", prompt) })
        }

        var loopCount = 0
        val maxLoops = 5 // 防止无限死循环

        while (loopCount < maxLoops) {
            val json = JSONObject().apply {
                put("model", model)
                put("messages", messages)
                put("tools", toolsJson)
                put("stream", false) // 使用非流式以方便解析 Tool Calls
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $key")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    emit(AgentEvent.Error("API 错误: ${response.code}"))
                    break
                }

                val respBody = response.body?.string() ?: break
                val respJson = JSONObject(respBody)
                val message = respJson.getJSONArray("choices").getJSONObject(0).getJSONObject("message")
                
                messages.put(message) // 将助手的回复加入上下文

                if (message.has("tool_calls")) {
                    val toolCalls = message.getJSONArray("tool_calls")
                    for (i in 0 until toolCalls.length()) {
                        val toolCall = toolCalls.getJSONObject(i)
                        val toolCallId = toolCall.getString("id")
                        val function = toolCall.getJSONObject("function")
                        val name = function.getString("name")
                        
                        if (name == "execute_shell") {
                            val args = JSONObject(function.getString("arguments"))
                            val command = args.getString("command")
                            
                            emit(AgentEvent.Progress("执行命令", command))
                            
                            val outputBuilder = StringBuilder()
                            prootManager.executeCommand(command).collect { logLine ->
                                outputBuilder.append(logLine).append("\n")
                                emit(AgentEvent.Progress("运行中", logLine))
                            }
                            
                            val toolResult = outputBuilder.toString()
                            messages.put(JSONObject().apply {
                                put("role", "tool")
                                put("tool_call_id", toolCallId)
                                put("content", toolResult)
                            })
                        }
                    }
                    loopCount++
                } else {
                    val text = message.optString("content", "")
                    emit(AgentEvent.Text(text))
                    break
                }
            } catch (e: Exception) {
                emit(AgentEvent.Error("网络异常: ${e.message}"))
                break
            }
        }
    }.flowOn(Dispatchers.IO)
}
