package com.example.myTools.bazi

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object BaZiManager {
    private const val PREF_NAME = "bazi_prefs"
    private const val KEY_LIST = "bazi_list"
    private const val KEY_TRASH_LIST = "bazi_trash_list"
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

    fun loadTrashList(context: Context): List<BaZiRecord> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_TRASH_LIST, null) ?: return emptyList()
        val type = object : TypeToken<List<BaZiRecord>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveTrashList(context: Context, list: List<BaZiRecord>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(list)
        prefs.edit { putString(KEY_TRASH_LIST, json) }
    }

    fun moveToTrash(context: Context, id: Long) {
        val activeList = loadList(context).toMutableList()
        val index = activeList.indexOfFirst { it.id == id }
        if (index != -1) {
            val item = activeList.removeAt(index)
            val trashedItem = item.copy(deletedAt = System.currentTimeMillis())
            saveList(context, activeList)

            val trashList = loadTrashList(context).toMutableList()
            trashList.add(0, trashedItem)
            saveTrashList(context, trashList)
        }
    }

    fun restoreFromTrash(context: Context, id: Long) {
        val trashList = loadTrashList(context).toMutableList()
        val index = trashList.indexOfFirst { it.id == id }
        if (index != -1) {
            val item = trashList.removeAt(index)
            val restoredItem = item.copy(deletedAt = 0L)
            saveTrashList(context, trashList)

            val activeList = loadList(context).toMutableList()
            if (restoredItem.isPinned) {
                activeList.add(0, restoredItem)
            } else {
                activeList.add(restoredItem)
            }
            saveList(context, activeList)
        }
    }

    fun permanentlyDeleteFromTrash(context: Context, id: Long) {
        val trashList = loadTrashList(context).filter { it.id != id }
        saveTrashList(context, trashList)
    }

    fun emptyTrash(context: Context) {
        saveTrashList(context, emptyList())
    }

    fun addOrUpdateRecord(context: Context, record: BaZiRecord) {
        val list = loadList(context).toMutableList()
        val index = list.indexOfFirst { it.id == record.id }
        if (index != -1) {
            list[index] = record
        } else {
            if (record.isPinned) {
                list.add(0, record)
            } else {
                list.add(record)
            }
        }
        saveList(context, list)
    }

    fun togglePinRecord(context: Context, id: Long): List<BaZiRecord> {
        val list = loadList(context).toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index != -1) {
            val record = list[index]
            val newPinned = !record.isPinned
            val updatedRecord = record.copy(isPinned = newPinned)
            list.removeAt(index)
            if (newPinned) {
                list.add(0, updatedRecord)
            } else {
                val firstUnpinnedIndex = list.indexOfFirst { !it.isPinned }
                if (firstUnpinnedIndex != -1) {
                    list.add(firstUnpinnedIndex, updatedRecord)
                } else {
                    list.add(updatedRecord)
                }
            }
            saveList(context, list)
            return list
        }
        return list
    }
}
