package com.niecodzienny.cellscanner

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CameraScannerScreen(
    uiState: UiState,
    onRescanClick: () -> Unit,
    onOpenUrlClick: (String) -> Unit,
    onSwitchLensClick: () -> Unit,
    scannerViewModel: ScannerViewModel
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val localPreviewView = remember { PreviewView(context) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Podgląd kamery
        AndroidView(
            factory = { localPreviewView },
            modifier = Modifier.fillMaxSize()
        )

        // Obszar skanowania – ustawiony w okolicach 1/3 od góry (padding top = 100.dp)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
                .size(200.dp)
                .border(2.dp, when (uiState) {
                    is UiState.CodeScanned -> Color.Green
                    else -> Color.Red
                })
        )

        // Nakładka z ogniskową w lewym górnym rogu (padding top = 40.dp)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 40.dp, start = 8.dp)
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Text(
                text = "Focal: ${
                    scannerViewModel.currentFocalLength?.let { String.format("%.1f mm", it) } ?: "?"
                }",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(4.dp)
            )
        }

        // Obszar wyświetlania zdekodowanych danych – z tłem bardzo ciemnym (alpha = 0.8f)
        if (uiState is UiState.CodeScanned) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Odczytano kod: ${uiState.code}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    uiState.decodedInfo?.forEach { (key, value) ->
                        Text(
                            text = "$key: $value",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(onClick = { onOpenUrlClick(uiState.code) }) {
                            Text("Otwórz w przeglądarce")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = onRescanClick) {
                            Text("Skanuj ponownie")
                        }
                    }
                }
            }
        } else {
            // Elementy interfejsu dla pozostałych stanów
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (uiState) {
                    is UiState.NoPermission -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Brak uprawnień do kamery.")
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                intent.data = Uri.fromParts("package", context.packageName, null)
                                context.startActivity(intent)
                            }) {
                                Text("Otwórz ustawienia")
                            }
                        }
                    }
                    is UiState.Scanning -> {
                        Text("Skanowanie...")
                    }
                    is UiState.Error -> {
                        Text("Błąd podczas skanowania.")
                        Button(onClick = onRescanClick) {
                            Text("Spróbuj ponownie")
                        }
                    }
                    UiState.Idle -> {
                        Text("Gotowy do skanowania.")
                        Button(onClick = onRescanClick) {
                            Text("Rozpocznij skanowanie")
                        }
                    }
                    else -> {} // Stan CodeScanned obsługiwany powyżej
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onSwitchLensClick) {
                    Text("Zmień obiektyw")
                }
            }
        }
    }

    LaunchedEffect(localPreviewView) {
        val act = activity ?: return@LaunchedEffect
        scannerViewModel.attachPreviewView(localPreviewView, act)
        scannerViewModel.setPermissionGranted(
            act.checkSelfPermission(android.Manifest.permission.CAMERA) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        )
        if (scannerViewModel.hasCameraPermission()) {
            scannerViewModel.bindCameraUseCases()
            scannerViewModel.updateCurrentFocalLength() // Ustawienie początkowej ogniskowej
        }
    }
}
