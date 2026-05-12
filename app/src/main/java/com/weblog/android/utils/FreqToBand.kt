package com.weblog.android.utils

fun freqToBand(freqMHz: Double): String = when {
    freqMHz in 1.8..2.0 -> "1.9MHz"
    freqMHz in 3.4..4.0 -> "3.5MHz"
    freqMHz in 6.9..7.5 -> "7MHz"
    freqMHz in 10.0..10.2 -> "10MHz"
    freqMHz in 14.0..14.4 -> "14MHz"
    freqMHz in 18.0..18.2 -> "18MHz"
    freqMHz in 21.0..21.5 -> "21MHz"
    freqMHz in 24.8..25.0 -> "24MHz"
    freqMHz in 28.0..29.8 -> "28MHz"
    freqMHz in 50.0..54.0 -> "50MHz"
    freqMHz in 140.0..150.0 -> "144MHz"
    freqMHz in 420.0..440.0 -> "430MHz"
    freqMHz in 1200.0..1300.0 -> "1200MHz"
    freqMHz in 2400.0..2450.0 -> "2400MHz"
    else -> ""
}

fun defaultRst(mode: String): String = when (mode) {
    "CW", "RTTY", "FT8", "FT4", "JS8", "PSK31" -> "599"
    else -> "59"
}

val MODES = listOf("SSB", "CW", "FM", "AM", "FT8", "FT4", "RTTY", "JS8", "PSK31", "SSTV", "D-STAR", "C4FM", "DMR")
val BANDS = listOf("", "1.9MHz", "3.5MHz", "7MHz", "10MHz", "14MHz", "18MHz", "21MHz", "24MHz", "28MHz", "50MHz", "144MHz", "430MHz", "1200MHz", "2400MHz")
val QSL_OPTIONS = listOf("", "ビューロー", "ダイレクト", "電子QSL", "なし")
