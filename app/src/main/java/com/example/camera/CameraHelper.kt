package com.example.camera

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraHelper(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val onFrameAnalyzed: (Bitmap) -> Unit
) {
    private val TAG = "CameraHelper"
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var surfaceProvider: Preview.SurfaceProvider? = null

    var isBackCamera: Boolean = true
        private set

    var isTorchEnabled: Boolean = false
        private set

    fun startCamera(
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Camera initialization failed", e)
                onError(e.message ?: "Camera initialization failed")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun bindPreviewSurface(provider: Preview.SurfaceProvider?) {
        surfaceProvider = provider
        bindCameraUseCases()
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return
        val lensFacing = if (isBackCamera) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .apply {
                setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageProxy(imageProxy)
                }
            }

        val useCases = mutableListOf<androidx.camera.core.UseCase>(imageAnalysis)

        surfaceProvider?.let { sProvider ->
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(sProvider)
            }
            useCases.add(preview)
        }

        try {
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                *useCases.toTypedArray()
            )
            // Restore torch state if it was enabled
            if (isTorchEnabled && camera?.cameraInfo?.hasFlashUnit() == true) {
                camera?.cameraControl?.enableTorch(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind camera use cases: ${e.message}")
        }
    }

    private fun processImageProxy(image: ImageProxy) {
        try {
            // CameraX built-in toBitmap handles all YUV/RGB strides, rotation, and formats
            val bitmap = image.toBitmap()
            onFrameAnalyzed(bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Frame conversion error: ${e.message}")
        } finally {
            image.close()
        }
    }

    fun toggleCameraLens() {
        isBackCamera = !isBackCamera
        bindCameraUseCases()
    }

    fun setTorch(enable: Boolean): Boolean {
        camera?.let { cam ->
            if (cam.cameraInfo.hasFlashUnit()) {
                isTorchEnabled = enable
                cam.cameraControl.enableTorch(enable)
                return isTorchEnabled
            }
        }
        isTorchEnabled = false
        return false
    }

    fun toggleTorch(): Boolean {
        return setTorch(!isTorchEnabled)
    }

    fun shutdown() {
        try {
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down camera helper: ${e.message}")
        }
    }
}
