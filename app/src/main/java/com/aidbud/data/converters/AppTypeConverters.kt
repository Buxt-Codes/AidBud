package com.aidbud.data.converters

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import java.nio.ByteBuffer

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

    @TypeConverter
    fun fromUriList(list: List<Uri>?): String? {
        return list?.map { it.toString() }?.let { Gson().toJson(it) }
    }

    @TypeConverter
    fun toUriList(json: String?): List<Uri>? {
        if (json == null) return null
        val type = object : TypeToken<List<String>>() {}.type
        val list = Gson().fromJson<List<String>>(json, type)
        return list.map { it.toUri() }
    }

    @TypeConverter
    fun fromFloatList(list: List<Float>): String {
        return list.joinToString(",")
    }

    @TypeConverter
    fun toFloatList(data: String): List<Float> {
        return data.split(",").map { it.toFloat() }
    }

    @TypeConverter
    fun fromMap(map: Map<String, Any>?): String {
        return Gson().toJson(map)
    }

    @TypeConverter
    fun toMap(json: String): Map<String, Any> {
        return Gson().fromJson(json, object : TypeToken<Map<String, Any>>() {}.type)
    }

    @TypeConverter
    fun fromFloatArray(array: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(4 * array.size)
        array.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    @TypeConverter
    fun toFloatArray(bytes: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(bytes)
        val floatArray = FloatArray(bytes.size / 4)
        for (i in floatArray.indices) {
            floatArray[i] = buffer.getFloat()
        }
        return floatArray
    }

    @TypeConverter
    fun fromJSONObject(jsonObject: JSONObject?): String? {
        return jsonObject?.toString()
    }

    @TypeConverter
    fun toJSONObject(jsonString: String?): JSONObject? {
        return try {
            if (jsonString.isNullOrEmpty()) null else JSONObject(jsonString)
        } catch (e: Exception) {
            null
        }
    }
}