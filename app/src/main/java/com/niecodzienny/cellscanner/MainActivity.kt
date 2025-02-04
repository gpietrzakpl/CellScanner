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
import com.niecodzienny.cellscanner.ui.theme.CellScannerTheme

class MainActivity : ComponentActivity() {
    private val scannerViewModel: ScannerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                Scaffold(
                    modifier = Modifier
                ) {
                    CameraScannerScreen(
                        uiState = scannerViewModel.uiState,
                        onRescanClick = { scannerViewModel.rescan() },
                        onOpenUrlClick = { code -> scannerViewModel.openUrl(this, code) },
                        onSwitchLensClick = { scannerViewModel.switchCameraLens() },
                        scannerViewModel = scannerViewModel
                    )
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

// zadanie 4: Wyrównanie obszaru skanowania z obszarem wyświetlanym.
