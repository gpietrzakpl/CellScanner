@file:OptIn(ExperimentalMaterial3Api::class)
package com.niecodzienny.cellscanner

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onExportClick: () -> Unit,
    appVersion: String,
    buildDate: String,
    changelog: List<String>
) {
    val context = LocalContext.current

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Ustawienia") }
        )
    }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onExportClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Eksportuj dane do CSV")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nowy przycisk do archiwizacji/skasowania lokalnych danych
            Button(
                onClick = {
                    val success = CsvLogger.archiveLocalData(context)
                    if (success) {
                        Toast.makeText(context, "Dane zostały zarchiwizowane", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Brak danych do archiwizacji", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skasuj dane lokalne")
            }

            Text("Wersja aplikacji: $appVersion", style = MaterialTheme.typography.bodyMedium)
            Text("Data kompilacji: $buildDate", style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(24.dp))

            Text("Zmiany w aplikacji:", style = MaterialTheme.typography.titleMedium)

            changelog.forEach { change ->
                Text("• $change", modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
