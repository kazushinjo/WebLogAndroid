package com.weblog.android.utils

import com.weblog.android.data.QSO
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object ADIFParser {

    fun toAdif(qsos: List<QSO>, myCall: String): String {
        val sb = StringBuilder()
        sb.appendLine("ADIF export from WebLog for Android v1.08")
        sb.appendLine("<ADIF_VER:5>3.1.0")
        sb.appendLine("<EOH>")
        sb.appendLine()
        for (q in qsos) {
            val (dateUtc, timeUtc) = jstToUtc(q.date, q.time)
            sb.appendAdif("CALL", q.callsign)
            sb.appendAdif("QSO_DATE", dateUtc)
            sb.appendAdif("TIME_ON", timeUtc)
            sb.appendAdif("BAND", q.band.lowercase())
            sb.appendAdif("FREQ", q.freq)
            sb.appendAdif("MODE", q.mode)
            sb.appendAdif("RST_SENT", q.rstSent)
            sb.appendAdif("RST_RCVD", q.rstRcvd)
            if (q.name.isNotEmpty()) sb.appendAdif("NAME", q.name)
            if (q.qth.isNotEmpty()) sb.appendAdif("QTH", q.qth)
            if (q.comment.isNotEmpty()) sb.appendAdif("COMMENT", q.comment)
            sb.appendAdif("STATION_CALLSIGN", myCall)
            sb.appendLine("<EOR>")
            sb.appendLine()
        }
        return sb.toString()
    }

    fun parseAdif(text: String, myCall: String): List<QSO> {
        val records = mutableListOf<QSO>()
        val body = if (text.contains("<EOH>", ignoreCase = true))
            text.substringAfter("<EOH>", text.substringAfter("<eoh>", text))
        else text

        val pattern = Regex("<([^:>]+)(?::(\\d+))?(?::[^>]*)?>([^<]*)", RegexOption.IGNORE_CASE)
        val blocks = body.split(Regex("<EOR>", RegexOption.IGNORE_CASE))
        for (block in blocks) {
            val fields = mutableMapOf<String, String>()
            for (m in pattern.findAll(block)) {
                val tag = m.groupValues[1].uppercase()
                val len = m.groupValues[2].toIntOrNull()
                val raw = m.groupValues[3]
                fields[tag] = if (len != null) raw.take(len) else raw
            }
            val call = fields["CALL"] ?: continue
            if (call.isBlank()) continue
            val (dateJst, timeJst) = utcToJst(
                fields["QSO_DATE"] ?: "",
                (fields["TIME_ON"] ?: "").take(4)
            )
            val freq = fields["FREQ"] ?: ""
            val band = fields["BAND"]?.let { bandFromAdif(it) }
                ?: freqToBand(freq.toDoubleOrNull() ?: 0.0)
            records.add(
                QSO(
                    myCall = myCall,
                    callsign = call,
                    date = dateJst,
                    time = timeJst,
                    band = band,
                    freq = freq,
                    mode = fields["MODE"] ?: "SSB",
                    rstSent = fields["RST_SENT"] ?: "",
                    rstRcvd = fields["RST_RCVD"] ?: "",
                    name = fields["NAME"] ?: "",
                    qth = fields["QTH"] ?: "",
                    comment = fields["COMMENT"] ?: ""
                )
            )
        }
        return records
    }

    fun toCsv(qsos: List<QSO>, myCall: String): String {
        val sb = StringBuilder()
        sb.appendLine("コールサイン,日付,時刻,バンド,モード,RST送,RST受,名前,QTH,JCC,QSL,備考,自局")
        for (q in qsos) {
            sb.appendLine(listOf(q.callsign, q.date, q.time, q.band, q.mode,
                q.rstSent, q.rstRcvd, q.name, q.qth, q.jcc, q.qsl, q.comment, myCall)
                .joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" })
        }
        return sb.toString()
    }

    fun parseHamlogCsv(text: String, myCall: String): List<QSO> {
        return text.lines().filter { it.isNotBlank() }.mapNotNull { line ->
            val cols = parseCsvLine(line)
            if (cols.size < 7) return@mapNotNull null
            val call = (cols.getOrNull(0) ?: "").trim()
            if (call.isBlank()) return@mapNotNull null

            val date = parseHamlogDate((cols.getOrNull(1) ?: "").trim())
            // 時刻は "11:01J", "1101J", "11:01", "1101" など様々な形式に対応
            // 非数字を全て除去して 4桁化
            val time = (cols.getOrNull(2) ?: "")
                .replace(Regex("[^0-9]"), "")
                .padEnd(4, '0')
                .take(4)
            val rstRcvd = (cols.getOrNull(3) ?: "").trim()
            val rstSent = (cols.getOrNull(4) ?: "").trim()
            val freq = (cols.getOrNull(5) ?: "").trim()
            val mode = (cols.getOrNull(6) ?: "").trim().ifEmpty { "SSB" }
            val jcc = (cols.getOrNull(7) ?: "").trim()
            val qsl = (cols.getOrNull(9) ?: "").trim()
            val name = (cols.getOrNull(10) ?: "").trim()
            val qth = (cols.getOrNull(11) ?: "").trim()
            val rem1 = (cols.getOrNull(12) ?: "").trim()
            val rem2 = (cols.getOrNull(13) ?: "").trim()
            val comment = listOf(rem1, rem2).filter { it.isNotEmpty() }.joinToString(" ")

            QSO(
                myCall = myCall,
                callsign = call,
                date = date,
                time = time,
                band = freqToBand(freq.toDoubleOrNull() ?: 0.0),
                freq = freq,
                mode = mode,
                rstSent = rstSent.ifEmpty { "59" },
                rstRcvd = rstRcvd.ifEmpty { "59" },
                name = name,
                qth = qth,
                jcc = jcc,
                qsl = qsl,
                comment = comment
            )
        }
    }

    private fun parseHamlogDate(s: String): String {
        if (s.isBlank()) return ""
        val parts = s.split('/', '-', '.').map { it.trim() }.filter { it.isNotBlank() }
        if (parts.size != 3) return s.replace("/", "").replace("-", "").replace(".", "")
        val yyRaw = parts[0]
        val year = when (yyRaw.length) {
            4 -> yyRaw
            2 -> {
                val n = yyRaw.toIntOrNull() ?: return s
                // HAMLOG 慣習: 50以上は 19xx、50未満は 20xx
                if (n >= 50) "19$yyRaw" else "20$yyRaw"
            }
            else -> return s
        }
        val mm = parts[1].padStart(2, '0').takeLast(2)
        val dd = parts[2].padStart(2, '0').takeLast(2)
        return "$year$mm$dd"
    }

    fun parseCsv(text: String, myCall: String): List<QSO> {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.size < 2) return emptyList()
        val header = parseCsvLine(lines[0]).map { it.trim() }
        return lines.drop(1).mapNotNull { line ->
            val cols = parseCsvLine(line)
            fun col(name: String) = cols.getOrNull(header.indexOf(name)) ?: ""
            val call = col("コールサイン").ifEmpty { cols.getOrNull(0) ?: "" }
            if (call.isBlank()) return@mapNotNull null
            val freq = col("周波数")
            QSO(
                myCall = myCall,
                callsign = call,
                date = col("日付"),
                time = col("時刻"),
                band = col("バンド").ifEmpty { freqToBand(freq.toDoubleOrNull() ?: 0.0) },
                freq = freq,
                mode = col("モード").ifEmpty { "SSB" },
                rstSent = col("RST送"),
                rstRcvd = col("RST受"),
                name = col("名前"),
                qth = col("QTH"),
                jcc = col("JCC"),
                qsl = col("QSL"),
                comment = col("備考")
            )
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuote = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuote && i + 1 < line.length && line[i + 1] == '"' -> { sb.append('"'); i++ }
                c == '"' -> inQuote = !inQuote
                c == ',' && !inQuote -> { result.add(sb.toString()); sb.clear() }
                else -> sb.append(c)
            }
            i++
        }
        result.add(sb.toString())
        return result
    }

    private fun StringBuilder.appendAdif(tag: String, value: String) {
        if (value.isNotEmpty()) append("<$tag:${value.length}>$value\n")
    }

    private fun jstToUtc(dateJst: String, timeJst: String): Pair<String, String> {
        return try {
            val dt = LocalDateTime.parse(
                "${dateJst.padEnd(8, '0')} ${timeJst.padEnd(4, '0')}",
                DateTimeFormatter.ofPattern("yyyyMMdd HHmm")
            )
            val utc = ZonedDateTime.of(dt, ZoneId.of("Asia/Tokyo"))
                .withZoneSameInstant(ZoneId.of("UTC"))
            Pair(utc.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                utc.format(DateTimeFormatter.ofPattern("HHmm")))
        } catch (e: Exception) {
            Pair(dateJst, timeJst)
        }
    }

    private fun utcToJst(dateUtc: String, timeUtc: String): Pair<String, String> {
        return try {
            val dt = LocalDateTime.parse(
                "${dateUtc.padEnd(8, '0')} ${timeUtc.padEnd(4, '0')}",
                DateTimeFormatter.ofPattern("yyyyMMdd HHmm")
            )
            val jst = ZonedDateTime.of(dt, ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.of("Asia/Tokyo"))
            Pair(jst.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                jst.format(DateTimeFormatter.ofPattern("HHmm")))
        } catch (e: Exception) {
            Pair(dateUtc, timeUtc)
        }
    }

    private fun bandFromAdif(s: String): String {
        val lower = s.lowercase().replace(" ", "")
        return when {
            lower.startsWith("160") || lower == "1.9mhz" -> "1.9MHz"
            lower.startsWith("80") || lower == "3.5mhz" -> "3.5MHz"
            lower.startsWith("40") || lower == "7mhz" -> "7MHz"
            lower.startsWith("30") || lower == "10mhz" -> "10MHz"
            lower.startsWith("20") || lower == "14mhz" -> "14MHz"
            lower.startsWith("17") || lower == "18mhz" -> "18MHz"
            lower.startsWith("15") || lower == "21mhz" -> "21MHz"
            lower.startsWith("12") || lower == "24mhz" -> "24MHz"
            lower.startsWith("10m") || lower == "28mhz" -> "28MHz"
            lower.startsWith("6") || lower == "50mhz" -> "50MHz"
            lower.startsWith("2m") || lower == "144mhz" -> "144MHz"
            lower.startsWith("70cm") || lower == "430mhz" -> "430MHz"
            lower.startsWith("23cm") || lower == "1200mhz" -> "1200MHz"
            lower.startsWith("13cm") || lower == "2400mhz" -> "2400MHz"
            else -> ""
        }
    }
}
