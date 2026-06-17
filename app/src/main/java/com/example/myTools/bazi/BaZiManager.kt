package com.example.myTools.bazi

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object BaZiManager {
    private const val PREF_NAME = "bazi_prefs"
    private const val KEY_LIST = "bazi_list"
    private val gson = Gson()

    fun loadList(context: Context): List<BaZiRecord> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_LIST, null) ?: return emptyList()
        val type = object : TypeToken<List<BaZiRecord>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveList(context: Context, list: List<BaZiRecord>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(list)
        prefs.edit { putString(KEY_LIST, json) }
    }

    fun addOrUpdateRecord(context: Context, record: BaZiRecord) {
        val list = loadList(context).toMutableList()
        val index = list.indexOfFirst { it.id == record.id }
        if (index != -1) {
            list[index] = record
        } else {
            list.add(record)
        }
        saveList(context, list)
    }

    fun deleteRecord(context: Context, id: Long) {
        val list = loadList(context).filter { it.id != id }
        saveList(context, list)
    }
}
