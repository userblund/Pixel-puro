package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PixelColor
import com.example.ui.pixel.PixelBrushIcon
import com.example.ui.pixel.PixelButton
import com.example.ui.pixel.PixelCloseIcon
import com.example.ui.pixel.PixelDropperIcon
import com.example.ui.pixel.pixelBevel
import com.example.ui.pixel.pixelShadow
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.PressStart2PFontFamily
import com.example.ui.theme.SilkscreenFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.util.PixelEngine
import java.util.Locale
import kotlin.math.roundToInt

enum class EditorTool {
    PEN,
    SWAP_COLOR,
    EYEDROPPER,
    CROP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PixelEditorDialog(
    initialBitmap: Bitmap,
    initialSelectedColor: PixelColor?,
    onSaveBitmap: (Bitmap) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var currentBitmap by remember { mutableStateOf(initialBitmap.copy(Bitmap.Config.ARGB_8888, true)) }
    val history = remember { mutableStateListOf<Bitmap>() }

    var activeTool by remember { mutableStateOf(EditorTool.PEN) }
    var brushSize by remember { mutableIntStateOf(1) } // 1px, 2px, 4px
    var currentColorArgb by remember {
        mutableIntStateOf(initialSelectedColor?.argb ?: 0xFF00E5FF.toInt())
    }

    var cropStart by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var cropEnd by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    var showCustomColorPicker by remember { mutableStateOf(false) }
    val customPalette = remember { mutableStateListOf<Int>() }

    // Channel values
    val curA = (currentColorArgb ushr 24) and 0xFF
    val curR = (currentColorArgb ushr 16) and 0xFF
    val curG = (currentColorArgb ushr 8) and 0xFF
    val curB = currentColorArgb and 0xFF

    var hexInputText by remember {
        mutableStateOf(String.format(Locale.US, "%02X%02X%02X%02X", curR, curG, curB, curA))
    }

    // Keep hex string in sync as RRGGBBAA when currentColorArgb changes externally
    LaunchedEffect(currentColorArgb) {
        val a = (currentColorArgb ushr 24) and 0xFF
        val r = (currentColorArgb ushr 16) and 0xFF
        val g = (currentColorArgb ushr 8) and 0xFF
        val b = currentColorArgb and 0xFF
        hexInputText = String.format(Locale.US, "%02X%02X%02X%02X", r, g, b, a)
    }

    fun pushHistory() {
        history.add(currentBitmap.copy(Bitmap.Config.ARGB_8888, true))
        if (history.size > 20) history.removeAt(0)
    }

    fun undo() {
        if (history.isNotEmpty()) {
            currentBitmap = history.removeAt(history.lastIndex)
        }
    }

    fun applyPen(px: Int, py: Int) {
        val updated = PixelEngine.drawPixelPen(currentBitmap, px, py, currentColorArgb, brushSize)
        currentBitmap = updated
    }

    // Parses HEX strictly in #RRGGBBAA or #RRGGBB format
    fun parseAndSetHex(rawHex: String): Boolean {
        val clean = rawHex.trim().removePrefix("#").removePrefix("0x")
        val argb = when (clean.length) {
            8 -> runCatching {
                val r = clean.substring(0, 2).toInt(16)
                val g = clean.substring(2, 4).toInt(16)
                val b = clean.substring(4, 6).toInt(16)
                val a = clean.substring(6, 8).toInt(16)
                AndroidColor.argb(a, r, g, b)
            }.getOrNull()
            6 -> runCatching {
                val r = clean.substring(0, 2).toInt(16)
                val g = clean.substring(2, 4).toInt(16)
                val b = clean.substring(4, 6).toInt(16)
                AndroidColor.argb(255, r, g, b)
            }.getOrNull()
            else -> null
        }
        return if (argb != null) {
            currentColorArgb = argb
            if (!customPalette.contains(argb)) {
                customPalette.add(0, argb)
                if (customPalette.size > 16) customPalette.removeAt(customPalette.lastIndex)
            }
            true
        } else false
    }

    fun updateRgbaChannel(r: Int = curR, g: Int = curG, b: Int = curB, a: Int = curA) {
        val newR = r.coerceIn(0, 255)
        val newG = g.coerceIn(0, 255)
        val newB = b.coerceIn(0, 255)
        val newA = a.coerceIn(0, 255)
        val newArgb = AndroidColor.argb(newA, newR, newG, newB)
        currentColorArgb = newArgb
        if (!customPalette.contains(newArgb)) {
            customPalette.add(0, newArgb)
            if (customPalette.size > 16) customPalette.removeAt(customPalette.lastIndex)
        }
    }

    val presetPalette = remember {
        listOf(
            0xFF00E5FF.toInt(), 0xFF00E676.toInt(), 0xFFFFD600.toInt(), 0xFFFF3D71.toInt(),
            0xFF7C4DFF.toInt(), 0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFF8B949E.toInt(),
            0xFFFF6D00.toInt(), 0xFF00B0FF.toInt(), 0xFFAEEA00.toInt(), 0xFFFF1744.toInt()
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkBackground,
        shape = RectangleShape,
        dragHandle = null,
        modifier = modifier.testTag("pixel_editor_dialog")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground, RectangleShape)
                .border(2.dp, CyanAccent, RectangleShape)
                .padding(10.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PixelBrushIcon(tint = Color(0xFFFFD600), size = 16.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "EDITOR PIXEL PURO",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontFamily = PressStart2PFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "EDICION QUIRURGICA Y COLOR RGBA/HEX8 [${currentBitmap.width}x${currentBitmap.height} PX]",
                            color = TextSecondary,
                            fontSize = 7.sp,
                            fontFamily = SilkscreenFontFamily
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(DarkSurface, RectangleShape)
                            .border(1.dp, if (history.isNotEmpty()) CyanAccent else DarkBorder, RectangleShape)
                            .clickable(enabled = history.isNotEmpty()) { undo() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "DESHACER",
                            color = if (history.isNotEmpty()) CyanAccent else TextMuted,
                            fontSize = 7.sp,
                            fontFamily = SilkscreenFontFamily
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(DarkSurface, RectangleShape)
                            .border(1.dp, DarkBorder, RectangleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        PixelCloseIcon(tint = Color.White, size = 10.dp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tool Selector
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Pen Tool
                Box(
                    modifier = Modifier
                        .background(if (activeTool == EditorTool.PEN) CyanAccent else DarkSurface, RectangleShape)
                        .border(1.dp, if (activeTool == EditorTool.PEN) Color.White else DarkBorder, RectangleShape)
                        .clickable { activeTool = EditorTool.PEN }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "LAPIZ ${brushSize}PX",
                        color = if (activeTool == EditorTool.PEN) Color.Black else Color.White,
                        fontSize = 8.sp,
                        fontFamily = SilkscreenFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Swap Color Tool
                Box(
                    modifier = Modifier
                        .background(if (activeTool == EditorTool.SWAP_COLOR) EmeraldAccent else DarkSurface, RectangleShape)
                        .border(1.dp, if (activeTool == EditorTool.SWAP_COLOR) Color.White else DarkBorder, RectangleShape)
                        .clickable { activeTool = EditorTool.SWAP_COLOR }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "SWAP COLOR",
                        color = if (activeTool == EditorTool.SWAP_COLOR) Color.Black else Color.White,
                        fontSize = 8.sp,
                        fontFamily = SilkscreenFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Dropper Tool
                Box(
                    modifier = Modifier
                        .background(if (activeTool == EditorTool.EYEDROPPER) AmberAccent else DarkSurface, RectangleShape)
                        .border(1.dp, if (activeTool == EditorTool.EYEDROPPER) Color.White else DarkBorder, RectangleShape)
                        .clickable { activeTool = EditorTool.EYEDROPPER }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "MUESTREAR",
                        color = if (activeTool == EditorTool.EYEDROPPER) Color.Black else Color.White,
                        fontSize = 8.sp,
                        fontFamily = SilkscreenFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Crop Tool
                Box(
                    modifier = Modifier
                        .background(if (activeTool == EditorTool.CROP) Color(0xFFFF5252) else DarkSurface, RectangleShape)
                        .border(1.dp, if (activeTool == EditorTool.CROP) Color.White else DarkBorder, RectangleShape)
                        .clickable {
                            activeTool = EditorTool.CROP
                            cropStart = null
                            cropEnd = null
                        }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "RECORTAR",
                        color = if (activeTool == EditorTool.CROP) Color.Black else Color.White,
                        fontSize = 8.sp,
                        fontFamily = SilkscreenFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Brush Size cycle
                if (activeTool == EditorTool.PEN) {
                    Box(
                        modifier = Modifier
                            .background(DarkBackground, RectangleShape)
                            .border(1.dp, CyanAccent, RectangleShape)
                            .clickable {
                                brushSize = when (brushSize) {
                                    1 -> 2
                                    2 -> 4
                                    else -> 1
                                }
                            }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text("${brushSize}PX", color = CyanAccent, fontSize = 8.sp, fontFamily = PressStart2PFontFamily)
                    }
                }
            }

            // Crop Action Banner
            if (activeTool == EditorTool.CROP) {
                Spacer(modifier = Modifier.height(4.dp))
                val minX = cropStart?.let { s -> cropEnd?.let { e -> minOf(s.first, e.first) } }
                val maxX = cropStart?.let { s -> cropEnd?.let { e -> maxOf(s.first, e.first) } }
                val minY = cropStart?.let { s -> cropEnd?.let { e -> minOf(s.second, e.second) } }
                val maxY = cropStart?.let { s -> cropEnd?.let { e -> maxOf(s.second, e.second) } }

                val selW = if (minX != null && maxX != null) maxX - minX + 1 else 0
                val selH = if (minY != null && maxY != null) maxY - minY + 1 else 0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2B1115), RectangleShape)
                        .border(1.dp, Color(0xFFFF5252), RectangleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (selW > 0 && selH > 0) "ÁREA: ${selW}x${selH} PX" else "ARRASTRA EN EL LIENZO",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontFamily = PressStart2PFontFamily
                        )
                        Text(
                            text = "RECORTE PNG MÁXIMA CALIDAD 1:1",
                            color = Color(0xFFFF8A80),
                            fontSize = 6.sp,
                            fontFamily = SilkscreenFontFamily
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (selW > 0 && selH > 0) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFF5252), RectangleShape)
                                    .border(1.dp, Color.White, RectangleShape)
                                    .clickable {
                                        pushHistory()
                                        val cropped = PixelEngine.cropBitmap(currentBitmap, minX!!, minY!!, selW, selH)
                                        currentBitmap = cropped
                                        cropStart = null
                                        cropEnd = null
                                        activeTool = EditorTool.PEN
                                        Toast.makeText(context, "Lienzo recortado a ${cropped.width}x${cropped.height} PX", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "✂ APLICAR",
                                    color = Color.White,
                                    fontSize = 7.sp,
                                    fontFamily = SilkscreenFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .background(DarkSurface, RectangleShape)
                                .border(1.dp, DarkBorder, RectangleShape)
                                .clickable {
                                    cropStart = null
                                    cropEnd = null
                                    activeTool = EditorTool.PEN
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "SALIR",
                                color = Color.White,
                                fontSize = 7.sp,
                                fontFamily = SilkscreenFontFamily
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Quick Operations Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(DarkSurface, RectangleShape)
                        .border(1.dp, DarkBorder, RectangleShape)
                        .clickable {
                            pushHistory()
                            val w = currentBitmap.width
                            val h = currentBitmap.height
                            val pixels = IntArray(w * h)
                            currentBitmap.getPixels(pixels, 0, w, 0, 0, w, h)
                            for (i in pixels.indices) {
                                val a = (pixels[i] ushr 24) and 0xFF
                                val r = 255 - ((pixels[i] ushr 16) and 0xFF)
                                val g = 255 - ((pixels[i] ushr 8) and 0xFF)
                                val b = 255 - (pixels[i] and 0xFF)
                                pixels[i] = AndroidColor.argb(a, r, g, b)
                            }
                            val inverted = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            inverted.setPixels(pixels, 0, w, 0, 0, w, h)
                            currentBitmap = inverted
                        }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("INVERTIR", color = Color.White, fontSize = 7.sp, fontFamily = SilkscreenFontFamily)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(DarkSurface, RectangleShape)
                        .border(1.dp, DarkBorder, RectangleShape)
                        .clickable {
                            pushHistory()
                            val w = currentBitmap.width
                            val h = currentBitmap.height
                            val rotated = Bitmap.createBitmap(h, w, Bitmap.Config.ARGB_8888)
                            for (y in 0 until h) {
                                for (x in 0 until w) {
                                    rotated.setPixel(h - 1 - y, x, currentBitmap.getPixel(x, y))
                                }
                            }
                            currentBitmap = rotated
                        }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("ROTAR 90", color = Color.White, fontSize = 7.sp, fontFamily = SilkscreenFontFamily)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(DarkSurface, RectangleShape)
                        .border(1.dp, DarkBorder, RectangleShape)
                        .clickable {
                            pushHistory()
                            val w = currentBitmap.width
                            val h = currentBitmap.height
                            val flipped = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            for (y in 0 until h) {
                                for (x in 0 until w) {
                                    flipped.setPixel(w - 1 - x, y, currentBitmap.getPixel(x, y))
                                }
                            }
                            currentBitmap = flipped
                        }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("VOLTEAR", color = Color.White, fontSize = 7.sp, fontFamily = SilkscreenFontFamily)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Canvas
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(DarkBackground, RectangleShape)
                    .border(2.dp, DarkBorder, RectangleShape)
                    .padding(4.dp)
            ) {
                val containerW = constraints.maxWidth.toFloat()
                val containerH = constraints.maxHeight.toFloat()

                val bmpW = currentBitmap.width.toFloat()
                val bmpH = currentBitmap.height.toFloat()

                val fitScale = minOf(containerW / bmpW, containerH / bmpH) * 0.95f
                val drawW = (bmpW * fitScale).roundToInt()
                val drawH = (bmpH * fitScale).roundToInt()
                val left = ((containerW - drawW) / 2f).roundToInt()
                val top = ((containerH - drawH) / 2f).roundToInt()

                fun handleTouch(touchX: Float, touchY: Float, isInitial: Boolean) {
                    if (touchX < left || touchX >= left + drawW || touchY < top || touchY >= top + drawH) return
                    val px = (((touchX - left) / drawW) * bmpW).toInt().coerceIn(0, currentBitmap.width - 1)
                    val py = (((touchY - top) / drawH) * bmpH).toInt().coerceIn(0, currentBitmap.height - 1)

                    when (activeTool) {
                        EditorTool.PEN -> {
                            if (isInitial) pushHistory()
                            applyPen(px, py)
                        }
                        EditorTool.SWAP_COLOR -> {
                            if (isInitial) {
                                pushHistory()
                                val targetToReplace = currentBitmap.getPixel(px, py)
                                currentBitmap = PixelEngine.replaceColor(currentBitmap, targetToReplace, currentColorArgb)
                                Toast.makeText(context, "Color reemplazado con RGBA #${String.format("%08X", currentColorArgb)}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        EditorTool.EYEDROPPER -> {
                            currentColorArgb = currentBitmap.getPixel(px, py)
                            activeTool = EditorTool.PEN
                            Toast.makeText(context, "Color muestreado: #${String.format("%08X", currentColorArgb)}", Toast.LENGTH_SHORT).show()
                        }
                        EditorTool.CROP -> {
                            if (isInitial) {
                                cropStart = Pair(px, py)
                                cropEnd = Pair(px, py)
                            } else {
                                cropEnd = Pair(px, py)
                            }
                        }
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(activeTool, currentColorArgb, brushSize) {
                            detectDragGestures(
                                onDragStart = { offset -> handleTouch(offset.x, offset.y, true) },
                                onDrag = { change, _ ->
                                    change.consume()
                                    handleTouch(change.position.x, change.position.y, false)
                                }
                            )
                        }
                        .pointerInput(activeTool, currentColorArgb) {
                            detectTapGestures { offset -> handleTouch(offset.x, offset.y, true) }
                        }
                ) {
                    val imageBitmap = currentBitmap.asImageBitmap()
                    drawImage(
                        image = imageBitmap,
                        dstOffset = IntOffset(left, top),
                        dstSize = IntSize(drawW, drawH),
                        filterQuality = FilterQuality.None
                    )

                    drawRect(
                        color = CyanAccent,
                        topLeft = Offset(left.toFloat(), top.toFloat()),
                        size = androidx.compose.ui.geometry.Size(drawW.toFloat(), drawH.toFloat()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                    )

                    // Draw Crop Region & Dimmed Outside Area
                    if (activeTool == EditorTool.CROP && cropStart != null && cropEnd != null) {
                        val s = cropStart!!
                        val e = cropEnd!!
                        val minX = minOf(s.first, e.first)
                        val maxX = maxOf(s.first, e.first)
                        val minY = minOf(s.second, e.second)
                        val maxY = maxOf(s.second, e.second)

                        val selLeft = left + (minX.toFloat() / bmpW * drawW)
                        val selTop = top + (minY.toFloat() / bmpH * drawH)
                        val selRight = left + ((maxX + 1).toFloat() / bmpW * drawW)
                        val selBottom = top + ((maxY + 1).toFloat() / bmpH * drawH)

                        val selWidth = (selRight - selLeft).coerceAtLeast(1f)
                        val selHeight = (selBottom - selTop).coerceAtLeast(1f)

                        // 4 Dimmed regions outside selection
                        // 1. Top
                        drawRect(
                            color = Color.Black.copy(alpha = 0.65f),
                            topLeft = Offset(left.toFloat(), top.toFloat()),
                            size = androidx.compose.ui.geometry.Size(drawW.toFloat(), (selTop - top).coerceAtLeast(0f))
                        )
                        // 2. Bottom
                        drawRect(
                            color = Color.Black.copy(alpha = 0.65f),
                            topLeft = Offset(left.toFloat(), selBottom),
                            size = androidx.compose.ui.geometry.Size(drawW.toFloat(), (top + drawH - selBottom).coerceAtLeast(0f))
                        )
                        // 3. Left
                        drawRect(
                            color = Color.Black.copy(alpha = 0.65f),
                            topLeft = Offset(left.toFloat(), selTop),
                            size = androidx.compose.ui.geometry.Size((selLeft - left).coerceAtLeast(0f), selHeight)
                        )
                        // 4. Right
                        drawRect(
                            color = Color.Black.copy(alpha = 0.65f),
                            topLeft = Offset(selRight, selTop),
                            size = androidx.compose.ui.geometry.Size((left + drawW - selRight).coerceAtLeast(0f), selHeight)
                        )

                        // Neon Red Crop border
                        drawRect(
                            color = Color(0xFFFF5252),
                            topLeft = Offset(selLeft, selTop),
                            size = androidx.compose.ui.geometry.Size(selWidth, selHeight),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Palette & Custom RGBA/HEX8 Selector Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RectangleShape)
                    .border(1.dp, CyanAccent, RectangleShape)
                    .padding(6.dp)
            ) {
                // Header Bar with active color swatch & HEX8/RGBA readout
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Color Swatch Box
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(Color(currentColorArgb), RectangleShape)
                                .border(2.dp, Color.White, RectangleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "COLOR ACTIVO [HEX8 #RRGGBBAA]:",
                                color = TextSecondary,
                                fontSize = 7.sp,
                                fontFamily = SilkscreenFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "#${String.format(Locale.US, "%02X%02X%02X%02X", curR, curG, curB, curA)} | RGBA($curR,$curG,$curB,$curA)",
                                color = Color(currentColorArgb),
                                fontSize = 8.sp,
                                fontFamily = SilkscreenFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Button to expand/collapse custom RGBA / HEX8 picker
                    Box(
                        modifier = Modifier
                            .background(if (showCustomColorPicker) CyanAccent else DarkBackground, RectangleShape)
                            .border(1.dp, if (showCustomColorPicker) Color.White else CyanAccent, RectangleShape)
                            .clickable { showCustomColorPicker = !showCustomColorPicker }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (showCustomColorPicker) "[ OCULTAR CONTROLES ]" else "[ 🎨 CUSTOM RGBA / HEX8 ]",
                            color = if (showCustomColorPicker) Color.Black else CyanAccent,
                            fontSize = 7.sp,
                            fontFamily = PressStart2PFontFamily
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Presets & Custom Swatches Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Custom swatches created by user
                    items(customPalette) { colorInt ->
                        val isSelected = currentColorArgb == colorInt
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(Color(colorInt), RectangleShape)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) CyanAccent else Color.White,
                                    shape = RectangleShape
                                )
                                .clickable { currentColorArgb = colorInt }
                        )
                    }

                    // Standard presets
                    items(presetPalette) { colorInt ->
                        val isSelected = currentColorArgb == colorInt
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(Color(colorInt), RectangleShape)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color.White else DarkBorder,
                                    shape = RectangleShape
                                )
                                .clickable { currentColorArgb = colorInt }
                        )
                    }
                }

                // Expanded Custom RGBA & HEX8 Control Panel
                if (showCustomColorPicker) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBackground, RectangleShape)
                            .border(1.dp, DarkBorder, RectangleShape)
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "ENTRADA DIRECTA HEX8 / RGBA:",
                            color = CyanAccent,
                            fontSize = 8.sp,
                            fontFamily = PressStart2PFontFamily,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // HEX8 Input Field + Apply Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = hexInputText,
                                onValueChange = { input ->
                                    hexInputText = input.uppercase(Locale.US)
                                    parseAndSetHex(input)
                                },
                                placeholder = {
                                    Text("#RRGGBBAA", fontSize = 8.sp, fontFamily = SilkscreenFontFamily)
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanAccent,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RectangleShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            )

                            PixelButton(
                                onClick = {
                                    if (parseAndSetHex(hexInputText)) {
                                        Toast.makeText(context, "Hex8 RRGGBBAA aplicado: #$hexInputText", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Hex invalido (Ej: 00E5FFFF o 00E5FF)", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                backgroundColor = CyanAccent,
                                textColor = Color.Black,
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("APLICAR", fontSize = 8.sp, fontFamily = SilkscreenFontFamily, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "CANALES DE COLOR Y TRANSPARENCIA (0-255):",
                            color = TextSecondary,
                            fontSize = 7.sp,
                            fontFamily = SilkscreenFontFamily,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Channel R (Rojo)
                        RgbaChannelSliderRow(
                            label = "R (ROJO)",
                            value = curR,
                            accentColor = Color(0xFFFF5252),
                            onValueChange = { updateRgbaChannel(r = it) }
                        )

                        // Channel G (Verde)
                        RgbaChannelSliderRow(
                            label = "G (VERDE)",
                            value = curG,
                            accentColor = Color(0xFF69F0AE),
                            onValueChange = { updateRgbaChannel(g = it) }
                        )

                        // Channel B (Azul)
                        RgbaChannelSliderRow(
                            label = "B (AZUL)",
                            value = curB,
                            accentColor = Color(0xFF40C4FF),
                            onValueChange = { updateRgbaChannel(b = it) }
                        )

                        // Channel A (Alfa / Transparencia)
                        RgbaChannelSliderRow(
                            label = "A (ALFA)",
                            value = curA,
                            accentColor = Color(0xFFFFD600),
                            onValueChange = { updateRgbaChannel(a = it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            PixelButton(
                onClick = {
                    onSaveBitmap(currentBitmap)
                    Toast.makeText(context, "PNG Lossless Guardado", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                backgroundColor = CyanAccent,
                textColor = Color.Black,
                borderColor = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "GUARDAR PNG [LOSSLESS]",
                    color = Color.Black,
                    fontSize = 9.sp,
                    fontFamily = SilkscreenFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RgbaChannelSliderRow(
    label: String,
    value: Int,
    accentColor: Color,
    onValueChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "$label: $value",
                color = accentColor,
                fontSize = 8.sp,
                fontFamily = SilkscreenFontFamily,
                fontWeight = FontWeight.Bold
            )

            // Step buttons (-10, -1, +1, +10)
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(
                    modifier = Modifier
                        .background(DarkSurface, RectangleShape)
                        .border(1.dp, DarkBorder, RectangleShape)
                        .clickable { onValueChange(value - 10) }
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text("-10", color = Color.White, fontSize = 7.sp, fontFamily = SilkscreenFontFamily)
                }
                Box(
                    modifier = Modifier
                        .background(DarkSurface, RectangleShape)
                        .border(1.dp, DarkBorder, RectangleShape)
                        .clickable { onValueChange(value - 1) }
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text("-1", color = Color.White, fontSize = 7.sp, fontFamily = SilkscreenFontFamily)
                }
                Box(
                    modifier = Modifier
                        .background(DarkSurface, RectangleShape)
                        .border(1.dp, DarkBorder, RectangleShape)
                        .clickable { onValueChange(value + 1) }
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text("+1", color = Color.White, fontSize = 7.sp, fontFamily = SilkscreenFontFamily)
                }
                Box(
                    modifier = Modifier
                        .background(DarkSurface, RectangleShape)
                        .border(1.dp, DarkBorder, RectangleShape)
                        .clickable { onValueChange(value + 10) }
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text("+10", color = Color.White, fontSize = 7.sp, fontFamily = SilkscreenFontFamily)
                }
            }
        }

        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = 0f..255f,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = DarkBorder
            ),
            modifier = Modifier.fillMaxWidth().height(24.dp)
        )
    }
}
