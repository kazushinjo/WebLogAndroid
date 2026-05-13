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
import com.weblog.android.data.NumberedQSO
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
    val numbered by vm.numberedQsos.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var rangeDialogFor by remember { mutableStateOf<ExportFormat?>(null) }
    // エクスポート対象を保持（ダイアログ確定 → ファイルピッカー起動 → 書き込み の橋渡し）
    var exportTargetQsos by remember { mutableStateOf<List<QSO>>(emptyList()) }

    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    val safeCall = currentCall.replace(Regex("[^A-Za-z0-9]"), "")

    // Export launchers
    val adifExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val content = ADIFParser.toAdif(exportTargetQsos, currentCall)
                context.contentResolver.openOutputStream(it)?.use { os ->
                    os.write(content.toByteArray(Charsets.UTF_8))
                }
                vm.showToast("ADIF ${exportTargetQsos.size}件をエクスポートしました")
            }
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val content = ADIFParser.toCsv(exportTargetQsos, currentCall)
                context.contentResolver.openOutputStream(it)?.use { os ->
                    os.write(content.toByteArray(Charsets.UTF_8))
                }
                vm.showToast("CSV ${exportTargetQsos.size}件をエクスポートしました")
            }
        }
    }

    val jsonExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val content = Gson().toJson(exportTargetQsos)
                context.contentResolver.openOutputStream(it)?.use { os ->
                    os.write(content.toByteArray(Charsets.UTF_8))
                }
                vm.showToast("JSON ${exportTargetQsos.size}件をバックアップしました")
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
                vm.insertAll(imported)
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
                val (imported, kind) = ADIFParser.parseCsvAuto(text, currentCall)
                vm.insertAll(imported)
                vm.showToast("${imported.size}件インポートしました ($kind)")
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
                vm.insertAll(imported)
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
                onClick = { rangeDialogFor = ExportFormat.ADIF },
                modifier = Modifier.fillMaxWidth(),
                enabled = currentCall.isNotEmpty() && qsos.isNotEmpty()
            ) { Text("ADIF エクスポート (.adi)") }
            Button(
                onClick = { rangeDialogFor = ExportFormat.CSV },
                modifier = Modifier.fillMaxWidth(),
                enabled = currentCall.isNotEmpty() && qsos.isNotEmpty()
            ) { Text("CSV エクスポート (.csv)") }
            Button(
                onClick = { rangeDialogFor = ExportFormat.JSON },
                modifier = Modifier.fillMaxWidth(),
                enabled = currentCall.isNotEmpty() && qsos.isNotEmpty()
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
            ) { Text("CSV インポート (HAMLOG / 日本語ヘッダ自動判別)") }
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

    rangeDialogFor?.let { format ->
        ExportRangeDialog(
            numbered = numbered,
            format = format,
            onCancel = { rangeDialogFor = null },
            onConfirm = { selected ->
                exportTargetQsos = selected
                rangeDialogFor = null
                val fname = when (format) {
                    ExportFormat.ADIF -> "${safeCall}_${today}.adi"
                    ExportFormat.CSV -> "${safeCall}_${today}.csv"
                    ExportFormat.JSON -> "${safeCall}_backup_${today}.json"
                }
                when (format) {
                    ExportFormat.ADIF -> adifExportLauncher.launch(fname)
                    ExportFormat.CSV -> csvExportLauncher.launch(fname)
                    ExportFormat.JSON -> jsonExportLauncher.launch(fname)
                }
            }
        )
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

enum class ExportFormat(val label: String) {
    ADIF("ADIF"),
    CSV("CSV"),
    JSON("JSON バックアップ");
}

private enum class RangeMode { ALL, BY_NO, BY_DATE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportRangeDialog(
    numbered: List<NumberedQSO>,
    format: ExportFormat,
    onCancel: () -> Unit,
    onConfirm: (List<QSO>) -> Unit
) {
    val total = numbered.size
    val minNo = 1
    val maxNo = total
    val sortedByDate = remember(numbered) {
        numbered.map { it.qso }.sortedBy { it.date }
    }
    val minDate = sortedByDate.firstOrNull()?.date ?: ""
    val maxDate = sortedByDate.lastOrNull()?.date ?: ""

    var mode by remember { mutableStateOf(RangeMode.ALL) }
    var fromNo by remember { mutableStateOf(minNo.toString()) }
    var toNo by remember { mutableStateOf(maxNo.toString()) }
    var fromDate by remember { mutableStateOf(minDate) }
    var toDate by remember { mutableStateOf(maxDate) }

    val previewCount = remember(mode, fromNo, toNo, fromDate, toDate, numbered) {
        computeExportTarget(numbered, mode, fromNo, toNo, fromDate, toDate).size
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("${format.label} エクスポート範囲") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("全 $total 件中、出力する範囲を選択してください。", style = MaterialTheme.typography.bodySmall)

                RadioRow(
                    label = "全件 ($total 件)",
                    selected = mode == RangeMode.ALL,
                    onClick = { mode = RangeMode.ALL }
                )

                RadioRow(
                    label = "番号で指定 (No: $minNo 〜 $maxNo)",
                    selected = mode == RangeMode.BY_NO,
                    onClick = { mode = RangeMode.BY_NO }
                )
                if (mode == RangeMode.BY_NO) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = fromNo,
                            onValueChange = { fromNo = it.filter { c -> c.isDigit() } },
                            label = { Text("開始No") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = toNo,
                            onValueChange = { toNo = it.filter { c -> c.isDigit() } },
                            label = { Text("終了No") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                RadioRow(
                    label = "日付で指定 ($minDate 〜 $maxDate)",
                    selected = mode == RangeMode.BY_DATE,
                    onClick = { mode = RangeMode.BY_DATE }
                )
                if (mode == RangeMode.BY_DATE) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = fromDate,
                            onValueChange = { fromDate = it.filter { c -> c.isDigit() }.take(8) },
                            label = { Text("開始 YYYYMMDD") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = toDate,
                            onValueChange = { toDate = it.filter { c -> c.isDigit() }.take(8) },
                            label = { Text("終了 YYYYMMDD") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    "対象: $previewCount 件",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = previewCount > 0,
                onClick = {
                    val selected = computeExportTarget(numbered, mode, fromNo, toNo, fromDate, toDate)
                    onConfirm(selected)
                }
            ) { Text("エクスポート") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("キャンセル") }
        }
    )
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, modifier = Modifier.padding(start = 4.dp))
    }
}

private fun computeExportTarget(
    numbered: List<NumberedQSO>,
    mode: RangeMode,
    fromNo: String,
    toNo: String,
    fromDate: String,
    toDate: String
): List<QSO> {
    return when (mode) {
        RangeMode.ALL -> numbered.map { it.qso }
        RangeMode.BY_NO -> {
            val a = fromNo.toIntOrNull() ?: return emptyList()
            val b = toNo.toIntOrNull() ?: return emptyList()
            val lo = minOf(a, b)
            val hi = maxOf(a, b)
            numbered.filter { it.no in lo..hi }.map { it.qso }
        }
        RangeMode.BY_DATE -> {
            if (fromDate.length != 8 || toDate.length != 8) return emptyList()
            val lo = minOf(fromDate, toDate)
            val hi = maxOf(fromDate, toDate)
            numbered.filter { it.qso.date in lo..hi }.map { it.qso }
        }
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
