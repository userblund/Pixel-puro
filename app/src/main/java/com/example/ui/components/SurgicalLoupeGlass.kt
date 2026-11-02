package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PixelColor
import com.example.ui.pixel.pixelBevel
import com.example.ui.pixel.pixelShadow
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.SilkscreenFontFamily

/**
 * 100% PURE PIXEL LOUPE ("El Cristal de la Lupa Pixel Puro")
 * Every single line, border, lens frame and matrix cell is rendered using crisp integer pixels
 * without any anti-aliased curved approximations.
 */
@Composable
fun SurgicalLoupeGlass(
    bitmap: Bitmap,
    pixelX: Int,
    pixelY: Int,
    selectedColor: PixelColor?,
    modifier: Modifier = Modifier
) {
    val lensSize = 138.dp
    val matrixRadius = 3 // 7x7 grid: -3..+3
    val composeColor = selectedColor?.toComposeColor() ?: CyanAccent

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.testTag("surgical_loupe_glass")
    ) {
        // Pixel-Beveled Readout Badge above the lens
        Box(
            modifier = Modifier
                .pixelShadow(2.dp, 2.dp, Color.Black)
                .background(DarkSurface, RectangleShape)
                .border(2.dp, composeColor, RectangleShape)
                .pixelBevel(Color.White.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.6f), 1.dp)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pure Square Pixel Swatch
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(composeColor, RectangleShape)
                        .border(1.dp, Color.White, RectangleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = selectedColor?.hex8Rgba ?: "#--------",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SilkscreenFontFamily
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "[$pixelX,$pixelY]",
                    color = CyanAccent,
                    fontSize = 8.sp,
                    fontFamily = SilkscreenFontFamily
                )
            }
        }

        Spacer(modifier = Modifier.size(4.dp))

        // Stepped Pixel Art Octagonal Lens ("El Cristal Pixel")
        Box(
            modifier = Modifier
                .size(lensSize)
                .pixelShadow(4.dp, 4.dp, Color.Black)
                .background(DarkBackground, RectangleShape)
                .border(3.dp, composeColor, RectangleShape)
                .pixelBevel(Color.White.copy(alpha = 0.4f), Color.Black.copy(alpha = 0.7f), 2.dp)
        ) {
            // Pixel Matrix Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridSize = matrixRadius * 2 + 1 // 7
                val cellSize = size.width / gridSize.toFloat()
                val bmpW = bitmap.width
                val bmpH = bitmap.height

                // Draw each pixel cell in the 7x7 matrix
                for (row in 0 until gridSize) {
                    for (col in 0 until gridSize) {
                        val dx = col - matrixRadius
                        val dy = row - matrixRadius
                        val sampleX = pixelX + dx
                        val sampleY = pixelY + dy

                        val cellLeft = col * cellSize
                        val cellTop = row * cellSize

                        if (sampleX in 0 until bmpW && sampleY in 0 until bmpH) {
                            val argb = bitmap.getPixel(sampleX, sampleY)
                            drawRect(
                                color = Color(argb),
                                topLeft = Offset(cellLeft, cellTop),
                                size = Size(cellSize, cellSize)
                            )
                        } else {
                            // Out of bounds: Pure 8-bit checkerboard pattern
                            val isEven = (col + row) % 2 == 0
                            drawRect(
                                color = if (isEven) Color(0xFF141926) else Color(0xFF0A0D14),
                                topLeft = Offset(cellLeft, cellTop),
                                size = Size(cellSize, cellSize)
                            )
                        }

                        // Pixel grid separator lines
                        drawRect(
                            color = Color(0x33FFFFFF),
                            topLeft = Offset(cellLeft, cellTop),
                            size = Size(cellSize, cellSize),
                            style = Stroke(width = 1f)
                        )
                    }
                }

                // Center Target Pixel Reticle: Dual square frame + pixel crosshair
                val centerLeft = matrixRadius * cellSize
                val centerTop = matrixRadius * cellSize

                drawRect(
                    color = Color.Black,
                    topLeft = Offset(centerLeft - 1f, centerTop - 1f),
                    size = Size(cellSize + 2f, cellSize + 2f),
                    style = Stroke(width = 3.dp.toPx())
                )
                drawRect(
                    color = Color.White,
                    topLeft = Offset(centerLeft, centerTop),
                    size = Size(cellSize, cellSize),
                    style = Stroke(width = 2.dp.toPx())
                )

                // 1-Pixel Crosshairs in center
                val midX = centerLeft + cellSize / 2f
                val midY = centerTop + cellSize / 2f
                val crossArm = cellSize * 0.35f

                drawLine(
                    color = Color.Black,
                    start = Offset(midX - crossArm, midY),
                    end = Offset(midX + crossArm, midY),
                    strokeWidth = 3f
                )
                drawLine(
                    color = Color.Black,
                    start = Offset(midX, midY - crossArm),
                    end = Offset(midX, midY + crossArm),
                    strokeWidth = 3f
                )
                drawLine(
                    color = Color.White,
                    start = Offset(midX - crossArm, midY),
                    end = Offset(midX + crossArm, midY),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = Color.White,
                    start = Offset(midX, midY - crossArm),
                    end = Offset(midX, midY + crossArm),
                    strokeWidth = 1.5f
                )

                // Pixel-art glare notch in top-left
                drawRect(
                    color = Color.White.copy(alpha = 0.35f),
                    topLeft = Offset(4f, 4f),
                    size = Size(cellSize, 4f)
                )
                drawRect(
                    color = Color.White.copy(alpha = 0.35f),
                    topLeft = Offset(4f, 4f),
                    size = Size(4f, cellSize)
                )
            }
        }

        // Stepped Pixel Art Handle ("El Mango Pixel")
        Canvas(modifier = Modifier.size(width = 18.dp, height = 24.dp)) {
            val stepW = size.width / 4f
            val stepH = size.height / 6f

            for (r in 0 until 6) {
                val inset = if (r == 0) 1 else 0
                val barW = size.width - (inset * 2 * stepW)
                drawRect(
                    color = composeColor,
                    topLeft = Offset(inset * stepW, r * stepH),
                    size = Size(barW, stepH - 1f)
                )
            }
        }
    }
}
