package com.example.myTools.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myTools.R

/**
 * 打賞頁面
 * */
@Composable
fun SupportScreen() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 64.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "打賞支持",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            "如果覺得這個App對你有幫助\n歡迎打賞支持作者",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SupportSection(context)
            }
        }
    }
}

@Composable
fun SupportSection(context: Context) {
    var wechatExpanded by rememberSaveable { mutableStateOf(true) }
    var cryptoExpanded by rememberSaveable { mutableStateOf(false) }

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
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "微信掃碼支持",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
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
                        .size(200.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            thickness = 0.5.dp,
            color = Color.Gray.copy(alpha = 0.3f)
        )

        // 加密貨幣部分
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { cryptoExpanded = !cryptoExpanded }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "加密貨幣支持 (USDT)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
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
                        "Base 網絡",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF26A17B)
                    )
                    CryptoAddressRow(
                        context = context,
                        label = "Base 錢包地址",
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
                        "Tron 網絡",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF26A17B)
                    )
                    CryptoAddressRow(
                        context = context,
                        label = "Tron 錢包地址",
                        address = tronAddress
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "⚠️ 警告：複製的錢包地址必須對應網絡 (Base或Tron)，錯誤的選擇將導致資產永久丟失。",
                    fontSize = 14.sp,
                    color = Color.Red.copy(alpha = 0.8f),
                    lineHeight = 16.sp,
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
                color = Color.Gray,
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false)
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
