package com.example.culator

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal const val HISTORY_RETENTION_MS = 7L * 24 * 60 * 60 * 1000

internal data class HistoryEntry(val equation: String, val result: String, val timestamp: Long)

internal fun recentHistory(entries: List<HistoryEntry>, now: Long, retentionMs: Long = HISTORY_RETENTION_MS): List<HistoryEntry> =
    entries.filter { it.timestamp >= now - retentionMs }.sortedByDescending { it.timestamp }

internal fun confirmedEntry(equation: String, now: Long): HistoryEntry? =
    evaluate(equation)?.let { HistoryEntry(equation, it.display(), now) }

internal class HistoryStore(context: Context, private val retentionMs: Long = HISTORY_RETENTION_MS) {
    private val preferences = context.applicationContext.getSharedPreferences("calculator_history", Context.MODE_PRIVATE)

    fun load(now: Long = System.currentTimeMillis()): List<HistoryEntry> {
        val array = runCatching { JSONArray(preferences.getString("entries", "[]")) }.getOrElse { JSONArray() }
        val saved = (0 until array.length()).mapNotNull { index ->
            runCatching {
                val entry = array.getJSONObject(index)
                HistoryEntry(entry.getString("equation"), entry.getString("result"), entry.getLong("timestamp"))
            }.getOrNull()
        }
        val recent = recentHistory(saved, now, retentionMs)
        if (recent != saved) save(recent)
        return recent
    }

    fun add(equation: String, now: Long = System.currentTimeMillis()): List<HistoryEntry> {
        val recent = load(now)
        val entry = confirmedEntry(equation, now) ?: return recent
        return (listOf(entry) + recent).also(::save)
    }

    fun delete(entry: HistoryEntry): List<HistoryEntry> {
        val entries = load().toMutableList()
        entries.remove(entry)
        return entries.toList().also(::save)
    }

    fun clear(): List<HistoryEntry> = emptyList<HistoryEntry>().also(::save)

    private fun save(entries: List<HistoryEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(JSONObject().put("equation", entry.equation).put("result", entry.result).put("timestamp", entry.timestamp))
        }
        preferences.edit().putString("entries", array.toString()).apply()
    }
}
