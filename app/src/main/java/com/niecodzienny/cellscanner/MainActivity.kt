package com.niecodzienny.cellscanner

import android.content.Intent
import android.net.Uri

import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.niecodzienny.cellscanner.ui.theme.CellScannerTheme
import java.util.concurrent.Executors

import androidx.camera.core.CameraSelector // Z CameraX
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.lifecycle.ProcessCameraProvider // Z CameraX
import androidx.camera.view.PreviewView // Z CameraX View

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Uruchom kamerę, jeśli użytkownik zgodził się na uprawnienia
                startCamera()
            } else {
                // Jeśli użytkownik nie zaakceptował uprawnień, wyświetl komunikat
                println("Uprawnienia do kamery zostały odrzucone.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Sprawdzamy, czy mamy uprawnienia do aparatu
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Jeśli mamy uprawnienia, uruchamiamy kamerę
            startCamera()
        } else {
            // Jeśli nie mamy uprawnień, prosimy o nie
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            CellScannerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CameraScanner(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    private fun startCamera() {
        // Zainicjalizuj kamerę tutaj lub wywołaj odpowiednią funkcję
    }
}

@Composable
fun CameraScanner(modifier: Modifier = Modifier) {
    // Stan przechowujący zeskanowany tekst
    var scannedText by remember { mutableStateOf("") }

    val context = LocalContext.current
    val previewView = PreviewView(context)

    // **Funkcja aktualizująca stan po zeskanowaniu kodu QR**
    fun onBarcodeScanned(barcodeValue: String) {
        scannedText = barcodeValue
    }

    // Rozmiar ramki, którą chcemy wyświetlić na ekranie
    val scanningAreaSize = 250.dp
    val scanningAreaPadding = 16.dp

    Box(modifier = modifier.fillMaxSize()) {
        // Widok kamery
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Ramka wokół obszaru skanowania
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(200.dp)
                .border(2.dp, Color.Red)  // Dodanie ramki
        )

        // Kolumna, która zawiera przyciski i tekst
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // **Kamera uruchamia się automatycznie po załadowaniu widoku**
            LaunchedEffect(true) {
                startCamera(previewView, context, ::onBarcodeScanned)
            }

            // Jeśli zeskanowano kod, wyświetlamy go poniżej
            if (scannedText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Odczytano kod QR: $scannedText", style = MaterialTheme.typography.bodyLarge)
            }
        }
        Button(
            onClick = {
                // Generowanie URL z zeskanowanym kodem
                val url = "https://www.gobelpower.com/lifepo4_decoder.html?code=$scannedText"
                // Tworzenie Intentu do otwarcia przeglądarki
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                // Uruchomienie przeglądarki
                context.startActivity(intent)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(top = 16.dp)  // Dodanie odstępu od innych elementów
        ) {
            Text("Skanuj kod")
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalGetImage::class)
fun startCamera(
    previewView: PreviewView,
    context: Context,
    onBarcodeScanned: (String) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        // Ustawienia podglądu kamery
        val preview = androidx.camera.core.Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        // **Dodano licznik klatek**
        var frameCounter = 0


        // Ustawienie analizy obrazu
        val imageAnalysis = androidx.camera.core.ImageAnalysis.Builder().build().also { analyzer ->
            analyzer.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                frameCounter++
                if (frameCounter % 5 == 0) { // Przetwarzaj co 5 klatek
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                    val scanner = BarcodeScanning.getClient()
                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            for (barcode in barcodes) {
                                val rawValue = barcode.rawValue
                                if (!rawValue.isNullOrEmpty()) {
                                    // Przekazujemy odczytany kod QR do funkcji
                                    onBarcodeScanned(rawValue)
                                }
                            }
                        }
                        .addOnFailureListener { exception ->
                            println("Błąd skanowania: ${exception.message}")
                        }
                        .addOnCompleteListener {
                            imageProxy.close()  // Zamykamy imageProxy po przetworzeniu
                        }
                } else {
                    imageProxy.close() // Zamykaj klatki bez analizy
                }
            } else {
                imageProxy.close() // Zamykaj klatki bez analizy
            }
            }
        }

        // Wybór kamery (tylna) i bindowanie do cyklu życia
        val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        cameraProvider.bindToLifecycle(
            context as ComponentActivity,
            cameraSelector,
            preview,
            imageAnalysis
        )
    }, ContextCompat.getMainExecutor(context))
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun CameraScannerPreview() {
    CellScannerTheme {
        CameraScanner()
    }
}
