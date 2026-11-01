package com.prootagent.app.mcp
import android.util.Log
import com.prootagent.app.core.ProotManager
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

class McpServer(private val port: Int, private val prootManager: ProotManager) {
    fun start() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val serverSocket = ServerSocket(port)
                while (true) {
                    val socket = serverSocket.accept()
                    handleClient(socket)
                }
            } catch (e: Exception) { Log.e("McpServer", "Error", e) }
        }
    }
    private fun handleClient(socket: Socket) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)
                var contentLength = 0
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.startsWith("Content-Length: ")) contentLength = line.substring(16).trim().toInt()
                    if (line.isEmpty()) break
                }
                val body = CharArray(contentLength)
                reader.read(body, 0, contentLength)
                val req = JSONObject(String(body))
                val resp = JSONObject().apply { put("jsonrpc", "2.0"); put("id", req.optInt("id", 0)) }
                if (req.optString("method") == "initialize") {
                    resp.put("result", JSONObject().put("protocolVersion", "2024-11-05").put("serverInfo", JSONObject().put("name", "ProotAgent")))
                } else if (req.optString("method") == "tools/list") {
                    resp.put("result", JSONObject().put("tools", org.json.JSONArray().put(JSONObject().put("name", "execute_shell").put("description", "Execute command"))))
                } else { resp.put("error", JSONObject().put("code", -32601).put("message", "Method not found")) }
                
                val respStr = resp.toString()
                writer.println("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${respStr.toByteArray().size}\r\nConnection: close\r\n\r\n$respStr")
                socket.close()
            } catch (e: Exception) { Log.e("McpServer", "Client error", e) }
        }
    }
}
