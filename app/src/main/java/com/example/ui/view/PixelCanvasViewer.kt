package com.example.ui.view

import android.graphics.Bitmap
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.model.PixelColor
import com.example.ui.components.SurgicalLoupeGlass
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import kotlin.math.roundToInt

@Composable
fun PixelCanvasViewer(
    bitmap: Bitmap,
    isolatedBitmap: Bitmap?,
    isEyedropperActive: Boolean,
    isGridEnabled: Boolean,
    useBilinearFilter: Boolean,
    currentScale: Float,
    onScaleChanged: (Float) -> Unit,
    onToggleUIVisibility: () -> Unit,
    onPixelSelected: (PixelColor, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val density = LocalDensity.current

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Eyedropper touch tracking
    var isTouching by remember { mutableStateOf(false) }
    var touchPos by remember { mutableStateOf(Offset.Zero) }
    var currentSampledColor by remember { mutableStateOf<PixelColor?>(null) }
    var currentSampleX by remember { mutableIntStateOf(0) }
    var currentSampleY by remember { mutableIntStateOf(0) }

    val activeDisplayBitmap = isolatedBitmap ?: bitmap

    // Transformable gesture for pinch to zoom & pan (unlimited surgical zoom up to 5000x)
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (currentScale * zoomChange).coerceIn(0.005f, 5000f)
        onScaleChanged(newScale)
        offsetX += panChange.x
        offsetY += panChange.y
    }

    // Auto-initialize eyedropper on center or top-left corner if active
    LaunchedEffect(isEyedropperActive, activeDisplayBitmap) {
        if (isEyedropperActive) {
            val bmpW = activeDisplayBitmap.width
            val bmpH = activeDisplayBitmap.height
            val px = currentSampleX.coerceIn(0, bmpW - 1)
            val py = currentSampleY.coerceIn(0, bmpH - 1)
            currentSampleX = px
            currentSampleY = py
            val argb = activeDisplayBitmap.getPixel(px, py)
            val pColor = PixelColor(argb)
            currentSampledColor = pColor
            onPixelSelected(pColor, px, py)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("pixel_canvas_viewer_container")
    ) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

        // Seamless mapping from screen coordinates to bitmap pixel coordinates.
        // Clamps strictly to valid bounds (0..bmpW-1, 0..bmpH-1) so corner touches NEVER fail!
        fun screenToBitmapPixel(screenX: Float, screenY: Float): Pair<Int, Int> {
            val bmpW = activeDisplayBitmap.width
            val bmpH = activeDisplayBitmap.height

            val centerX = containerWidth / 2f + offsetX
            val centerY = containerHeight / 2f + offsetY

            val drawW = bmpW * currentScale
            val drawH = bmpH * currentScale

            val left = centerX - drawW / 2f
            val top = centerY - drawH / 2f

            val relX = if (drawW > 0) (screenX - left) / drawW else 0f
            val relY = if (drawH > 0) (screenY - top) / drawH else 0f

            val px = (relX * bmpW).toInt().coerceIn(0, bmpW - 1)
            val py = (relY * bmpH).toInt().coerceIn(0, bmpH - 1)
            return Pair(px, py)
        }

        // Sample pixel at screen coordinates
        fun sampleAt(screenX: Float, screenY: Float) {
            val (px, py) = screenToBitmapPixel(screenX, screenY)
            if (px != currentSampleX || py != currentSampleY || currentSampledColor == null) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                currentSampleX = px
                currentSampleY = py
                val argb = activeDisplayBitmap.getPixel(px, py)
                val pColor = PixelColor(argb)
                currentSampledColor = pColor
                onPixelSelected(pColor, px, py)
            }
        }

        // Initialize touch position near center if unset
        if (touchPos == Offset.Zero) {
            touchPos = Offset(containerWidth / 2f, containerHeight / 2f)
        }

        // Canvas for pure hardware pixel drawing
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .transformable(state = transformState)
                .pointerInput(isEyedropperActive) {
                    if (isEyedropperActive) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                isTouching = true
                                touchPos = offset
                                sampleAt(offset.x, offset.y)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                touchPos = change.position
                                sampleAt(change.position.x, change.position.y)
                            },
                            onDragEnd = {
                                isTouching = false
                            },
                            onDragCancel = {
                                isTouching = false
                            }
                        )
                    }
                }
                .pointerInput(isEyedropperActive) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            val fitScale = minOf(
                                containerWidth / activeDisplayBitmap.width.toFloat(),
                                containerHeight / activeDisplayBitmap.height.toFloat()
                            ) * 0.95f

                            val nextScale = when {
                                currentScale < fitScale * 1.05f -> 1.0f
                                kotlin.math.abs(currentScale - 1.0f) < 0.1f -> 16.0f
                                currentScale in 1.1f..20.0f -> 64.0f
                                currentScale in 20.1f..100.0f -> 512.0f
                                else -> fitScale
                            }
                            onScaleChanged(nextScale)
                            // Smoothly center on tapped region
                            offsetX = (containerWidth / 2f - tapOffset.x)
                            offsetY = (containerHeight / 2f - tapOffset.y)
                        },
                        onTap = { offset ->
                            if (isEyedropperActive) {
                                touchPos = offset
                                sampleAt(offset.x, offset.y)
                            } else {
                                // Toggle UI for pure pixel immersive view
                                onToggleUIVisibility()
                            }
                        }
                    )
                }
        ) {
            val bmpW = activeDisplayBitmap.width
            val bmpH = activeDisplayBitmap.height

            val centerX = size.width / 2f + offsetX
            val centerY = size.height / 2f + offsetY

            val drawW = bmpW * currentScale
            val drawH = bmpH * currentScale

            val fLeft = centerX - drawW / 2f
            val fTop = centerY - drawH / 2f
            val fRight = fLeft + drawW
            val fBottom = fTop + drawH

            // 1. Only draw what intersects the visible canvas viewport (0..size.width, 0..size.height)
            // This prevents allocating millions of pixels in GPU texture space at high zoom (up to 5000x)!
            val visL = maxOf(0f, fLeft)
            val visT = maxOf(0f, fTop)
            val visR = minOf(size.width, fRight)
            val visB = minOf(size.height, fBottom)

            if (visR > visL && visB > visT) {
                // Transparency checkerboard under the visible viewport
                drawCheckerboard(visL, visT, visR - visL, visB - visT)

                // Draw bitmap with exact FilterQuality.None (Nearest Neighbor) or Bilinear
                val filterQuality = if (useBilinearFilter) FilterQuality.Low else FilterQuality.None
                val imageBitmap = activeDisplayBitmap.asImageBitmap()

                // Calculate sub-rectangle of source bitmap that intersects the visible screen
                val srcX0 = ((visL - fLeft) / currentScale).toInt().coerceIn(0, bmpW - 1)
                val srcY0 = ((visT - fTop) / currentScale).toInt().coerceIn(0, bmpH - 1)
                val srcX1 = (((visR - fLeft) / currentScale).toInt() + 1).coerceIn(srcX0 + 1, bmpW)
                val srcY1 = (((visB - fTop) / currentScale).toInt() + 1).coerceIn(srcY0 + 1, bmpH)

                val dstLeft = (fLeft + srcX0 * currentScale).roundToInt()
                val dstTop = (fTop + srcY0 * currentScale).roundToInt()
                val dstRight = (fLeft + srcX1 * currentScale).roundToInt()
                val dstBottom = (fTop + srcY1 * currentScale).roundToInt()

                drawImage(
                    image = imageBitmap,
                    srcOffset = IntOffset(srcX0, srcY0),
                    srcSize = IntSize(srcX1 - srcX0, srcY1 - srcY0),
                    dstOffset = IntOffset(dstLeft, dstTop),
                    dstSize = IntSize(dstRight - dstLeft, dstBottom - dstTop),
                    filterQuality = filterQuality
                )
            }

            // 2. Draw Pixel Grid if enabled or high zoom (scale >= 8)
            if (isGridEnabled || currentScale >= 8f) {
                drawPixelGrid(
                    bitmapWidth = bmpW,
                    bitmapHeight = bmpH,
                    left = fLeft,
                    top = fTop,
                    pixelScale = currentScale
                )
            }

            // 3. Highlight sampled pixel and crosshair on canvas if eyedropper active
            if (isEyedropperActive && currentSampleX in 0 until bmpW && currentSampleY in 0 until bmpH) {
                val targetPixelLeft = fLeft + currentSampleX * currentScale
                val targetPixelTop = fTop + currentSampleY * currentScale
                val targetCenter = Offset(targetPixelLeft + currentScale / 2f, targetPixelTop + currentScale / 2f)

                val activeColor = currentSampledColor?.toComposeColor() ?: CyanAccent

                // Full-span surgical crosshair across the image
                drawLine(
                    color = activeColor.copy(alpha = 0.5f),
                    start = Offset(fLeft, targetCenter.y),
                    end = Offset(fRight, targetCenter.y),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = activeColor.copy(alpha = 0.5f),
                    start = Offset(targetCenter.x, fTop),
                    end = Offset(targetCenter.x, fBottom),
                    strokeWidth = 1.dp.toPx()
                )

                // High-visibility neon box around target pixel (min size 16dp for visibility on 1:1 scale)
                val boxSize = maxOf(currentScale, 16.dp.toPx())
                val boxLeft = targetCenter.x - boxSize / 2f
                val boxTop = targetCenter.y - boxSize / 2f

                drawRect(
                    color = Color.Black,
                    topLeft = Offset(boxLeft - 1f, boxTop - 1f),
                    size = Size(boxSize + 2f, boxSize + 2f),
                    style = Stroke(width = 3.dp.toPx())
                )
                drawRect(
                    color = activeColor,
                    topLeft = Offset(boxLeft, boxTop),
                    size = Size(boxSize, boxSize),
                    style = Stroke(width = 2.dp.toPx())
                )

                // If touching, draw connection line from finger touch to the target pixel center
                if (isTouching) {
                    drawLine(
                        color = CyanAccent.copy(alpha = 0.6f),
                        start = touchPos,
                        end = targetCenter,
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }
            }
        }

        // ==========================================
        // SURGICAL OFFSET LOUPE WITH AUTHENTIC GLASS LENS
        // "seria como una lupa yo agarro el mango pero el zoom se muestra en otra parte el objetivo es sacar el color quirúrgicamente"
        // "además no me deja ver el cristal que color seleccione"
        // ==========================================
        if (isEyedropperActive && currentSampleX in 0 until activeDisplayBitmap.width && currentSampleY in 0 until activeDisplayBitmap.height) {
            val densityVal = density.density
            val loupeWidthPx = 140 * densityVal

            val targetX = (touchPos.x - loupeWidthPx / 2f).coerceIn(12f, containerWidth - loupeWidthPx - 12f)
            val targetY = (touchPos.y - 180 * densityVal).let {
                if (it < 70f) touchPos.y + 75 * densityVal else it
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(targetX.roundToInt(), targetY.roundToInt()) }
                    .testTag("surgical_loupe_overlay")
            ) {
                SurgicalLoupeGlass(
                    bitmap = activeDisplayBitmap,
                    pixelX = currentSampleX,
                    pixelY = currentSampleY,
                    selectedColor = currentSampledColor
                )
            }
        }
    }
}

private fun DrawScope.drawCheckerboard(x: Float, y: Float, width: Float, height: Float) {
    val tileSize = 24f
    val dark = Color(0xFF1E222D)
    val light = Color(0xFF282D3C)

    val cols = (width / tileSize).toInt() + 1
    val rows = (height / tileSize).toInt() + 1

    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val color = if ((r + c) % 2 == 0) light else dark
            val rx = x + c * tileSize
            val ry = y + r * tileSize
            val rw = minOf(tileSize, x + width - rx)
            val rh = minOf(tileSize, y + height - ry)
            if (rw > 0 && rh > 0) {
                drawRect(
                    color = color,
                    topLeft = Offset(rx, ry),
                    size = Size(rw, rh)
                )
            }
        }
    }
}

private fun DrawScope.drawPixelGrid(
    bitmapWidth: Int,
    bitmapHeight: Int,
    left: Float,
    top: Float,
    pixelScale: Float
) {
    if (pixelScale < 4f) return // Avoid rendering thousands of sub-pixel lines when zoomed out

    val gridColor = Color(0x3300E5FF)
    val strokeWidth = 1f

    val startX = maxOf(0, ((-left) / pixelScale).toInt())
    val endX = minOf(bitmapWidth, ((size.width - left) / pixelScale).toInt() + 1)

    val gridTop = maxOf(0f, top)
    val gridBottom = minOf(size.height, top + bitmapHeight * pixelScale)

    for (x in startX..endX) {
        val px = left + x * pixelScale
        drawLine(
            color = gridColor,
            start = Offset(px, gridTop),
            end = Offset(px, gridBottom),
            strokeWidth = strokeWidth
        )
    }

    val startY = maxOf(0, ((-top) / pixelScale).toInt())
    val endY = minOf(bitmapHeight, ((size.height - top) / pixelScale).toInt() + 1)

    val gridLeft = maxOf(0f, left)
    val gridRight = minOf(size.width, left + bitmapWidth * pixelScale)

    for (y in startY..endY) {
        val py = top + y * pixelScale
        drawLine(
            color = gridColor,
            start = Offset(gridLeft, py),
            end = Offset(gridRight, py),
            strokeWidth = strokeWidth
        )
    }
}
