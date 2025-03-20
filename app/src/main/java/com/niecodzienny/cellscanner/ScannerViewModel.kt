package com.niecodzienny.cellscanner

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.io.File
import androidx.core.content.FileProvider
import android.widget.Toast

class ScannerViewModel : ViewModel() {

    companion object {
        const val APP_VERSION = "1.0.1"
    }

    var uiState by mutableStateOf<UiState>(UiState.Idle)
        private set

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private var previewView: PreviewView? = null
    private var currentActivity: ComponentActivity? = null

    private var cameraPermissionGranted = false
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    private var hasScanned = false
    private var isAnalyzing = false
    private val executor = Executors.newSingleThreadExecutor()
    private var scanningArea: Rect? = null

    private val PREFS_UNIQUE_CODES = "prefs_unique_codes"
    private val KEY_UNIQUE_CODES = "unique_codes_set"

    private fun logUniqueScan(code: String) {
        val prefs = currentActivity?.getSharedPreferences(PREFS_UNIQUE_CODES, Context.MODE_PRIVATE) ?: return
        val scannedCodes = prefs.getStringSet(KEY_UNIQUE_CODES, mutableSetOf()) ?: mutableSetOf()

        val isUnique = scannedCodes.add(code)
        if (isUnique) {
            prefs.edit().putStringSet(KEY_UNIQUE_CODES, scannedCodes).apply()
            firebaseAnalytics.logEvent("unique_code_scanned", Bundle().apply {
                putInt("unique_codes_count", scannedCodes.size)
            })
        }
    }

    private var lastScanTimestamp: Long = 0L

    private fun logInvalidScanIfNeeded(isValid: Boolean) {
        if (!isValid) {
            firebaseAnalytics.logEvent("invalid_code_scanned", null)
        }
    }

    private fun logTimeBetweenScans() {
        val currentTimestamp = System.currentTimeMillis()

        if (lastScanTimestamp != 0L) {
            val timeBetweenScansSeconds = (currentTimestamp - lastScanTimestamp) / 1000
            firebaseAnalytics.logEvent("time_between_scans", Bundle().apply {
                putLong("seconds_between_scans", timeBetweenScansSeconds)
            })
        }

        lastScanTimestamp = currentTimestamp
    }

    fun hasCameraPermission() = cameraPermissionGranted

    fun initFirebase(context: Context) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    }

    fun onPermissionDenied() {
        uiState = UiState.NoPermission
    }

    fun setPermissionGranted(granted: Boolean) {
        cameraPermissionGranted = granted
    }

    fun attachPreviewView(previewView: PreviewView, activity: ComponentActivity) {
        this.previewView = previewView
        this.currentActivity = activity
        previewView.post {
            val density = previewView.resources.displayMetrics.density
            val frameSizePx = (200 * density).toInt()
            val left = (previewView.width - frameSizePx) / 2
            val top = (previewView.height - frameSizePx) / 2
            scanningArea = Rect(left, top, left + frameSizePx, top + frameSizePx)
        }
    }

    fun rescan() {
        uiState = UiState.Scanning
        hasScanned = false // Reset flagi, aby umożliwić ponowne skanowanie
        isAnalyzing = false // Upewnij się, że isAnalyzing jest zresetowane
    }

    fun startCamera() { // Ta funkcja jest wywoływana po przyznaniu uprawnień
        if (!cameraPermissionGranted) {
            uiState = UiState.NoPermission
            return
        }
        uiState = UiState.Scanning // Ustaw stan na skanowanie
    }

    @OptIn(ExperimentalGetImage::class)
    suspend fun bindCameraUseCases() = withContext(Dispatchers.Main) {
        val activity = currentActivity ?: return@withContext
        val previewView = this@ScannerViewModel.previewView ?: return@withContext

        val cameraProvider = ProcessCameraProvider.getInstance(activity).get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(previewView.display.rotation)
            .build().also { analyzer ->
                analyzer.setAnalyzer(executor) { imageProxy ->
                    if (isAnalyzing || hasScanned) {
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    isAnalyzing = true
                    val mediaImage = imageProxy.image
                    if (mediaImage != null && scanningArea != null) {

                        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                        val imageWidth = mediaImage.width
                        val imageHeight = mediaImage.height

                        val previewWidth = previewView.width.toFloat()
                        val previewHeight = previewView.height.toFloat()

                        val scaleX: Float
                        val scaleY: Float

                        if (rotationDegrees == 90 || rotationDegrees == 270) {
                            scaleX = imageHeight / previewWidth
                            scaleY = imageWidth / previewHeight
                        } else {
                            scaleX = imageWidth / previewWidth
                            scaleY = imageHeight / previewHeight
                        }

                        val margin = 150  // margines 30px dookoła, możesz dopasować tę wartość eksperymentalnie

                        val scaledScanningArea = Rect(
                            ((scanningArea!!.left + margin) * scaleX).toInt(),
                            ((scanningArea!!.top + margin) * scaleY).toInt(),
                            ((scanningArea!!.right - margin) * scaleX).toInt(),
                            ((scanningArea!!.bottom - margin) * scaleY).toInt()
                        )

                        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

                        val options = BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_DATA_MATRIX)
                            .build()

                        BarcodeScanning.getClient(options).process(image)
                            .addOnSuccessListener { barcodes ->
                                val barcode = barcodes.firstOrNull {
                                    it.boundingBox?.let { box ->
                                        scaledScanningArea.contains(box.centerX(), box.centerY())
                                    } ?: false
                                }

                                barcode?.rawValue?.let {
                                    onBarcodeScanned(it)
                                }
                            }
                            .addOnFailureListener {
                                println("Błąd skanowania: ${it.message}")
                            }
                            .addOnCompleteListener {
                                isAnalyzing = false
                                imageProxy.close()
                            }
                    } else {
                        isAnalyzing = false
                        imageProxy.close()
                    }
                }
            }


        cameraProvider.unbindAll()
        try {
            cameraProvider.bindToLifecycle(activity, cameraSelector, preview, imageAnalysis)
        } catch (e: Exception) {
            uiState = UiState.Error
            println("Błąd powiązania kamery: ${e.message}")
        }
    }



    private fun onBarcodeScanned(code: String) {
        if (hasScanned) return
        hasScanned = true

        // Najpierw próbujemy użyć BatteryQrDecoder (jeśli już masz tę logikę)
        val isValidQr = BatteryQrDecoder.validateCode(code)
        val decodedInfo = if (isValidQr) {
            BatteryQrDecoder.decodeInformation(code)
        } else {
            // Jeśli kod nie przeszedł walidacji dla QR lub chcesz sprawdzić DataMatrix,
            // wywołujemy DataMatrixDecoder
            DataMatrixDecoder.decodeInformation(code)
        }

        uiState = UiState.CodeScanned(code, decodedInfo)

        // Logowanie i inne operacje (np. eksport CSV, logi Firebase) pozostają bez zmian:
        CsvLogger.logScan(currentActivity ?: return, code, decodedInfo != null)
        logUniqueScan(code)
        logTimeBetweenScans()
        logInvalidScanIfNeeded(isValidQr)

        // Przykładowe logowanie wybranych danych:
        decodedInfo?.let { info ->
            firebaseAnalytics.logEvent("cell_code_scanned", Bundle().apply {
                putString("vendor_code", info["Vendor Code"])
                putString("product_type", info["Product Type"])
                putString("cell_chemistry", info["Cell Chemistry"])
                putString("production_date", info["Production Date"])
                putString("code_type", if (code.length == 24) "QR" else "DataMatrix")
            })
        } ?: run {
            firebaseAnalytics.logEvent("cell_code_invalid", Bundle().apply {
                putString("reason", "validation_failed")
                putString("code_type", if (code.length == 24) "QR" else "DataMatrix")
            })
        }
    }

    fun openUrl(context: Context, code: String) {
        val url = "https://www.gobelpower.com/lifepo4_decoder.html?code=$code"
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    fun switchCameraLens() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        viewModelScope.launch { bindCameraUseCases() }
    }

    fun exportCsv(context: Context) {
        val file = File(context.filesDir, "scan_logs.csv")
        if (!file.exists()) {
            Toast.makeText(context, "Brak danych do eksportu", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Eksportuj logi CSV"))
    }
}