package com.gongkao.cuotiben.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColors = lightColorScheme(
    primary = Color(0xFF2B5CE6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDF1FF),
    onPrimaryContainer = Color(0xFF1B3EA8),
    secondary = Color(0xFF20263A),
    onSecondary = Color.White,
    background = Color(0xFFF3F4F7),
    onBackground = Color(0xFF16181D),
    surface = Color.White,
    onSurface = Color(0xFF16181D),
    surfaceVariant = Color(0xFFF2F4F8),
    onSurfaceVariant = Color(0xFF5B6170),
    outline = Color(0xFFE6E8EE),
    outlineVariant = Color(0xFFEDEFF4),
    error = Color(0xFFD64545),
    errorContainer = Color(0xFFFDEEEE),
    onErrorContainer = Color(0xFFD64545)
)

/** 统一浅色墨蓝主题，保证学习场景长时间阅读的舒适度 */
@Composable
fun CuotiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColors,
        content = content
    )
}
