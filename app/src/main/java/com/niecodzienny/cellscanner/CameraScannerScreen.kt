package com.niecodzienny.cellscanner

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

        // Link do repozytorium w prawym górnym rogu
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 8.dp)
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/gpietrzak-pl/CellScanner"))
                    context.startActivity(intent)
                }
        ) {
            Text(
                text = "Repozytorium",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.padding(4.dp)
            )
        }

        // Obszar wyświetlania zdekodowanych danych – tło bardzo ciemne (alpha = 0.8f)
        if (uiState is UiState.CodeScanned) {
            // Przyciski i dane umieszczone w jednej kolumnie, przyciski tej samej szerokości
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                // Wszystkie przyciski o tej samej szerokości, ułożone jeden pod drugim
                Button(
                    onClick = { onOpenUrlClick(uiState.code) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Otwórz w przeglądarce")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onRescanClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Skanuj ponownie")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onSwitchLensClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Zmień obiektyw")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://suppi.pl/gpietrzak"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Wesprzyj mnie")
                }
            }
        } else {
            // Dla pozostałych stanów przyciski umieszczone w kolumnie
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (uiState) {
                    is UiState.NoPermission -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Brak uprawnień do kamery.", color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    intent.data = Uri.fromParts("package", context.packageName, null)
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Otwórz ustawienia")
                            }
                        }
                    }
                    is UiState.Scanning -> {
                        Text("Skanowanie...", color = Color.White)
                    }
                    is UiState.Error -> {
                        Text("Błąd podczas skanowania.", color = Color.White)
                        Button(
                            onClick = onRescanClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Spróbuj ponownie")
                        }
                    }
                    UiState.Idle -> {
                        Text("Gotowy do skanowania.", color = Color.White)
                        Button(
                            onClick = onRescanClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Rozpocznij skanowanie")
                        }
                    }
                    else -> {} // Stan CodeScanned obsługiwany powyżej
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onSwitchLensClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Zmień obiektyw")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://suppi.pl/gpietrzak"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Wesprzyj mnie")
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
