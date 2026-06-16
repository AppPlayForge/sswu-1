package com.example.myTools.caliper

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt


enum class RulerUnit {
    CM, INCH
}


@Composable
fun CaliperScreen(onBack: () -> Unit) {
    // 移除 KeepScreenOn()，防止忘記關閉導致耗電

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var currentUnit by remember { mutableStateOf(RulerUnit.CM) }
    var calibrationFactor by remember { mutableFloatStateOf(CalibrationManager.loadFactor(context)) }
    var isCalibrating by remember { mutableStateOf(false) }

    val rulerBlue = Color(0xFF2196F3)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(rulerBlue)
    ) {
        // 1. 尺規畫布
        CaliperRulerCanvas(
            modifier = Modifier.fillMaxSize(),
            unit = currentUnit,
            calibrationFactor = calibrationFactor,
            orientationKey = isLandscape
        )

        // 2. 返回按鈕
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(12.dp)
                .size(44.dp)
                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(50))
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = Color.White
            )
        }

        // 3. 底部控制區 (一般模式)
        if (!isCalibrating) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 橫屏提示
                if (!isLandscape) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.ScreenRotation,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "旋轉手機可測量更長物體",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 16.sp
                        )
                    }
                }

                // 校準按鈕
                Row(
                    modifier = Modifier
                        .clickable { isCalibrating = true }
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "校準", tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("校準", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }

                // 單位選擇器
                UnitSelector(
                    currentUnit = currentUnit,
                    onUnitSelected = { currentUnit = it }
                )
            }
        }

        // 3. 校準模式面板 (改進版：只佔用底部，不遮擋上方)
        if (isCalibrating) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    // 使用深色背景，但只在底部
                    .background(
                        Color(0xFF121212).copy(alpha = 0.95f),
                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    // 加上狀態欄 padding 避免在橫屏時被擋住
                    .navigationBarsPadding()
                    .padding(24.dp)
            ) {
                CalibrationControls(
                    currentFactor = calibrationFactor,
                    onFactorChange = {
                        calibrationFactor = it
                        CalibrationManager.saveFactor(context, it)
                    },
                    onDone = { isCalibrating = false }
                )
            }
        }
    }
}

@Composable
fun CalibrationControls(
    currentFactor: Float,
    onFactorChange: (Float) -> Unit,
    onDone: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("尺規校準模式", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "請將真實尺規貼在螢幕上方。\n調整滑桿，直到螢幕刻度與真實刻度完全重合。",
            color = Color.LightGray,
            fontSize = 14.sp,
            style = LocalTextStyle.current.copy(lineHeight = 18.sp),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 微調控制區
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { onFactorChange(currentFactor - 0.002f) }) {
                Icon(Icons.Default.Remove, null, tint = Color.White)
            }

            Slider(
                value = currentFactor,
                onValueChange = onFactorChange,
                valueRange = 0.8f..1.2f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFFEB3B),
                    activeTrackColor = Color(0xFF2196F3)
                )
            )

            IconButton(onClick = { onFactorChange(currentFactor + 0.002f) }) {
                Icon(Icons.Default.Add, null, tint = Color.White)
            }
        }

        Text(
            text = "當前比例: ${(currentFactor * 100).toInt()}%",
            color = Color(0xFFFFEB3B),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row {
            Button(
                onClick = { onFactorChange(1.0f) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) { Text("重置") }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) { Text("完成設定") }
        }
    }
}

@SuppressLint("ModifierParameter")
@Composable
fun CaliperRulerCanvas(
    modifier: Modifier,
    unit: RulerUnit,
    calibrationFactor: Float,
    orientationKey: Boolean
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    // ★ 改進 1：使用 View 系統震動 (兼容性更好) ★
    val view = LocalView.current

    val displayMetrics = context.resources.displayMetrics
    val xdpi = displayMetrics.xdpi

    val adjustedXdpi = xdpi * calibrationFactor

    val pixelsPerMm = adjustedXdpi / 25.4f
    val snapStepPixels = if (unit == RulerUnit.CM) pixelsPerMm else adjustedXdpi / 16f

    var line1Pos by remember(orientationKey) { mutableFloatStateOf(-1f) }
    var line2Pos by remember(orientationKey) { mutableFloatStateOf(-1f) }
    var rawLine1Pos by remember(orientationKey) { mutableFloatStateOf(-1f) }
    var rawLine2Pos by remember(orientationKey) { mutableFloatStateOf(-1f) }
    var draggingLine by remember(orientationKey) { mutableIntStateOf(0) }
    val touchThreshold = with(density) { 40.dp.toPx() }

    Canvas(
        modifier = modifier.pointerInput(orientationKey) {
            detectDragGestures(
                onDragStart = { offset ->
                    val dist1 = abs(offset.x - line1Pos)
                    val dist2 = abs(offset.x - line2Pos)
                    draggingLine = when {
                        dist1 < touchThreshold && dist1 < dist2 -> { rawLine1Pos = line1Pos; 1 }
                        dist2 < touchThreshold && dist2 < dist1 -> { rawLine2Pos = line2Pos; 2 }
                        dist1 < touchThreshold -> { rawLine1Pos = line1Pos; 1 }
                        dist2 < touchThreshold -> { rawLine2Pos = line2Pos; 2 }
                        else -> 0
                    }
                },
                onDragEnd = { draggingLine = 0 },
                onDragCancel = { draggingLine = 0 },
                onDrag = { change, dragAmount ->
                    change.consume()
                    val delta = dragAmount.x
                    val maxWidth = size.width.toFloat()

                    if (draggingLine == 1) {
                        rawLine1Pos = (rawLine1Pos + delta).coerceIn(0f, maxWidth)
                        val snappedPos = (rawLine1Pos / snapStepPixels).roundToInt() * snapStepPixels

                        if (abs(snappedPos - line1Pos) > 0.1f) {
                            // ★ 改進 1：使用 CLOCK_TICK (手感更脆，支援度更高) ★
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            line1Pos = snappedPos
                        }
                    } else if (draggingLine == 2) {
                        rawLine2Pos = (rawLine2Pos + delta).coerceIn(0f, maxWidth)
                        val snappedPos = (rawLine2Pos / snapStepPixels).roundToInt() * snapStepPixels

                        if (abs(snappedPos - line2Pos) > 0.1f) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            line2Pos = snappedPos
                        }
                    }
                }
            )
        }
    ) {
        val width = size.width
        val height = size.height

        if (line1Pos < 0f && width > 0) {
            line1Pos = (width * 0.2f / snapStepPixels).roundToInt() * snapStepPixels
            line2Pos = (width * 0.8f / snapStepPixels).roundToInt() * snapStepPixels
            rawLine1Pos = line1Pos
            rawLine2Pos = line2Pos
        }

        // 1. 刻度繪製 (上下雙向刻度)
        val unitPixels = if (unit == RulerUnit.CM) pixelsPerMm else (adjustedXdpi / 16f)
        val totalSteps = (width / unitPixels).toInt()
        val tickColor = Color.White
        val strokeWidth = 2.dp.toPx()

        for (i in 0..totalSteps) {
            val x = i * unitPixels
            val isMajor = if (unit == RulerUnit.CM) i % 10 == 0 else i % 16 == 0
            val isMid = if (unit == RulerUnit.CM) i % 5 == 0 else i % 8 == 0
            val tickHeight = when {
                isMajor -> height * 0.18f // 稍微縮小刻度比例，給中間留更多空間
                isMid -> height * 0.12f
                else -> height * 0.06f
            }

            // 上方刻度
            drawLine(
                tickColor,
                Offset(x, 0f),
                Offset(x, tickHeight),
                strokeWidth = if (isMajor) strokeWidth * 1.5f else strokeWidth
            )
            // 下方刻度
            drawLine(
                tickColor,
                Offset(x, height),
                Offset(x, height - tickHeight),
                strokeWidth = if (isMajor) strokeWidth * 1.5f else strokeWidth
            )

            if (isMajor) {
                val value = if (unit == RulerUnit.CM) i / 10 else i / 16
                val textResult = textMeasurer.measure(
                    value.toString(),
                    TextStyle(color = Color.White, fontSize = 14.sp)
                )

                // 上方文字
                drawText(
                    textResult,
                    topLeft = Offset(x - textResult.size.width / 2, tickHeight + 5f)
                )
                // 下方文字
                drawText(
                    textResult,
                    topLeft = Offset(x - textResult.size.width / 2, height - tickHeight - textResult.size.height - 5f)
                )
            }
        }

        // 2. 輔助線
        val guideColor = Color(0xFFFFEB3B)
        val guideStrokeWidth = 4.dp.toPx()
        val guideEffect = PathEffect.dashPathEffect(floatArrayOf(25f, 15f), 0f)

        fun drawGuideline(xPos: Float) {
            drawLine(guideColor, Offset(xPos, 0f), Offset(xPos, height), strokeWidth = guideStrokeWidth, pathEffect = guideEffect)
            drawCircle(guideColor, radius = 18f, center = Offset(xPos, height * 0.25f))
            drawCircle(guideColor, radius = 18f, center = Offset(xPos, height * 0.75f))
        }
        drawGuideline(line1Pos)
        drawGuideline(line2Pos)

        // 3. 結果
        val distancePixels = abs(line1Pos - line2Pos)
        val displayValue = if (unit == RulerUnit.CM) (distancePixels / pixelsPerMm) / 10f else distancePixels / adjustedXdpi
        val resultText = "%.2f %s".format(displayValue, if (unit == RulerUnit.CM) "cm" else "in")

        val textLayoutResult = textMeasurer.measure(resultText, TextStyle(color = guideColor, fontSize = 48.sp, fontWeight = FontWeight.Bold))
        val centerX = width / 2f
        val centerY = height / 2f

        drawRect(Color.Black.copy(alpha = 0.4f), topLeft = Offset(centerX - textLayoutResult.size.width/2 - 20f, centerY - textLayoutResult.size.height/2 - 10f), size = Size(textLayoutResult.size.width+40f, textLayoutResult.size.height+20f))
        drawText(textLayoutResult, topLeft = Offset(centerX - textLayoutResult.size.width/2, centerY - textLayoutResult.size.height/2))

        if (distancePixels > 80f) {
            val yPos = centerY + textLayoutResult.size.height
            drawLine(guideColor, Offset(line1Pos, yPos), Offset(line2Pos, yPos), strokeWidth = 3f)
        }
    }
}

@Composable
fun UnitSelector(currentUnit: RulerUnit, onUnitSelected: (RulerUnit) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(50)).padding(4.dp)) {
        UnitButton("Inch", currentUnit == RulerUnit.INCH) { onUnitSelected(RulerUnit.INCH) }
        Spacer(Modifier.width(4.dp))
        UnitButton("CM", currentUnit == RulerUnit.CM) { onUnitSelected(RulerUnit.CM) }
    }
}

@Composable
fun UnitButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (isSelected) Color.White else Color.Transparent, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (isSelected) Color(0xFF2196F3) else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}