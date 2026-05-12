package com.weblog.android.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QSODao {
    @Query("SELECT * FROM qso WHERE myCall = :myCall ORDER BY date DESC, time DESC")
    fun getAll(myCall: String): Flow<List<QSO>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(qso: QSO)

    @Update
    suspend fun update(qso: QSO)

    @Delete
    suspend fun delete(qso: QSO)

    @Query("DELETE FROM qso WHERE myCall = :myCall")
    suspend fun deleteAll(myCall: String)

    @Query("SELECT * FROM qso WHERE myCall = :myCall AND callsign = :callsign ORDER BY date DESC, time DESC")
    suspend fun findByCallsign(myCall: String, callsign: String): List<QSO>

    @Query("SELECT DISTINCT callsign, name, qth, jcc FROM qso WHERE myCall = :myCall AND callsign LIKE :prefix || '%' ORDER BY callsign ASC LIMIT 10")
    suspend fun suggestCallsigns(myCall: String, prefix: String): List<CallsignSuggestion>
}

data class CallsignSuggestion(
    val callsign: String,
    val name: String,
    val qth: String,
    val jcc: String
)
