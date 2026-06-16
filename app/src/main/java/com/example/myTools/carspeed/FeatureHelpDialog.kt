package com.example.myTools.carspeed

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp


@Composable
fun FeatureHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Lightbulb, contentDescription = null) },
        title = { Text(text = "功能提示", fontSize = 20.sp) },
        text = { Text(text = "點擊右上角的燈泡圖示，\n可以讓螢幕保持10分鐘或恆亮。") },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("我知道了")
            }
        }
    )
}
