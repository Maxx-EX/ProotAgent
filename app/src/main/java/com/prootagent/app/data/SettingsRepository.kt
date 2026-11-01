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
        val UPDATE_SOURCE = intPreferencesKey("update_source")
        val THEME_MODE = intPreferencesKey("theme_mode")
        val DOWNLOAD_URI = stringPreferencesKey("download_uri")
        val ROOTFS_SOURCE = intPreferencesKey("rootfs_source")
        val OS_TYPE = intPreferencesKey("os_type")
        
        val SSH_HOST = stringPreferencesKey("ssh_host")
        val SSH_PORT = intPreferencesKey("ssh_port")
        val SSH_USER = stringPreferencesKey("ssh_user")
        val SSH_PASS = stringPreferencesKey("ssh_pass")
        val WORK_MODE = intPreferencesKey("work_mode")
    }
    
    val apiUrl = context.dataStore.data.map { it[API_URL] ?: "https://api.openai.com/v1" }
    val apiKey = context.dataStore.data.map { it[API_KEY] ?: "" }
    val apiModel = context.dataStore.data.map { it[API_MODEL] ?: "gpt-4o" }
    val updateSource = context.dataStore.data.map { it[UPDATE_SOURCE] ?: 1 }
    val themeMode = context.dataStore.data.map { it[THEME_MODE] ?: 0 }
    val downloadUri = context.dataStore.data.map { it[DOWNLOAD_URI] ?: "" }
    val rootfsSource = context.dataStore.data.map { it[ROOTFS_SOURCE] ?: 0 }
    val osType = context.dataStore.data.map { it[OS_TYPE] ?: 0 }
    
    val sshHost = context.dataStore.data.map { it[SSH_HOST] ?: "127.0.0.1" }
    val sshPort = context.dataStore.data.map { it[SSH_PORT] ?: 8022 }
    val sshUser = context.dataStore.data.map { it[SSH_USER] ?: "" }
    val sshPass = context.dataStore.data.map { it[SSH_PASS] ?: "" }
    val workMode = context.dataStore.data.map { it[WORK_MODE] ?: 0 }

    suspend fun saveConfig(url: String, key: String, model: String) = context.dataStore.edit { it[API_URL] = url; it[API_KEY] = key; it[API_MODEL] = model }
    suspend fun saveThemeMode(mode: Int) = context.dataStore.edit { it[THEME_MODE] = mode }
    suspend fun saveOsType(type: Int) = context.dataStore.edit { it[OS_TYPE] = type }
    suspend fun saveUpdateSource(source: Int) = context.dataStore.edit { it[UPDATE_SOURCE] = source }
    suspend fun saveRootfsSource(source: Int) = context.dataStore.edit { it[ROOTFS_SOURCE] = source }
    suspend fun saveSshConfig(host: String, port: Int, user: String, pass: String) = context.dataStore.edit { 
        it[SSH_HOST] = host; it[SSH_PORT] = port; it[SSH_USER] = user; it[SSH_PASS] = pass 
    }
    suspend fun saveWorkMode(mode: Int) = context.dataStore.edit { it[WORK_MODE] = mode }
}
