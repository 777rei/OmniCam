<div align="center">

# 🛡️ OmniCam Security Console
### **Advanced Privacy-First Multi-Channel CCTV & Remote Surveillance Engine**
*Created with precision by **Rei (777rei / reialwaysfreemium)***

[![Android SDK](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](https://opensource.org/licenses/MIT)
[![Privacy First](https://img.shields.io/badge/Privacy-Zero_Telemetry-00E5FF?style=for-the-badge)]()

</div>

---

## 🚀 Overview

**OmniCam** is an enterprise-grade, privacy-first mobile surveillance and CCTV management console built natively for Android using **Kotlin** and **Jetpack Compose**. Designed for absolute local data sovereignty, OmniCam transforms any Android device into a professional security node, multi-channel grid monitor, USB UVC camera viewer, Wi-Fi LAN CCTV scanner, and advanced DVR playback workstation without relying on external cloud telemetry.

---

## ✨ Key Architectural Features

### 1. 🌐 Multi-Channel CCTV Grid & Live Viewports
- **Dynamic Layout Switching:** Seamlessly toggle between 1x1, 2x2, and 3x3 multi-stream grids.
- **Independent Stream Feeds:** Every camera node in the grid runs an isolated real-time rendering loop with live FPS, bitrate, and resolution readouts (`30 FPS • 1080p FHD`).
- **Preset Security Zones:** Instant configuration for Main Entrance, Perimeter Driveway, Server Room, and Warehouse Loading Dock.

### 2. ⏪ Authentic Post-Recording DVR Engine
- **Circular Buffer Recording:** Automatically captures and indexes continuous surveillance streams into an on-device circular memory buffer.
- **Timeline Scrubber:** Interactive millisecond-precision timeline with color-coded motion and intruder alert markers.
- **Playback Controls:** Play, pause, fast forward (`1X`, `2X`, `4X`), step `-10s` / `+10s`, and instantly jump back to live view.
- **Archived Clips Tray:** Quick access tray to review flagged intrusion events and security clips.

### 3. 🔍 Local Hardware & Network Discovery
- **Wi-Fi Subnet Scanner:** Asynchronously probes common security ports (`554`, `80`, `8080`, `8000`) across local LAN subnets to discover ONVIF, RTSP, and MJPEG cameras.
- **USB / UVC Camera Support:** Automatically detects and connects external USB webcams and borescopes via Android USB host APIs.
- **Direct RTSP / MJPEG Input:** Connect any custom IP camera or remote phone node securely.

### 4. 🚨 Intelligent Motion Detection & Security Tools
- **On-Device Computer Vision:** Real-time motion and intruder analysis with adjustable sensitivity and detection zones.
- **Siren & Intercom:** Built-in tactical emergency siren generator and two-way intercom audio simulation.
- **Night Vision Filters:** Toggle between Normal, IR Monochrome, and Night Vision Green surveillance filters.
- **Secure Snapshots:** Capture timestamped forensic snapshots directly to the Android Media Gallery with cryptographic watermarks.

---

## 🏗️ Project Structure

```tree
com.example/
├── camera/            # CameraX helper, synthetic multi-scene video source
├── data/
│   ├── local/         # Room Database entities, DAOs, and database builders
│   └── repository/    # CctvRepository for local camera and motion event state
├── engine/
│   ├── audio/         # Emergency siren sound manager
│   ├── discovery/     # Local LAN subnet scanner and USB UVC device enumerator
│   ├── dvr/           # Circular buffer DVR engine and historical playback frames
│   ├── motion/        # Computer vision motion analysis and bounding boxes
│   └── streaming/     # Built-in lightweight MJPEG HTTP streaming server
├── ui/
│   ├── components/    # Reusable viewports, scrubber, PTZ controls, and Add Camera dialogs
│   ├── screens/       # CameraGridScreen, LiveFeedDetailScreen, CameraNodeScreen, EventLogScreen, SettingsScreen
│   └── theme/         # Material 3 dark security console theme & color tokens
└── util/              # Timestamp watermarking and gallery snapshot manager
```

---

## 🛠️ Getting Started & Local Setup

### Prerequisites
- **Android Studio** (Koala or newer recommended)
- **JDK 17** or higher
- Android SDK (compileSdk 34+, minSdk 24+)

### Cloning & Building
1. Clone the repository:
   ```bash
   git clone https://github.com/777rei/OmniCam.git
   cd OmniCam
   ```
2. Open the project folder in Android Studio.
3. Sync Gradle and build the project:
   ```bash
   gradle assembleDebug
   ```
4. Install the generated debug APK on your device:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 🔒 Privacy & Security Guarantee

OmniCam is engineered under a strict **Zero-Telemetry, Privacy-First** doctrine:
- **No Third-Party Trackers:** Zero cloud analytics, crash reporting SDKs, or external data collection.
- **Local Database Only:** All motion event logs, camera configurations, and snapshots remain strictly on-device in encrypted local Room storage.
- **Direct P2P LAN Streaming:** Video streams are broadcasted locally over your private Wi-Fi network.

---

## 📜 License

Distributed under the **MIT License**. See `LICENSE` for more information.

---

<div align="center">
  <p><b>Created with ❤️ by Rei (777rei / reialwaysfreemium)</b></p>
  <p><i>Secure • Private • Skibidi </i></p>
</div>
