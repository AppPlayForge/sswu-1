package com.example.myTools.carspeed

import android.graphics.BlurMaskFilter
import android.graphics.Paint as NativePaint
import android.graphics.Rect as NativeRect
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * 指南針組合函數。
 */
@Composable
fun CompassGauge(
    modifier: Modifier = Modifier,
    degrees: Float, // degrees 代表 "手機的朝向"
) {
    val cyanColor = Color(0xFF00E5FF)
    val compassRoseColor = cyanColor.copy(alpha = 0.5f)
    val needleColor = Color(0xFFFF1744)

    val density = LocalDensity.current

    // 預先快取 Native Paint 物件與 Rect，避免在 onDraw 高頻繪製階段進行物件配置
    val textPaint = remember(density) {
        NativePaint().apply {
            isAntiAlias = true
            textSize = with(density) { 12.sp.toPx() }
            color = Color.White.toArgb()
            textAlign = NativePaint.Align.CENTER
            setShadowLayer(5f, 0f, 0f, cyanColor.toArgb())
        }
    }

    val needleGlowPaint = remember(density) {
        NativePaint().apply {
            isAntiAlias = true
            maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL)
            color = needleColor.toArgb()
        }
    }

    val textBounds = remember { NativeRect() }

    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = this.center

        // 1. 繪製固定的背景圓和方向文字 (增加科技感外發光)
        drawCircle(
            color = compassRoseColor,
            radius = radius,
            style = Stroke(width = 1.dp.toPx()),
        )

        val directions = listOf("北", "東", "南", "西")
        for ((index, direction) in directions.withIndex()) {
            val angle = index * 90f
            val angleInRad = Math.toRadians(angle.toDouble()).toFloat()

            // 計算文字位置
            val textPosition = center + Offset(
                x = (radius * 0.75f) * sin(angleInRad),
                y = (radius * 0.75f) * -cos(angleInRad),
            )

            textPaint.getTextBounds(direction, 0, direction.length, textBounds)
            drawContext.canvas.nativeCanvas.drawText(
                direction,
                textPosition.x,
                textPosition.y + (textBounds.height() / 2f),
                textPaint,
            )
        }

        // 2. 繪製旋轉的科技感指針
        rotate(degrees = degrees, pivot = center) {
            val needlePath = Path().apply {
                moveTo(center.x, center.y - radius * 0.75f) // 尖端
                lineTo(center.x - 5.dp.toPx(), center.y + 2.dp.toPx())
                lineTo(center.x + 5.dp.toPx(), center.y + 2.dp.toPx())
                close()
            }

            drawContext.canvas.nativeCanvas.drawPath(needlePath.asAndroidPath(), needleGlowPaint)
            drawPath(path = needlePath, color = needleColor)
        }

        // 3. 中心裝飾
        drawCircle(color = Color.White, radius = 2.dp.toPx(), center = center)
    }
}
