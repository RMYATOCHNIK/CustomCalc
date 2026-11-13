package com.rmyatochnik.customcalculator

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HistoryItem(
    val id: Long = System.currentTimeMillis(),
    val expression: String,
    val result: String,
    val timestamp: String
)

object HistoryManager {
    private const val PREFS_NAME = "calc_history"
    private const val KEY_HISTORY = "history_list"
    private const val MAX_HISTORY_ITEMS = 50

    fun getHistory(context: Context): List<HistoryItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        val list = mutableListOf<HistoryItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    HistoryItem(
                        id = obj.optLong("id", System.currentTimeMillis()),
                        expression = obj.getString("expression"),
                        result = obj.getString("result"),
                        timestamp = obj.getString("timestamp")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.reversed() // Most recent first
    }

    fun addHistory(context: Context, expression: String, result: String) {
        if (expression.isBlank() || result.isBlank() || result == "Error" || result == "На 0 нельзя делить") return
        val current = getHistory(context).toMutableList()
        val sdf = SimpleDateFormat("HH:mm, dd MMM", Locale.getDefault())
        val timestamp = sdf.format(Date())

        // Don't add duplicate of the very last item
        if (current.isNotEmpty() && current.first().expression == expression && current.first().result == result) {
            return
        }

        val newItem = HistoryItem(
            id = System.currentTimeMillis(),
            expression = expression,
            result = result,
            timestamp = timestamp
        )

        // Keep newest first
        current.add(0, newItem)
        if (current.size > MAX_HISTORY_ITEMS) {
            current.removeAt(current.size - 1)
        }

        saveHistoryList(context, current)
    }

    fun deleteItem(context: Context, id: Long) {
        val current = getHistory(context).filter { it.id != id }
        saveHistoryList(context, current)
    }

    fun clearHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun saveHistoryList(context: Context, list: List<HistoryItem>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("expression", item.expression)
                put("result", item.result)
                put("timestamp", item.timestamp)
            }
            array.put(obj)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }
}
