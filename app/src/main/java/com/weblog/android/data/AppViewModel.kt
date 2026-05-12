package com.weblog.android.data

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = QSODatabase.getInstance(application).qsoDao()
    private val ds = application.dataStore

    private val CALLSIGNS_KEY = stringPreferencesKey("callsigns")
    private val CURRENT_CALL_KEY = stringPreferencesKey("current_call")

    private val _callsigns = MutableStateFlow<List<String>>(emptyList())
    val callsigns: StateFlow<List<String>> = _callsigns

    private val _currentCall = MutableStateFlow("")
    val currentCall: StateFlow<String> = _currentCall

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage

    val qsos: StateFlow<List<QSO>> = _currentCall
        .flatMapLatest { call -> if (call.isEmpty()) flowOf(emptyList()) else dao.getAll(call) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            ds.data.collect { prefs ->
                val saved = prefs[CALLSIGNS_KEY]?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
                _callsigns.value = saved
                val cur = prefs[CURRENT_CALL_KEY] ?: ""
                _currentCall.value = if (cur in saved) cur else saved.firstOrNull() ?: ""
            }
        }
    }

    fun addCallsign(call: String) {
        val upper = call.uppercase().trim()
        if (upper.isEmpty()) return
        viewModelScope.launch {
            val list = (_callsigns.value + upper).distinct()
            saveCallsigns(list, upper)
        }
    }

    fun removeCallsign(call: String) {
        viewModelScope.launch {
            val list = _callsigns.value.filter { it != call }
            val cur = if (_currentCall.value == call) list.firstOrNull() ?: "" else _currentCall.value
            saveCallsigns(list, cur)
        }
    }

    fun setCurrentCall(call: String) {
        viewModelScope.launch {
            ds.edit { it[CURRENT_CALL_KEY] = call }
            _currentCall.value = call
        }
    }

    private suspend fun saveCallsigns(list: List<String>, current: String) {
        ds.edit {
            it[CALLSIGNS_KEY] = list.joinToString(",")
            it[CURRENT_CALL_KEY] = current
        }
        _callsigns.value = list
        _currentCall.value = current
    }

    fun insertQSO(qso: QSO) = viewModelScope.launch { dao.insert(qso) }
    fun updateQSO(qso: QSO) = viewModelScope.launch { dao.update(qso) }
    fun deleteQSO(qso: QSO) = viewModelScope.launch { dao.delete(qso) }
    fun deleteAll() = viewModelScope.launch { dao.deleteAll(_currentCall.value) }

    suspend fun findDuplicates(callsign: String): List<QSO> =
        dao.findByCallsign(_currentCall.value, callsign)

    suspend fun suggestCallsigns(prefix: String): List<CallsignSuggestion> =
        dao.suggestCallsigns(_currentCall.value, prefix)

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
