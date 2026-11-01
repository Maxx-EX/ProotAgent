package com.prootagent.app.data

import com.prootagent.app.core.ProotManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.RequestBody.Companion.toMediaType
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class AgentEvent {
    data class Text(val text: String) : AgentEvent()
    data class Progress(val summary: String, val log: String) : AgentEvent()
    data class Error(val message: String) : AgentEvent()
    object Done : AgentEvent()
}

class AgentRepository(private val settings: SettingsRepository, private val prootManager: ProotManager) {
    private val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS).build()
    private var currentEventSource: EventSource? = null

    private val systemPrompt = """
        你是 AndroidLinuxAI 端侧容器智能调度 Agent。
        环境：Android APP + Proot Linux。你当前拥有一个 execute_shell 工具。
        规则：
        1. 任何操作必须调用工具执行，禁止空口说白话。
        2. 交付文件必须在输出中说明绝对路径、大小和哈希。
    """.trimIndent()

    private val toolsJson = JSONArray().put(JSONObject().apply {
        put("type", "function"); put("function", JSONObject().apply {
            put("name", "execute_shell"); put("description", "Execute shell command in Proot.")
            put("parameters", JSONObject().apply {
                put("type", "object"); put("properties", JSONObject().put("command", JSONObject().apply { put("type", "string") }))
                put("required", JSONArray().put("command"))
            })
        })
    })

    fun stopGeneration() {
        currentEventSource?.cancel()
        currentEventSource = null
    }

    fun runAgent(prompt: String): Flow<AgentEvent> = channelFlow {
        val url = settings.apiUrl.first().trimEnd('/') + "/chat/completions"
        val key = settings.apiKey.first()
        if (key.isBlank()) { send(AgentEvent.Error("请先在设置中配置 API Key")); return@channelFlow }

        var messages = JSONArray().apply { 
            put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
            put(JSONObject().apply { put("role", "user"); put("content", prompt) }) 
        }

        try {
            while (true) {
                val json = JSONObject().apply { put("model", settings.apiModel.first()); put("messages", messages); put("tools", toolsJson); put("stream", true) }
                val request = Request.Builder().url(url).addHeader("Authorization", "Bearer $key").post(json.toString().toRequestBody("application/json".toMediaType())).build()

                var hasToolCalls = false
                var toolCallId = ""
                var toolCallName = ""
                var toolCallArgs = StringBuilder()
                var aiFullText = StringBuilder()

                suspend fun processStream(): Boolean = suspendCancellableCoroutine { continuation ->
                    val listener = object : EventSourceListener() {
                        override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                            if (data == "[DONE]") { continuation.resume(hasToolCalls); return }
                            try {
                                val delta = JSONObject(data).getJSONArray("choices").getJSONObject(0).getJSONObject("delta")
                                if (delta.has("content")) {
                                    val text = delta.getString("content")
                                    aiFullText.append(text)
                                    trySend(AgentEvent.Text(text))
                                }
                                if (delta.has("tool_calls")) {
                                    hasToolCalls = true
                                    val tc = delta.getJSONArray("tool_calls").getJSONObject(0)
                                    if (tc.has("id")) toolCallId = tc.getString("id")
                                    if (tc.has("function")) {
                                        val fn = tc.getJSONObject("function")
                                        if (fn.has("name")) toolCallName = fn.getString("name")
                                        if (fn.has("arguments")) toolCallArgs.append(fn.getString("arguments"))
                                    }
                                }
                            } catch (e: Exception) {}
                        }
                        override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                            trySend(AgentEvent.Error("流断开: ${t?.message}"))
                            continuation.resume(false)
                        }
                        override fun onClosed(eventSource: EventSource) {
                            continuation.resume(hasToolCalls)
                        }
                    }
                    currentEventSource = EventSources.createFactory(client).newEventSource(request, listener)
                    continuation.invokeOnCancellation { currentEventSource?.cancel() }
                }

                val calledTool = processStream()
                currentEventSource = null

                if (calledTool) {
                    messages.put(JSONObject().apply {
                        put("role", "assistant")
                        put("tool_calls", JSONArray().put(JSONObject().apply {
                            put("id", toolCallId); put("type", "function")
                            put("function", JSONObject().apply { put("name", toolCallName); put("arguments", toolCallArgs.toString()) })
                        }))
                    })
                    
                    val cmd = try { JSONObject(toolCallArgs.toString()).getString("command") } catch (e: Exception) { "echo 解析参数失败" }
                    send(AgentEvent.Progress("执行命令", cmd))
                    
                    val output = StringBuilder()
                    prootManager.executeCommand(cmd).collect { logLine ->
                        output.append(logLine).append("\n")
                        send(AgentEvent.Progress("运行中", logLine))
                    }
                    messages.put(JSONObject().apply { put("role", "tool"); put("tool_call_id", toolCallId); put("content", output.toString()) })
                } else {
                    break
                }
            }
            send(AgentEvent.Done)
        } catch (e: Exception) {
            send(AgentEvent.Error("异常: ${e.message}"))
        }
    }
}
