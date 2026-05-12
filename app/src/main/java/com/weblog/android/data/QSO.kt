package com.weblog.android.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "qso")
data class QSO(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val myCall: String = "",
    val callsign: String = "",
    val date: String = "",
    val time: String = "",
    val band: String = "",
    val freq: String = "",
    val mode: String = "SSB",
    val rstSent: String = "59",
    val rstRcvd: String = "59",
    val name: String = "",
    val qth: String = "",
    val jcc: String = "",
    val qsl: String = "",
    val comment: String = ""
)
