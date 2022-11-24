package com.cuttingedge.virtualmic

object Constants {
    const val DEFAULT_SAMPLING_RATE = 48000
    const val DEFAULT_NETWORK_PORT = 44456
    const val DEFAULT_IP_ADDRESS = "192.168.0.101"
}

sealed class AppState {
    object Streaming : AppState()
    object NotStreaming: AppState()
}