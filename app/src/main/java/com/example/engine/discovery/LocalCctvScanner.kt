package com.example.engine.discovery

/**
 * OmniCam CCTV & Hardware Device Discovery Engine
 * Created by Rei (777rei / reialwaysfreemium)
 * Privacy-First On-Device Architecture
 */

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

data class DiscoveredCamera(
    val id: String,
    val name: String,
    val ip: String,
    val port: Int,
    val protocol: String, // "RTSP", "MJPEG", "HTTP", "USB", "PHONE"
    val streamUrl: String,
    val deviceType: String // "IP Camera", "USB Webcam", "OmniCam Phone", "ONVIF CCTV"
)

data class UsbCameraInfo(
    val deviceId: Int,
    val deviceName: String,
    val vendorId: Int,
    val productId: Int,
    val isVideoDevice: Boolean
)

class LocalCctvScanner(private val context: Context) {

    private val TAG = "LocalCctvScanner"

    /**
     * Scans connected USB video devices and external camera hardware.
     */
    fun scanUsbCameras(): List<DiscoveredCamera> {
        val discovered = mutableListOf<DiscoveredCamera>()

        // 1. Scan via Android UsbManager (UVC Video Class)
        try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
            usbManager?.deviceList?.values?.forEach { usbDevice ->
                var isVideo = false
                // Check device class or interface class for Video (14 = USB_CLASS_VIDEO)
                if (usbDevice.deviceClass == UsbConstants.USB_CLASS_VIDEO || usbDevice.deviceClass == 239) {
                    isVideo = true
                }
                for (i in 0 until usbDevice.interfaceCount) {
                    val iface = usbDevice.getInterface(i)
                    if (iface.interfaceClass == UsbConstants.USB_CLASS_VIDEO) {
                        isVideo = true
                        break
                    }
                }
                val label = usbDevice.productName?.takeIf { it.isNotBlank() } ?: "USB Video Device (${usbDevice.deviceName})"
                discovered.add(
                    DiscoveredCamera(
                        id = "usb-${usbDevice.deviceId}",
                        name = label,
                        ip = "USB-HOST",
                        port = 0,
                        protocol = "USB/UVC",
                        streamUrl = "usb://${usbDevice.vendorId}:${usbDevice.productId}",
                        deviceType = if (isVideo) "USB UVC Webcam" else "USB Camera Interface"
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying USB devices: ${e.message}")
        }

        // 2. Scan via CameraManager for External USB Cameras (Android Camera2 API)
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            cameraManager?.cameraIdList?.forEach { camId ->
                val chars = cameraManager.getCameraCharacteristics(camId)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_EXTERNAL) {
                    discovered.add(
                        DiscoveredCamera(
                            id = "ext-cam-$camId",
                            name = "External USB Camera #$camId",
                            ip = "CAMERA2-EXT",
                            port = 0,
                            protocol = "USB/UVC",
                            streamUrl = "camera2://$camId",
                            deviceType = "Plugged USB Camera"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking external Camera2 devices: ${e.message}")
        }

        return discovered
    }

    /**
     * Scans local Wi-Fi subnet for RTSP, ONVIF, and MJPEG network CCTV cameras.
     */
    suspend fun scanLocalSubnet(
        localIp: String,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): List<DiscoveredCamera> = withContext(Dispatchers.IO) {
        val discovered = mutableListOf<DiscoveredCamera>()
        val parts = localIp.split(".")
        if (parts.size != 4) return@withContext discovered

        val subnetPrefix = "${parts[0]}.${parts[1]}.${parts[2]}."
        val myLastOctet = parts[3].toIntOrNull() ?: 1

        // Scan priority hosts: gateway (.1), nearby hosts (.2 to .50), and common CCTV IPs (.100 to .125, .200 to .220)
        val candidateIps = mutableListOf<String>()
        candidateIps.add("${subnetPrefix}1") // Gateway / Router CCTV Hub
        for (i in 2..40) {
            if (i != myLastOctet) candidateIps.add("$subnetPrefix$i")
        }
        for (i in 100..120) {
            if (i != myLastOctet) candidateIps.add("$subnetPrefix$i")
        }
        for (i in 200..215) {
            if (i != myLastOctet) candidateIps.add("$subnetPrefix$i")
        }

        val ports = listOf(554, 8080, 80, 8000, 8899) // RTSP, HTTP-MJPEG, Web-CCTV, ONVIF/Hikvision

        coroutineScope {
            val tasks = candidateIps.mapIndexed { index, host ->
                async {
                    for (port in ports) {
                        if (isPortOpen(host, port, timeoutMs = 200)) {
                            val protocol = when (port) {
                                554 -> "RTSP"
                                8080 -> "MJPEG"
                                8000, 8899 -> "ONVIF"
                                else -> "HTTP"
                            }
                            val streamUrl = when (port) {
                                554 -> "rtsp://$host:554/live"
                                8080 -> "http://$host:8080/live"
                                else -> "http://$host:$port/video"
                            }
                            val devName = when (port) {
                                554 -> "RTSP CCTV [$host]"
                                8080 -> "OmniCam Node [$host]"
                                8000, 8899 -> "ONVIF Security Cam [$host]"
                                else -> "IP Camera [$host]"
                            }
                            synchronized(discovered) {
                                discovered.add(
                                    DiscoveredCamera(
                                        id = "net-$host-$port",
                                        name = devName,
                                        ip = host,
                                        port = port,
                                        protocol = protocol,
                                        streamUrl = streamUrl,
                                        deviceType = "$protocol CCTV Device"
                                    )
                                )
                            }
                            break // Found a camera service on this host
                        }
                    }
                    onProgress(index + 1, candidateIps.size)
                }
            }
            tasks.awaitAll()
        }

        discovered
    }

    private fun isPortOpen(host: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}
