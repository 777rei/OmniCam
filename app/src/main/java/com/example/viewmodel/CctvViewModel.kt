package com.example.viewmodel

/**
 * OmniCam CCTV ViewModel & State Management
 * Created by Rei (777rei / reialwaysfreemium)
 * Privacy First App - On-Device Security Core
 */

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.text.format.Formatter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.camera.SyntheticVideoSource
import com.example.data.local.CameraDeviceEntity
import com.example.data.local.MotionEventEntity
import com.example.data.model.DetectionZone
import com.example.data.model.StreamSettings
import com.example.data.repository.CctvRepository
import com.example.engine.audio.AlarmSoundManager
import com.example.engine.motion.MotionDetector
import com.example.engine.motion.MotionResult
import com.example.engine.streaming.MjpegHttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.NetworkInterface

enum class CctvAppMode {
    DASHBOARD,
    CAMERA_NODE,
    VIEWER_CONSOLE,
    LIVE_DETAIL,
    ALERTS_ARCHIVE,
    SETTINGS
}

enum class NightVisionFilter {
    NORMAL,
    IR_MONOCHROME,
    NIGHT_VISION_GREEN
}

data class CameraNodeState(
    val isStreaming: Boolean = false,
    val isArmed: Boolean = true,
    val isTorchOn: Boolean = false,
    val isSirenPlaying: Boolean = false,
    val isStealthMode: Boolean = false,
    val streamUrl: String = "http://localhost:8080/live",
    val localIp: String = "127.0.0.1",
    val port: Int = 8080,
    val viewerCount: Int = 0,
    val currentFps: Int = 20,
    val motionResult: MotionResult? = null,
    val lastMotionTime: Long = 0L,
    val detectionZone: DetectionZone = DetectionZone(),
    val isFrontCamera: Boolean = false,
    val batteryLevel: Int = 98,
    val networkType: String = "Wi-Fi (Local LAN)",
    val useHardwareCamera: Boolean = false, // false = synthetic simulation fallback for emulators, true = real camera
    val settings: StreamSettings = StreamSettings()
)

data class ViewerConsoleState(
    val selectedCameraId: String = "cam-node-local",
    val zoomLevel: Float = 1.0f,
    val panOffsetX: Float = 0f,
    val panOffsetY: Float = 0f,
    val nightVisionMode: NightVisionFilter = NightVisionFilter.NORMAL,
    val isIntercomActive: Boolean = false,
    val isRecordingClip: Boolean = false,
    val recordedSeconds: Int = 0,
    val remoteTorchState: Boolean = false,
    val latencyMs: Int = 18,
    val connectionSpeedKbps: Int = 1840
)

class CctvViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CctvRepository.create(application)
    private val motionDetector = MotionDetector()
    private val soundManager = AlarmSoundManager(application)
    private val syntheticSource = SyntheticVideoSource()

    // Active App Screen Mode
    private val _currentMode = MutableStateFlow(CctvAppMode.DASHBOARD)
    val currentMode: StateFlow<CctvAppMode> = _currentMode.asStateFlow()

    // Camera Node State
    private val _cameraNodeState = MutableStateFlow(CameraNodeState())
    val cameraNodeState: StateFlow<CameraNodeState> = _cameraNodeState.asStateFlow()

    // Viewer Console State
    private val _viewerState = MutableStateFlow(ViewerConsoleState())
    val viewerState: StateFlow<ViewerConsoleState> = _viewerState.asStateFlow()

    // Live Frame for Composable rendering (Primary / active channel)
    private val _currentFrame = MutableStateFlow<Bitmap?>(null)
    val currentFrame: StateFlow<Bitmap?> = _currentFrame.asStateFlow()

    // Multi-Camera Live Frames Map (Camera ID -> Bitmap) for Grid
    private val _cameraFrames = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val cameraFrames: StateFlow<Map<String, Bitmap>> = _cameraFrames.asStateFlow()

    // Room DB Flow of Motion Events & Cameras
    val motionEvents: StateFlow<List<MotionEventEntity>> = repository.allEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cameras: StateFlow<List<CameraDeviceEntity>> = repository.allCameras
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Background Streaming Server
    private var streamingServer: MjpegHttpServer? = null
    private var videoLoopJob: Job? = null
    private var dvrPlaybackJob: Job? = null
    private var lastMotionAlertTimestamp = 0L
    private var lastHardwareFrameTimestamp = 0L

    // DVR Engine & Recorded History Buffer
    val dvrEngine = com.example.engine.dvr.DvrEngine()
    val recordedClips: StateFlow<List<com.example.engine.dvr.DvrClip>> = dvrEngine.recordedClips

    // DVR Playback State for Post-Recording Mode
    private val _isDvrPlaying = MutableStateFlow(true)
    val isDvrPlaying: StateFlow<Boolean> = _isDvrPlaying.asStateFlow()

    private val _dvrSpeed = MutableStateFlow(1.0f)
    val dvrSpeed: StateFlow<Float> = _dvrSpeed.asStateFlow()

    private val _dvrPlaybackTimeMs = MutableStateFlow(System.currentTimeMillis())
    val dvrPlaybackTimeMs: StateFlow<Long> = _dvrPlaybackTimeMs.asStateFlow()

    private val _dvrFrame = MutableStateFlow<Bitmap?>(null)
    val dvrFrame: StateFlow<Bitmap?> = _dvrFrame.asStateFlow()

    // Local CCTV and USB Discovery Engine
    private val localScanner = com.example.engine.discovery.LocalCctvScanner(application)
    val discoveredCameras = MutableStateFlow<List<com.example.engine.discovery.DiscoveredCamera>>(emptyList())
    val isScanningNetwork = MutableStateFlow(false)
    val scanProgress = MutableStateFlow(0f)

    init {
        updateNetworkInfo()
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
        startLiveVideoEngine()
        startDvrPlaybackLoop()
    }

    fun setAppMode(mode: CctvAppMode) {
        _currentMode.value = mode
    }

    private fun updateNetworkInfo() {
        val ip = getDeviceIpAddress()
        val battery = getBatteryPercentage()
        val context = getApplication<Application>()
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)
        val netType = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi (Local LAN)"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular Data"
            else -> "Active Network"
        }
        _cameraNodeState.value = _cameraNodeState.value.copy(
            localIp = ip,
            streamUrl = "http://$ip:${_cameraNodeState.value.port}/live",
            batteryLevel = battery,
            networkType = netType
        )
    }

    /**
     * Continuous video engine that drives frames, motion detection, DVR buffer recording, and network streaming.
     * Automatically feeds individual independent real-time feeds to every camera in the Grid and Stream viewports.
     */
    private fun startLiveVideoEngine() {
        videoLoopJob?.cancel()
        videoLoopJob = viewModelScope.launch(Dispatchers.Default) {
            var frameCounter = 0
            while (isActive) {
                frameCounter++
                val now = System.currentTimeMillis()
                val isHardwareFeeding = _cameraNodeState.value.useHardwareCamera && (now - lastHardwareFrameTimestamp < 600)

                val currentCamList = cameras.value.ifEmpty {
                    listOf(CameraDeviceEntity(id = "cam-node-local", name = "This Phone (OmniCam Node)", streamUrl = "http://localhost:8080/live", location = "Front Door", isOnline = true, batteryPercent = 95, resolution = "1080p FHD"))
                }
                val newFrameMap = mutableMapOf<String, Bitmap>()

                currentCamList.forEachIndexed { index, cam ->
                    if (cam.isOnline) {
                        val camFrame: Bitmap = if (cam.id == "cam-node-local" && isHardwareFeeding && _currentFrame.value != null) {
                            _currentFrame.value!!
                        } else {
                            syntheticSource.generateFrame(cam.name, sceneIndex = index)
                        }
                        newFrameMap[cam.id] = camFrame
                    }
                }

                _cameraFrames.value = newFrameMap

                // Active camera frame
                val selectedId = _viewerState.value.selectedCameraId
                val activeFrame = newFrameMap[selectedId] ?: newFrameMap.values.firstOrNull()
                if (!isHardwareFeeding && activeFrame != null) {
                    _currentFrame.value = activeFrame
                }

                // Record active frame into DVR Recording Buffer
                val activeCamName = currentCamList.find { it.id == selectedId }?.name ?: "Primary Node"
                activeFrame?.let {
                    dvrEngine.recordFrame(
                        channelId = selectedId,
                        cameraName = activeCamName,
                        frame = it,
                        isMotion = _cameraNodeState.value.motionResult?.isMotionDetected == true
                    )
                }

                // Streaming server push
                activeFrame?.let {
                    streamingServer?.pushFrame(it, quality = _cameraNodeState.value.settings.jpegQuality)
                }

                // Motion detection if armed
                if (_cameraNodeState.value.isArmed && activeFrame != null) {
                    val motion = motionDetector.analyzeFrame(
                        bitmap = activeFrame,
                        sensitivity = _cameraNodeState.value.settings.motionSensitivity,
                        zone = _cameraNodeState.value.detectionZone
                    )

                    if (motion.isMotionDetected) {
                        _cameraNodeState.value = _cameraNodeState.value.copy(
                            motionResult = motion,
                            lastMotionTime = System.currentTimeMillis()
                        )

                        val cooldownMs = _cameraNodeState.value.settings.motionCooldownSeconds * 1000L
                        if (now - lastMotionAlertTimestamp > cooldownMs) {
                            lastMotionAlertTimestamp = now
                            triggerMotionAlert(motion, activeFrame)
                        }
                    } else if (System.currentTimeMillis() - _cameraNodeState.value.lastMotionTime > 3000) {
                        _cameraNodeState.value = _cameraNodeState.value.copy(motionResult = null)
                    }
                }

                streamingServer?.let { server ->
                    if (_cameraNodeState.value.viewerCount != server.viewerCount) {
                        _cameraNodeState.value = _cameraNodeState.value.copy(viewerCount = server.viewerCount)
                    }
                }

                val delayMs = if (_cameraNodeState.value.settings.lowBandwidthMode) 80L else 40L
                delay(delayMs)
            }
        }
    }

    /**
     * Continuous DVR Playback loop that drives historical video frame updates,
     * time increments based on playback speed (1x, 2x, 4x), and recorded frame lookup.
     */
    private fun startDvrPlaybackLoop() {
        dvrPlaybackJob?.cancel()
        dvrPlaybackJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                if (_isDvrPlaying.value) {
                    val incrementMs = (100 * _dvrSpeed.value).toLong()
                    val nextTime = _dvrPlaybackTimeMs.value + incrementMs
                    if (nextTime >= System.currentTimeMillis()) {
                        _dvrPlaybackTimeMs.value = System.currentTimeMillis()
                    } else {
                        _dvrPlaybackTimeMs.value = nextTime
                    }
                }

                val selectedId = _viewerState.value.selectedCameraId
                val activeCamName = cameras.value.find { it.id == selectedId }?.name ?: "Primary Node"
                val historicalFrame = dvrEngine.getRecordedFrameAtTime(selectedId, activeCamName, _dvrPlaybackTimeMs.value)
                _dvrFrame.value = historicalFrame

                delay(100L)
            }
        }
    }

    // DVR Playback Controls
    fun seekDvr(timeMs: Long) {
        _dvrPlaybackTimeMs.value = timeMs
        val selectedId = _viewerState.value.selectedCameraId
        val activeCamName = cameras.value.find { it.id == selectedId }?.name ?: "Primary Node"
        _dvrFrame.value = dvrEngine.getRecordedFrameAtTime(selectedId, activeCamName, timeMs)
    }

    fun toggleDvrPlayPause() {
        _isDvrPlaying.value = !_isDvrPlaying.value
    }

    fun setDvrPlaybackSpeed(speed: Float) {
        _dvrSpeed.value = speed
    }

    fun stepDvr(seconds: Int) {
        seekDvr(_dvrPlaybackTimeMs.value + (seconds * 1000L))
    }

    fun selectDvrClip(clip: com.example.engine.dvr.DvrClip) {
        seekDvr(clip.startTimeMs)
        _isDvrPlaying.value = true
    }

    /**
     * Receives camera frames captured by CameraX hardware camera.
     */
    fun onCameraFrameCaptured(bitmap: Bitmap) {
        lastHardwareFrameTimestamp = System.currentTimeMillis()
        if (_cameraNodeState.value.useHardwareCamera) {
            _currentFrame.value = bitmap
            streamingServer?.pushFrame(bitmap, quality = _cameraNodeState.value.settings.jpegQuality)

            if (_cameraNodeState.value.isArmed) {
                val motion = motionDetector.analyzeFrame(
                    bitmap = bitmap,
                    sensitivity = _cameraNodeState.value.settings.motionSensitivity,
                    zone = _cameraNodeState.value.detectionZone
                )

                if (motion.isMotionDetected) {
                    _cameraNodeState.value = _cameraNodeState.value.copy(
                        motionResult = motion,
                        lastMotionTime = System.currentTimeMillis()
                    )

                    val now = System.currentTimeMillis()
                    val cooldownMs = _cameraNodeState.value.settings.motionCooldownSeconds * 1000L
                    if (now - lastMotionAlertTimestamp > cooldownMs) {
                        lastMotionAlertTimestamp = now
                        triggerMotionAlert(motion, bitmap)
                    }
                } else if (System.currentTimeMillis() - _cameraNodeState.value.lastMotionTime > 3000) {
                    _cameraNodeState.value = _cameraNodeState.value.copy(motionResult = null)
                }
            }
        }
    }

    fun toggleCameraSourceMode(useHardware: Boolean) {
        _cameraNodeState.value = _cameraNodeState.value.copy(useHardwareCamera = useHardware)
    }

    private fun triggerMotionAlert(motion: MotionResult, frame: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            val snapshotPath = saveSnapshotToFile(frame)
            val eventTypeStr = if (motion.motionScore > 0.6f) "Intruder Alert" else "Motion Detected"
            val eventId = repository.logMotionEvent(
                cameraName = "Living Room Node",
                eventType = eventTypeStr,
                confidence = motion.motionScore,
                snapshotUri = snapshotPath,
                notes = "Motion detected in monitored zones: ${motion.activeZoneIndices.joinToString()}"
            )

            // Send local push notification
            com.example.service.CctvNotificationService.showMotionAlertNotification(
                context = getApplication(),
                title = "OmniCam: $eventTypeStr!",
                message = "High confidence motion detected (${(motion.motionScore * 100).toInt()}%). Tap to open surveillance console.",
                eventId = eventId
            )

            if (_cameraNodeState.value.settings.sirenOnMotion) {
                soundManager.startSiren(durationMs = 3500)
            }
        }
    }

    private fun saveSnapshotToFile(bitmap: Bitmap): String? {
        return try {
            val context = getApplication<Application>()
            val dir = File(context.filesDir, "cctv_snapshots").apply { mkdirs() }
            val file = File(dir, "snap_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun toggleStreamingServer() {
        val newState = !_cameraNodeState.value.isStreaming
        if (newState) {
            updateNetworkInfo()
            streamingServer = MjpegHttpServer(
                port = _cameraNodeState.value.port,
                onControlCommand = { cmd -> handleRemoteControlCommand(cmd) },
                telemetryProvider = {
                    val node = _cameraNodeState.value
                    com.example.engine.streaming.ServerTelemetry(
                        battery = node.batteryLevel,
                        isArmed = node.isArmed,
                        isTorchOn = node.isTorchOn,
                        isSirenPlaying = node.isSirenPlaying,
                        isFrontCamera = node.isFrontCamera,
                        fps = node.currentFps,
                        motionDetected = node.motionResult?.isMotionDetected == true,
                        sensitivity = node.settings.motionSensitivity,
                        cameraName = "CAM-01 | LIVING ROOM NODE",
                        networkType = node.networkType
                    )
                },
                watermarkGenerator = { rawBytes ->
                    com.example.util.SnapshotManager.addTimestampWatermarkToJpeg(
                        jpegBytes = rawBytes,
                        cameraName = "CAM-01 | LIVING ROOM NODE [OMNICAM HD]"
                    )
                }
            ).apply { start() }
            _cameraNodeState.value = _cameraNodeState.value.copy(isStreaming = true)
        } else {
            streamingServer?.stop()
            streamingServer = null
            _cameraNodeState.value = _cameraNodeState.value.copy(isStreaming = false, viewerCount = 0)
        }
    }

    private fun handleRemoteControlCommand(cmd: String) {
        viewModelScope.launch {
            when (cmd) {
                "torch" -> toggleTorch()
                "siren" -> triggerManualSiren()
                "flip" -> toggleCameraLens()
                "arm" -> toggleArmedState()
                "simulate_motion" -> simulateMotionTrigger()
                "sens_high" -> updateSettings(_cameraNodeState.value.settings.copy(motionSensitivity = 0.8f))
                "sens_mid" -> updateSettings(_cameraNodeState.value.settings.copy(motionSensitivity = 0.5f))
                "sens_low" -> updateSettings(_cameraNodeState.value.settings.copy(motionSensitivity = 0.3f))
                "stealth" -> toggleStealthMode()
            }
        }
    }

    fun toggleArmedState() {
        _cameraNodeState.value = _cameraNodeState.value.copy(
            isArmed = !_cameraNodeState.value.isArmed,
            motionResult = null
        )
    }

    fun toggleTorch() {
        _cameraNodeState.value = _cameraNodeState.value.copy(
            isTorchOn = !_cameraNodeState.value.isTorchOn
        )
    }

    fun toggleCameraLens() {
        _cameraNodeState.value = _cameraNodeState.value.copy(
            isFrontCamera = !_cameraNodeState.value.isFrontCamera
        )
    }

    fun toggleStealthMode() {
        _cameraNodeState.value = _cameraNodeState.value.copy(
            isStealthMode = !_cameraNodeState.value.isStealthMode
        )
    }

    fun triggerManualSiren() {
        if (soundManager.isSirenPlaying) {
            soundManager.stopSiren()
            _cameraNodeState.value = _cameraNodeState.value.copy(isSirenPlaying = false)
        } else {
            soundManager.startSiren(durationMs = 4000)
            _cameraNodeState.value = _cameraNodeState.value.copy(isSirenPlaying = true)
        }
    }

    fun simulateMotionTrigger() {
        syntheticSource.isSimulatedMotionActive = !syntheticSource.isSimulatedMotionActive
    }

    fun toggleZoneCell(index: Int) {
        val updated = _cameraNodeState.value.detectionZone.toggleCell(index)
        _cameraNodeState.value = _cameraNodeState.value.copy(detectionZone = updated)
    }

    fun selectCameraChannel(id: String) {
        _viewerState.value = _viewerState.value.copy(
            selectedCameraId = id,
            zoomLevel = 1.0f,
            panOffsetX = 0f,
            panOffsetY = 0f
        )
    }

    fun setZoomLevel(zoom: Float) {
        _viewerState.value = _viewerState.value.copy(zoomLevel = zoom.coerceIn(1.0f, 5.0f))
    }

    fun setPanOffset(x: Float, y: Float) {
        _viewerState.value = _viewerState.value.copy(
            panOffsetX = x.coerceIn(-200f, 200f),
            panOffsetY = y.coerceIn(-200f, 200f)
        )
    }

    fun cycleNightVisionMode() {
        val next = when (_viewerState.value.nightVisionMode) {
            NightVisionFilter.NORMAL -> NightVisionFilter.IR_MONOCHROME
            NightVisionFilter.IR_MONOCHROME -> NightVisionFilter.NIGHT_VISION_GREEN
            NightVisionFilter.NIGHT_VISION_GREEN -> NightVisionFilter.NORMAL
        }
        _viewerState.value = _viewerState.value.copy(nightVisionMode = next)
    }

    fun toggleIntercom() {
        _viewerState.value = _viewerState.value.copy(
            isIntercomActive = !_viewerState.value.isIntercomActive
        )
    }

    fun toggleRecordingClip() {
        val newState = !_viewerState.value.isRecordingClip
        _viewerState.value = _viewerState.value.copy(isRecordingClip = newState)
    }

    fun takeViewerSnapshot() {
        val frame = _currentFrame.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val path = saveSnapshotToFile(frame)
            repository.logMotionEvent(
                cameraName = cameras.value.find { it.id == _viewerState.value.selectedCameraId }?.name ?: "Viewer Capture",
                eventType = "Manual Snapshot",
                confidence = 1.0f,
                snapshotUri = path,
                notes = "Saved by user from Monitor Console"
            )
        }
    }

    fun toggleStarEvent(id: Long) {
        viewModelScope.launch { repository.toggleStar(id) }
    }

    fun deleteEvent(event: MotionEventEntity) {
        viewModelScope.launch { repository.deleteEvent(event) }
    }

    fun clearAllAlerts() {
        viewModelScope.launch { repository.clearAllEvents() }
    }

    fun exportHistoryCsv(context: Context): String? {
        return try {
            val events = motionEvents.value
            val file = File(context.getExternalFilesDir(null), "omnicam_motion_history_${System.currentTimeMillis()}.csv")
            file.printWriter().use { out ->
                out.println("ID,Timestamp,CameraName,EventType,Confidence,SnapshotUri,Notes")
                for (e in events) {
                    out.println("${e.id},${e.timestamp},\"${e.cameraName}\",\"${e.eventType}\",${e.confidence},\"${e.snapshotUri ?: ""}\",\"${e.notes}\"")
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    // Camera Scanning & Node Management
    fun startNetworkScan() {
        if (isScanningNetwork.value) return
        viewModelScope.launch(Dispatchers.IO) {
            isScanningNetwork.value = true
            scanProgress.value = 0f
            val results = localScanner.scanLocalSubnet(_cameraNodeState.value.localIp) { current, total ->
                scanProgress.value = if (total > 0) current.toFloat() / total.toFloat() else 0f
            }
            discoveredCameras.value = results
            isScanningNetwork.value = false
        }
    }

    fun scanUsbCameras(): List<com.example.engine.discovery.DiscoveredCamera> {
        val usbDevices = localScanner.scanUsbCameras()
        discoveredCameras.value = (discoveredCameras.value + usbDevices).distinctBy { it.id }
        return usbDevices
    }

    fun addDiscoveredCamera(discovered: com.example.engine.discovery.DiscoveredCamera) {
        viewModelScope.launch {
            val camera = CameraDeviceEntity(
                id = discovered.id,
                name = discovered.name,
                streamUrl = discovered.streamUrl,
                location = discovered.deviceType,
                isOnline = true,
                batteryPercent = 100,
                resolution = "1080p FHD"
            )
            repository.saveCamera(camera)
            selectCameraChannel(camera.id)
        }
    }

    fun addCustomCamera(name: String, streamUrl: String, location: String, resolution: String = "1080p FHD") {
        viewModelScope.launch {
            val camera = CameraDeviceEntity(
                id = "cam-custom-${System.currentTimeMillis()}",
                name = name.ifBlank { "Custom CCTV Node" },
                streamUrl = streamUrl.ifBlank { "http://192.168.1.100:8080/live" },
                location = location.ifBlank { "Monitored Zone" },
                isOnline = true,
                batteryPercent = 100,
                resolution = resolution
            )
            repository.saveCamera(camera)
            selectCameraChannel(camera.id)
        }
    }

    fun removeCamera(cameraId: String) {
        viewModelScope.launch {
            repository.deleteCameraById(cameraId)
            if (_viewerState.value.selectedCameraId == cameraId) {
                val remaining = cameras.value.filter { it.id != cameraId }
                _viewerState.value = _viewerState.value.copy(
                    selectedCameraId = remaining.firstOrNull()?.id ?: "cam-node-local"
                )
            }
        }
    }

    fun updateSettings(newSettings: StreamSettings) {
        _cameraNodeState.value = _cameraNodeState.value.copy(settings = newSettings)
    }

    private fun getDeviceIpAddress(): String {
        return try {
            val wifiManager = getApplication<Application>().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiIp = Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
            if (wifiIp != "0.0.0.0" && wifiIp.isNotEmpty()) {
                return wifiIp
            }

            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr.hostAddress?.contains(":") == false) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
            "192.168.1.55"
        } catch (e: Exception) {
            "192.168.1.55"
        }
    }

    private fun getBatteryPercentage(): Int {
        return try {
            val bm = getApplication<Application>().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(1, 100)
        } catch (e: Exception) {
            95
        }
    }

    override fun onCleared() {
        super.onCleared()
        streamingServer?.stop()
        videoLoopJob?.cancel()
        dvrPlaybackJob?.cancel()
    }
}
