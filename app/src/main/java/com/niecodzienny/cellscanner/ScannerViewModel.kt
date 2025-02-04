package com.niecodzienny.cellscanner

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.math.roundToInt

// Importy dla Camera2 interop
import androidx.camera.camera2.interop.Camera2CameraInfo
import android.hardware.camera2.CameraCharacteristics

sealed class UiState {
    object NoPermission : UiState()
    object Scanning : UiState()
    data class CodeScanned(val code: String, val decodedInfo: Map<String, String>?) : UiState()
    object Idle : UiState()
    object Error : UiState()
}

class ScannerViewModel : ViewModel() {

    var uiState by mutableStateOf<UiState>(UiState.Idle)
        private set

    private var previewView: PreviewView? = null
    private var currentActivity: ComponentActivity? = null

    private var cameraPermissionGranted = false
    // Domyślnie używamy tylnej kamery (tryb BACK)
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    // Definiujemy tryby kamery
    private enum class CameraMode {
        BACK,
        TELEPHOTO,
        FRONT
    }
    // Aktualny tryb – początkowo BACK
    private var currentCameraMode = CameraMode.BACK

    // Właściwość przechowująca aktualną wartość ogniskowej (w mm)
    var currentFocalLength by mutableStateOf<Float?>(null)
        private set

    private var isAnalyzing = false
    private val executor = Executors.newSingleThreadExecutor()

    // Obszar skanowania – odpowiada ramce wyświetlanej w UI
    private var scanningArea: Rect? = null

    fun hasCameraPermission() = cameraPermissionGranted

    fun onPermissionDenied() {
        uiState = UiState.NoPermission
    }

    fun setPermissionGranted(granted: Boolean) {
        cameraPermissionGranted = granted
    }

    /**
     * Przypisuje podgląd kamery i ustawia obszar skanowania.
     *
     * Aby pozycja obszaru skanowania odpowiadała wyświetlanej ramce (w UI mamy ramkę
     * ustawioną z paddingiem top = 100.dp), pobieramy lokalizację PreviewView na ekranie.
     *
     * desiredScanningTopDp – wartość (w dp) określająca pozycję ramki w UI.
     */
    fun attachPreviewView(previewView: PreviewView, activity: ComponentActivity) {
        this.previewView = previewView
        this.currentActivity = activity
        previewView.post {
            val density = previewView.resources.displayMetrics.density
            val frameSizePx = (200 * density).toInt()
            // Wartość taka sama jak w UI (padding top = 100.dp)
            val desiredScanningTopDp = 100
            val desiredScanningTopPx = (desiredScanningTopDp * density).toInt()
            // Pobieramy lokalizację PreviewView na ekranie
            val location = IntArray(2)
            previewView.getLocationOnScreen(location)
            val viewTop = location[1]
            // Obliczamy pozycję ramki w układzie PreviewView
            // Jeśli PreviewView nie zaczyna się od 0, korygujemy tę wartość
            val adjustedTop = desiredScanningTopPx - viewTop
            val left = (previewView.width - frameSizePx) / 2
            val top = adjustedTop
            val right = left + frameSizePx
            val bottom = top + frameSizePx
            scanningArea = Rect(left, top, right, bottom)
            println("DEBUG: scanningArea=$scanningArea, viewTop=$viewTop, desiredScanningTopPx=$desiredScanningTopPx")
        }
    }

    fun rescan() {
        uiState = UiState.Scanning
    }

    fun startCamera() {
        if (!cameraPermissionGranted) {
            uiState = UiState.NoPermission
            return
        }
        uiState = UiState.Scanning
    }

    @OptIn(ExperimentalGetImage::class)
    suspend fun bindCameraUseCases() = withContext(Dispatchers.Main) {
        val activity = currentActivity ?: return@withContext
        val previewView = this@ScannerViewModel.previewView ?: return@withContext

        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(previewView.display.rotation)
            .build().also { analyzer ->
                analyzer.setAnalyzer(executor) { imageProxy ->
                    if (isAnalyzing) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    isAnalyzing = true

                    val contrast = 1.2f
                    val nv21bytes = convertImageProxyToNV21WithContrast(imageProxy, contrast)
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    val imageWidth = imageProxy.width
                    val imageHeight = imageProxy.height
                    val previewWidth = previewView.width
                    val previewHeight = previewView.height

                    val scale = minOf(
                        previewWidth.toFloat() / imageWidth.toFloat(),
                        previewHeight.toFloat() / imageHeight.toFloat()
                    )
                    val offsetX = ((previewWidth - imageWidth * scale) / 2f)
                    val offsetY = ((previewHeight - imageHeight * scale) / 2f)

                    val image = InputImage.fromByteArray(
                        nv21bytes,
                        imageWidth,
                        imageHeight,
                        rotationDegrees,
                        InputImage.IMAGE_FORMAT_NV21
                    )

                    val scanner = BarcodeScanning.getClient()
                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            if (barcodes.isEmpty()) {
                                println("DEBUG: Brak wykrytych kodów w klatce.")
                            }

                            val finalBarcodes = if (scanningArea != null) {
                                // Sprawdzamy, czy środek bounding boxa znajduje się w obszarze skanowania
                                val filtered = barcodes.filter { barcode ->
                                    val box = barcode.boundingBox ?: return@filter false
                                    val scaledBox = Rect(
                                        (offsetX + box.left * scale).roundToInt(),
                                        (offsetY + box.top * scale).roundToInt(),
                                        (offsetX + box.right * scale).roundToInt(),
                                        (offsetY + box.bottom * scale).roundToInt()
                                    )
                                    val centerX = (scaledBox.left + scaledBox.right) / 2
                                    val centerY = (scaledBox.top + scaledBox.bottom) / 2
                                    val contains = scanningArea?.contains(centerX, centerY) ?: false
                                    if (!contains) {
                                        println("DEBUG: Odrzucono kod. center=($centerX,$centerY), scanningArea=$scanningArea")
                                    }
                                    contains
                                }
                                if (filtered.isEmpty() && barcodes.isNotEmpty()) {
                                    println("DEBUG: Kody wykryte, ale żaden nie mieści się w ramce!")
                                }
                                filtered
                            } else {
                                println("DEBUG: scanningArea jest null, pomijam filtr.")
                                barcodes
                            }

                            val barcode = finalBarcodes.firstOrNull()
                            val rawValue = barcode?.rawValue

                            if (!rawValue.isNullOrEmpty()) {
                                onBarcodeScanned(rawValue)
                            } else {
                                if (barcodes.isNotEmpty() && finalBarcodes.isEmpty()) {
                                    println("DEBUG: Żaden kod nie został przefiltrowany do ramki.")
                                } else {
                                    println("DEBUG: Znaleziono kody, ale rawValue jest puste lub brak kodów.")
                                }
                            }
                        }
                        .addOnFailureListener {
                            uiState = UiState.Error
                            println("DEBUG: Błąd podczas skanowania: ${it.message}")
                        }
                        .addOnCompleteListener {
                            isAnalyzing = false
                            imageProxy.close()
                        }
                }
            }

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            activity,
            cameraSelector,
            preview,
            imageAnalysis
        )
    }

    private fun onBarcodeScanned(code: String) {
        println("DEBUG: Zeskanowano kod: $code")
        if (BatteryQrDecoder.validateCode(code)) {
            val info = BatteryQrDecoder.decodeInformation(code)
            uiState = UiState.CodeScanned(code, info)
        } else {
            uiState = UiState.CodeScanned(code, null)
        }
    }

    fun openUrl(context: Context, code: String) {
        val url = "https://www.gobelpower.com/lifepo4_decoder.html?code=$code"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    /**
     * Przełącza obiektyw – cyklicznie: BACK -> TELEPHOTO (jeśli dostępny) -> FRONT -> BACK.
     * Po zmianie wywołujemy ponowne przypisanie use case'ów oraz aktualizację ogniskowej.
     */
    fun switchCameraLens() {
        viewModelScope.launch {
            when (currentCameraMode) {
                CameraMode.BACK -> {
                    val teleSelector = getTelephotoCameraSelector()
                    if (teleSelector != null) {
                        currentCameraMode = CameraMode.TELEPHOTO
                        cameraSelector = teleSelector
                    } else {
                        currentCameraMode = CameraMode.FRONT
                        cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                    }
                }
                CameraMode.TELEPHOTO -> {
                    currentCameraMode = CameraMode.FRONT
                    cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                }
                CameraMode.FRONT -> {
                    currentCameraMode = CameraMode.BACK
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                }
            }
            bindCameraUseCases()
            updateCurrentFocalLength()
        }
    }

    /**
     * Wyszukuje kamerę tylną o najwyższej wartości ogniskowej (jeśli dostępnych jest więcej niż jedna).
     * Jeśli nie ma więcej niż jednej kamery tylnej, zwracamy null.
     */
    private suspend fun getTelephotoCameraSelector(): CameraSelector? {
        val activity = currentActivity ?: return null
        return withContext(Dispatchers.IO) {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(activity).get()
                val backCameras = cameraProvider.availableCameraInfos.filter { cameraInfo ->
                    val lensFacing = Camera2CameraInfo.from(cameraInfo)
                        .getCameraCharacteristic(CameraCharacteristics.LENS_FACING)
                    lensFacing == CameraSelector.LENS_FACING_BACK
                }
                if (backCameras.size < 2) {
                    return@withContext null
                }
                val teleCameraInfo = backCameras.maxByOrNull { cameraInfo ->
                    val focalArray = Camera2CameraInfo.from(cameraInfo)
                        .getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    focalArray?.maxOrNull() ?: 0f
                } ?: return@withContext null

                CameraSelector.Builder().addCameraFilter { cameraInfos ->
                    cameraInfos.filter { it == teleCameraInfo }
                }.build()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Aktualizuje właściwość currentFocalLength w zależności od aktualnie wybranego trybu kamery.
     */
    suspend fun updateCurrentFocalLength() {
        val activity = currentActivity ?: return
        val cameraProvider = ProcessCameraProvider.getInstance(activity).get()
        val cameraInfos = cameraProvider.availableCameraInfos
        val focal: Float? = when (currentCameraMode) {
            CameraMode.BACK -> {
                val backCameras = cameraInfos.filter {
                    val lensFacing = Camera2CameraInfo.from(it)
                        .getCameraCharacteristic(CameraCharacteristics.LENS_FACING)
                    lensFacing == CameraSelector.LENS_FACING_BACK
                }
                if (backCameras.isNotEmpty()) {
                    // Dla trybu BACK wybieramy kamerę o najniższej ogniskowej (szerokokątną)
                    val cameraInfo = backCameras.minByOrNull {
                        val focalArray = Camera2CameraInfo.from(it)
                            .getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                        focalArray?.minOrNull() ?: Float.MAX_VALUE
                    }
                    val focalArray = cameraInfo?.let {
                        Camera2CameraInfo.from(it)
                            .getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    }
                    focalArray?.minOrNull()
                } else null
            }
            CameraMode.TELEPHOTO -> {
                val backCameras = cameraInfos.filter {
                    val lensFacing = Camera2CameraInfo.from(it)
                        .getCameraCharacteristic(CameraCharacteristics.LENS_FACING)
                    lensFacing == CameraSelector.LENS_FACING_BACK
                }
                if (backCameras.size >= 2) {
                    val cameraInfo = backCameras.maxByOrNull {
                        val focalArray = Camera2CameraInfo.from(it)
                            .getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                        focalArray?.maxOrNull() ?: 0f
                    }
                    val focalArray = cameraInfo?.let {
                        Camera2CameraInfo.from(it)
                            .getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    }
                    focalArray?.maxOrNull()
                } else null
            }
            CameraMode.FRONT -> {
                val frontCameras = cameraInfos.filter {
                    val lensFacing = Camera2CameraInfo.from(it)
                        .getCameraCharacteristic(CameraCharacteristics.LENS_FACING)
                    lensFacing == CameraSelector.LENS_FACING_FRONT
                }
                if (frontCameras.isNotEmpty()) {
                    val cameraInfo = frontCameras.first()
                    val focalArray = Camera2CameraInfo.from(cameraInfo)
                        .getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    focalArray?.firstOrNull()
                } else null
            }
        }
        currentFocalLength = focal
    }

    /**
     * Konwertuje ImageProxy do formatu NV21 oraz zwiększa kontrast kanału Y.
     */
    private fun convertImageProxyToNV21WithContrast(imageProxy: ImageProxy, contrast: Float): ByteArray {
        val width = imageProxy.width
        val height = imageProxy.height
        val ySize = width * height
        val uvSize = width * height / 4
        val nv21 = ByteArray(ySize + uvSize * 2)
        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer
        // Kopiujemy dane z kanału Y
        yBuffer.get(nv21, 0, ySize)
        // Zwiększamy kontrast kanału Y
        for (i in 0 until ySize) {
            val y = nv21[i].toInt() and 0xFF
            val yShifted = y - 128
            val yContrasted = (yShifted * contrast).toInt() + 128
            nv21[i] = yContrasted.coerceIn(0, 255).toByte()
        }
        val uvBufferPos = ySize
        val uBytes = ByteArray(uvSize)
        val vBytes = ByteArray(uvSize)
        uBuffer.get(uBytes, 0, uvSize)
        vBuffer.get(vBytes, 0, uvSize)
        for (i in 0 until uvSize) {
            nv21[uvBufferPos + i * 2] = vBytes[i]
            nv21[uvBufferPos + i * 2 + 1] = uBytes[i]
        }
        return nv21
    }
}
