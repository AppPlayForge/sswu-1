package com.example.myTools.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

@Composable
fun DataManagementDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var showActivationDialog by remember { mutableStateOf(false) }
    var activationCode by remember { mutableStateOf("") }
    var isActivated by remember { mutableStateOf(DataManagementUtils.isActivated(context)) }

    // 導入文件啟動器 (CSV 全量)
    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val csv = reader.readText()
                    if (DataManagementUtils.importAllFromCsv(context, csv)) {
                        Toast.makeText(context, "數據導入成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "導入失敗: 格式不正確", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "導入失敗: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 導出 CSV 文件啟動器 (全量)
    val exportAllCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            try {
                val csv = DataManagementUtils.exportAllToCsv(context)
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(csv)
                    }
                }
                Toast.makeText(context, "全量數據導出成功", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "導出失敗: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("數據管理", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("備份與恢復您的八字、生日和月經紀錄。導出數據為高級服務，需購買激活碼。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                Text("Excel 兼容備份 (推薦)", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Button(
                        onClick = {
                            if (isActivated) {
                                exportAllCsvLauncher.launch("sswu_data_${System.currentTimeMillis()}.csv")
                            } else {
                                showActivationDialog = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("導出 CSV")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (isActivated) {
                                importCsvLauncher.launch(arrayOf("text/comma-separated-values", "text/csv", "application/octet-stream"))
                            } else {
                                showActivationDialog = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("導入 CSV")
                    }
                }

                if (!isActivated) {
                    TextButton(
                        onClick = { showActivationDialog = true },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            "服務未激活？點擊輸入激活碼",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Text(
                        "服務已激活",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("返回")
            }
        }
    )

    if (showActivationDialog) {
        val deviceId = DataManagementUtils.getDeviceId(context)

        AlertDialog(
            onDismissRequest = { showActivationDialog = false },
            title = { Text("激活服務") },
            text = {
                Column {
                    Text(
                        text = "請輸入您的激活碼（一機一碼）。如需購買請通過郵件聯係作者：",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    TextButton(
                        onClick = { sendActivationEmail(context, deviceId) },
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = "發送郵件",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "sswuss@outlook.com",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline
                            )
                        )
                    }

                    Text(
                        text = "價格：$1.3 USD / $9 HKD / ￥9 CNY",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("您的設備 ID (請提供給作者):", style = MaterialTheme.typography.labelSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SelectionContainer(modifier = Modifier.weight(1f)) {
                            Text(deviceId, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { 
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Device ID", deviceId))
                            Toast.makeText(context, "已複製設備 ID", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "複製", modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TextField(
                        value = activationCode,
                        onValueChange = { activationCode = it },
                        label = { Text("激活碼") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (DataManagementUtils.activate(context, activationCode)) {
                        Toast.makeText(context, "激活成功！", Toast.LENGTH_SHORT).show()
                        isActivated = true
                        showActivationDialog = false
                    } else {
                        Toast.makeText(context, "激活碼無效", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("激活")
                }
            },
            dismissButton = {
                TextButton(onClick = { showActivationDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

private fun sendActivationEmail(context: Context, deviceId: String) {
    val email = "sswuss@outlook.com"
    val subject = "購買激活碼"
    val body = "作者你好，\n\n我想購買激活服務，我的設備 ID 是：\n$deviceId\n\n謝謝！"
    
    val uri = "mailto:$email?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}".toUri()
    val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "未找到郵件應用，請手動發送郵件至 $email", Toast.LENGTH_LONG).show()
    }
}
