package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.NormalizedRect
import com.example.model.OpticalTransferFactor
import com.example.model.ScreenBufferAnalysisReport
import com.example.ui.pixel.PixelBitmapIcon
import com.example.ui.pixel.PixelButton
import com.example.ui.pixel.PixelCloseIcon
import com.example.ui.pixel.PixelMonitorIcon
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

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import com.example.ui.pixel.PixelCameraIcon
import com.example.util.CameraCalibrationManager
import com.example.util.CameraCalibrationProfile
import kotlinx.coroutines.launch

@Composable
fun ScreenVsBufferAnalyzerDialog(
    cameraPhoto: Bitmap,
    referenceBuffer: Bitmap,
    onSaveReconstructed: (Bitmap) -> Unit,
    onLoadToCanvas: (Bitmap) -> Unit,
    onOpenCameraForImagen2: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentCameraPhoto by remember { mutableStateOf(cameraPhoto) }
    var currentReferenceBuffer by remember { mutableStateOf(referenceBuffer) }

    var normalizedRect by remember {
        mutableStateOf(NormalizedRect(left = 0.12f, top = 0.15f, right = 0.88f, bottom = 0.85f))
    }

    var report by remember {
        mutableStateOf(PixelEngine.analyzeScreenVsBuffer(currentCameraPhoto, normalizedRect, currentReferenceBuffer))
    }

    var simulatedBuffer by remember {
        mutableStateOf<Bitmap?>(null)
    }

    var image3Verification by remember {
        mutableStateOf<Bitmap?>(null)
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Axiomas, 1: Simulación 100%, 2: Imagen 3 (Calibración)
    var activeAspectPreset by remember { mutableStateOf("16:9") }

    // Photo picker launchers for uploading Imagen 1 and Imagen 2 directly inside the dialog
    val imagen1PickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val bmp = PixelEngine.loadUriLossless(context, uri)
                if (bmp != null) {
                    currentReferenceBuffer = bmp
                    Toast.makeText(context, "✔ Imagen 1 (Buffer PC) cargada exitosamente", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error al cargar Imagen 1", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val imagen2PickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val bmp = PixelEngine.loadUriLossless(context, uri)
                if (bmp != null) {
                    currentCameraPhoto = bmp
                    Toast.makeText(context, "✔ Imagen 2 (Foto Cámara) cargada exitosamente", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error al cargar Imagen 2", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(normalizedRect, currentCameraPhoto, currentReferenceBuffer) {
        report = PixelEngine.analyzeScreenVsBuffer(currentCameraPhoto, normalizedRect, currentReferenceBuffer)
        simulatedBuffer = null
        image3Verification = PixelEngine.generateCalibrationVerificationImage(currentCameraPhoto, normalizedRect, currentReferenceBuffer, report)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground.copy(alpha = 0.98f))
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pixelShadow(3.dp, 3.dp, Color.Black)
                        .background(DarkSurface, RectangleShape)
                        .border(2.dp, Color(0xFFFF1744), RectangleShape)
                        .pixelBevel(Color.White.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.8f), 2.dp)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PixelMonitorIcon(tint = Color(0xFFFF1744), size = 18.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "COTEJO PANTALLA PC <-> BUFFER",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontFamily = PressStart2PFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "RECUADRO ROJO | 0% < CONTENIDO < 100%",
                                    color = Color(0xFFFF1744),
                                    fontSize = 7.sp,
                                    fontFamily = SilkscreenFontFamily
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(DarkBackground, RectangleShape)
                                .border(1.dp, Color(0xFFFF1744), RectangleShape)
                                .clickable { onDismiss() }
                                .testTag("close_screen_analyzer_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            PixelCloseIcon(tint = Color.White, size = 12.dp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Image Upload Bar (Imagen 1 & Imagen 2 Selection Buttons)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Upload / Change Imagen 1 (Buffer PC)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(DarkSurface, RectangleShape)
                            .border(1.dp, CyanAccent, RectangleShape)
                            .clickable {
                                imagen1PickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                            .padding(vertical = 5.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "📁 IMAGEN 1 (BUFFER PC)",
                                color = CyanAccent,
                                fontSize = 6.5.sp,
                                fontFamily = PressStart2PFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${currentReferenceBuffer.width}x${currentReferenceBuffer.height} PX [SUBIR]",
                                color = TextSecondary,
                                fontSize = 6.sp,
                                fontFamily = SilkscreenFontFamily
                            )
                        }
                    }

                    // Upload / Change Imagen 2 (Foto Cámara)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(DarkSurface, RectangleShape)
                            .border(1.dp, Color(0xFFFF1744), RectangleShape)
                            .clickable {
                                imagen2PickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                            .padding(vertical = 5.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "📸 IMAGEN 2 (FOTO CÁMARA)",
                                color = Color(0xFFFF1744),
                                fontSize = 6.5.sp,
                                fontFamily = PressStart2PFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${currentCameraPhoto.width}x${currentCameraPhoto.height} PX [SUBIR]",
                                color = TextSecondary,
                                fontSize = 6.sp,
                                fontFamily = SilkscreenFontFamily
                            )
                        }
                    }

                    // Camera Trigger Button for Imagen 2
                    Box(
                        modifier = Modifier
                            .background(DarkSurface, RectangleShape)
                            .border(1.dp, EmeraldAccent, RectangleShape)
                            .clickable {
                                onOpenCameraForImagen2()
                            }
                            .padding(horizontal = 6.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            PixelCameraIcon(tint = EmeraldAccent, size = 12.dp)
                            Text(
                                text = "FOTO",
                                color = EmeraldAccent,
                                fontSize = 6.sp,
                                fontFamily = PressStart2PFontFamily
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))


                // Previsualización simultánea: Imagen 1 completa y sin recuadro;
                // Imagen 2 con recuadro rojo ajustable.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .pixelShadow(2.dp, 2.dp, Color.Black)
                        .background(Color.Black, RectangleShape)
                        .border(1.dp, DarkBorder, RectangleShape)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxSize()
                                .background(Color(0xFF080D14), RectangleShape)
                                .testTag("imagen1_preview")
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val bitmap = currentReferenceBuffer.asImageBitmap()
                                val scale = minOf(size.width / bitmap.width.toFloat(), size.height / bitmap.height.toFloat())
                                val drawWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
                                val drawHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
                                val left = ((size.width - drawWidth) / 2f).toInt()
                                val top = ((size.height - drawHeight) / 2f).toInt()
                                drawImage(
                                    image = bitmap,
                                    dstOffset = androidx.compose.ui.unit.IntOffset(left, top),
                                    dstSize = androidx.compose.ui.unit.IntSize(drawWidth, drawHeight)
                                )
                            }
                            Box(
                                modifier = Modifier.align(Alignment.TopCenter)
                                    .background(Color.Black.copy(alpha = 0.88f), RectangleShape)
                                    .border(1.dp, CyanAccent, RectangleShape)
                                    .padding(horizontal = 4.dp, vertical = 3.dp)
                            ) {
                                Text("IMAGEN 1 · BUFFER PC", color = CyanAccent, fontSize = 6.sp, fontFamily = PressStart2PFontFamily)
                            }
                            Text(
                                text = "${currentReferenceBuffer.width} × ${currentReferenceBuffer.height} PX · SIN RECUADRO",
                                modifier = Modifier.align(Alignment.BottomCenter)
                                    .background(Color.Black.copy(alpha = 0.88f), RectangleShape)
                                    .padding(horizontal = 3.dp, vertical = 2.dp),
                                color = CyanAccent, fontSize = 5.sp, fontFamily = SilkscreenFontFamily
                            )
                        }

                        BoxWithConstraints(
                            modifier = Modifier.weight(1f).fillMaxSize()
                                .background(Color(0xFF080D14), RectangleShape)
                                .testTag("imagen2_preview")
                        ) {
                            Canvas(
                                modifier = Modifier.fillMaxSize()
                                    .pointerInput(currentCameraPhoto) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            val scale = minOf(
                                                size.width / currentCameraPhoto.width.toFloat(),
                                                size.height / currentCameraPhoto.height.toFloat()
                                            )
                                            val drawWidth = (currentCameraPhoto.width * scale).coerceAtLeast(1f)
                                            val drawHeight = (currentCameraPhoto.height * scale).coerceAtLeast(1f)
                                            val deltaX = dragAmount.x / drawWidth
                                            val deltaY = dragAmount.y / drawHeight
                                            val newLeft = (normalizedRect.left + deltaX).coerceIn(0.01f, normalizedRect.right - 0.1f)
                                            val newRight = (normalizedRect.right + deltaX).coerceIn(newLeft + 0.1f, 0.99f)
                                            val newTop = (normalizedRect.top + deltaY).coerceIn(0.01f, normalizedRect.bottom - 0.1f)
                                            val newBottom = (normalizedRect.bottom + deltaY).coerceIn(newTop + 0.1f, 0.99f)
                                            normalizedRect = NormalizedRect(newLeft, newTop, newRight, newBottom)
                                        }
                                    }
                            ) {
                                val bitmap = currentCameraPhoto.asImageBitmap()
                                val scale = minOf(size.width / bitmap.width.toFloat(), size.height / bitmap.height.toFloat())
                                val drawWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
                                val drawHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
                                val left = ((size.width - drawWidth) / 2f).toInt()
                                val top = ((size.height - drawHeight) / 2f).toInt()
                                drawImage(
                                    image = bitmap,
                                    dstOffset = androidx.compose.ui.unit.IntOffset(left, top),
                                    dstSize = androidx.compose.ui.unit.IntSize(drawWidth, drawHeight)
                                )
                                val boxLeft = left + normalizedRect.left * drawWidth
                                val boxTop = top + normalizedRect.top * drawHeight
                                val boxWidth = normalizedRect.width * drawWidth
                                val boxHeight = normalizedRect.height * drawHeight
                                drawRect(Color.Black.copy(alpha = 0.5f), Offset(left.toFloat(), top.toFloat()), Size(drawWidth.toFloat(), (boxTop - top).coerceAtLeast(0f)))
                                drawRect(Color.Black.copy(alpha = 0.5f), Offset(left.toFloat(), boxTop + boxHeight), Size(drawWidth.toFloat(), (top + drawHeight - boxTop - boxHeight).coerceAtLeast(0f)))
                                drawRect(Color.Black.copy(alpha = 0.5f), Offset(left.toFloat(), boxTop), Size((boxLeft - left).coerceAtLeast(0f), boxHeight))
                                drawRect(Color.Black.copy(alpha = 0.5f), Offset(boxLeft + boxWidth, boxTop), Size((left + drawWidth - boxLeft - boxWidth).coerceAtLeast(0f), boxHeight))
                                drawRect(
                                    color = Color(0xFFFF1744),
                                    topLeft = Offset(boxLeft, boxTop),
                                    size = Size(boxWidth, boxHeight),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                val cornerLen = 10.dp.toPx()
                                val strokeW = 3.dp.toPx()
                                drawLine(Color.White, Offset(boxLeft, boxTop), Offset(boxLeft + cornerLen, boxTop), strokeWidth = strokeW)
                                drawLine(Color.White, Offset(boxLeft, boxTop), Offset(boxLeft, boxTop + cornerLen), strokeWidth = strokeW)
                                drawLine(Color.White, Offset(boxLeft + boxWidth, boxTop), Offset(boxLeft + boxWidth - cornerLen, boxTop), strokeWidth = strokeW)
                                drawLine(Color.White, Offset(boxLeft + boxWidth, boxTop), Offset(boxLeft + boxWidth, boxTop + cornerLen), strokeWidth = strokeW)
                                drawLine(Color.White, Offset(boxLeft, boxTop + boxHeight), Offset(boxLeft + cornerLen, boxTop + boxHeight), strokeWidth = strokeW)
                                drawLine(Color.White, Offset(boxLeft, boxTop + boxHeight), Offset(boxLeft, boxTop + boxHeight - cornerLen), strokeWidth = strokeW)
                                drawLine(Color.White, Offset(boxLeft + boxWidth, boxTop + boxHeight), Offset(boxLeft + boxWidth - cornerLen, boxTop + boxHeight), strokeWidth = strokeW)
                                drawLine(Color.White, Offset(boxLeft + boxWidth, boxTop + boxHeight), Offset(boxLeft + boxWidth, boxTop + boxHeight - cornerLen), strokeWidth = strokeW)
                            }
                            Box(
                                modifier = Modifier.align(Alignment.TopCenter)
                                    .background(Color.Black.copy(alpha = 0.88f), RectangleShape)
                                    .border(1.dp, Color(0xFFFF1744), RectangleShape)
                                    .padding(horizontal = 4.dp, vertical = 3.dp)
                            ) {
                                Text("IMAGEN 2 · FOTO", color = Color(0xFFFF1744), fontSize = 6.sp, fontFamily = PressStart2PFontFamily)
                            }
                            Text(
                                text = "RECUADRO ROJO · ARRASTRA PARA AJUSTAR",
                                modifier = Modifier.align(Alignment.BottomCenter)
                                    .background(Color.Black.copy(alpha = 0.88f), RectangleShape)
                                    .padding(horizontal = 3.dp, vertical = 2.dp),
                                color = Color(0xFFFF1744), fontSize = 5.sp, fontFamily = SilkscreenFontFamily
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Aspect Ratio / Box Presets Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val presets = listOf("16:9", "16:10", "4:3", "CENTRO")
                    presets.forEach { p ->
                        val isSel = activeAspectPreset == p
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (isSel) Color(0xFFFF1744) else DarkSurface, RectangleShape)
                                .border(1.dp, if (isSel) Color.White else DarkBorder, RectangleShape)
                                .clickable {
                                    activeAspectPreset = p
                                    normalizedRect = when (p) {
                                        "16:9" -> NormalizedRect(0.10f, 0.22f, 0.90f, 0.78f)
                                        "16:10" -> NormalizedRect(0.12f, 0.18f, 0.88f, 0.82f)
                                        "4:3" -> NormalizedRect(0.18f, 0.15f, 0.82f, 0.85f)
                                        else -> NormalizedRect(0.15f, 0.15f, 0.85f, 0.85f)
                                    }
                                }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = p,
                                color = if (isSel) Color.White else TextSecondary,
                                fontSize = 7.sp,
                                fontFamily = PressStart2PFontFamily
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Navigation Tabs (3-Step Flow)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf(
                        "1. AXIOMAS",
                        "2. RECONSTRUCCIÓN",
                        "3. IMAGEN 3 (CALIBRACIÓN)"
                    )
                    tabs.forEachIndexed { index, title ->
                        val isSel = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (isSel) DarkSurface else DarkBackground, RectangleShape)
                                .border(1.dp, if (isSel) CyanAccent else DarkBorder, RectangleShape)
                                .clickable { selectedTab = index }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (isSel) CyanAccent else TextMuted,
                                fontSize = 6.5.sp,
                                fontFamily = PressStart2PFontFamily,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Tab Content (Scrollable)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // Epistemological & Optical Transfer Axiom Banner
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSurface, RectangleShape)
                                    .border(1.dp, Color(0xFFFF1744), RectangleShape)
                                    .padding(8.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        text = "INTERPRETACIÓN DEL SISTEMA:",
                                        color = Color(0xFFFF1744),
                                        fontSize = 8.sp,
                                        fontFamily = PressStart2PFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "• La pantalla de la PC está dentro del recuadro rojo.\n" +
                                                "• Contenido actual: 0% < ${String.format(Locale.US, "%.1f", report.fidelityPercentage)}% < 100%.\n" +
                                                "• NO es 100% debido a la degradación fotónica, distorsión de perspectiva, respuesta no lineal del sensor y desacoplo espectral.\n" +
                                                "• A continuación se detalla qué tendría que ocurrir físicamente y matemáticamente para que llegue al 100%:",
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontFamily = SilkscreenFontFamily,
                                        lineHeight = 12.sp
                                    )
                                }
                            }

                            // Factor Cards
                            report.factors.forEach { factor ->
                                FactorDetailCard(factor = factor)
                            }
                        }

                        1 -> {
                            // Reconstructive Simulation View
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSurface, RectangleShape)
                                    .border(1.dp, EmeraldAccent, RectangleShape)
                                    .padding(8.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "SIMULACIÓN DE COMPENSACIÓN INVERSA",
                                        color = EmeraldAccent,
                                        fontSize = 8.sp,
                                        fontFamily = PressStart2PFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Aplica la matriz de corrección cromática 3x3, sustracción de flare ambiental (${report.ambientFlareOffset} LSB) e inversión gamma (γ_inv = ${String.format(Locale.US, "%.3f", report.inverseGamma)}) directamente sobre los píxeles del recuadro rojo.",
                                        color = TextSecondary,
                                        fontSize = 7.sp,
                                        fontFamily = SilkscreenFontFamily
                                    )

                                    if (simulatedBuffer == null) {
                                        PixelButton(
                                            onClick = {
                                                simulatedBuffer = PixelEngine.simulateReconstructedBuffer(
                                                    cameraPhoto = cameraPhoto,
                                                    normalizedRect = normalizedRect,
                                                    report = report,
                                                    targetWidth = referenceBuffer.width.coerceIn(128, 512),
                                                    targetHeight = referenceBuffer.height.coerceIn(128, 512)
                                                )
                                            },
                                            backgroundColor = DarkBackground,
                                            borderColor = EmeraldAccent,
                                            textColor = EmeraldAccent,
                                            modifier = Modifier.fillMaxWidth().testTag("simulate_inverse_button")
                                        ) {
                                            Text(
                                                text = "APLICAR TRANSFORMACIÓN (HACIA 100%)",
                                                color = EmeraldAccent,
                                                fontSize = 8.sp,
                                                fontFamily = PressStart2PFontFamily
                                            )
                                        }
                                    } else {
                                        // Display Reconstructed Buffer
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            // Reconstructed
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = "RECONSTRUIDO",
                                                    color = EmeraldAccent,
                                                    fontSize = 7.sp,
                                                    fontFamily = PressStart2PFontFamily
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(130.dp)
                                                        .background(Color.Black, RectangleShape)
                                                        .border(1.dp, EmeraldAccent, RectangleShape)
                                                ) {
                                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                                        drawImage(
                                                            image = simulatedBuffer!!.asImageBitmap(),
                                                            dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
                                                        )
                                                    }
                                                }
                                            }

                                            // Reference
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = "BUFFER ORIGINAL",
                                                    color = CyanAccent,
                                                    fontSize = 7.sp,
                                                    fontFamily = PressStart2PFontFamily
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(130.dp)
                                                        .background(Color.Black, RectangleShape)
                                                        .border(1.dp, CyanAccent, RectangleShape)
                                                ) {
                                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                                        drawImage(
                                                            image = referenceBuffer.asImageBitmap(),
                                                            dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Actions
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            PixelButton(
                                                onClick = {
                                                    simulatedBuffer?.let { onSaveReconstructed(it) }
                                                },
                                                backgroundColor = DarkBackground,
                                                borderColor = EmeraldAccent,
                                                textColor = EmeraldAccent,
                                                modifier = Modifier.weight(1f).testTag("save_reconstructed_button")
                                            ) {
                                                Text(
                                                    text = "GUARDAR EN GALERÍA",
                                                    color = EmeraldAccent,
                                                    fontSize = 7.sp,
                                                    fontFamily = PressStart2PFontFamily
                                                )
                                            }

                                            PixelButton(
                                                onClick = {
                                                    simulatedBuffer?.let { onLoadToCanvas(it) }
                                                    onDismiss()
                                                },
                                                backgroundColor = DarkBackground,
                                                borderColor = CyanAccent,
                                                textColor = CyanAccent,
                                                modifier = Modifier.weight(1f).testTag("load_reconstructed_button")
                                            ) {
                                                Text(
                                                    text = "CARGAR AL LIENZO",
                                                    color = CyanAccent,
                                                    fontSize = 7.sp,
                                                    fontFamily = PressStart2PFontFamily
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // IMAGEN 3: Verification Composite & Calibration Profile Generator
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSurface, RectangleShape)
                                    .border(1.dp, Color(0xFFFF1744), RectangleShape)
                                    .padding(8.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "TERCERA IMAGEN (VERIFICACIÓN & CALIBRACIÓN GENERADA)",
                                        color = Color(0xFFFF1744),
                                        fontSize = 8.sp,
                                        fontFamily = PressStart2PFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Al reunir Imagen 1 (Buffer PC) e Imagen 2 (Foto Cámara con Recuadro Rojo), la APK genera automáticamente esta tercera imagen para verificar si identificó correctamente la pantalla y guardar el Perfil de Calibración.",
                                        color = TextSecondary,
                                        fontSize = 7.sp,
                                        fontFamily = SilkscreenFontFamily,
                                        lineHeight = 11.sp
                                    )

                                    if (image3Verification != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp)
                                                .background(Color.Black, RectangleShape)
                                                .border(1.dp, Color(0xFFFF1744), RectangleShape)
                                        ) {
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                drawImage(
                                                    image = image3Verification!!.asImageBitmap(),
                                                    dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Save Calibration Profile Button
                                        PixelButton(
                                            onClick = {
                                                val profile = CameraCalibrationProfile(
                                                    profileName = "Calibración Recuadro Rojo Pantalla PC",
                                                    colorCorrectionMatrix = report.colorCorrectionMatrix,
                                                    inverseGamma = report.inverseGamma,
                                                    ambientFlareOffset = report.ambientFlareOffset,
                                                    rectLeft = normalizedRect.left,
                                                    rectTop = normalizedRect.top,
                                                    rectRight = normalizedRect.right,
                                                    rectBottom = normalizedRect.bottom,
                                                    fidelityPercentage = report.fidelityPercentage,
                                                    opticalCertainty = report.opticalCertainty
                                                )
                                                CameraCalibrationManager.saveProfile(context, profile)
                                                Toast.makeText(context, "✔ PERFIL DE CALIBRACIÓN GUARDADO: Las fotos futuras de la cámara aplicarán esta calibración automáticamente", Toast.LENGTH_LONG).show()
                                            },
                                            backgroundColor = EmeraldAccent,
                                            borderColor = Color.White,
                                            textColor = Color.Black,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("save_calibration_profile_button")
                                        ) {
                                            Text(
                                                text = "★ GUARDAR PERFIL DE CALIBRACIÓN PARA CÁMARA ★",
                                                color = Color.Black,
                                                fontSize = 7.5.sp,
                                                fontFamily = PressStart2PFontFamily,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            PixelButton(
                                                onClick = {
                                                    image3Verification?.let { onSaveReconstructed(it) }
                                                },
                                                backgroundColor = DarkBackground,
                                                borderColor = Color(0xFFFF1744),
                                                textColor = Color(0xFFFF1744),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = "GUARDAR IMAGEN 3 EN GALERÍA",
                                                    color = Color(0xFFFF1744),
                                                    fontSize = 7.sp,
                                                    fontFamily = PressStart2PFontFamily
                                                )
                                            }

                                            PixelButton(
                                                onClick = {
                                                    val recon = PixelEngine.simulateReconstructedBuffer(
                                                        cameraPhoto = currentCameraPhoto,
                                                        normalizedRect = normalizedRect,
                                                        report = report,
                                                        targetWidth = referenceBuffer.width,
                                                        targetHeight = referenceBuffer.height
                                                    )
                                                    onLoadToCanvas(recon)
                                                    onDismiss()
                                                },
                                                backgroundColor = DarkBackground,
                                                borderColor = CyanAccent,
                                                textColor = CyanAccent,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = "CARGAR AL LIENZO",
                                                    color = CyanAccent,
                                                    fontSize = 7.sp,
                                                    fontFamily = PressStart2PFontFamily
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FactorDetailCard(factor: OpticalTransferFactor) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pixelShadow(2.dp, 2.dp, Color.Black)
            .background(DarkSurface, RectangleShape)
            .border(1.dp, DarkBorder, RectangleShape)
            .padding(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = factor.title,
                    color = AmberAccent,
                    fontSize = 8.sp,
                    fontFamily = PressStart2PFontFamily,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format(Locale.US, "-%.1f%%", factor.degradationPenaltyPercent),
                    color = Color(0xFFFF1744),
                    fontSize = 7.sp,
                    fontFamily = PressStart2PFontFamily
                )
            }

            Text(
                text = factor.subtitle,
                color = CyanAccent,
                fontSize = 7.sp,
                fontFamily = SilkscreenFontFamily
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "ESTADO OBSERVADO:",
                color = TextMuted,
                fontSize = 7.sp,
                fontFamily = SilkscreenFontFamily,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = factor.currentObserved,
                color = Color.White,
                fontSize = 8.sp,
                fontFamily = SilkscreenFontFamily
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "¿QUÉ TENDRÍA QUE PASAR PARA QUE SEA 100%?:",
                color = EmeraldAccent,
                fontSize = 7.sp,
                fontFamily = SilkscreenFontFamily,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = factor.requiredCondition,
                color = Color(0xFFE0E0E0),
                fontSize = 8.sp,
                fontFamily = SilkscreenFontFamily
            )

            Spacer(modifier = Modifier.height(2.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f), RectangleShape)
                    .border(1.dp, DarkBorder, RectangleShape)
                    .padding(4.dp)
            ) {
                Text(
                    text = "FÓRMULA: " + factor.theoreticalFormula,
                    color = CyanAccent,
                    fontSize = 7.sp,
                    fontFamily = SilkscreenFontFamily
                )
            }
        }
    }
}
