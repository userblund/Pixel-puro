package com.example.ui.pixel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent

/**
 * Renders an array of 0s and 1s on an N x N grid as pure, crisp, integer pixels.
 */
@Composable
fun PixelBitmapIcon(
    matrix: Array<IntArray>,
    tint: Color = CyanAccent,
    secondaryTint: Color? = null,
    modifier: Modifier = Modifier.size(16.dp)
) {
    Canvas(modifier = modifier) {
        val rows = matrix.size
        val cols = matrix[0].size
        val cellW = size.width / cols.toFloat()
        val cellH = size.height / rows.toFloat()

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val value = matrix[r][c]
                if (value == 1) {
                    drawRect(
                        color = tint,
                        topLeft = Offset(c * cellW, r * cellH),
                        size = Size(cellW, cellH)
                    )
                } else if (value == 2 && secondaryTint != null) {
                    drawRect(
                        color = secondaryTint,
                        topLeft = Offset(c * cellW, r * cellH),
                        size = Size(cellW, cellH)
                    )
                }
            }
        }
    }
}

// 12x12 Pixel Eyedropper (Pipette)
private val DROPPER_MATRIX = arrayOf(
    intArrayOf(0,0,0,0,0,0,0,1,1,1,0,0),
    intArrayOf(0,0,0,0,0,0,1,1,2,1,1,0),
    intArrayOf(0,0,0,0,0,1,1,2,1,1,1,0),
    intArrayOf(0,0,0,0,1,1,1,1,1,1,0,0),
    intArrayOf(0,0,0,1,1,2,1,1,1,0,0,0),
    intArrayOf(0,0,1,1,2,1,1,1,0,0,0,0),
    intArrayOf(0,1,1,2,1,1,1,0,0,0,0,0),
    intArrayOf(1,1,2,1,1,1,0,0,0,0,0,0),
    intArrayOf(1,2,1,1,1,0,0,0,0,0,0,0),
    intArrayOf(1,1,1,0,0,0,0,0,0,0,0,0),
    intArrayOf(1,1,0,0,0,0,0,0,0,0,0,0),
    intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0)
)

// 12x12 Pixel Camera (Integrity & Screenshot)
private val CAMERA_MATRIX = arrayOf(
    intArrayOf(0,0,0,0,1,1,1,1,0,0,0,0),
    intArrayOf(0,0,0,1,0,0,0,0,1,0,0,0),
    intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1),
    intArrayOf(1,0,0,0,0,0,0,0,0,0,0,1),
    intArrayOf(1,0,0,1,1,1,1,0,0,1,0,1),
    intArrayOf(1,0,1,1,0,0,1,1,0,0,0,1),
    intArrayOf(1,0,1,0,2,2,0,1,0,0,0,1),
    intArrayOf(1,0,1,0,2,2,0,1,0,0,0,1),
    intArrayOf(1,0,1,1,0,0,1,1,0,0,0,1),
    intArrayOf(1,0,0,1,1,1,1,0,0,0,0,1),
    intArrayOf(1,0,0,0,0,0,0,0,0,0,0,1),
    intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1)
)

// 12x12 Pixel Grid
private val GRID_MATRIX = arrayOf(
    intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1),
    intArrayOf(1,0,0,0,1,0,0,0,1,0,0,1),
    intArrayOf(1,0,0,0,1,0,0,0,1,0,0,1),
    intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1),
    intArrayOf(1,0,0,0,1,0,0,0,1,0,0,1),
    intArrayOf(1,0,0,0,1,0,0,0,1,0,0,1),
    intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1),
    intArrayOf(1,0,0,0,1,0,0,0,1,0,0,1),
    intArrayOf(1,0,0,0,1,0,0,0,1,0,0,1),
    intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1),
    intArrayOf(1,0,0,0,1,0,0,0,1,0,0,1),
    intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1)
)

// 12x12 Pixel Palette
private val PALETTE_MATRIX = arrayOf(
    intArrayOf(0,0,1,1,1,1,1,1,1,0,0,0),
    intArrayOf(0,1,2,2,1,0,0,2,2,1,0,0),
    intArrayOf(1,2,2,1,0,0,0,2,2,1,0,0),
    intArrayOf(1,1,1,0,0,0,0,1,1,1,1,0),
    intArrayOf(1,0,0,0,0,0,0,0,0,0,1,0),
    intArrayOf(1,2,2,1,0,0,0,0,0,0,1,0),
    intArrayOf(1,2,2,1,0,0,0,0,0,1,0,0),
    intArrayOf(1,1,1,0,0,0,0,0,1,0,0,0),
    intArrayOf(0,1,0,0,0,0,0,1,0,0,0,0),
    intArrayOf(0,1,0,0,0,0,1,0,0,0,0,0),
    intArrayOf(0,0,1,1,1,1,0,0,0,0,0,0),
    intArrayOf(0,0,0,0,0,0,0,0,0,0,0,0)
)

// 12x12 Pixel Brush / Pen
private val BRUSH_MATRIX = arrayOf(
    intArrayOf(0,0,0,0,0,0,0,0,1,1,1,0),
    intArrayOf(0,0,0,0,0,0,0,1,2,2,1,1),
    intArrayOf(0,0,0,0,0,0,1,2,2,1,1,1),
    intArrayOf(0,0,0,0,0,1,2,2,1,1,0,0),
    intArrayOf(0,0,0,0,1,2,2,1,1,0,0,0),
    intArrayOf(0,0,0,1,2,2,1,1,0,0,0,0),
    intArrayOf(0,0,1,2,2,1,1,0,0,0,0,0),
    intArrayOf(0,1,2,2,1,1,0,0,0,0,0,0),
    intArrayOf(1,2,2,1,1,0,0,0,0,0,0,0),
    intArrayOf(1,1,1,1,0,0,0,0,0,0,0,0),
    intArrayOf(1,1,0,0,0,0,0,0,0,0,0,0),
    intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0)
)

// 12x12 Pixel Tuning / Filter Sliders
private val TUNE_MATRIX = arrayOf(
    intArrayOf(0,1,0,0,0,1,0,0,0,1,0,0),
    intArrayOf(1,1,1,0,0,1,0,0,0,1,0,0),
    intArrayOf(1,2,1,0,0,1,0,0,1,1,1,0),
    intArrayOf(1,1,1,0,0,1,0,0,1,2,1,0),
    intArrayOf(0,1,0,0,1,1,1,0,1,1,1,0),
    intArrayOf(0,1,0,0,1,2,1,0,0,1,0,0),
    intArrayOf(0,1,0,0,1,1,1,0,0,1,0,0),
    intArrayOf(0,1,0,0,0,1,0,0,0,1,0,0),
    intArrayOf(0,1,0,0,0,1,0,0,0,1,0,0),
    intArrayOf(0,1,0,0,0,1,0,0,0,1,0,0),
    intArrayOf(0,1,0,0,0,1,0,0,0,1,0,0),
    intArrayOf(0,1,0,0,0,1,0,0,0,1,0,0)
)

// 12x12 Pixel Floating Overlay (Windows)
private val LAYERS_MATRIX = arrayOf(
    intArrayOf(1,1,1,1,1,1,1,1,0,0,0,0),
    intArrayOf(1,2,2,2,2,2,2,1,0,0,0,0),
    intArrayOf(1,2,0,0,0,0,2,1,0,0,0,0),
    intArrayOf(1,2,0,1,1,1,1,1,1,1,1,1),
    intArrayOf(1,2,0,1,2,2,2,2,2,2,2,1),
    intArrayOf(1,2,0,1,2,0,0,0,0,0,2,1),
    intArrayOf(1,1,1,1,2,0,0,0,0,0,2,1),
    intArrayOf(0,0,0,1,2,0,0,0,0,0,2,1),
    intArrayOf(0,0,0,1,2,0,0,0,0,0,2,1),
    intArrayOf(0,0,0,1,2,0,0,0,0,0,2,1),
    intArrayOf(0,0,0,1,2,2,2,2,2,2,2,1),
    intArrayOf(0,0,0,1,1,1,1,1,1,1,1,1)
)

// 12x12 Pixel Image Gallery
private val IMAGE_MATRIX = arrayOf(
    intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1),
    intArrayOf(1,0,0,0,0,0,0,0,0,0,0,1),
    intArrayOf(1,0,2,2,0,0,0,0,0,0,0,1),
    intArrayOf(1,0,2,2,0,0,0,0,0,0,0,1),
    intArrayOf(1,0,0,0,0,0,0,1,0,0,0,1),
    intArrayOf(1,0,0,0,0,0,1,1,1,0,0,1),
    intArrayOf(1,0,0,1,0,1,1,1,1,1,0,1),
    intArrayOf(1,0,1,1,1,1,1,1,1,1,1,1),
    intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1),
    intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1),
    intArrayOf(1,0,0,0,0,0,0,0,0,0,0,1),
    intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1)
)

// 10x10 Pixel Close 'X'
private val CLOSE_MATRIX = arrayOf(
    intArrayOf(1,1,0,0,0,0,0,0,1,1),
    intArrayOf(0,1,1,0,0,0,0,1,1,0),
    intArrayOf(0,0,1,1,0,0,1,1,0,0),
    intArrayOf(0,0,0,1,1,1,1,0,0,0),
    intArrayOf(0,0,0,0,1,1,0,0,0,0),
    intArrayOf(0,0,0,0,1,1,0,0,0,0),
    intArrayOf(0,0,0,1,1,1,1,0,0,0),
    intArrayOf(0,0,1,1,0,0,1,1,0,0),
    intArrayOf(0,1,1,0,0,0,0,1,1,0),
    intArrayOf(1,1,0,0,0,0,0,0,1,1)
)

// 10x10 Pixel Lightning Flash
private val FLASH_MATRIX = arrayOf(
    intArrayOf(0,0,0,0,1,1,0,0,0,0),
    intArrayOf(0,0,0,1,1,0,0,0,0,0),
    intArrayOf(0,0,1,1,0,0,0,0,0,0),
    intArrayOf(0,1,1,1,1,1,1,1,0,0),
    intArrayOf(0,0,0,0,1,1,0,0,0,0),
    intArrayOf(0,0,0,1,1,0,0,0,0,0),
    intArrayOf(0,0,1,1,0,0,0,0,0,0),
    intArrayOf(0,1,1,0,0,0,0,0,0,0),
    intArrayOf(0,1,0,0,0,0,0,0,0,0),
    intArrayOf(0,0,0,0,0,0,0,0,0,0)
)

// 10x10 Pixel Camera Switch Loop
private val SWITCH_CAM_MATRIX = arrayOf(
    intArrayOf(0,1,1,1,1,1,1,0,0,0),
    intArrayOf(1,1,0,0,0,0,1,1,0,0),
    intArrayOf(1,0,0,0,0,0,0,1,0,0),
    intArrayOf(1,0,0,0,0,0,0,1,1,1),
    intArrayOf(1,0,0,0,0,0,0,0,1,0),
    intArrayOf(0,1,0,0,0,0,0,0,0,1),
    intArrayOf(1,1,1,0,0,0,0,0,0,1),
    intArrayOf(0,0,1,0,0,0,0,0,0,1),
    intArrayOf(0,0,1,1,0,0,0,0,1,1),
    intArrayOf(0,0,0,1,1,1,1,1,1,0)
)

// 8x8 Pixel Directional Arrows
private val ARROW_UP = arrayOf(
    intArrayOf(0,0,0,1,1,0,0,0),
    intArrayOf(0,0,1,1,1,1,0,0),
    intArrayOf(0,1,1,1,1,1,1,0),
    intArrayOf(1,1,0,1,1,0,1,1),
    intArrayOf(0,0,0,1,1,0,0,0),
    intArrayOf(0,0,0,1,1,0,0,0),
    intArrayOf(0,0,0,1,1,0,0,0),
    intArrayOf(0,0,0,1,1,0,0,0)
)
private val ARROW_DOWN = arrayOf(
    intArrayOf(0,0,0,1,1,0,0,0),
    intArrayOf(0,0,0,1,1,0,0,0),
    intArrayOf(0,0,0,1,1,0,0,0),
    intArrayOf(0,0,0,1,1,0,0,0),
    intArrayOf(1,1,0,1,1,0,1,1),
    intArrayOf(0,1,1,1,1,1,1,0),
    intArrayOf(0,0,1,1,1,1,0,0),
    intArrayOf(0,0,0,1,1,0,0,0)
)
private val ARROW_LEFT = arrayOf(
    intArrayOf(0,0,0,1,0,0,0,0),
    intArrayOf(0,0,1,1,0,0,0,0),
    intArrayOf(0,1,1,0,0,0,0,0),
    intArrayOf(1,1,1,1,1,1,1,1),
    intArrayOf(1,1,1,1,1,1,1,1),
    intArrayOf(0,1,1,0,0,0,0,0),
    intArrayOf(0,0,1,1,0,0,0,0),
    intArrayOf(0,0,0,1,0,0,0,0)
)
private val ARROW_RIGHT = arrayOf(
    intArrayOf(0,0,0,0,1,0,0,0),
    intArrayOf(0,0,0,0,1,1,0,0),
    intArrayOf(0,0,0,0,0,1,1,0),
    intArrayOf(1,1,1,1,1,1,1,1),
    intArrayOf(1,1,1,1,1,1,1,1),
    intArrayOf(0,0,0,0,0,1,1,0),
    intArrayOf(0,0,0,0,1,1,0,0),
    intArrayOf(0,0,0,0,1,0,0,0)
)

@Composable
fun PixelDropperIcon(tint: Color = CyanAccent, size: Dp = 16.dp) {
    PixelBitmapIcon(DROPPER_MATRIX, tint = tint, secondaryTint = Color.White, modifier = Modifier.size(size))
}

@Composable
fun PixelCameraIcon(tint: Color = CyanAccent, size: Dp = 16.dp) {
    PixelBitmapIcon(CAMERA_MATRIX, tint = tint, secondaryTint = Color.White, modifier = Modifier.size(size))
}

@Composable
fun PixelGridIcon(tint: Color = CyanAccent, size: Dp = 16.dp) {
    PixelBitmapIcon(GRID_MATRIX, tint = tint, modifier = Modifier.size(size))
}

@Composable
fun PixelPaletteIcon(tint: Color = EmeraldAccent, size: Dp = 16.dp) {
    PixelBitmapIcon(PALETTE_MATRIX, tint = tint, secondaryTint = Color(0xFFFFB300), modifier = Modifier.size(size))
}

@Composable
fun PixelBrushIcon(tint: Color = Color(0xFFFFD600), size: Dp = 16.dp) {
    PixelBitmapIcon(BRUSH_MATRIX, tint = tint, secondaryTint = Color.White, modifier = Modifier.size(size))
}

@Composable
fun PixelTuneIcon(tint: Color = Color(0xFFFF4081), size: Dp = 16.dp) {
    PixelBitmapIcon(TUNE_MATRIX, tint = tint, secondaryTint = Color.White, modifier = Modifier.size(size))
}

@Composable
fun PixelLayersIcon(tint: Color = Color(0xFFFF4081), size: Dp = 16.dp) {
    PixelBitmapIcon(LAYERS_MATRIX, tint = tint, secondaryTint = CyanAccent, modifier = Modifier.size(size))
}

@Composable
fun PixelImageIcon(tint: Color = CyanAccent, size: Dp = 16.dp) {
    PixelBitmapIcon(IMAGE_MATRIX, tint = tint, secondaryTint = Color(0xFFFFD600), modifier = Modifier.size(size))
}

@Composable
fun PixelCloseIcon(tint: Color = Color.White, size: Dp = 14.dp) {
    PixelBitmapIcon(CLOSE_MATRIX, tint = tint, modifier = Modifier.size(size))
}

@Composable
fun PixelFlashIcon(tint: Color = Color(0xFFFFD600), size: Dp = 16.dp) {
    PixelBitmapIcon(FLASH_MATRIX, tint = tint, modifier = Modifier.size(size))
}

@Composable
fun PixelSwitchCameraIcon(tint: Color = CyanAccent, size: Dp = 16.dp) {
    PixelBitmapIcon(SWITCH_CAM_MATRIX, tint = tint, modifier = Modifier.size(size))
}

@Composable
fun PixelArrowUpIcon(tint: Color = Color.White, size: Dp = 12.dp) {
    PixelBitmapIcon(ARROW_UP, tint = tint, modifier = Modifier.size(size))
}

@Composable
fun PixelArrowDownIcon(tint: Color = Color.White, size: Dp = 12.dp) {
    PixelBitmapIcon(ARROW_DOWN, tint = tint, modifier = Modifier.size(size))
}

@Composable
fun PixelArrowLeftIcon(tint: Color = Color.White, size: Dp = 12.dp) {
    PixelBitmapIcon(ARROW_LEFT, tint = tint, modifier = Modifier.size(size))
}

@Composable
fun PixelArrowRightIcon(tint: Color = Color.White, size: Dp = 12.dp) {
    PixelBitmapIcon(ARROW_RIGHT, tint = tint, modifier = Modifier.size(size))
}

// 12x12 Pixel Monitor / Screen Icon
private val MONITOR_MATRIX = arrayOf(
    intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1),
    intArrayOf(1,0,0,0,0,0,0,0,0,0,0,1),
    intArrayOf(1,0,2,2,2,2,2,2,2,2,0,1),
    intArrayOf(1,0,2,2,2,2,2,2,2,2,0,1),
    intArrayOf(1,0,2,2,2,2,2,2,2,2,0,1),
    intArrayOf(1,0,2,2,2,2,2,2,2,2,0,1),
    intArrayOf(1,0,0,0,0,0,0,0,0,0,0,1),
    intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1),
    intArrayOf(0,0,0,0,1,1,1,1,0,0,0,0),
    intArrayOf(0,0,0,0,0,1,1,0,0,0,0,0),
    intArrayOf(0,0,1,1,1,1,1,1,1,1,0,0),
    intArrayOf(0,0,1,1,1,1,1,1,1,1,0,0)
)

@Composable
fun PixelMonitorIcon(tint: Color = Color(0xFFFF1744), size: Dp = 16.dp) {
    PixelBitmapIcon(MONITOR_MATRIX, tint = tint, secondaryTint = CyanAccent, modifier = Modifier.size(size))
}
