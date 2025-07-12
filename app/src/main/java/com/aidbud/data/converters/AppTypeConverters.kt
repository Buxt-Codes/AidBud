package com.aidbud.data.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object AppTypeConverters {

    // Converts a List<String> to a JSON String for storage
    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return Gson().toJson(list)
    }

    // Converts a JSON String back to a List<String>
    @TypeConverter
    fun toStringList(json: String?): List<String>? {
        if (json == null) return null
        val type = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(json, type)
    }
}