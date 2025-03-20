package com.niecodzienny.cellscanner

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.*

object CsvLogger {

    private const val FILE_NAME_PREFIX = "scan_logs"

    fun getCurrentFileName(): String {
        val currentDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        return "${FILE_NAME_PREFIX}_$currentDate.csv"
    }

    // Zapisuje pojedynczy log do pliku
    fun logScan(context: Context, code: String, isValid: Boolean) {
        try {
            val logFile = File(context.filesDir, getCurrentFileName())
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val status = if (isValid) "VALID" else "INVALID"
            // Jeśli plik nie istnieje, dodaj nagłówek
            if (!logFile.exists()) {
                logFile.appendText("Timestamp,Code,Status\n")
            }
            logFile.appendText("$timestamp,$code,$status\n")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Eksportuje plik CSV do katalogu Downloads – eksportuje tylko dane, które zostały zapisane po ostatniej archiwizacji
    fun exportLogFile(context: Context) {
        try {
            val sourceFile = File(context.filesDir, getCurrentFileName())
            if (!sourceFile.exists() || sourceFile.length() == 0L) {
                Toast.makeText(context, "Brak danych do eksportu.", Toast.LENGTH_SHORT).show()
                return
            }
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            // Nazwa eksportowanego pliku zawiera datę
            val exportFile = File(downloadsDir, "cellscanner_logs_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.csv")
            sourceFile.copyTo(exportFile, overwrite = true)
            Toast.makeText(context, "Plik CSV został zapisany: ${exportFile.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Eksport się nie powiódł: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Archiwizuje bieżący plik logów:
    // - Zmienia jego nazwę na "archive_scan_logs_YYYYMMDD.csv"
    // - Tworzy nowy pusty plik, w którym będą zapisywane kolejne logi
    fun archiveLocalData(context: Context): Boolean {
        val currentFile = File(context.filesDir, getCurrentFileName())
        if (currentFile.exists() && currentFile.length() > 0L) {
            val archiveFile = File(context.filesDir, "archive_${getCurrentFileName()}")
            val renamed = currentFile.renameTo(archiveFile)
            if (renamed) {
                currentFile.createNewFile()
                return true
            }
        }
        return false
    }
}