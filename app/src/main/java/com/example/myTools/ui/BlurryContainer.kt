package com.example.myTools.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp

/**
 * 模糊容器，根據 isBlur 狀態對內容進行模糊處理
 */
@Composable
fun BlurryContainer(
    isBlur: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val blurRadius by animateDpAsState(
        targetValue = if (isBlur) 16.dp else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "blurAnimation"
    )

    Box(
        modifier = modifier.then(
            if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier
        )
    ) {
        content()
    }
}
