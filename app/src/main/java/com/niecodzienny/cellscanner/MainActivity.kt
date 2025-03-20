package com.niecodzienny.cellscanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.niecodzienny.cellscanner.ui.theme.CellScannerTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.zIndex
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.niecodzienny.cellscanner.BuildConfig
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private val scannerViewModel: ScannerViewModel by viewModels()
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        // Logowanie pierwszego uruchomienia aplikacji
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, "app_start")
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, bundle)

        // Przekazanie instancji Firebase do ViewModelu
        scannerViewModel.initFirebase(this)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            scannerViewModel.setPermissionGranted(true)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            CellScannerTheme {
                var showSettings by remember { mutableStateOf(false) }

                Scaffold(
                    modifier = Modifier
                ) { innerPadding ->
                    Box(modifier = Modifier) {
                        if (showSettings) {
                            SettingsScreen(
                                onExportClick = { scannerViewModel.exportCsv(this@MainActivity) },
                                appVersion = ScannerViewModel.APP_VERSION,
                                buildDate = BuildConfig.BUILD_DATE,
                                changelog = listOf(
                                    "v1.0.1 - Dodanie analityki Firebase i eksportu danych",
                                    "v1.0.0 - Pierwsze wydanie aplikacji"
                                )
                            )
                        } else {
                            CameraScannerScreen(
                                uiState = scannerViewModel.uiState,
                                onRescanClick = { scannerViewModel.rescan() },
                                onOpenUrlClick = { code -> scannerViewModel.openUrl(this@MainActivity, code) },
                                onSwitchLensClick = { scannerViewModel.switchCameraLens() },
                                scannerViewModel = scannerViewModel
                            )
                        }


                    }
                }
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            scannerViewModel.setPermissionGranted(isGranted)
            if (isGranted) {
                scannerViewModel.startCamera()
            } else {
                scannerViewModel.onPermissionDenied()
            }
        }
}

// zadanie 2: Udoskonalić funkcjonalność przełączania obiektywu
// Obecnie przełączanie na kamerę frontową działa poprawnie, ale przełączanie na teleobiektyw
// (czyli wybór tylnej kamery o najwyższej ogniskowej) nadal nie działa prawidłowo.
// Konieczne jest dopracowanie logiki wyboru teleobiektywu (np. poprzez dodatkowe sprawdzenia lub
// użycie innego sposobu selekcji) tak, aby funkcjonalność działała dla wszystkich urządzeń.
// (Pozostawiamy to zadanie do poprawienia.)

// zadanie 3: Obsługa kodów typu DataMatrix.
// Aktualnie klient ML Kit został skonfigurowany, aby wykrywać zarówno kody QR, jak i DataMatrix.
// Jeśli struktura kodów DataMatrix różni się od struktury kodów QR, konieczne może być
// rozszerzenie dekodera (BatteryQrDecoder) o obsługę DataMatrix.

// zadanie 4: Wyrównać obszar skanowania z obszarem wyświetlanym.
