package com.example.myTools.note

import android.net.Uri
import androidx.compose.ui.graphics.Color

data class OpenedTxtFile(
    val uri: Uri,
    val fileName: String,
    val content: String
)

data class NoteColorOption(
    val name: String,
    val lightColor: Color,
    val darkColor: Color,
    val hex: String
)

val NOTE_COLORS = listOf(
    NoteColorOption("預設", Color.Transparent, Color.Transparent, ""),
    NoteColorOption("珊瑚紅", Color(0xFFFCE8E6), Color(0xFF4A2020), "#FCE8E6"),
    NoteColorOption("暖活力橙", Color(0xFFFEF0D5), Color(0xFF4D3810), "#FEF0D5"),
    NoteColorOption("檸檬黃", Color(0xFFFFF8D6), Color(0xFF4D4610), "#FFF8D6"),
    NoteColorOption("薄荷綠", Color(0xFFE6F4EA), Color(0xFF1B3D2B), "#E6F4EA"),
    NoteColorOption("天空藍", Color(0xFFE8F0FE), Color(0xFF1B2F4E), "#E8F0FE"),
    NoteColorOption("丁香紫", Color(0xFFF3E8FD), Color(0xFF3B1E54), "#F3E8FD"),
    NoteColorOption("櫻花粉", Color(0xFFFDE8F3), Color(0xFF4E1D3B), "#FDE8F3")
)
