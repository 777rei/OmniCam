package com.example.data.model

data class StreamSettings(
    val port: Int = 8080,
    val resolutionWidth: Int = 640,
    val resolutionHeight: Int = 480,
    val targetFps: Int = 15,
    val jpegQuality: Int = 65, // 10 to 90
    val lowBandwidthMode: Boolean = false,
    val encryptionKey: String = "OMNI-SEC-256-AES",
    val requireAuth: Boolean = false,
    val authToken: String = "safe77",
    val motionSensitivity: Float = 0.25f, // 0.05 to 0.8
    val motionCooldownSeconds: Int = 4,
    val soundDetectionEnabled: Boolean = true,
    val soundThresholdDb: Float = 68.0f,
    val sirenOnMotion: Boolean = false,
    val flashOnMotion: Boolean = false,
    val autoCloudSync: Boolean = true
)

data class DetectionZone(
    val rows: Int = 4,
    val cols: Int = 4,
    // Matrix of active cells (true = monitored, false = ignored)
    val activeCells: List<Boolean> = List(16) { true }
) {
    fun isCellActive(row: Int, col: Int): Boolean {
        val index = row * cols + col
        return if (index in activeCells.indices) activeCells[index] else true
    }

    fun toggleCell(index: Int): DetectionZone {
        if (index !in activeCells.indices) return this
        val updated = activeCells.toMutableList()
        updated[index] = !updated[index]
        return copy(activeCells = updated)
    }
}
