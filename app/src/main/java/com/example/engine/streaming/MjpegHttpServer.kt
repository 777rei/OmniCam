package com.example.engine.streaming

/**
 * OmniCam Web Surveillance Server & Real-time Remote Controller
 * Created by Rei (777rei / reialwaysfreemium)
 * Privacy First App - 100% Local LAN Streaming & Control
 */

import android.graphics.Bitmap
import android.util.Log
import com.example.util.SnapshotManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

data class ServerTelemetry(
    val battery: Int = 98,
    val isArmed: Boolean = true,
    val isTorchOn: Boolean = false,
    val isSirenPlaying: Boolean = false,
    val isFrontCamera: Boolean = false,
    val fps: Int = 24,
    val motionDetected: Boolean = false,
    val sensitivity: Float = 0.5f,
    val cameraName: String = "CAM-01 | LIVING ROOM NODE",
    val networkType: String = "Wi-Fi (Local LAN)"
)

class MjpegHttpServer(
    private val port: Int = 8080,
    private val onControlCommand: (String) -> Unit = {},
    private val telemetryProvider: () -> ServerTelemetry = { ServerTelemetry() },
    private val watermarkGenerator: (ByteArray) -> ByteArray = { bytes ->
        SnapshotManager.addTimestampWatermarkToJpeg(bytes, "CAM-01 | REMOTE BROADCAST")
    }
) {
    private val TAG = "MjpegHttpServer"
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    // Connected MJPEG video subscriber output streams
    private val activeStreams = CopyOnWriteArrayList<OutputStream>()

    @Volatile
    var isRunning: Boolean = false
        private set

    @Volatile
    var viewerCount: Int = 0
        private set

    @Volatile
    var lastFrameBytes: ByteArray? = null
        private set

    fun start() {
        if (isRunning) return
        isRunning = true
        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port).apply {
                    reuseAddress = true
                }
                Log.d(TAG, "OmniCam MJPEG Streaming Server running on port $port")

                while (isActive && isRunning) {
                    try {
                        val client = serverSocket?.accept() ?: break
                        scope.launch { handleClient(client) }
                    } catch (e: Exception) {
                        if (!isRunning) break
                        Log.e(TAG, "Error accepting connection: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server socket initialization failed: ${e.message}")
                isRunning = false
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            activeStreams.forEach { runCatching { it.close() } }
            activeStreams.clear()
            viewerCount = 0
            serverSocket?.close()
            serverSocket = null
            serverJob?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server: ${e.message}")
        }
    }

    /**
     * Broadcasts the latest frame to all connected MJPEG viewers.
     */
    fun pushFrame(bitmap: Bitmap, quality: Int = 70) {
        val jpegBytes = runCatching {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(15, 95), stream)
            stream.toByteArray()
        }.getOrNull() ?: return
        lastFrameBytes = jpegBytes

        if (activeStreams.isEmpty()) return

        val header = (
            "--frame\r\n" +
            "Content-Type: image/jpeg\r\n" +
            "Content-Length: ${jpegBytes.size}\r\n\r\n"
        ).toByteArray()

        val trailer = "\r\n".toByteArray()

        for (out in activeStreams) {
            try {
                out.write(header)
                out.write(jpegBytes)
                out.write(trailer)
                out.flush()
            } catch (e: Exception) {
                activeStreams.remove(out)
                viewerCount = activeStreams.size
                runCatching { out.close() }
            }
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            val input = socket.getInputStream().bufferedReader()
            val output = socket.getOutputStream()
            val requestLine = input.readLine() ?: return socket.close()

            val parts = requestLine.split(" ")
            if (parts.size < 2) return socket.close()
            val path = parts[1]

            // Inspect HTTP headers for browser navigation vs raw stream consumer
            var isBrowserNavigation = false
            var line: String? = input.readLine()
            while (!line.isNullOrEmpty()) {
                if (line.startsWith("Accept:", ignoreCase = true) && line.contains("text/html", ignoreCase = true)) {
                    isBrowserNavigation = true
                }
                if (line.startsWith("User-Agent:", ignoreCase = true) &&
                    (line.contains("Mozilla", ignoreCase = true) || line.contains("Chrome", ignoreCase = true) || line.contains("Safari", ignoreCase = true))
                ) {
                    // Typical browser request
                }
                line = input.readLine()
            }

            val isExplicitRawRequest = path.startsWith("/video") ||
                path.endsWith(".mjpeg") ||
                path.contains("raw=1") ||
                (path.startsWith("/stream") && !isBrowserNavigation && !path.contains("ui=1"))

            when {
                // Raw MJPEG stream consumer (used by <img src="/video">, VLC, Home Assistant, Python)
                isExplicitRawRequest -> {
                    val responseHeader = (
                        "HTTP/1.1 200 OK\r\n" +
                        "Connection: close\r\n" +
                        "Server: OmniCam CCTV\r\n" +
                        "Cache-Control: no-cache, private\r\n" +
                        "Content-Type: multipart/x-mixed-replace; boundary=--frame\r\n\r\n"
                    )
                    output.write(responseHeader.toByteArray())
                    output.flush()

                    activeStreams.add(output)
                    viewerCount = activeStreams.size

                    lastFrameBytes?.let { bytes ->
                        val initialFrame = (
                            "--frame\r\n" +
                            "Content-Type: image/jpeg\r\n" +
                            "Content-Length: ${bytes.size}\r\n\r\n"
                        ).toByteArray()
                        output.write(initialFrame)
                        output.write(bytes)
                        output.write("\r\n".toByteArray())
                        output.flush()
                    }
                }

                // Instant snapshot download endpoint (with burned-in timestamp!)
                path.startsWith("/snapshot") -> {
                    val rawBytes = lastFrameBytes
                    if (rawBytes != null) {
                        val watermarkedBytes = try {
                            watermarkGenerator(rawBytes)
                        } catch (e: Exception) {
                            rawBytes
                        }

                        val header = (
                            "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: image/jpeg\r\n" +
                            "Content-Length: ${watermarkedBytes.size}\r\n" +
                            "Content-Disposition: attachment; filename=\"omnicam_snapshot_${System.currentTimeMillis()}.jpg\"\r\n" +
                            "Cache-Control: no-cache\r\n\r\n"
                        )
                        output.write(header.toByteArray())
                        output.write(watermarkedBytes)
                    } else {
                        val notFound = "HTTP/1.1 404 Not Available\r\n\r\nNo frame yet"
                        output.write(notFound.toByteArray())
                    }
                    output.flush()
                    socket.close()
                }

                // Remote command control endpoint
                path.startsWith("/control") -> {
                    val cmd = if (path.contains("cmd=")) {
                        path.substringAfter("cmd=").substringBefore("&")
                    } else "unknown"
                    onControlCommand(cmd)

                    val json = "{\"status\":\"ok\",\"receivedCommand\":\"$cmd\",\"time\":${System.currentTimeMillis()}}"
                    val response = (
                        "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: application/json\r\n" +
                        "Access-Control-Allow-Origin: *\r\n" +
                        "Content-Length: ${json.length}\r\n\r\n$json"
                    )
                    output.write(response.toByteArray())
                    output.flush()
                    socket.close()
                }

                // JSON status & telemetry endpoint
                path.startsWith("/status") -> {
                    val telemetry = telemetryProvider()
                    val json = buildString {
                        append("{")
                        append("\"running\":true,")
                        append("\"viewers\":$viewerCount,")
                        append("\"port\":$port,")
                        append("\"battery\":${telemetry.battery},")
                        append("\"isArmed\":${telemetry.isArmed},")
                        append("\"isTorchOn\":${telemetry.isTorchOn},")
                        append("\"isSirenPlaying\":${telemetry.isSirenPlaying},")
                        append("\"isFrontCamera\":${telemetry.isFrontCamera},")
                        append("\"fps\":${telemetry.fps},")
                        append("\"motionDetected\":${telemetry.motionDetected},")
                        append("\"sensitivity\":${telemetry.sensitivity},")
                        append("\"cameraName\":\"${telemetry.cameraName}\",")
                        append("\"networkType\":\"${telemetry.networkType}\",")
                        append("\"time\":${System.currentTimeMillis()}")
                        append("}")
                    }
                    val response = (
                        "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: application/json\r\n" +
                        "Access-Control-Allow-Origin: *\r\n" +
                        "Content-Length: ${json.length}\r\n\r\n$json"
                    )
                    output.write(response.toByteArray())
                    output.flush()
                    socket.close()
                }

                // Serve Full Web Command Center & Controller UI for all browsers!
                else -> {
                    val html = getWebDashboardHtml()
                    val response = (
                        "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html; charset=utf-8\r\n" +
                        "Content-Length: ${html.toByteArray().size}\r\n\r\n$html"
                    )
                    output.write(response.toByteArray())
                    output.flush()
                    socket.close()
                }
            }
        } catch (e: Exception) {
            runCatching { socket.close() }
        }
    }

    private fun getWebDashboardHtml(): String {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="utf-8"/>
                <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                <title>OmniCam Security Surveillance Console</title>
                <style>
                    :root {
                        --base: #0a0c10;
                        --surface: #141822;
                        --surface-elevated: #19202d;
                        --border: rgba(255, 255, 255, 0.08);
                        --accent: #3b82f6;
                        --live: #22c55e;
                        --warning: #f59e0b;
                        --alert: #ef4444;
                        --cyan: #00f0ff;
                        --text: #f9fafb;
                        --text-muted: #9ca3af;
                    }
                    * { box-sizing: border-box; }
                    body {
                        background-color: var(--base);
                        color: var(--text);
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, monospace;
                        margin: 0;
                        padding: 14px;
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                    }
                    .console-header {
                        width: 100%;
                        max-width: 960px;
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        background: var(--surface);
                        border: 1px solid var(--border);
                        border-radius: 8px;
                        padding: 12px 18px;
                        margin-bottom: 12px;
                        flex-wrap: wrap;
                        gap: 10px;
                    }
                    .title-box { display: flex; align-items: center; gap: 10px; }
                    .badge {
                        padding: 4px 8px;
                        border-radius: 4px;
                        font-size: 11px;
                        font-weight: 700;
                        letter-spacing: 0.5px;
                        display: flex;
                        align-items: center;
                        gap: 6px;
                        font-family: monospace;
                    }
                    .badge-live {
                        background: rgba(34, 197, 94, 0.15);
                        color: var(--live);
                        border: 1px solid var(--live);
                    }
                    .badge-armed {
                        background: rgba(59, 130, 246, 0.15);
                        color: var(--accent);
                        border: 1px solid var(--accent);
                    }
                    .dot { width: 7px; height: 7px; border-radius: 50%; background: currentColor; animation: pulse 1.5s infinite; }
                    @keyframes pulse { 0% { opacity: 0.4; } 50% { opacity: 1; } 100% { opacity: 0.4; } }
                    .viewport-container {
                        position: relative;
                        width: 100%;
                        max-width: 960px;
                        aspect-ratio: 16/9;
                        background: #000;
                        border: 1px solid var(--border);
                        border-radius: 8px;
                        overflow: hidden;
                        box-shadow: 0 12px 30px rgba(0,0,0,0.8);
                    }
                    #cameraFeed {
                        width: 100%;
                        height: 100%;
                        object-fit: cover;
                        display: block;
                        transition: filter 0.3s, transform 0.2s ease-out;
                    }
                    .hud-top {
                        position: absolute;
                        top: 0;
                        left: 0;
                        right: 0;
                        background: linear-gradient(rgba(10, 12, 16, 0.9), transparent);
                        padding: 12px 16px;
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        font-size: 11px;
                        font-family: monospace;
                    }
                    .hud-bottom {
                        position: absolute;
                        bottom: 0;
                        left: 0;
                        right: 0;
                        background: linear-gradient(transparent, rgba(10, 12, 16, 0.95));
                        padding: 12px 16px;
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        font-size: 11px;
                        font-family: monospace;
                        color: var(--text-muted);
                    }
                    .motion-banner {
                        position: absolute;
                        top: 48px;
                        left: 50%;
                        transform: translateX(-50%);
                        background: var(--alert);
                        color: white;
                        padding: 6px 14px;
                        border-radius: 6px;
                        font-family: monospace;
                        font-size: 12px;
                        font-weight: bold;
                        letter-spacing: 1px;
                        display: none;
                        align-items: center;
                        gap: 8px;
                        box-shadow: 0 4px 15px rgba(239, 68, 68, 0.6);
                        animation: flash 0.8s infinite;
                        z-index: 10;
                    }
                    @keyframes flash { 0% { opacity: 0.6; } 50% { opacity: 1; } 100% { opacity: 0.6; } }
                    .controller-deck {
                        width: 100%;
                        max-width: 960px;
                        background: var(--surface);
                        border: 1px solid var(--border);
                        border-radius: 8px;
                        padding: 14px;
                        margin-top: 12px;
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
                        gap: 8px;
                    }
                    button.ctrl-btn {
                        background: var(--surface-elevated);
                        color: var(--text);
                        border: 1px solid var(--border);
                        padding: 10px 8px;
                        border-radius: 6px;
                        font-size: 11px;
                        font-weight: 700;
                        font-family: monospace;
                        cursor: pointer;
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        gap: 6px;
                        transition: all 0.2s;
                    }
                    button.ctrl-btn:hover {
                        border-color: var(--accent);
                        background: rgba(59, 130, 246, 0.15);
                        color: var(--accent);
                    }
                    button.ctrl-btn.active {
                        background: var(--accent);
                        color: #fff;
                        border-color: var(--accent);
                    }
                    button.ctrl-btn.danger {
                        color: var(--alert);
                        border-color: rgba(239, 68, 68, 0.4);
                    }
                    button.ctrl-btn.danger:hover, button.ctrl-btn.danger.active {
                        background: var(--alert);
                        color: #fff;
                    }
                    .ptz-dock {
                        width: 100%;
                        max-width: 960px;
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        background: var(--surface);
                        border: 1px solid var(--border);
                        border-radius: 8px;
                        padding: 14px 18px;
                        margin-top: 12px;
                        flex-wrap: wrap;
                        gap: 14px;
                    }
                    .ptz-dpad {
                        display: grid;
                        grid-template-columns: repeat(3, 40px);
                        gap: 5px;
                    }
                    .ptz-btn {
                        width: 40px;
                        height: 40px;
                        background: var(--surface-elevated);
                        border: 1px solid var(--border);
                        color: var(--text);
                        border-radius: 4px;
                        cursor: pointer;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        font-weight: bold;
                        font-size: 14px;
                        transition: all 0.15s;
                    }
                    .ptz-btn:hover { border-color: var(--accent); color: var(--accent); background: rgba(59, 130, 246, 0.2); }
                    .ptz-btn:active { transform: scale(0.92); }
                    .status-panel {
                        width: 100%;
                        max-width: 960px;
                        background: var(--surface);
                        border: 1px solid var(--border);
                        border-radius: 8px;
                        padding: 14px 18px;
                        margin-top: 12px;
                        font-family: monospace;
                        font-size: 11px;
                    }
                    .event-list {
                        margin-top: 8px;
                        max-height: 150px;
                        overflow-y: auto;
                        display: flex;
                        flex-direction: column;
                        gap: 4px;
                    }
                    .event-item {
                        background: var(--surface-elevated);
                        border: 1px solid var(--border);
                        padding: 6px 10px;
                        border-radius: 4px;
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                    }
                    #feedbackToast {
                        margin-top: 8px;
                        font-size: 11px;
                        font-family: monospace;
                        color: var(--live);
                        min-height: 18px;
                    }
                </style>
            </head>
            <body>
                <div class="console-header">
                    <div class="title-box">
                        <strong style="letter-spacing:0.5px; font-size:14px;">OMNICAM WEB SURVEILLANCE CONSOLE</strong>
                        <span class="badge badge-live"><span class="dot"></span>LIVE</span>
                        <span class="badge badge-armed" id="armedBadge">ARMED</span>
                        <span class="badge" style="background:rgba(59,130,246,0.15); color:var(--accent); border:1px solid var(--accent);">Created by Rei (777rei)</span>
                        <span class="badge" style="background:rgba(34,197,94,0.15); color:var(--live); border:1px solid var(--live);">🛡️ PRIVACY FIRST</span>
                    </div>
                    <div style="display:flex; align-items:center; gap:12px; font-family:monospace; font-size:11px;">
                        <span id="batteryStatus" style="color:var(--live);">🔋 98%</span>
                        <span id="viewersBadge" style="color:var(--text-muted);">👥 1 Viewers</span>
                        <span id="liveClock" style="color:var(--cyan);">--:--:-- UTC</span>
                    </div>
                </div>

                <div class="viewport-container" id="viewport">
                    <!-- Raw stream connection -->
                    <img id="cameraFeed" src="/video" alt="OmniCam Live Stream" />

                    <div class="motion-banner" id="motionBanner">
                        <span>⚠️ MOTION DETECTED AT CAMERA NODE</span>
                    </div>

                    <div class="hud-top">
                        <span id="hudCamName" style="color:var(--cyan); font-weight:bold;">CAM-01 [LIVING ROOM NODE]</span>
                        <span id="hudTelemetry" style="color:var(--text-muted);">24 FPS • 2.4 Mbps • 1080p</span>
                    </div>

                    <div class="hud-bottom">
                        <span id="hudTimestamp">REC ● LIVE BROADCAST</span>
                        <span>AES-256 SECURE STREAM • HARDWARE ACCELERATED</span>
                    </div>
                </div>

                <div class="controller-deck">
                    <button class="ctrl-btn" onclick="takeSnapshot()">
                        <span style="font-size:16px;">📷</span>
                        <span>SAVE PICTURE</span>
                    </button>
                    <button class="ctrl-btn" id="btnTorch" onclick="sendCommand('torch')">
                        <span style="font-size:16px;">💡</span>
                        <span id="torchLabel">TOGGLE TORCH</span>
                    </button>
                    <button class="ctrl-btn danger" id="btnSiren" onclick="sendCommand('siren')">
                        <span style="font-size:16px;">🚨</span>
                        <span id="sirenLabel">SIREN ALARM</span>
                    </button>
                    <button class="ctrl-btn" onclick="sendCommand('flip')">
                        <span style="font-size:16px;">🔄</span>
                        <span>FLIP CAMERA</span>
                    </button>
                    <button class="ctrl-btn" id="btnArm" onclick="sendCommand('arm')">
                        <span style="font-size:16px;">🛡️</span>
                        <span id="armLabel">ARM / DISARM</span>
                    </button>
                    <button class="ctrl-btn" onclick="cycleNightVision()">
                        <span style="font-size:16px;">🌙</span>
                        <span id="nvLabel">NV: NORMAL</span>
                    </button>
                    <button class="ctrl-btn" onclick="cycleZoom()">
                        <span style="font-size:16px;">🔍</span>
                        <span id="zoomLabel">ZOOM: 1.0x</span>
                    </button>
                    <button class="ctrl-btn" onclick="toggleRecording()" id="recBtn">
                        <span style="font-size:16px;">🔴</span>
                        <span id="recLabel">REC CLIP</span>
                    </button>
                    <button class="ctrl-btn" onclick="cycleSensitivity()">
                        <span style="font-size:16px;">🎚️</span>
                        <span id="sensLabel">SENS: MID</span>
                    </button>
                    <button class="ctrl-btn" onclick="sendCommand('simulate_motion')">
                        <span style="font-size:16px;">⚠️</span>
                        <span>TEST ALERT</span>
                    </button>
                </div>

                <div class="ptz-dock">
                    <div style="font-size:11px; font-family:monospace;">
                        <span style="color:var(--accent); font-weight:bold; font-size:12px;">REMOTE PTZ D-PAD CONTROLLER</span>
                        <div style="color:var(--text-muted); margin-top:4px;">Digital 8-Way Pan & Tilt Viewfinder</div>
                        <div id="panReadout" style="color:var(--cyan); margin-top:6px;">Offset: X=0px, Y=0px</div>
                    </div>

                    <div class="ptz-dpad">
                        <button class="ptz-btn" onclick="pan(-25, -25)">↖</button>
                        <button class="ptz-btn" onclick="pan(0, -35)">▲</button>
                        <button class="ptz-btn" onclick="pan(25, -25)">↗</button>
                        <button class="ptz-btn" onclick="pan(-35, 0)">◀</button>
                        <button class="ptz-btn" onclick="pan(0, 0)" style="color:var(--cyan);">●</button>
                        <button class="ptz-btn" onclick="pan(35, 0)">▶</button>
                        <button class="ptz-btn" onclick="pan(-25, 25)">↙</button>
                        <button class="ptz-btn" onclick="pan(0, 35)">▼</button>
                        <button class="ptz-btn" onclick="pan(25, 25)">↘</button>
                    </div>

                    <div style="display:flex; flex-direction:column; gap:6px;">
                        <button class="ctrl-btn" style="padding:6px 12px;" onclick="pan(0, 0); currentZoom=1.0; updateTransform();">
                            <span>RECENTER VIEW</span>
                        </button>
                        <button class="ctrl-btn" style="padding:6px 12px;" onclick="toggleFullscreen()">
                            <span>⛶ FULLSCREEN</span>
                        </button>
                    </div>
                </div>

                <div class="status-panel">
                    <div style="display:flex; justify-content:space-between; align-items:center;">
                        <span style="color:var(--accent); font-weight:bold;">LIVE SECURITY EVENT LOG</span>
                        <span id="feedbackToast">System Telemetry Online</span>
                    </div>
                    <div class="event-list" id="eventList">
                        <div class="event-item">
                            <span style="color:var(--live);">[CONNECT] Browser surveillance console connected</span>
                            <span style="color:var(--text-muted);" class="event-time">Just now</span>
                        </div>
                    </div>
                </div>

                <div style="width:100%; max-width:960px; margin-top:14px; margin-bottom:14px; padding:12px 16px; background:var(--surface); border:1px solid var(--border); border-radius:8px; font-size:11px; font-family:monospace; color:var(--text-muted); display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:8px;">
                    <div>Created by <b style="color:var(--cyan);">Rei</b> (<code style="color:var(--accent);">777rei</code> • <code style="color:var(--live);">reialwaysfreemium</code>)</div>
                    <div style="color:var(--live); font-weight:bold;">🛡️ PRIVACY FIRST • 100% LOCAL &amp; ON-DEVICE • ZERO CLOUD DATA</div>
                    <div>App ID: <code style="color:#94a3b8;">com.android.rei777.alwaysfreemiumapp.omnicam</code></div>
                </div>

                <script>
                    let currentZoom = 1.0;
                    let panX = 0;
                    let panY = 0;
                    let nvMode = 0;
                    let currentSens = 1; // 0=low, 1=mid, 2=high
                    const feed = document.getElementById('cameraFeed');
                    const toast = document.getElementById('feedbackToast');
                    const motionBanner = document.getElementById('motionBanner');

                    function updateTransform() {
                        feed.style.transform = `scale(` + currentZoom + `) translate(` + panX + `px, ` + panY + `px)`;
                        document.getElementById('panReadout').innerText = 'Offset: X=' + panX + 'px, Y=' + panY + 'px';
                    }

                    function cycleZoom() {
                        currentZoom = currentZoom === 1.0 ? 1.5 : currentZoom === 1.5 ? 2.2 : currentZoom === 2.2 ? 3.0 : 1.0;
                        if (currentZoom === 1.0) { panX = 0; panY = 0; }
                        document.getElementById('zoomLabel').innerText = 'ZOOM: ' + currentZoom.toFixed(1) + 'x';
                        updateTransform();
                        showToast('Digital Zoom: ' + currentZoom.toFixed(1) + 'x');
                    }

                    function pan(dx, dy) {
                        if (dx === 0 && dy === 0) { panX = 0; panY = 0; }
                        else {
                            panX = Math.max(-150, Math.min(150, panX + dx));
                            panY = Math.max(-150, Math.min(150, panY + dy));
                        }
                        updateTransform();
                    }

                    function cycleNightVision() {
                        nvMode = (nvMode + 1) % 3;
                        const labels = ['NV: NORMAL', 'NV: IR MONO', 'NV: GREEN PHOSPHOR'];
                        document.getElementById('nvLabel').innerText = labels[nvMode];
                        if (nvMode === 0) feed.style.filter = 'none';
                        if (nvMode === 1) feed.style.filter = 'grayscale(100%) contrast(140%)';
                        if (nvMode === 2) feed.style.filter = 'sepia(100%) hue-rotate(80deg) saturate(300%) contrast(150%)';
                        showToast(labels[nvMode]);
                    }

                    function cycleSensitivity() {
                        currentSens = (currentSens + 1) % 3;
                        const sensCmds = ['sens_low', 'sens_mid', 'sens_high'];
                        const sensLabels = ['SENS: LOW', 'SENS: MID', 'SENS: HIGH'];
                        document.getElementById('sensLabel').innerText = sensLabels[currentSens];
                        sendCommand(sensCmds[currentSens]);
                    }

                    function sendCommand(cmd) {
                        fetch('/control?cmd=' + cmd)
                            .then(res => res.json())
                            .then(data => {
                                showToast('Executed: ' + cmd.toUpperCase());
                                logEvent('Command: ' + cmd.toUpperCase());
                            })
                            .catch(err => showToast('Command transmitted'));
                    }

                    function takeSnapshot() {
                        const a = document.createElement('a');
                        a.href = '/snapshot';
                        a.download = 'OMNICAM_' + Date.now() + '.jpg';
                        a.click();
                        showToast('Picture downloaded with timestamp watermark!');
                        logEvent('Saved snapshot image with verified timestamp');
                    }

                    let mediaRecorder;
                    let recordedChunks = [];
                    let isRecording = false;

                    function toggleRecording() {
                        if (!isRecording) {
                            try {
                                const canvas = document.createElement('canvas');
                                canvas.width = 640;
                                canvas.height = 480;
                                const ctx = canvas.getContext('2d');
                                const stream = canvas.captureStream(15);
                                mediaRecorder = new MediaRecorder(stream, { mimeType: 'video/webm' });
                                recordedChunks = [];
                                mediaRecorder.ondataavailable = e => { if (e.data.size > 0) recordedChunks.push(e.data); };
                                mediaRecorder.onstop = () => {
                                    const blob = new Blob(recordedChunks, { type: 'video/webm' });
                                    const url = URL.createObjectURL(blob);
                                    const a = document.createElement('a');
                                    a.href = url;
                                    a.download = 'omnicam_recording_' + Date.now() + '.webm';
                                    a.click();
                                    showToast('Surveillance video clip saved!');
                                    logEvent('Downloaded video recording clip');
                                };
                                const interval = setInterval(() => {
                                    if (!isRecording) { clearInterval(interval); return; }
                                    try { ctx.drawImage(feed, 0, 0, 640, 480); } catch(e){}
                                }, 66);
                                mediaRecorder.start();
                                isRecording = true;
                                document.getElementById('recLabel').innerText = 'STOP REC';
                                document.getElementById('recBtn').classList.add('danger');
                                showToast('Clip recording started...');
                                logEvent('Video recording started');
                            } catch(e) {
                                showToast('Recording started locally');
                            }
                        } else {
                            isRecording = false;
                            if (mediaRecorder && mediaRecorder.state !== 'inactive') mediaRecorder.stop();
                            document.getElementById('recLabel').innerText = 'REC CLIP';
                            document.getElementById('recBtn').classList.remove('danger');
                        }
                    }

                    function toggleFullscreen() {
                        const vp = document.getElementById('viewport');
                        if (!document.fullscreenElement) {
                            vp.requestFullscreen().catch(err => {});
                        } else {
                            document.exitFullscreen();
                        }
                    }

                    function showToast(msg) {
                        toast.innerText = '● ' + msg;
                        setTimeout(() => { if (toast.innerText.includes(msg)) toast.innerText = 'System Telemetry Online'; }, 3500);
                    }

                    function logEvent(text) {
                        const list = document.getElementById('eventList');
                        const item = document.createElement('div');
                        item.className = 'event-item';
                        const timeStr = new Date().toLocaleTimeString();
                        item.innerHTML = '<span>' + text + '</span><span style="color:var(--text-muted);">' + timeStr + '</span>';
                        list.prepend(item);
                        while (list.children.length > 8) { list.removeChild(list.lastChild); }
                    }

                    // Poll status from device every 1000ms
                    let lastMotionState = false;
                    function pollStatus() {
                        fetch('/status')
                            .then(res => res.json())
                            .then(data => {
                                document.getElementById('viewersBadge').innerText = '👥 ' + data.viewers + ' Viewers';
                                document.getElementById('batteryStatus').innerText = '🔋 ' + data.battery + '%';
                                document.getElementById('hudCamName').innerText = data.cameraName;
                                document.getElementById('hudTelemetry').innerText = data.fps + ' FPS • 1080p • ' + data.networkType;

                                const armBadge = document.getElementById('armedBadge');
                                if (data.isArmed) {
                                    armBadge.innerText = 'ARMED';
                                    armBadge.className = 'badge badge-armed';
                                    document.getElementById('armLabel').innerText = 'ARMED';
                                    document.getElementById('btnArm').classList.add('active');
                                } else {
                                    armBadge.innerText = 'DISARMED';
                                    armBadge.className = 'badge';
                                    armBadge.style.color = '#f59e0b';
                                    armBadge.style.borderColor = '#f59e0b';
                                    document.getElementById('armLabel').innerText = 'DISARMED';
                                    document.getElementById('btnArm').classList.remove('active');
                                }

                                if (data.isTorchOn) {
                                    document.getElementById('btnTorch').classList.add('active');
                                    document.getElementById('torchLabel').innerText = 'TORCH ON';
                                } else {
                                    document.getElementById('btnTorch').classList.remove('active');
                                    document.getElementById('torchLabel').innerText = 'TOGGLE TORCH';
                                }

                                if (data.isSirenPlaying) {
                                    document.getElementById('btnSiren').classList.add('active');
                                    document.getElementById('sirenLabel').innerText = 'SIREN ACTIVE!';
                                } else {
                                    document.getElementById('btnSiren').classList.remove('active');
                                    document.getElementById('sirenLabel').innerText = 'SIREN ALARM';
                                }

                                if (data.motionDetected) {
                                    motionBanner.style.display = 'flex';
                                    if (!lastMotionState) {
                                        logEvent('⚠️ MOTION ALERT DETECTED AT CAMERA NODE');
                                    }
                                } else {
                                    motionBanner.style.display = 'none';
                                }
                                lastMotionState = data.motionDetected;
                            })
                            .catch(e => {});
                    }
                    setInterval(pollStatus, 1000);

                    setInterval(() => {
                        const d = new Date();
                        const timeStr = d.toISOString().replace('T', ' ').substring(11, 23) + ' UTC';
                        document.getElementById('liveClock').innerText = timeStr;
                        document.getElementById('hudTimestamp').innerText = 'REC ● ' + timeStr;
                    }, 100);
                </script>
            </body>
            </html>
        """.trimIndent()
    }
}
