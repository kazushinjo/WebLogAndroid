package com.weblog.android.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class JCCEntry(
    @SerializedName("id") val id: String,
    @SerializedName("pref") val pref: String,
    @SerializedName("city") val city: String,
    @SerializedName("yomi") val yomi: String
)

object JCCStore {
    private var entries: List<JCCEntry> = emptyList()
    private val byId = mutableMapOf<String, JCCEntry>()

    fun init(context: Context) {
        if (entries.isNotEmpty()) return
        try {
            val json = context.assets.open("jcc.json").bufferedReader().readText()
            entries = Gson().fromJson(json, Array<JCCEntry>::class.java).toList()
            for (e in entries) byId[e.id.uppercase()] = e
        } catch (_: Exception) {}
    }

    fun lookup(id: String): JCCEntry? = byId[id.uppercase()]

    fun suggest(query: String): List<JCCEntry> {
        if (query.isEmpty()) return emptyList()
        val q = query.lowercase()
        return entries.filter { e ->
            e.yomi.contains(q) ||
            e.city.contains(query) ||
            e.pref.contains(query) ||
            e.id.startsWith(q.uppercase())
        }.take(20)
    }
}
