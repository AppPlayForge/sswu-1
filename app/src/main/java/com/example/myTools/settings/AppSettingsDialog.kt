package com.example.myTools.settings

import android.Manifest
import android.app.AlarmManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Support
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.myTools.R

/**
 * 應用的通用設置對話框 - 重新設計的 UI
 */
@Composable
fun AppSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionUpdateTrigger by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionUpdateTrigger++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 權限狀態
    val isNotificationGranted = remember(permissionUpdateTrigger) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }
    val isGpsGranted = remember(permissionUpdateTrigger) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    val isAlarmGranted = remember(permissionUpdateTrigger) {
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
    }

    val notificationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionUpdateTrigger++ }
    val locationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionUpdateTrigger++ }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFFFDF7)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 分組 1：權限管理
                SettingsGroup(title = "功能權限", icon = Icons.Default.Security) {
                    PermissionRow(
                        title = "通知提醒",
                        isGranted = isNotificationGranted,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                openAppSettings(context)
                            }
                        }
                    )
                    PermissionRow(
                        title = "定位服務",
                        isGranted = isGpsGranted,
                        onClick = { locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
                    )
                    PermissionRow(
                        title = "精確鬧鐘",
                        isGranted = isAlarmGranted,
                        onClick = {
                            val intent =
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            context.startActivity(intent)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 分組 2：關於與聯繫
                SettingsGroup(title = "關於與支持", icon = Icons.Default.Support) {
                    SettingsItem(
                        title = "聯繫作者",
                        subtitle = "sswuss@outlook.com",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = "mailto:sswuss@outlook.com".toUri()
                                putExtra(Intent.EXTRA_SUBJECT, "App 反饋")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                            }
                        }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("關注我們", modifier = Modifier.weight(1f), fontSize = 16.sp)
                        IconButton(onClick = {
                            val intent =
                                Intent(Intent.ACTION_VIEW, "https://youtu.be/SDCEfVyvQis".toUri())
                            context.startActivity(intent)

                        }) {
                            Icon(Icons.Default.PlayCircle, "Youtube", tint = Color.Red)
                        }

                        IconButton(onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                "https://m.bilibili.com/space/297639121".toUri()
                            )
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.Subscriptions, "Bilibili", tint = Color(0xFFFB7299))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 分組 3：打賞支持
                SettingsGroup(title = "讚賞支持", icon = Icons.Default.Support) {
                    SupportSection(context)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 分組 4：作者作品
                SettingsGroup(title = "更多作品", icon = Icons.Default.Code) {
                    WorkLinkItem(
                        title = "分享App",
                        url = "https://github.com/AppPlayForge/sswu-1.git"
                    )
                    WorkLinkItem(title = "其它應用", url = "https://github.com/AppPlayForge")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 底部信息
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("版本號: v1.1.1", fontSize = 14.sp, color = Color.Gray)
                    TextButton(onClick = onDismiss) {
                        Text(
                            "返回",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8BC34A)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsGroup(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF795548)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF795548)
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
            content()
        }
    }
}

@Composable
fun WorkLinkItem(title: String, url: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                context.startActivity(intent)
            }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Language,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, modifier = Modifier.weight(1f), fontSize = 16.sp)
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
    }
}

@Composable
fun SupportSection(context: Context) {
    var wechatExpanded by remember { mutableStateOf(false) }
    var cryptoExpanded by remember { mutableStateOf(false) }

    val wechatRotation by animateFloatAsState(
        targetValue = if (wechatExpanded) 180f else 0f,
        label = "wechatRotation"
    )
    val cryptoRotation by animateFloatAsState(
        targetValue = if (cryptoExpanded) 180f else 0f,
        label = "cryptoRotation"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        // 微信部分
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { wechatExpanded = !wechatExpanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "微信掃碼支持",
                fontSize = 14.sp,
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.rotate(wechatRotation)
            )
        }

        AnimatedVisibility(visible = wechatExpanded) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = painterResource(id = R.drawable.wechat_pay_qr),
                    contentDescription = "微信收款碼",
                    modifier = Modifier
                        .size(160.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 0.5.dp,
            color = Color.LightGray.copy(alpha = 0.3f)
        )

        // 加密貨幣部分
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { cryptoExpanded = !cryptoExpanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "加密貨幣支持(USDT)",
                fontSize = 14.sp,
                color = Color(0xFF26A17B),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Color(0xFF26A17B),
                modifier = Modifier.rotate(cryptoRotation)
            )
        }

        AnimatedVisibility(visible = cryptoExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                val baseAddress = "0x82C1Fb29DcAB7C69842A17e9c56887185857d61E"
                val tronAddress = "TXdXDAZaaA2M9mCbXusUVXsBAT3Bfq13M3"

                // Base / EVM Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.base_pay_qr),
                        contentDescription = "Base QR",
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Base網絡",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF26A17B)
                    )
                    CryptoAddressRow(
                        context = context,
                        label = "Base錢包地址",
                        address = baseAddress
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))

                // Tron Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.tron_pay_qr),
                        contentDescription = "Tron QR",
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Tron網絡",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF26A17B)
                    )
                    CryptoAddressRow(
                        context = context,
                        label = "Tron錢包地址",
                        address = tronAddress
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "⚠️ 警告：複製的錢包地址必須對應網絡 (Base或Tron)，錯誤的選擇將導致資產永久丟失。",
                    fontSize = 14.sp,
                    color = Color.Red.copy(alpha = 0.7f),
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CryptoAddressRow(context: Context, label: String, address: String) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                address,
                fontSize = 10.sp,
                color = Color.DarkGray,
                maxLines = 1
            )
            IconButton(
                onClick = {
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText(label, address))
                    Toast.makeText(context, "已複製 $label", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF26A17B)
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(title: String, isGranted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, modifier = Modifier.weight(1f), fontSize = 16.sp)
        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isGranted) Color(0xFF4CAF50) else Color.Gray
        )
    }
}

@Composable
fun SettingsItem(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Text(title, fontSize = 16.sp)
        Text(subtitle, fontSize = 12.sp, color = Color.Gray)
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}
