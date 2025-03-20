// (Cały kod CameraScannerScreen.kt - bez istotnych zmian, ale ważny LaunchedEffect)
package com.niecodzienny.cellscanner

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.analytics.FirebaseAnalytics
import android.os.Bundle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ArrowBack

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
    val firebaseAnalytics = remember { FirebaseAnalytics.getInstance(context) }

    // Dodaj deklarację zmiennej stanu
    var showSettings by remember { mutableStateOf(false) }

    val iconSize = if (showSettings) 64.dp else 48.dp
    val topPadding = if (showSettings) 16.dp else 40.dp

    LaunchedEffect(Unit) {
        // Logowanie zdarzenia otwarcia aplikacji do Firebase
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, null)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showSettings) {
            SettingsScreen(
                onExportClick = { scannerViewModel.exportCsv(context) },
                appVersion = ScannerViewModel.APP_VERSION,
                buildDate = BuildConfig.BUILD_DATE,
                changelog = listOf(
                    "v1.0.1 - Dodanie analityki Firebase i eksportu danych",
                    "v1.0.0 - Pierwsze wydanie aplikacji"
                )
            )
        } else {
            // UI podglądu kamery
            // Podgląd kamery
            AndroidView(
                factory = { localPreviewView },
                modifier = Modifier.fillMaxSize()
            )

            if (uiState is UiState.CodeScanned) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                )
            }

            // Ramka obszaru skanowania
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(220.dp)
                    .border(
                        2.dp, when (uiState) {
                            is UiState.CodeScanned -> Color.Green
                            else -> Color.Red
                        }
                    )
            )

            // Link do repozytorium w prawym górnym rogu
            // Repozytorium i ikona zębatki w kolumnie w prawym górnym rogu
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = topPadding, end = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Link do repozytorium
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/gpietrzak-pl/CellScanner")
                            )
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
                Spacer(modifier = Modifier.height(4.dp))
                // Ikona zębatki poniżej linku
                IconButton(
                    onClick = { showSettings = !showSettings },
                    modifier = Modifier.size(iconSize)
                ) {
                    Icon(
                        imageVector = if (showSettings) Icons.Default.ArrowBack else Icons.Default.Settings,
                        contentDescription = if (showSettings) "Powrót" else "Ustawienia",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Usunięto wyświetlanie ogniskowej
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 40.dp, start = 8.dp)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // Text(text = "Focal: N/A") //Można dać statyczny
            }

            // Wyświetlanie danych zeskanowanego kodu
            if (uiState is UiState.CodeScanned) {
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
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize * 1.5
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    uiState.decodedInfo?.forEach { (key, value) ->
                        val style = if (key == "Cell Chemistry" || key == "Production Date") {
                            MaterialTheme.typography.bodyMedium.copy(
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize * 1.5,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            MaterialTheme.typography.bodyMedium.copy(
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize * 1.5
                            )
                        }
                        Text(text = "$key: $value", color = Color.White, style = style)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        CsvLogger.exportLogFile(context)
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Eksportuj logi CSV")
                    }
                    Button(onClick = {
                        onOpenUrlClick(uiState.code)
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Otwórz w przeglądarce")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRescanClick, modifier = Modifier.fillMaxWidth()) {
                        Text("Skanuj ponownie")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        onSwitchLensClick()
                        firebaseAnalytics.logEvent("camera_lens_switched", null)
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Zmień obiektyw")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        val intent =
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://suppi.pl/gpietrzak"))
                        context.startActivity(intent)
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Wesprzyj mnie")
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (uiState) {
                        UiState.NoPermission -> {
                            Text("Brak uprawnień do kamery.", color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                intent.data = Uri.fromParts("package", context.packageName, null)
                                context.startActivity(intent)
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text("Otwórz ustawienia")
                            }
                        }

                        UiState.Scanning -> Text("Skanowanie...", color = Color.White)
                        UiState.Error -> {
                            Text("Błąd podczas skanowania.", color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onRescanClick, modifier = Modifier.fillMaxWidth()) {
                                Text("Spróbuj ponownie")
                            }
                        }

                        UiState.Idle -> {
                            Text("Gotowy do skanowania.", color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onRescanClick, modifier = Modifier.fillMaxWidth()) {
                                Text("Rozpocznij skanowanie")
                            }
                        }

                        else -> Unit
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        onSwitchLensClick()
                        firebaseAnalytics.logEvent("camera_lens_switched", null)
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Zmień obiektyw")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        val intent =
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://suppi.pl/gpietrzak"))
                        context.startActivity(intent)
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Wesprzyj mnie")
                    }
                }
            }
        }
    }

    LaunchedEffect(localPreviewView, scannerViewModel.hasCameraPermission()) {
        val act = activity ?: return@LaunchedEffect
        scannerViewModel.attachPreviewView(localPreviewView, act)
        scannerViewModel.setPermissionGranted( // Ustaw uprawnienia
            act.checkSelfPermission(android.Manifest.permission.CAMERA) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        )
        if (scannerViewModel.hasCameraPermission()) { // Powiąż przypadki użycia TYLKO jeśli są uprawnienia
            scannerViewModel.bindCameraUseCases()
        }
    }
}