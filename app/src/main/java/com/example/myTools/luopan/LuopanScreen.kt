package com.example.myTools.luopan

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import android.hardware.SensorManager
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myTools.KeepScreenOn
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withSave

// --- 數據源 ---
val twentyFourMountains = listOf(
    "子", "癸", "丑", "艮", "寅", "甲",
    "卯", "乙", "辰", "巽", "巳", "丙",
    "午", "丁", "未", "坤", "申", "庚",
    "酉", "辛", "戌", "乾", "亥", "壬"
)

val sixtyFourHexagrams = listOf(
    "乾", "夬", "大有", "大壯", "小畜", "需", "大畜", "泰",
    "履", "兌", "睽", "歸妹", "中孚", "節", "損", "臨",
    "同人", "革", "離", "豐", "家人", "既濟", "賁", "明夷",
    "無妄", "隨", "噬嗑", "震", "益", "屯", "頤", "復",
    "姤", "大過", "鼎", "恆", "巽", "井", "蠱", "升",
    "訟", "困", "未濟", "解", "渙", "坎", "蒙", "師",
    "遁", "咸", "旅", "小過", "漸", "蹇", "艮", "謙",
    "否", "萃", "晉", "豫", "觀", "比", "剝", "坤"
)

// 八個方位 (改為中文，方便閱讀)
val directions = listOf(
    "北" to 0, "東北" to 45, "東" to 90, "東南" to 135,
    "南" to 180, "西南" to 225, "西" to 270, "西北" to 315
)

@Composable
fun LuopanScreen(onBack: () -> Unit) {
    // 保持螢幕常亮
    KeepScreenOn()

    val context = LocalContext.current
    val compassManager = remember { CompassManager(context) }

    // 角度與精準度狀態
    val smoothAzimuth = remember { Animatable(0f) }
    var sensorAccuracy by remember { mutableIntStateOf(SensorManager.SENSOR_STATUS_ACCURACY_HIGH) }

    // 縮放與平移狀態
    var zoomLevel by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val animatedZoom by animateFloatAsState(targetValue = zoomLevel, animationSpec = tween(400), label = "Zoom")
    val animatedOffset by animateOffsetAsState(targetValue = panOffset, animationSpec = tween(400), label = "Pan")

    LaunchedEffect(Unit) {
        compassManager.compassFlow.collectLatest { data ->
            sensorAccuracy = data.accuracy
            
            val targetDegree = data.azimuth
            val current = smoothAzimuth.value
            var target = targetDegree
            
            if (current - target > 180) target += 360
            else if (target - current > 180) target -= 360
            
            // 使用 Spring 動畫：反應極快且具備物理真實感，不會像 tween 那樣死板
            smoothAzimuth.animateTo(
                target, 
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    val displayDegree = (smoothAzimuth.value % 360 + 360) % 360

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101828)),
        contentAlignment = Alignment.Center
    ) {
        // 1. 羅盤層 (GPU 加速旋轉)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(zoomLevel) {
                    if (zoomLevel > 1f) {
                        detectTransformGestures { _, pan, _, _ ->
                            val newX = (panOffset.x + pan.x).coerceIn(-1000f, 1000f)
                            val newY = (panOffset.y + pan.y).coerceIn(-1000f, 1000f)
                            panOffset = Offset(newX, newY)
                        }
                    }
                }
        ) {
            // ★ 性能優化核心 ★
            // 我們不再傳入 rotation 給 Canvas 讓它一直重畫
            // 而是直接旋轉這個 Composable 元件 (GPU 紋理旋轉)
            StaticLuopanDial(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.95f)
                    .aspectRatio(1f)
                    .graphicsLayer {
                        // ★ 性能優化：在 Lambda 中讀取值，僅觸發 Layer 旋轉，不觸發 Recomposition ★
                        scaleX = animatedZoom
                        scaleY = animatedZoom
                        translationX = animatedOffset.x
                        translationY = animatedOffset.y
                        rotationZ = -smoothAzimuth.value
                    }
            )
        }

        // 2. 十字天心紅線 (固定層)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height / 2

            drawLine(Color.Red.copy(alpha = 0.6f), Offset(cx, 0f), Offset(cx, size.height), 3f)
            drawLine(Color.Red.copy(alpha = 0.6f), Offset(0f, cy), Offset(size.width, cy), 3f)

            val path = Path().apply {
                moveTo(cx, 30f)
                lineTo(cx - 20f, 60f)
                lineTo(cx + 20f, 60f)
                close()
            }
            drawPath(path, Color.Red)
        }

        // 3. UI 覆蓋層
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 48.dp, start = 8.dp, end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 第一行：包含返回按鈕與標題文字
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // 返回按鈕 (靠左對齊)
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                    )
                }

                // 標題文字 (居中對齊)
                if (zoomLevel == 1f) {
                    Text(
                        "風水羅盤 (九運)",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 第二行：度數文字 (僅在全覽模式顯示)
            if (zoomLevel == 1f) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${displayDegree.toInt()}°",
                    color = Color(0xFFFFD700),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )

                // 磁場校正提示
                if (sensorAccuracy < SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color.Red.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "請揮動手機以校正磁場 (畫8字)",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ZoomButton(Icons.Default.KeyboardArrowUp, "北", { zoomLevel = 2.5f; panOffset = Offset(0f, 1100f) }, zoomLevel > 1f && panOffset.y > 500f)
                ZoomButton(Icons.Default.KeyboardArrowDown, "南", { zoomLevel = 2.5f; panOffset = Offset(0f, -1100f) }, zoomLevel > 1f && panOffset.y < -500f)
                ZoomButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "西", { zoomLevel = 2.5f; panOffset = Offset(1100f, 0f) }, zoomLevel > 1f && panOffset.x > 500f)
                ZoomButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "東", { zoomLevel = 2.5f; panOffset = Offset(-1100f, 0f) }, zoomLevel > 1f && panOffset.x < -500f)
                ZoomButton(Icons.Default.ZoomOutMap, "全覽", { zoomLevel = 1f; panOffset = Offset.Zero }, zoomLevel == 1f)
            }
        }
    }
}


// 靜態羅盤 (只負責畫，不負責轉，由外部 graphicsLayer 控制旋轉)
@Composable
fun StaticLuopanDial(
    modifier: Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val radius = size.width / 2

        // 半徑比例
        val rTickStart = radius * 0.94f
        val rTickEnd = radius * 0.99f
        val rDegreeText = radius * 0.88f
        val rDirection = radius * 0.80f // ★ 新增：方位標識層 (N, S, E, W)
        val r64Outer = radius * 0.75f   // 內縮以容納方位
        val r64Inner = radius * 0.62f
        val r24Outer = radius * 0.62f
        val r24Inner = radius * 0.48f
        val rPool = radius * 0.18f

        // 底盤
        drawCircle(color = Color(0xFF0D121D), radius = radius)

        // --- 1. 刻度 & 數字 ---
        drawCircle(color = Color.White, radius = rTickStart, style = Stroke(width = 1f))

        val textPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 24f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        for (i in 0 until 360) {
            val isMajor = i % 10 == 0
            val angleRad = Math.toRadians(i.toDouble() - 90)

            // 刻度線
            val tickLen = if (isMajor) (rTickEnd - rTickStart) else (rTickEnd - rTickStart) * 0.5f
            val start = Offset(cx + rTickStart * cos(angleRad).toFloat(), cy + rTickStart * sin(angleRad).toFloat())
            val end = Offset(cx + (rTickStart + tickLen) * cos(angleRad).toFloat(), cy + (rTickStart + tickLen) * sin(angleRad).toFloat())
            drawLine(color = if (isMajor) Color.White else Color.Gray, start = start, end = end, strokeWidth = if (isMajor) 2f else 1f)

            // 數字 (每10度)
            if (i % 10 == 0) {
                drawContext.canvas.nativeCanvas.apply {
                    withSave {
                        val tx = cx + rDegreeText * cos(angleRad).toFloat()
                        val ty = cy + rDegreeText * sin(angleRad).toFloat()
                        translate(tx, ty)
                        rotate(i.toFloat() + 180)
                        val textOffset = (textPaint.descent() + textPaint.ascent()) / 2
                        drawText("$i", 0f, -textOffset, textPaint)
                    }
                }
            }
        }

        // --- 2. ★ 新增：方位標識 (N, E, S, W) ★ ---
        val dirPaint = Paint().apply {
            textSize = 40f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        directions.forEach { (label, angle) ->
            val angleRad = Math.toRadians(angle.toDouble() - 90)

            drawContext.canvas.nativeCanvas.apply {
                withSave {
                    val tx = cx + rDirection * cos(angleRad).toFloat()
                    val ty = cy + rDirection * sin(angleRad).toFloat()
                    translate(tx, ty)
                    rotate(angle.toFloat() + 180) // 讓字頭朝內

                    // N (0度) 用紅色，其他用白色 (或金色)
                    dirPaint.color =
                        if (angle == 0) android.graphics.Color.RED else "#FFD700".toColorInt()

                    val textOffset = (dirPaint.descent() + dirPaint.ascent()) / 2
                    drawText(label, 0f, -textOffset, dirPaint)
                }
            }
        }

        // --- 3. 六十四卦 ---
        drawCircle(brush = Brush.radialGradient(colors = listOf(Color(0xFF243045), Color(0xFF1A2230)), radius = r64Outer), radius = r64Outer)
        val step64 = 360f / 64f
        for (i in 0 until 64) {
            val angle = i * step64
            val rad = Math.toRadians(angle.toDouble() - 90)
            drawLine(Color(0xFF4B5563), Offset(cx + r64Inner * cos(rad).toFloat(), cy + r64Inner * sin(rad).toFloat()), Offset(cx + r64Outer * cos(rad).toFloat(), cy + r64Outer * sin(rad).toFloat()), strokeWidth = 1f)

            val textStr = sixtyFourHexagrams[i]
            val textAngle = angle + (step64 / 2)
            val textRad = Math.toRadians(textAngle.toDouble() - 90)
            val textR = (r64Outer + r64Inner) / 2

            drawContext.canvas.nativeCanvas.apply {
                withSave {
                    translate(
                        cx + textR * cos(textRad).toFloat(),
                        cy + textR * sin(textRad).toFloat()
                    )
                    rotate(textAngle + 180)
                    val paint = Paint().apply {
                        color = "#E0E0E0".toColorInt(); textSize = 26f; textAlign =
                        Paint.Align.CENTER; typeface = Typeface.DEFAULT; isAntiAlias = true
                    }

                    if (textStr.length == 1) {
                        drawText(textStr, 0f, 10f, paint)
                    } else {
                        val lineHeight = paint.textSize * 1.0f
                        val totalHeight = lineHeight * (textStr.length - 1)
                        val startY = -(totalHeight / 2) + (paint.textSize * 0.3f)
                        textStr.forEachIndexed { index, char ->
                            drawText(
                                char.toString(),
                                0f,
                                startY + (index * lineHeight),
                                paint
                            )
                        }
                    }
                }
            }
        }

        // --- 4. 二十四山 ---
        drawCircle(brush = Brush.radialGradient(colors = listOf(Color(0xFF1E2A3F), Color(0xFF151E2E)), radius = r24Outer), radius = r24Outer)
        drawCircle(color = Color(0xFFFFD700), radius = r24Outer, style = Stroke(width = 2f))
        val step24 = 360f / 24f
        for (i in 0 until 24) {
            val angle = i * step24
            val rad = Math.toRadians(angle.toDouble() - 90)
            drawLine(Color(0xFF374151), Offset(cx + r24Inner * cos(rad).toFloat(), cy + r24Inner * sin(rad).toFloat()), Offset(cx + r24Outer * cos(rad).toFloat(), cy + r24Outer * sin(rad).toFloat()), strokeWidth = 2f)

            val textAngle = angle + (step24 / 2)
            val textRad = Math.toRadians(textAngle.toDouble() - 90)
            val textR = (r24Outer + r24Inner) / 2
            drawContext.canvas.nativeCanvas.apply {
                withSave {
                    translate(
                        cx + textR * cos(textRad).toFloat(),
                        cy + textR * sin(textRad).toFloat()
                    )
                    rotate(textAngle + 180)
                    val paint = Paint().apply {
                        color = "#FFD700".toColorInt(); textSize = 38f; textAlign =
                        Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true
                    }
                    drawText(twentyFourMountains[i], 0f, 12f, paint)
                }
            }
        }

        // --- 5. 天池 ---
        drawCircle(color = Color.Black, radius = r24Inner)
        drawCircle(color = Color.Red, radius = rPool, style = Stroke(width = 4f))
        drawCircle(Color.Red, radius = 6f, center = Offset(cx, cy + 15f))
        drawCircle(Color.Red, radius = 6f, center = Offset(cx, cy - 15f))
    }
}

// 輔助組件 (ZoomButton 保持不變)
@Composable
fun ZoomButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, isSelected: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(onClick = onClick, colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (isSelected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.2f), contentColor = if (isSelected) Color.Black else Color.White), modifier = Modifier.size(48.dp)) { Icon(imageVector = icon, contentDescription = label) }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = if (isSelected) Color(0xFFFFD700) else Color.Gray, fontSize = 10.sp)
    }
}