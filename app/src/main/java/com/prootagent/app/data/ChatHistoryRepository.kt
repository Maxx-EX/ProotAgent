package com.prootagent.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class HistoryRecord(val id: Long, val title: String, val content: String)

class ChatHistoryRepository(private val context: Context) {
    private val historyFile = File(context.filesDir, "chat_history.json")

    suspend fun saveChat(id: Long, title: String, messagesJson: String) = withContext(Dispatchers.IO) {
        val records = loadAll().toMutableList()
        records.removeAll { it.id == id }
        records.add(0, HistoryRecord(id, title, messagesJson))
        val jsonArray = JSONArray()
        records.take(50).forEach { // 最多保留 50 条
            jsonArray.put(JSONObject().apply {
                put("id", it.id)
                put("title", it.title)
                put("content", it.content)
            })
        }
        historyFile.writeText(jsonArray.toString())
    }

    suspend fun loadAll(): List<HistoryRecord> = withContext(Dispatchers.IO) {
        if (!historyFile.exists()) return@withContext emptyList()
        try {
            val jsonArray = JSONArray(historyFile.readText())
            val list = mutableListOf<HistoryRecord>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(HistoryRecord(obj.getLong("id"), obj.getString("title"), obj.getString("content")))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun deleteChat(id: Long) = withContext(Dispatchers.IO) {
        val records = loadAll().filter { it.id != id }
        val jsonArray = JSONArray()
        records.forEach {
            jsonArray.put(JSONObject().apply {
                put("id", it.id); put("title", it.title); put("content", it.content)
            })
        }
        historyFile.writeText(jsonArray.toString())
    }
}
