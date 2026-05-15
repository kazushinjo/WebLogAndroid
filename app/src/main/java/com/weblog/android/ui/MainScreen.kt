package com.weblog.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.weblog.android.data.AppViewModel
import com.weblog.android.data.QSO

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: AppViewModel) {
    val config = LocalConfiguration.current
    val isWide = config.screenWidthDp >= 600

    val callsigns by vm.callsigns.collectAsState()
    val currentCall by vm.currentCall.collectAsState()

    var showImportExport by remember { mutableStateOf(false) }
    var showAddCallsign by remember { mutableStateOf(false) }
    var newCallsignText by remember { mutableStateOf("") }
    var editingQSO by remember { mutableStateOf<QSO?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val callsignBar: @Composable () -> Unit = {
        CallsignBar(
            callsigns = callsigns,
            currentCall = currentCall,
            onSelect = { vm.setCurrentCall(it) },
            onAdd = { showAddCallsign = true },
            onRemove = { vm.removeCallsign(it) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WebLog v1.08") },
                actions = {
                    IconButton(onClick = { showImportExport = true }) {
                        Icon(Icons.Default.SwapVert, contentDescription = "入出力")
                    }
                }
            )
        }
    ) { padding ->
        if (isWide) {
            Row(modifier = Modifier.fillMaxSize().padding(padding)) {
                Column(modifier = Modifier.weight(0.42f)) {
                    callsignBar()
                    EntryFormScreen(
                        vm = vm,
                        currentCall = currentCall,
                        editingQSO = null,
                        onSaved = {}
                    )
                }
                VerticalDivider(modifier = Modifier.fillMaxHeight())
                Box(modifier = Modifier.weight(0.58f)) {
                    LogListScreen(
                        vm = vm,
                        currentCall = currentCall,
                        onEdit = { editingQSO = it }
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                callsignBar()
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                        text = { Text("登録") }, icon = { Icon(Icons.Default.Edit, null) })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                        text = { Text("記録一覧") }, icon = { Icon(Icons.AutoMirrored.Filled.List, null) })
                }
                when (selectedTab) {
                    0 -> EntryFormScreen(
                        vm = vm,
                        currentCall = currentCall,
                        editingQSO = null,
                        onSaved = { selectedTab = 1 }
                    )
                    1 -> LogListScreen(
                        vm = vm,
                        currentCall = currentCall,
                        onEdit = { editingQSO = it }
                    )
                }
            }
        }
    }

    if (showImportExport) {
        ImportExportScreen(vm = vm, currentCall = currentCall, onDismiss = { showImportExport = false })
    }

    if (showAddCallsign) {
        AlertDialog(
            onDismissRequest = { showAddCallsign = false; newCallsignText = "" },
            title = { Text("コールサイン追加") },
            text = {
                OutlinedTextField(
                    value = newCallsignText,
                    onValueChange = { newCallsignText = it.uppercase() },
                    label = { Text("JA1XXX") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.addCallsign(newCallsignText)
                    newCallsignText = ""
                    showAddCallsign = false
                }) { Text("追加") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCallsign = false; newCallsignText = "" }) { Text("キャンセル") }
            }
        )
    }

    editingQSO?.let { qso ->
        EditQSODialog(vm = vm, qso = qso, currentCall = currentCall, onDismiss = { editingQSO = null })
    }
}

@Composable
fun CallsignBar(
    callsigns: List<String>,
    currentCall: String,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(tonalElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text("自局", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Box {
                TextButton(onClick = { expanded = true }) {
                    Text(currentCall.ifEmpty { "(未設定)" })
                    Icon(Icons.Default.ArrowDropDown, null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("(未設定)") }, onClick = { onSelect(""); expanded = false })
                    callsigns.forEach { cs ->
                        DropdownMenuItem(text = { Text(cs) }, onClick = { onSelect(cs); expanded = false })
                    }
                }
            }
            IconButton(onClick = onAdd) { Icon(Icons.Default.AddCircleOutline, "追加") }
            if (currentCall.isNotEmpty()) {
                IconButton(onClick = { onRemove(currentCall) }) {
                    Icon(Icons.Default.RemoveCircleOutline, "削除",
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditQSODialog(vm: AppViewModel, qso: QSO, currentCall: String, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        EntryFormScreen(
            vm = vm,
            currentCall = currentCall,
            editingQSO = qso,
            onSaved = onDismiss
        )
    }
}
