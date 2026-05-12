package com.weblog.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.weblog.android.data.AppViewModel
import com.weblog.android.data.QSO
import com.weblog.android.utils.BANDS
import com.weblog.android.utils.MODES

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogListScreen(
    vm: AppViewModel,
    currentCall: String,
    onEdit: (QSO) -> Unit
) {
    val qsos by vm.qsos.collectAsState()
    var searchText by remember { mutableStateOf("") }
    var filterBand by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<QSO?>(null) }

    val filtered = remember(qsos, searchText, filterBand, filterMode) {
        qsos.filter { q ->
            (searchText.isEmpty() || listOf(q.callsign, q.name, q.qth, q.comment)
                .any { it.contains(searchText, ignoreCase = true) }) &&
            (filterBand.isEmpty() || q.band == filterBand) &&
            (filterMode.isEmpty() || q.mode == filterMode)
        }
    }

    val jccCount = remember(filtered) { filtered.map { it.jcc }.filter { it.isNotEmpty() }.toSet().size }
    val bandStats = remember(filtered) {
        filtered.groupBy { it.band }.filter { it.key.isNotEmpty() }
            .map { "${it.key}:${it.value.size}" }.joinToString("  ")
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("検索") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            singleLine = true
        )

        // Filters
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactDropdown(
                label = "バンド",
                options = BANDS,
                selected = filterBand,
                onSelect = { filterBand = it },
                modifier = Modifier.weight(1f)
            )
            CompactDropdown(
                label = "モード",
                options = listOf("") + MODES,
                selected = filterMode,
                onSelect = { filterMode = it },
                modifier = Modifier.weight(1f)
            )
            Text(
                "${filtered.size}件",
                modifier = Modifier.align(Alignment.CenterVertically),
                style = MaterialTheme.typography.labelMedium
            )
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("交信記録なし", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filtered, key = { it.id }) { qso ->
                    QSORow(qso = qso, onEdit = { onEdit(qso) }, onDelete = { deleteTarget = qso })
                    HorizontalDivider()
                }
            }

            // Stats
            Surface(tonalElevation = 1.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Text("総交信数: ${filtered.size}  JCC: $jccCount",
                        style = MaterialTheme.typography.labelSmall)
                    if (bandStats.isNotEmpty()) {
                        Text(bandStats, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }

    deleteTarget?.let { qso ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("削除確認") },
            text = { Text("${qso.callsign} の記録を削除しますか？") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteQSO(qso)
                    vm.showToast("削除しました")
                    deleteTarget = null
                }) { Text("削除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("キャンセル") }
            }
        )
    }
}

@Composable
fun QSORow(qso: QSO, onEdit: () -> Unit, onDelete: () -> Unit) {
    ListItem(
        headlineContent = {
            Text("${qso.callsign}  ${qso.date} ${qso.time}",
                style = MaterialTheme.typography.bodyMedium)
        },
        supportingContent = {
            val info = buildString {
                append("${qso.band} ${qso.mode}  ${qso.rstSent}/${qso.rstRcvd}")
                if (qso.name.isNotEmpty()) append("  ${qso.name}")
                if (qso.qth.isNotEmpty()) append(" ${qso.qth}")
            }
            Text(info, style = MaterialTheme.typography.bodySmall)
        },
        trailingContent = {
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "編集", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "削除", modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
}

@Composable
fun CompactDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }) {
            Text("${label}▼ ${selected.ifEmpty { "全" }}", style = MaterialTheme.typography.labelSmall)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt.ifEmpty { "全て" }) },
                    onClick = { onSelect(opt); expanded = false }
                )
            }
        }
    }
}
