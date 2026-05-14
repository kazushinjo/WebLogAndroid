package com.weblog.android.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.weblog.android.data.AppViewModel
import com.weblog.android.data.CallsignSuggestion
import com.weblog.android.data.QSO
import com.weblog.android.utils.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntryFormScreen(
    vm: AppViewModel,
    currentCall: String,
    editingQSO: QSO?,
    onSaved: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val commentBringIntoView = remember { BringIntoViewRequester() }

    var callsign by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var freq by remember { mutableStateOf("") }
    var band by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("SSB") }
    var rstSent by remember { mutableStateOf("59") }
    var rstRcvd by remember { mutableStateOf("59") }
    var name by remember { mutableStateOf("") }
    var qth by remember { mutableStateOf("") }
    var jcc by remember { mutableStateOf("") }
    var qsl by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }

    var duplicates by remember { mutableStateOf<List<QSO>>(emptyList()) }
    var callSuggestions by remember { mutableStateOf<List<CallsignSuggestion>>(emptyList()) }
    var jccSuggestions by remember { mutableStateOf<List<JCCEntry>>(emptyList()) }

    fun setNow() {
        val jst = LocalDateTime.now(ZoneId.of("Asia/Tokyo"))
        date = jst.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        time = jst.format(DateTimeFormatter.ofPattern("HHmm"))
    }

    fun populate(q: QSO) {
        callsign = q.callsign; date = q.date; time = q.time
        freq = q.freq; band = q.band; mode = q.mode
        rstSent = q.rstSent; rstRcvd = q.rstRcvd
        name = q.name; qth = q.qth; jcc = q.jcc; qsl = q.qsl; comment = q.comment
    }

    fun reset() {
        callsign = ""; date = ""; time = ""; freq = ""; band = ""; mode = "SSB"
        rstSent = "59"; rstRcvd = "59"; name = ""; qth = ""; jcc = ""; qsl = ""; comment = ""
        duplicates = emptyList(); callSuggestions = emptyList(); jccSuggestions = emptyList()
        setNow()
    }

    LaunchedEffect(editingQSO) {
        if (editingQSO != null) populate(editingQSO) else { reset(); setNow() }
    }

    Box(modifier = Modifier.fillMaxSize().imePadding()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Callsign
        Column {
            OutlinedTextField(
                value = callsign,
                onValueChange = { v ->
                    callsign = v.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }
                    scope.launch {
                        duplicates = if (v.length >= 2) vm.findDuplicates(v.uppercase()) else emptyList()
                        callSuggestions = if (v.length >= 2) vm.suggestCallsigns(v.uppercase()) else emptyList()
                    }
                },
                label = { Text("コールサイン") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Next
                ),
                singleLine = true
            )
            if (callSuggestions.isNotEmpty() && editingQSO == null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    callSuggestions.forEach { s ->
                        ListItem(
                            headlineContent = { Text(s.callsign) },
                            supportingContent = { Text("${s.name} ${s.qth}") },
                            modifier = Modifier.clickable {
                                callsign = s.callsign; name = s.name
                                qth = s.qth; jcc = s.jcc
                                callSuggestions = emptyList()
                                scope.launch { duplicates = vm.findDuplicates(s.callsign) }
                            }
                        )
                    }
                }
            }
            if (duplicates.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("交信済み ${duplicates.size}件（最終: ${duplicates.first().date} ${duplicates.first().time}）",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Date / Time
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = date,
                onValueChange = { v -> date = v.filter { it.isDigit() }.take(8) },
                label = { Text("日付(YYYYMMDD)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                visualTransformation = DateVisualTransformation(),
                singleLine = true
            )
            OutlinedTextField(
                value = time,
                onValueChange = { v -> time = v.filter { it.isDigit() }.take(4) },
                label = { Text("時刻(HHMM)") },
                modifier = Modifier.weight(1f),
                trailingIcon = {
                    IconButton(onClick = { setNow() }) {
                        Icon(Icons.Default.AccessTime, "現在")
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                visualTransformation = TimeVisualTransformation(),
                singleLine = true
            )
        }

        // Freq / Band
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = freq,
                onValueChange = { v ->
                    freq = v
                    band = freqToBand(v.toDoubleOrNull() ?: 0.0)
                },
                label = { Text("周波数(MHz)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                singleLine = true
            )
            DropdownField(
                label = "バンド",
                options = BANDS,
                selected = band,
                onSelect = { band = it },
                modifier = Modifier.weight(1f)
            )
        }

        // Mode
        DropdownField(
            label = "モード",
            options = MODES,
            selected = mode,
            onSelect = { m ->
                mode = m
                val def = defaultRst(m)
                rstSent = def; rstRcvd = def
            },
            modifier = Modifier.fillMaxWidth()
        )

        // RST
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = rstSent,
                onValueChange = { rstSent = it },
                label = { Text("RST送") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                singleLine = true
            )
            OutlinedTextField(
                value = rstRcvd,
                onValueChange = { rstRcvd = it },
                label = { Text("RST受") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                singleLine = true
            )
        }

        // Name
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("名前") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            singleLine = true
        )

        // QTH / JCC
        Column {
            OutlinedTextField(
                value = qth,
                onValueChange = { v ->
                    qth = v
                    jccSuggestions = JCCStore.suggest(v)
                },
                label = { Text("QTH") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                singleLine = true
            )
            if (jccSuggestions.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    jccSuggestions.take(8).forEach { entry ->
                        ListItem(
                            headlineContent = { Text("${entry.pref} ${entry.city}") },
                            supportingContent = { Text(entry.id) },
                            modifier = Modifier.clickable {
                                qth = "${entry.pref}${entry.city}"
                                jcc = entry.id
                                jccSuggestions = emptyList()
                            }
                        )
                    }
                }
            }
            OutlinedTextField(
                value = jcc,
                onValueChange = { v ->
                    jcc = v.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }
                    JCCStore.lookup(jcc)?.let { e -> qth = "${e.pref}${e.city}" }
                },
                label = { Text("JCCコード") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
                singleLine = true
            )
        }

        // QSL
        DropdownField(
            label = "QSL",
            options = QSL_OPTIONS,
            selected = qsl,
            onSelect = { qsl = it },
            modifier = Modifier.fillMaxWidth()
        )

        // Comment
        OutlinedTextField(
            value = comment,
            onValueChange = { v ->
                val bare = v.removePrefix("%").removeSuffix("%")
                comment = if (bare.contains("移動地")) "%$bare%" else bare
            },
            label = { Text("備考") },
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(commentBringIntoView)
                .onFocusChanged { if (it.isFocused) scope.launch { commentBringIntoView.bringIntoView() } },
            minLines = 2
        )

        // Submit
        Button(
            onClick = {
                focusManager.clearFocus()
                if (editingQSO != null) {
                    vm.updateQSO(editingQSO.copy(
                        callsign = callsign, date = date, time = time,
                        freq = freq, band = band, mode = mode,
                        rstSent = rstSent, rstRcvd = rstRcvd,
                        name = name, qth = qth, jcc = jcc, qsl = qsl, comment = comment
                    ))
                    vm.showToast("更新しました")
                } else {
                    vm.insertQSO(QSO(
                        myCall = currentCall,
                        callsign = callsign, date = date, time = time,
                        freq = freq, band = band, mode = mode,
                        rstSent = rstSent, rstRcvd = rstRcvd,
                        name = name, qth = qth, jcc = jcc, qsl = qsl, comment = comment
                    ))
                    vm.showToast("記録しました")
                    reset(); setNow()
                }
                onSaved()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = callsign.isNotEmpty() && currentCall.isNotEmpty()
        ) {
            Text(if (editingQSO != null) "更新" else "登録")
        }

        Spacer(Modifier.height(32.dp))
    }
    } // Box imePadding
}

@Composable
fun DropdownField(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedTextField(
            value = selected.ifEmpty { "(選択)" },
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, null)
                }
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt.ifEmpty { "(なし)" }) },
                    onClick = { onSelect(opt); expanded = false }
                )
            }
        }
    }
}

class DateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = text.text.take(8)
        val out = StringBuilder()
        for (i in trimmed.indices) {
            out.append(trimmed[i])
            if (i == 3 || i == 5) out.append('/')
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = when {
                offset <= 3 -> offset
                offset <= 5 -> offset + 1
                offset <= 8 -> offset + 2
                else -> 10
            }
            override fun transformedToOriginal(offset: Int): Int = when {
                offset <= 3 -> offset
                offset <= 6 -> (offset - 1).coerceAtMost(8)
                else -> (offset - 2).coerceAtMost(8)
            }
        }
        return TransformedText(AnnotatedString(out.toString()), offsetMapping)
    }
}

class TimeVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = text.text.take(4)
        val out = StringBuilder()
        for (i in trimmed.indices) {
            out.append(trimmed[i])
            if (i == 1) out.append(':')
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = when {
                offset <= 1 -> offset
                offset <= 4 -> offset + 1
                else -> 5
            }
            override fun transformedToOriginal(offset: Int): Int = when {
                offset <= 1 -> offset
                else -> (offset - 1).coerceAtMost(4)
            }
        }
        return TransformedText(AnnotatedString(out.toString()), offsetMapping)
    }
}
