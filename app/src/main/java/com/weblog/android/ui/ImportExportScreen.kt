package com.weblog.android.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.weblog.android.data.AppViewModel
import com.weblog.android.data.QSO
import com.weblog.android.utils.ADIFParser
import com.weblog.android.utils.freqToBand
import kotlinx.coroutines.launch
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportScreen(
    vm: AppViewModel,
    currentCall: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val qsos by vm.qsos.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    val safeCall = currentCall.replace(Regex("[^A-Za-z0-9]"), "")

    // Export launchers
    val adifExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val content = ADIFParser.toAdif(qsos, currentCall)
                context.contentResolver.openOutputStream(it)?.use { os ->
                    os.write(content.toByteArray(Charsets.UTF_8))
                }
                vm.showToast("ADIFエクスポート完了")
            }
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val content = ADIFParser.toCsv(qsos, currentCall)
                context.contentResolver.openOutputStream(it)?.use { os ->
                    os.write(content.toByteArray(Charsets.UTF_8))
                }
                vm.showToast("CSVエクスポート完了")
            }
        }
    }

    val jsonExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val content = Gson().toJson(qsos)
                context.contentResolver.openOutputStream(it)?.use { os ->
                    os.write(content.toByteArray(Charsets.UTF_8))
                }
                vm.showToast("バックアップ完了")
            }
        }
    }

    // Import launchers
    val adifImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val text = readTextAutoEncoding(context, it)
                val imported = ADIFParser.parseAdif(text, currentCall)
                imported.forEach { q -> vm.insertQSO(q) }
                vm.showToast("${imported.size}件インポートしました")
                onDismiss()
            }
        }
    }

    val csvImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val text = readTextAutoEncoding(context, it)
                val imported = ADIFParser.parseCsv(text, currentCall)
                imported.forEach { q -> vm.insertQSO(q) }
                vm.showToast("${imported.size}件インポートしました")
                onDismiss()
            }
        }
    }

    val hamlogCsvImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val text = readTextAutoEncoding(context, it)
                val imported = ADIFParser.parseHamlogCsv(text, currentCall)
                imported.forEach { q -> vm.insertQSO(q) }
                vm.showToast("${imported.size}件インポートしました (HAMLOG)")
                onDismiss()
            }
        }
    }

    val jsonImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val text = readTextAutoEncoding(context, it)
                val imported = parseWebLogJson(text, currentCall)
                imported.forEach { q -> vm.insertQSO(q) }
                vm.showToast("${imported.size}件復元しました")
                onDismiss()
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("エクスポート", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = { adifExportLauncher.launch("${safeCall}_${today}.adi") },
                modifier = Modifier.fillMaxWidth(),
                enabled = currentCall.isNotEmpty()
            ) { Text("ADIF エクスポート (.adi)") }
            Button(
                onClick = { csvExportLauncher.launch("${safeCall}_${today}.csv") },
                modifier = Modifier.fillMaxWidth(),
                enabled = currentCall.isNotEmpty()
            ) { Text("CSV エクスポート (.csv)") }
            Button(
                onClick = { jsonExportLauncher.launch("${safeCall}_backup_${today}.json") },
                modifier = Modifier.fillMaxWidth(),
                enabled = currentCall.isNotEmpty()
            ) { Text("バックアップ (JSON)") }

            HorizontalDivider()

            Text("インポート", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(
                onClick = { adifImportLauncher.launch("*/*") },
                modifier = Modifier.fillMaxWidth(),
                enabled = currentCall.isNotEmpty()
            ) { Text("ADIF インポート (.adi / .txt)") }
            OutlinedButton(
                onClick = { csvImportLauncher.launch("*/*") },
                modifier = Modifier.fillMaxWidth(),
                enabled = currentCall.isNotEmpty()
            ) { Text("CSV インポート (日本語ヘッダ)") }
            OutlinedButton(
                onClick = { hamlogCsvImportLauncher.launch("*/*") },
                modifier = Modifier.fillMaxWidth(),
                enabled = currentCall.isNotEmpty()
            ) { Text("HAMLOG CSV インポート (PC版 weblog)") }
            OutlinedButton(
                onClick = { jsonImportLauncher.launch("*/*") },
                modifier = Modifier.fillMaxWidth(),
                enabled = currentCall.isNotEmpty()
            ) { Text("バックアップから復元 (JSON)") }

            HorizontalDivider()

            Button(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                enabled = currentCall.isNotEmpty()
            ) { Text("全件削除") }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("全件削除") },
            text = { Text("$currentCall の全交信記録を削除します。この操作は取り消せません。事前にバックアップを取ってください。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteAll()
                    vm.showToast("全件削除しました")
                    showDeleteConfirm = false
                    onDismiss()
                }) { Text("削除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("キャンセル") }
            }
        )
    }
}

private fun readTextAutoEncoding(context: android.content.Context, uri: Uri): String {
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return ""
    // UTF-8 BOM
    if (bytes.size >= 3 &&
        bytes[0] == 0xEF.toByte() &&
        bytes[1] == 0xBB.toByte() &&
        bytes[2] == 0xBF.toByte()
    ) {
        return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
    }
    // UTF-8 として valid ならそれを採用、ダメなら Shift_JIS にフォールバック
    return try {
        val decoder = Charsets.UTF_8.newDecoder().apply {
            onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
            onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
        }
        decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString()
    } catch (_: Exception) {
        try {
            String(bytes, Charset.forName("Shift_JIS"))
        } catch (_: Exception) {
            String(bytes, Charsets.UTF_8)
        }
    }
}

private fun parseWebLogJson(text: String, myCall: String): List<QSO> {
    if (text.isBlank()) return emptyList()
    val root: JsonElement = try {
        JsonParser.parseString(text)
    } catch (_: Exception) {
        return emptyList()
    }
    val array: JsonArray = when {
        root.isJsonArray -> root.asJsonArray
        root.isJsonObject && root.asJsonObject.has("qsos") &&
            root.asJsonObject.get("qsos").isJsonArray -> root.asJsonObject.getAsJsonArray("qsos")
        else -> return emptyList()
    }

    fun JsonElement.str(key: String): String {
        if (!isJsonObject) return ""
        val e = asJsonObject.get(key) ?: return ""
        return if (e.isJsonNull) "" else e.asString
    }

    return array.mapNotNull { el ->
        if (!el.isJsonObject) return@mapNotNull null
        val call = el.str("callsign").trim().ifEmpty { return@mapNotNull null }
        val freq = el.str("freq").trim()
        val explicitBand = el.str("band").trim()
        val band = explicitBand.ifEmpty { freqToBand(freq.toDoubleOrNull() ?: 0.0) }
        val mode = el.str("mode").trim().ifEmpty { "SSB" }
        QSO(
            myCall = myCall,
            callsign = call,
            date = el.str("date").trim(),
            time = el.str("time").trim(),
            band = band,
            freq = freq,
            mode = mode,
            rstSent = el.str("rstSent").trim().ifEmpty { "59" },
            rstRcvd = el.str("rstRcvd").trim().ifEmpty { "59" },
            name = el.str("name").trim(),
            qth = el.str("qth").trim(),
            jcc = el.str("jcc").trim(),
            qsl = el.str("qsl").trim(),
            // PC版は "remarks" / Android は "comment"
            comment = el.str("comment").ifEmpty { el.str("remarks") }.trim()
        )
    }
}
