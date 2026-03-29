package com.sr.fixit106.room

import androidx.compose.ui.input.key.type
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class StringListConverters {
    private val listType: Type = object : TypeToken<List<String>>() {}.type

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return if (value == null) {
            null
        } else {
            Gson().fromJson(value, listType)
        }
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return if(list == null){
            null
        } else{
            Gson().toJson(list)
        }
    }
}