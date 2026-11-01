package com.prootagent.app.data
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")
class SettingsRepository(private val context: Context) {
    companion object {
        val API_URL = stringPreferencesKey("api_url")
        val API_KEY = stringPreferencesKey("api_key")
        val API_MODEL = stringPreferencesKey("api_model")
        val UPDATE_SOURCE = intPreferencesKey("update_source") // 0=GitHub, 1=国内镜像
    }
    
    val apiUrl = context.dataStore.data.map { it[API_URL] ?: "https://api.openai.com/v1" }
    val apiKey = context.dataStore.data.map { it[API_KEY] ?: "" }
    val apiModel = context.dataStore.data.map { it[API_MODEL] ?: "gpt-4o" }
    val updateSource = context.dataStore.data.map { it[UPDATE_SOURCE] ?: 0 }
    
    suspend fun saveConfig(url: String, key: String, model: String) {
        context.dataStore.edit { it[API_URL] = url; it[API_KEY] = key; it[API_MODEL] = model }
    }
    
    suspend fun saveUpdateSource(source: Int) {
        context.dataStore.edit { it[UPDATE_SOURCE] = source }
    }
}
