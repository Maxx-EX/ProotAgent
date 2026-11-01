package com.prootagent.app.core

object NativeGuard {
    init {
        System.loadLibrary("agent_guard")
    }
    external fun isProcessAlive(pid: Int): Boolean
    external fun getEnvironmentString(): String
}
