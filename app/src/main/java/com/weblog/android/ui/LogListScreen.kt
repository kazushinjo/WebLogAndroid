package com.weblog.android.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weblog.android.data.AppViewModel
import com.weblog.android.data.NumberedQSO
import com.weblog.android.data.QSO
import com.weblog.android.utils.BANDS
import com.weblog.android.utils.MODES

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LogListScreen(
    vm: AppViewModel,
    currentCall: String,
    onEdit: (QSO) -> Unit
) {
    val numbered by vm.numberedQsos.collectAsState()
    var searchText by remember { mutableStateOf("") }
    var filterBand by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<QSO?>(null) }

    // フィルタは全件 (NumberedQSO 単位) で実施、その後 No 降順で表示
    val filtered = remember(numbered, searchText, filterBand, filterMode) {
        numbered.asSequence().filter { (_, q) ->
            (searchText.isEmpty() || listOf(q.callsign, q.name, q.qth, q.comment)
                .any { it.contains(searchText, ignoreCase = true) }) &&
            (filterBand.isEmpty() || q.band == filterBand) &&
            (filterMode.isEmpty() || q.mode == filterMode)
        }.sortedByDescending { it.no }.toList()
    }

    // 表示は最新 300 件まで（残りはフィルタ・検索で絞り込んで確認）
    val displayLimit = 300
    val visibleList = remember(filtered) {
        filtered.take(displayLimit)
    }

    val jccCount = remember(filtered) { filtered.map { it.qso.jcc }.filter { it.isNotEmpty() }.toSet().size }
    val bandStats = remember(filtered) {
        filtered.groupBy { it.qso.band }.filter { it.key.isNotEmpty() }
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
            val hScroll = rememberScrollState()
            val remaining = filtered.size - visibleList.size

            LazyColumn(modifier = Modifier.weight(1f)) {
                stickyHeader {
                    QSOHeaderRow(scrollState = hScroll)
                }
                items(visibleList, key = { it.qso.id }) { nq ->
                    QSORow(
                        no = nq.no,
                        qso = nq.qso,
                        scrollState = hScroll,
                        onEdit = { onEdit(nq.qso) },
                        onDelete = { deleteTarget = nq.qso }
                    )
                    HorizontalDivider(thickness = 0.5.dp)
                }
                if (remaining > 0) {
                    item(key = "more-info") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "他に $remaining 件あります（検索/フィルタで絞り込んでください）",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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

// 表形式列幅
private val W_NO = 56.dp
private val W_DATE = 76.dp
private val W_TIME = 44.dp
private val W_CALL = 110.dp
private val W_BAND = 60.dp
private val W_FREQ = 64.dp
private val W_MODE = 48.dp
private val W_RST = 36.dp
private val W_NAME = 110.dp
private val W_QTH = 200.dp
private val W_JCC = 56.dp
private val W_QSL = 40.dp
private val W_COMMENT = 140.dp
private val W_ICON = 36.dp

@Composable
fun QSOHeaderRow(scrollState: androidx.compose.foundation.ScrollState) {
    Surface(tonalElevation = 2.dp, color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderCell("No", W_NO)
            HeaderCell("日付", W_DATE)
            HeaderCell("時刻", W_TIME)
            HeaderCell("コールサイン", W_CALL)
            HeaderCell("バンド", W_BAND)
            HeaderCell("周波数", W_FREQ)
            HeaderCell("モード", W_MODE)
            HeaderCell("送", W_RST)
            HeaderCell("受", W_RST)
            HeaderCell("名前", W_NAME)
            HeaderCell("QTH", W_QTH)
            HeaderCell("JCC", W_JCC)
            HeaderCell("QSL", W_QSL)
            HeaderCell("備考", W_COMMENT)
            HeaderCell("編集", W_ICON)
            HeaderCell("削除", W_ICON)
        }
    }
}

@Composable
private fun HeaderCell(text: String, width: Dp) {
    Text(
        text = text,
        modifier = Modifier.width(width).padding(horizontal = 4.dp),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun QSORow(
    no: Int,
    qso: QSO,
    scrollState: androidx.compose.foundation.ScrollState,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cell(no.toString(), W_NO, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Cell(qso.date, W_DATE)
        Cell(qso.time, W_TIME)
        Cell(qso.callsign, W_CALL, bold = true, color = MaterialTheme.colorScheme.primary)
        Cell(qso.band, W_BAND)
        Cell(qso.freq, W_FREQ)
        Cell(qso.mode, W_MODE)
        Cell(qso.rstSent, W_RST)
        Cell(qso.rstRcvd, W_RST)
        Cell(qso.name, W_NAME)
        Cell(qso.qth, W_QTH)
        Cell(qso.jcc, W_JCC)
        Cell(qso.qsl, W_QSL)
        Cell(qso.comment, W_COMMENT)
        Box(modifier = Modifier.width(W_ICON), contentAlignment = Alignment.Center) {
            IconButton(onClick = onEdit, modifier = Modifier.size(W_ICON)) {
                Icon(Icons.Default.Edit, "編集", modifier = Modifier.size(18.dp))
            }
        }
        Box(modifier = Modifier.width(W_ICON), contentAlignment = Alignment.Center) {
            IconButton(onClick = onDelete, modifier = Modifier.size(W_ICON)) {
                Icon(
                    Icons.Default.Delete, "削除",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun Cell(
    text: String,
    width: Dp,
    bold: Boolean = false,
    color: Color = Color.Unspecified
) {
    Text(
        text = text,
        modifier = Modifier.width(width).padding(horizontal = 4.dp),
        fontSize = 11.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
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
