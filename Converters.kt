package com.example.memopwd.data

import androidx.room.TypeConverter
import org.json.JSONArray

object Converters {

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        if (value.isNullOrEmpty()) return "[]"
        return JSONArray(value).toString()
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(value)
            List(arr.length()) { i -> arr.optString(i) }.filter { it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromLongList(value: List<Long>?): String {
        if (value.isNullOrEmpty()) return "[]"
        return JSONArray(value).toString()
    }

    @TypeConverter
    fun toLongList(value: String?): List<Long> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(value)
            List(arr.length()) { i -> arr.optLong(i) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
