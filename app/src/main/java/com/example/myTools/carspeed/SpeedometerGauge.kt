package com.example.myTools.carspeed

import android.graphics.BlurMaskFilter
import android.graphics.Paint as NativePaint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * 儀表板組合函數，包含完整的燈光效果和紅色警示區。
 */
@Composable
fun SpeedometerGauge(
    modifier: Modifier = Modifier,
    speed: Float,
) {
    val maxSpeed = 160 // 提升最高速度，視覺更平衡
    val startAngle = 150f
    val sweepAngle = 240f

    // 科技感配色
    val cyanColor = Color(0xFF00E5FF) // 電子青
    val blueColor = Color(0xFF2979FF) // 科技藍
    val needleColor = Color(0xFFFF1744) // 亮紅
    val warningColor = Color(0xFFFF9100) // 橙色警示
    val bgColor = Color.White.copy(alpha = 0.05f)

    val density = LocalDensity.current

    // 預先快取 Native Paint 物件，避開 onDraw 階段的高頻記憶體分配
    val textPaint = remember(density) {
        NativePaint().apply {
            isAntiAlias = true
            textSize = with(density) { 14.sp.toPx() }
            color = Color.White.copy(alpha = 0.7f).toArgb()
            textAlign = NativePaint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
    }

    val speedTextPaint = remember(density) {
        NativePaint().apply {
            isAntiAlias = true
            textSize = with(density) { 100.sp.toPx() }
            color = cyanColor.toArgb()
            textAlign = NativePaint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setShadowLayer(25f, 0f, 0f, cyanColor.copy(alpha = 0.8f).toArgb())
        }
    }

    val unitTextPaint = remember(density) {
        NativePaint().apply {
            isAntiAlias = true
            textSize = with(density) { 16.sp.toPx() }
            color = Color.White.copy(alpha = 0.5f).toArgb()
            textAlign = NativePaint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.2f
        }
    }

    val needleGlowPaint = remember(density) {
        NativePaint().apply {
            isAntiAlias = true
            strokeWidth = with(density) { 6.dp.toPx() }
            color = needleColor.toArgb()
            style = NativePaint.Style.STROKE
            maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
        }
    }

    Canvas(modifier = modifier) {
        val center = this.center
        val radius = size.minDimension / 2f

        // 1. 繪製外圍霓虹光圈
        drawArc(
            brush = Brush.sweepGradient(
                0f to cyanColor.copy(alpha = 0.1f),
                0.5f to blueColor.copy(alpha = 0.3f),
                1f to cyanColor.copy(alpha = 0.1f),
            ),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = 2.dp.toPx()),
        )

        // 2. 繪製背景主弧
        drawArc(
            color = bgColor,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
        )

        // 3. 繪製刻度
        for (i in (0..maxSpeed) step 10) {
            val angle = startAngle + (i.toFloat() / maxSpeed) * sweepAngle
            val angleInRad = Math.toRadians(angle.toDouble()).toFloat()

            val isMajor = i % 20 == 0
            val tickLen = if (isMajor) 15.dp.toPx() else 8.dp.toPx()
            val tickColor = if (i > 120) warningColor else if (isMajor) cyanColor else Color.White.copy(alpha = 0.3f)

            val start = center + Offset((radius - tickLen - 5.dp.toPx()) * cos(angleInRad), (radius - tickLen - 5.dp.toPx()) * sin(angleInRad))
            val end = center + Offset((radius - 5.dp.toPx()) * cos(angleInRad), (radius - 5.dp.toPx()) * sin(angleInRad))

            drawLine(
                color = tickColor,
                start = start,
                end = end,
                strokeWidth = if (isMajor) 3.dp.toPx() else 1.5.dp.toPx(),
                cap = StrokeCap.Round,
            )

            if (isMajor) {
                val textPos = center + Offset((radius - 35.dp.toPx()) * cos(angleInRad), (radius - 35.dp.toPx()) * sin(angleInRad))
                drawContext.canvas.nativeCanvas.drawText(i.toString(), textPos.x, textPos.y + 5.dp.toPx(), textPaint)
            }
        }

        // 4. 繪製中心數字
        drawContext.canvas.nativeCanvas.drawText(
            "%.0f".format(speed),
            center.x,
            center.y + 40.dp.toPx(),
            speedTextPaint,
        )

        drawContext.canvas.nativeCanvas.drawText(
            "KM/H",
            center.x,
            center.y + 75.dp.toPx(),
            unitTextPaint,
        )

        // 5. 繪製科技感指針
        val clampedSpeed = speed.coerceIn(0f, maxSpeed.toFloat())
        val needleAngle = startAngle + (clampedSpeed / maxSpeed) * sweepAngle
        val needleAngleRad = Math.toRadians(needleAngle.toDouble()).toFloat()

        val needleLen = radius * 0.85f
        val needleEnd = center + Offset(needleLen * cos(needleAngleRad), needleLen * sin(needleAngleRad))

        drawContext.canvas.nativeCanvas.drawLine(center.x, center.y, needleEnd.x, needleEnd.y, needleGlowPaint)

        drawLine(
            color = needleColor,
            start = center,
            end = needleEnd,
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )

        // 6. 中心裝飾
        drawCircle(color = Color.Black, radius = 12.dp.toPx())
        drawCircle(
            brush = Brush.radialGradient(listOf(cyanColor, blueColor)),
            radius = 6.dp.toPx(),
        )
    }
}
