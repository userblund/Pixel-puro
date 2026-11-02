package com.example.ui.pixel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.PressStart2PFontFamily
import com.example.ui.theme.SilkscreenFontFamily

/**
 * Solid pixel-art shadow: offset by integer pixels with ZERO gaussian blur.
 */
fun Modifier.pixelShadow(
    offsetX: Dp = 3.dp,
    offsetY: Dp = 3.dp,
    color: Color = Color(0xFF000000)
): Modifier = this.drawBehind {
    drawRect(
        color = color,
        topLeft = Offset(offsetX.toPx(), offsetY.toPx()),
        size = size
    )
}

/**
 * Classic 3D pixel bevel: Light highlight on top/left, dark shadow on bottom/right.
 */
fun Modifier.pixelBevel(
    lightColor: Color = Color.White.copy(alpha = 0.35f),
    darkColor: Color = Color.Black.copy(alpha = 0.5f),
    borderWidth: Dp = 2.dp
): Modifier = this.drawBehind {
    val bw = borderWidth.toPx()
    // Top highlight
    drawRect(color = lightColor, topLeft = Offset.Zero, size = Size(size.width, bw))
    // Left highlight
    drawRect(color = lightColor, topLeft = Offset.Zero, size = Size(bw, size.height))
    // Bottom shadow
    drawRect(color = darkColor, topLeft = Offset(0f, size.height - bw), size = Size(size.width, bw))
    // Right shadow
    drawRect(color = darkColor, topLeft = Offset(size.width - bw, 0f), size = Size(bw, size.height))
}

/**
 * Authentic Arcade/Pixel Art Button
 */
@Composable
fun PixelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = CyanAccent,
    textColor: Color = Color.Black,
    borderColor: Color = Color.White,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .pixelShadow(3.dp, 3.dp, Color.Black)
            .background(backgroundColor, RectangleShape)
            .border(2.dp, borderColor, RectangleShape)
            .pixelBevel(Color.White.copy(alpha = 0.4f), Color.Black.copy(alpha = 0.5f), 2.dp)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Stepped Pixel Badge / Box (Crisp rectangle with 2px pixel border)
 */
@Composable
fun PixelContainer(
    modifier: Modifier = Modifier,
    backgroundColor: Color = DarkSurface,
    borderColor: Color = DarkBorder,
    borderWidth: Dp = 2.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .pixelShadow(3.dp, 3.dp, Color.Black)
            .background(backgroundColor, RectangleShape)
            .border(borderWidth, borderColor, RectangleShape)
            .padding(8.dp)
    ) {
        content()
    }
}
