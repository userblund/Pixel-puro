package com.example.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.os.Environment
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.Surface
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import com.example.ui.pixel.PixelButton
import com.example.ui.pixel.PixelCameraIcon
import com.example.ui.pixel.PixelCloseIcon
import com.example.ui.pixel.PixelFlashIcon
import com.example.ui.pixel.PixelMonitorIcon
import com.example.ui.pixel.PixelSwitchCameraIcon
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.log2

import com.example.util.CameraCalibrationManager

private data class CameraRgbaFrame(
    val width: Int,
    val height: Int,
    val rgba: ByteArray,
    val rotationDegrees: Int
)

/**
 * Copia el plano RGBA_8888 a memoria propia antes de cerrar ImageProxy.
 * La copia queda empaquetada como R,G,B,A sin padding entre píxeles ni filas.
 */
private fun copyRgbaFrame(image: ImageProxy): CameraRgbaFrame {
    require(image.format == PixelFormat.RGBA_8888) {
        "ImageAnalysis no entregó PixelFormat.RGBA_8888 (formato=${image.format})."
    }
    val plane = image.planes.single()
    val source = plane.buffer
    val start = source.position()
    val limit = source.limit()
    val pixelStride = plane.pixelStride
    val rowStride = plane.rowStride
    val crop = image.cropRect
    val cropWidth = crop.width()
    val cropHeight = crop.height()
    require(pixelStride >= 4) { "Pixel stride RGBA inesperado: $pixelStride" }
    require(cropWidth > 0 && cropHeight > 0) {
        "La región de recorte de la cámara está vacía: $crop."
    }

    // ImageProxy puede contener más píxeles que los que Preview muestra.
    // El ViewPort compartido define cropRect; copiar el frame completo ignoraría ese recorte.
    val output = ByteArray(Math.multiplyExact(Math.multiplyExact(cropWidth, cropHeight), 4))
    for (y in 0 until cropHeight) {
        val rowStart = start + (crop.top + y) * rowStride
        for (x in 0 until cropWidth) {
            val src = rowStart + (crop.left + x) * pixelStride
            require(src >= start && src + 3 < limit) {
                "El buffer RGBA no contiene el píxel recortado ($x,$y) completo."
            }
            val dst = (y * cropWidth + x) * 4
            output[dst] = source.get(src)
            output[dst + 1] = source.get(src + 1)
            output[dst + 2] = source.get(src + 2)
            output[dst + 3] = source.get(src + 3)
        }
    }
    return CameraRgbaFrame(cropWidth, cropHeight, output, image.imageInfo.rotationDegrees)
}

/** Refleja horizontalmente el fotograma para igualar la vista previa de la cámara frontal. */
private fun mirrorRgbaFrameHorizontally(frame: CameraRgbaFrame): CameraRgbaFrame {
    val output = ByteArray(frame.rgba.size)
    for (y in 0 until frame.height) {
        for (x in 0 until frame.width) {
            val src = (y * frame.width + x) * 4
            val dst = (y * frame.width + (frame.width - 1 - x)) * 4
            output[dst] = frame.rgba[src]
            output[dst + 1] = frame.rgba[src + 1]
            output[dst + 2] = frame.rgba[src + 2]
            output[dst + 3] = frame.rgba[src + 3]
        }
    }
    return CameraRgbaFrame(frame.width, frame.height, output, frame.rotationDegrees)
}

/** Rota píxeles mediante reordenamiento de bytes; no interpola ni mezcla píxeles. */
private fun rotateRgbaFrame(frame: CameraRgbaFrame): CameraRgbaFrame {
    val rotation = ((frame.rotationDegrees % 360) + 360) % 360
    if (rotation == 0) return frame
    require(rotation == 90 || rotation == 180 || rotation == 270) {
        "Rotación de cámara no compatible: $rotation°."
    }
    val outWidth = if (rotation == 90 || rotation == 270) frame.height else frame.width
    val outHeight = if (rotation == 90 || rotation == 270) frame.width else frame.height
    val output = ByteArray(frame.rgba.size)
    for (y in 0 until frame.height) {
        for (x in 0 until frame.width) {
            val (dx, dy) = when (rotation) {
                90 -> frame.height - 1 - y to x
                180 -> frame.width - 1 - x to frame.height - 1 - y
                else -> y to frame.width - 1 - x
            }
            val src = (y * frame.width + x) * 4
            val dst = (dy * outWidth + dx) * 4
            output[dst] = frame.rgba[src]
            output[dst + 1] = frame.rgba[src + 1]
            output[dst + 2] = frame.rgba[src + 2]
            output[dst + 3] = frame.rgba[src + 3]
        }
    }
    return CameraRgbaFrame(outWidth, outHeight, output, 0)
}

/**
 * Bitmap Android usa Config.ARGB_8888 como representación de 32 bpp.
 * Los bytes RGBA se convierten canal por canal sin cuantización ni interpolación.
 */
private fun rgbaFrameToBitmap(frame: CameraRgbaFrame): Bitmap {
    val pixels = IntArray(Math.multiplyExact(frame.width, frame.height))
    for (i in pixels.indices) {
        val offset = i * 4
        val r = frame.rgba[offset].toInt() and 0xFF
        val g = frame.rgba[offset + 1].toInt() and 0xFF
        val b = frame.rgba[offset + 2].toInt() and 0xFF
        val a = frame.rgba[offset + 3].toInt() and 0xFF
        pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
    }
    return Bitmap.createBitmap(frame.width, frame.height, Bitmap.Config.ARGB_8888).also {
        it.setPixels(pixels, 0, frame.width, 0, 0, frame.width, frame.height)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PixelCameraScreen(
    onPhotoCaptured: (Bitmap) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    var savedCalibrationProfile by remember {
        mutableStateOf(CameraCalibrationManager.loadProfile(context))
    }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Permiso de cámara necesario para capturar fotos", Toast.LENGTH_SHORT).show()
        }
    }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    // Flash modes: 0 = AUTO, 1 = ON (with torch), 2 = OFF
    var flashState by remember { mutableIntStateOf(0) }
    var isCapturing by remember { mutableStateOf(false) }

    // Telemetría del flujo de análisis RGBA_8888; no representa la calidad RAW de la foto.
    var shadowClippingPercent by remember { mutableFloatStateOf(0f) }
    var highlightClippingPercent by remember { mutableFloatStateOf(0f) }
    var dynamicRangeEv by remember { mutableFloatStateOf(10.5f) }
    var certaintyScore by remember { mutableIntStateOf(95) }
    var opticalAdvice by remember { mutableStateOf("Análisis RGBA_8888; RAW si está disponible") }
    var showPrinciplesSheet by remember { mutableStateOf(false) }
    var isScreenBoxGuideActive by remember { mutableStateOf(false) }

    var previewViewInstance by remember { mutableStateOf<PreviewView?>(null) }
    var cameraProviderInstance by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var activeCamera by remember { mutableStateOf<Camera?>(null) }
    var imageCaptureInstance by remember { mutableStateOf<ImageCapture?>(null) }
    var rawCaptureSupported by remember { mutableStateOf(false) }
    // Último fotograma completo RGBA_8888, copiado antes de cerrar ImageProxy.
    val latestRgbaFrame = remember { AtomicReference<CameraRgbaFrame?>(null) }
    val lastRgbaCopyAtMs = remember { AtomicLong(0L) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Function to rebind camera cleanly with live analysis and maximum quality PNG capture
    fun rebindCamera(
        provider: ProcessCameraProvider,
        previewView: PreviewView,
        targetLens: Int,
        flashModeCode: Int
    ) {
        val selector = CameraSelector.Builder()
            .requireLensFacing(targetLens)
            .build()

        if (!provider.hasCamera(selector)) {
            Toast.makeText(context, "Lente no disponible en este dispositivo", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            provider.unbindAll()
            activeCamera = null
            imageCaptureInstance = null
            rawCaptureSupported = false
            latestRgbaFrame.set(null)
            lastRgbaCopyAtMs.set(0L)

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val capFlashMode = when (flashModeCode) {
                0 -> ImageCapture.FLASH_MODE_AUTO
                1 -> ImageCapture.FLASH_MODE_ON
                else -> ImageCapture.FLASH_MODE_OFF
            }

            // ImageCapture no admite RGBA_8888. Si el dispositivo lo permite,
            // conservar también el RAW_SENSOR original junto con el JPEG de visualización.
            val supportedCaptureFormats = try {
                ImageCapture.getImageCaptureCapabilities(provider.getCameraInfo(selector)).supportedOutputFormats
            } catch (_: Exception) {
                emptySet<Int>()
            }
            val supportsRawJpeg = supportedCaptureFormats.contains(ImageCapture.OUTPUT_FORMAT_RAW_JPEG)
            val captureBuilder = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setJpegQuality(100)
                .setFlashMode(capFlashMode)
            captureBuilder.setOutputFormat(
                if (supportsRawJpeg) ImageCapture.OUTPUT_FORMAT_RAW_JPEG
                else ImageCapture.OUTPUT_FORMAT_JPEG
            )
            var capture = captureBuilder.build()

            // ImageAnalysis convierte fotogramas del flujo de análisis a RGBA_8888.
            val imageAnalysis = ImageAnalysis.Builder()
                // Solicita 1080p; CameraX selecciona una resolución realmente admitida por cada lente.
                .setTargetResolution(Size(1920, 1080))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                try {
                    // Copiar a 10 fps evita asignar un buffer completo en cada frame de 30/60 fps.
                    val nowMs = SystemClock.elapsedRealtime()
                    val previousCopyMs = lastRgbaCopyAtMs.get()
                    if (nowMs - previousCopyMs >= 100L && lastRgbaCopyAtMs.compareAndSet(previousCopyMs, nowMs)) {
                        latestRgbaFrame.set(copyRgbaFrame(imageProxy))
                    }
                    val plane = imageProxy.planes[0]
                    val buffer = plane.buffer
                    val bufferStart = buffer.position()
                    val pixelStride = plane.pixelStride
                    val rowStride = plane.rowStride
                    val width = imageProxy.width
                    val height = imageProxy.height

                    var darkCount = 0
                    var brightCount = 0
                    var totalSamples = 0
                    var minLuma = 255
                    var maxLuma = 0

                    val stepY = maxOf(1, height / 24)
                    val stepX = maxOf(1, width / 24)

                    for (y in 0 until height step stepY) {
                        val rowOffset = y * rowStride
                        for (x in 0 until width step stepX) {
                            val index = bufferStart + rowOffset + x * pixelStride
                            if (index + 2 < buffer.limit()) {
                                val r = buffer.get(index).toInt() and 0xFF
                                val g = buffer.get(index + 1).toInt() and 0xFF
                                val b = buffer.get(index + 2).toInt() and 0xFF
                                val luma = (r * 299 + g * 587 + b * 114) / 1000

                                if (luma < 12) darkCount++
                                if (luma > 243) brightCount++
                                if (luma < minLuma) minLuma = luma
                                if (luma > maxLuma) maxLuma = luma
                                totalSamples++
                            }
                        }
                    }

                    if (totalSamples > 0) {
                        val shadowPct = (darkCount * 100f / totalSamples)
                        val lightPct = (brightCount * 100f / totalSamples)
                        val lumaRange = (maxLuma - minLuma).coerceAtLeast(1)
                        val ev = (log2(lumaRange.toFloat()) + 4f).coerceIn(4f, 14f)
                        val certainty = (100 - (shadowPct * 0.8f + lightPct * 1.2f).toInt()).coerceIn(20, 100)

                        val advice = when {
                            lightPct > 12f -> "⚠ LUCES ALTAS QUEMADAS: Reubica el encuadre para conservar los información de color."
                            shadowPct > 22f -> "⚠ PENUMBRA: Activa FLASH / ANTORCHA para dar la información lumínica necesaria."
                            certainty >= 85 -> "✔ VISTA ÓPTIMA: Información lumínica equilibrada para PNG derivado de la captura."
                            else -> "ℹ Sensor estabilizando rango dinámico en tiempo real..."
                        }

                        ContextCompat.getMainExecutor(context).execute {
                            shadowClippingPercent = shadowPct
                            highlightClippingPercent = lightPct
                            dynamicRangeEv = ev
                            certaintyScore = certainty
                            opticalAdvice = advice
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    imageProxy.close()
                }
            }

            // Compartir ViewPort hace que Preview, ImageAnalysis e ImageCapture
            // observen la misma región del sensor (mismo recorte geométrico).
            val viewPort = previewView.viewPort
                ?: throw IllegalStateException("La vista previa aún no tiene un ViewPort válido.")
            fun buildUseCaseGroup(imageCapture: ImageCapture): UseCaseGroup {
                return UseCaseGroup.Builder()
                    .setViewPort(viewPort)
                    .addUseCase(preview)
                    .addUseCase(imageAnalysis)
                    .addUseCase(imageCapture)
                    .build()
            }

            // La capacidad RAW anunciada no garantiza que la combinación de tres
            // flujos sea aceptada por el HAL. Si el enlace RAW falla, reintentar JPEG.
            val camera = try {
                provider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    buildUseCaseGroup(capture)
                )
            } catch (rawBindError: Exception) {
                if (!supportsRawJpeg) throw rawBindError
                provider.unbindAll()
                capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setJpegQuality(100)
                    .setFlashMode(capFlashMode)
                    .setOutputFormat(ImageCapture.OUTPUT_FORMAT_JPEG)
                    .build()
                rawCaptureSupported = false
                provider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    buildUseCaseGroup(capture)
                )
            }

            rawCaptureSupported = capture.outputFormat == ImageCapture.OUTPUT_FORMAT_RAW_JPEG
            activeCamera = camera
            imageCaptureInstance = capture

            // Torch light control
            val shouldTorch = (flashModeCode == 1)
            if (camera.cameraInfo.hasFlashUnit()) {
                camera.cameraControl.enableTorch(shouldTorch)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error al configurar cámara: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Rebind whenever lensFacing, flashState, or cameraProvider changes
    LaunchedEffect(lensFacing, flashState, previewViewInstance, cameraProviderInstance) {
        val provider = cameraProviderInstance
        val previewView = previewViewInstance
        if (provider != null && previewView != null && hasCameraPermission) {
            rebindCamera(provider, previewView, lensFacing, flashState)
        }
    }

    if (!hasCameraPermission) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .pixelShadow(4.dp, 4.dp, Color.Black)
                    .background(DarkSurface, RectangleShape)
                    .border(2.dp, CyanAccent, RectangleShape)
                    .pixelBevel(Color.White.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.8f), 2.dp)
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PixelCameraIcon(tint = CyanAccent, size = 32.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "CÁMARA REQUERIDA",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = PressStart2PFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "PixelPuro necesita acceso a la cámara para capturar fotos en PNG procesado; RAW se conserva cuando está disponible.",
                        color = TextSecondary,
                        fontSize = 9.sp,
                        fontFamily = SilkscreenFontFamily,
                        lineHeight = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PixelButton(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        backgroundColor = CyanAccent,
                        textColor = Color.Black,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "ACTIVAR CÁMARA",
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontFamily = SilkscreenFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    PixelButton(
                        onClick = onClose,
                        backgroundColor = DarkBackground,
                        textColor = Color.White,
                        borderColor = DarkBorder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "VOLVER A GALERÍA",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontFamily = SilkscreenFontFamily
                        )
                    }
                }
            }
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("pixel_camera_screen")
    ) {
        // 1. Live Camera Viewfinder (Large, full, clear, unobstructed view)
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    // FILL_CENTER se combina con el ViewPort compartido de CameraX:
                    // lo visible en Preview coincide geométricamente con el recorte capturado.
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                val mainExecutor = ContextCompat.getMainExecutor(ctx)
                val layoutListener = object : View.OnLayoutChangeListener {
                    override fun onLayoutChange(
                        changedView: View,
                        left: Int,
                        top: Int,
                        right: Int,
                        bottom: Int,
                        oldLeft: Int,
                        oldTop: Int,
                        oldRight: Int,
                        oldBottom: Int
                    ) {
                        if (!cameraProviderFuture.isDone) return
                        val provider = runCatching { cameraProviderFuture.get() }.getOrNull() ?: return
                        if (previewView.viewPort != null) {
                            previewView.removeOnLayoutChangeListener(this)
                            cameraProviderInstance = provider
                            previewViewInstance = previewView
                        }
                    }
                }
                previewView.addOnLayoutChangeListener(layoutListener)
                cameraProviderFuture.addListener({
                    val provider = cameraProviderFuture.get()
                    if (previewView.viewPort != null) {
                        previewView.removeOnLayoutChangeListener(layoutListener)
                        cameraProviderInstance = provider
                        previewViewInstance = previewView
                    }
                }, mainExecutor)

                previewView
            }
        )

        // 2. High-Precision Reticle (Rule of thirds + Center crosshair)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stepX = size.width / 3f
            val stepY = size.height / 3f

            drawLine(Color(0x3300E5FF), Offset(stepX, 0f), Offset(stepX, size.height), 1f)
            drawLine(Color(0x3300E5FF), Offset(stepX * 2, 0f), Offset(stepX * 2, size.height), 1f)
            drawLine(Color(0x3300E5FF), Offset(0f, stepY), Offset(size.width, stepY), 1f)
            drawLine(Color(0x3300E5FF), Offset(0f, stepY * 2), Offset(size.width, stepY * 2), 1f)

            val cx = size.width / 2f
            val cy = size.height / 2f
            val arm = 16.dp.toPx()

            drawLine(CyanAccent, Offset(cx - arm, cy), Offset(cx + arm, cy), 2f)
            drawLine(CyanAccent, Offset(cx, cy - arm), Offset(cx, cy + arm), 2f)
        }

        // 2b. Live Red Bounding Box for PC Screen Transfer ("Recuadro Rojo Pantalla PC")
        if (isScreenBoxGuideActive) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val boxLeft = size.width * 0.10f
                val boxTop = size.height * 0.22f
                val boxWidth = size.width * 0.80f
                val boxHeight = size.height * 0.46f

                // Draw shaded vignette outside the red box
                drawRect(
                    color = Color.Black.copy(alpha = 0.45f),
                    topLeft = Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(size.width, boxTop)
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.45f),
                    topLeft = Offset(0f, boxTop + boxHeight),
                    size = androidx.compose.ui.geometry.Size(size.width, size.height - (boxTop + boxHeight))
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.45f),
                    topLeft = Offset(0f, boxTop),
                    size = androidx.compose.ui.geometry.Size(boxLeft, boxHeight)
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.45f),
                    topLeft = Offset(boxLeft + boxWidth, boxTop),
                    size = androidx.compose.ui.geometry.Size(size.width - (boxLeft + boxWidth), boxHeight)
                )

                // Red Bounding Box
                drawRect(
                    color = Color(0xFFFF1744),
                    topLeft = Offset(boxLeft, boxTop),
                    size = androidx.compose.ui.geometry.Size(boxWidth, boxHeight),
                    style = Stroke(width = 3.dp.toPx())
                )

                // Corner brackets
                val cornerLen = 22.dp.toPx()
                val strokeW = 4.dp.toPx()
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
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.85f), RectangleShape)
                    .border(1.dp, Color(0xFFFF1744), RectangleShape)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "PANTALLA PC EN RECUADRO ROJO\n0% < CONTENIDO ESTIMADO < 100%",
                    color = Color(0xFFFF1744),
                    fontSize = 7.sp,
                    fontFamily = PressStart2PFontFamily,
                    textAlign = TextAlign.Center,
                    lineHeight = 11.sp
                )
            }
        }

        // 3. Top HUD: guía de exposición y estado real de los formatos.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // Main Top Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pixelShadow(3.dp, 3.dp, Color.Black)
                    .background(DarkSurface.copy(alpha = 0.94f), RectangleShape)
                    .border(2.dp, CyanAccent, RectangleShape)
                    .pixelBevel(Color.White.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.8f), 2.dp)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PixelCameraIcon(tint = CyanAccent, size = 18.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "CAPTURA DE ALTA CALIDAD",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontFamily = PressStart2PFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .background(EmeraldAccent, RectangleShape)
                                        .padding(horizontal = 3.dp, vertical = 1.dp)
                                ) {
                                    Text("RGBA_8888 (análisis)", color = Color.Black, fontSize = 6.sp, fontFamily = PressStart2PFontFamily)
                                }
                            }
                            Text(
                                text = "ANÁLISIS RGBA_8888 | RAW: ${if (rawCaptureSupported) "DISPONIBLE" else "NO DISPONIBLE"}",
                                color = EmeraldAccent,
                                fontSize = 7.sp,
                                fontFamily = SilkscreenFontFamily
                            )
                        }
                    }

                    // Exit Button (min 48dp touch target)
                    PixelButton(
                        onClick = onClose,
                        backgroundColor = DarkBackground,
                        textColor = Color.White,
                        borderColor = DarkBorder,
                        modifier = Modifier.height(44.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PixelCloseIcon(tint = Color.White, size = 12.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SALIR",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontFamily = PressStart2PFontFamily
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Real-Time Optical Instructor / Guía Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F1B2B).copy(alpha = 0.92f), RectangleShape)
                    .border(1.dp, if (certaintyScore >= 80) EmeraldAccent else AmberAccent, RectangleShape)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GUÍA INSTRUCTOR:",
                            color = if (certaintyScore >= 80) EmeraldAccent else AmberAccent,
                            fontSize = 7.sp,
                            fontFamily = PressStart2PFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = opticalAdvice,
                            color = Color.White,
                            fontSize = 7.sp,
                            fontFamily = SilkscreenFontFamily,
                            lineHeight = 11.sp
                        )
                        Text(
                            text = "Rango EV: ${String.format(Locale.US, "%.1f", dynamicRangeEv)} | Sombras: ${shadowClippingPercent.toInt()}% | Luces: ${highlightClippingPercent.toInt()}%",
                            color = TextSecondary,
                            fontSize = 6.sp,
                            fontFamily = SilkscreenFontFamily
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Button to open Pinocchio Epistemological Principles modal
                    Box(
                        modifier = Modifier
                            .background(DarkSurface, RectangleShape)
                            .border(1.dp, CyanAccent, RectangleShape)
                            .clickable { showPrinciplesSheet = true }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "📜 PRINCIPIOS",
                            color = CyanAccent,
                            fontSize = 7.sp,
                            fontFamily = SilkscreenFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 4. Bottom Controls Deck: Large Thumb-Friendly Arcade Buttons
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .pixelShadow(0.dp, (-3).dp, Color.Black)
                .background(DarkSurface.copy(alpha = 0.95f), RectangleShape)
                .border(2.dp, CyanAccent, RectangleShape)
                .pixelBevel(Color.White.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.8f), 2.dp)
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Perfil de calibración disponible y guía de encuadre de pantalla PC
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (isScreenBoxGuideActive) Color(0xFFFF1744) else DarkBackground, RectangleShape)
                            .border(1.dp, if (isScreenBoxGuideActive) Color.White else Color(0xFFFF1744), RectangleShape)
                            .clickable {
                                isScreenBoxGuideActive = !isScreenBoxGuideActive
                                if (isScreenBoxGuideActive) {
                                    opticalAdvice = "Guía activa: Encuadra la pantalla PC dentro del recuadro rojo para cotejo contra buffer 32-bit"
                                    Toast.makeText(context, "Modo Guía Pantalla PC: Activo", Toast.LENGTH_SHORT).show()
                                } else {
                                    opticalAdvice = "Análisis RGBA_8888; RAW si está disponible"
                                }
                            }
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PixelMonitorIcon(tint = if (isScreenBoxGuideActive) Color.White else Color(0xFFFF1744), size = 14.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isScreenBoxGuideActive) "RECUADRO ROJO ACTIVO" else "GUÍA RECUADRO ROJO",
                                color = if (isScreenBoxGuideActive) Color.White else Color(0xFFFF1744),
                                fontSize = 6.5.sp,
                                fontFamily = PressStart2PFontFamily
                            )
                        }
                    }

                    if (savedCalibrationProfile != null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(DarkBackground, RectangleShape)
                                .border(1.dp, EmeraldAccent, RectangleShape)
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "PERFIL DISPONIBLE · ORIGINAL INTACTO",
                                color = EmeraldAccent,
                                fontSize = 6.5.sp,
                                fontFamily = PressStart2PFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Controls Row: [ GIRAR ] - [ DISPARAR PNG ] - [ FLASH ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Button: Switch Camera (GIRAR CÁMARA)
                    PixelButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            val newLens = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                            lensFacing = newLens
                            val label = if (newLens == CameraSelector.LENS_FACING_BACK) "Trasera" else "Frontal"
                            Toast.makeText(context, "Cámara: $label", Toast.LENGTH_SHORT).show()
                        },
                        backgroundColor = DarkBackground,
                        textColor = CyanAccent,
                        borderColor = CyanAccent,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            PixelSwitchCameraIcon(tint = CyanAccent, size = 18.dp)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "GIRAR",
                                color = CyanAccent,
                                fontSize = 8.sp,
                                fontFamily = PressStart2PFontFamily
                            )
                        }
                    }

                    // Button: Big Arcade Shutter (DISPARAR CAPTURA DE ALTA CALIDAD)
                    PixelButton(
                        onClick = {
                            val capture = imageCaptureInstance
                            if (capture == null || isCapturing) return@PixelButton
                            isCapturing = true
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                            val rgbaFrame = latestRgbaFrame.get()
                            if (rgbaFrame == null) {
                                isCapturing = false
                                Toast.makeText(context, "Aún no hay un fotograma RGBA_8888 disponible. Espera a que la vista previa se estabilice.", Toast.LENGTH_LONG).show()
                                return@PixelButton
                            }

                            try {
                                // La imagen principal del editor procede exclusivamente del plano RGBA_8888.
                                // La rotación reordena píxeles y no aplica interpolación.
                                var uprightFrame = rotateRgbaFrame(rgbaFrame)
                                // PreviewView espeja automáticamente la cámara frontal.
                                // ImageAnalysis no aplica ese espejo por sí mismo.
                                if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                                    uprightFrame = mirrorRgbaFrameHorizontally(uprightFrame)
                                }
                                val rgbaBitmap = rgbaFrameToBitmap(uprightFrame)
                                val stamp = System.currentTimeMillis()

                                // La captura fija RAW/JPEG, si existe, se conserva por separado.
                                // Nunca se utiliza su JPEG como fuente del editor.
                                if (rawCaptureSupported) {
                                    val rawDirectory = File(
                                        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir,
                                        "PixelPuro/RAW"
                                    ).apply { mkdirs() }
                                    val rawFile = File(rawDirectory, "PixelPuro_${stamp}.dng")
                                    val jpegFile = File(context.cacheDir, "PixelPuro_${stamp}_aux.jpg")
                                    val rawOptions = ImageCapture.OutputFileOptions.Builder(rawFile).build()
                                    val jpegOptions = ImageCapture.OutputFileOptions.Builder(jpegFile).build()
                                    val stillCapture = imageCaptureInstance
                                    if (stillCapture != null) {
                                        var rawSaved = false
                                        var jpegSaved = false
                                        stillCapture.takePicture(
                                            rawOptions,
                                            jpegOptions,
                                            cameraExecutor,
                                            object : ImageCapture.OnImageSavedCallback {
                                                override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                                                    when (results.imageFormat) {
                                                        ImageFormat.RAW_SENSOR -> rawSaved = true
                                                        ImageFormat.JPEG -> jpegSaved = true
                                                    }
                                                    if (rawSaved && jpegSaved) jpegFile.delete()
                                                }
                                                override fun onError(exception: ImageCaptureException) {
                                                    jpegFile.delete()
                                                    ContextCompat.getMainExecutor(context).execute {
                                                        Toast.makeText(context, "El fotograma RGBA se capturó; RAW adicional falló: ${exception.message}", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }

                                scope.launch(Dispatchers.IO) {
                                    val uri = PixelEngine.saveBitmapToGallery(
                                        context,
                                        rgbaBitmap,
                                        "RGBA8888_${stamp}"
                                    )
                                    ContextCompat.getMainExecutor(context).execute {
                                        isCapturing = false
                                        val status = if (uri != null) "PNG lossless guardado." else "PNG disponible en memoria."
                                        val rawStatus = if (rawCaptureSupported) " RAW se conserva aparte cuando termina la captura." else " RAW no está disponible para esta lente."
                                        Toast.makeText(
                                            context,
                                            "$status Fuente: fotograma RGBA_8888 real (${uprightFrame.width}×${uprightFrame.height}).$rawStatus",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        onPhotoCaptured(rgbaBitmap)
                                    }
                                }
                            } catch (e: Exception) {
                                isCapturing = false
                                Toast.makeText(context, "No se pudo convertir el fotograma RGBA_8888: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        backgroundColor = if (isCapturing) Color.Gray else EmeraldAccent,
                        textColor = Color.Black,
                        borderColor = Color.White,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(60.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isCapturing) "CAPTURANDO..." else "DISPARAR",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontFamily = PressStart2PFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "RGBA_8888 PNG + RAW aparte",
                                color = Color(0xFF1B5E20),
                                fontSize = 7.sp,
                                fontFamily = SilkscreenFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Button: Flash / Torch
                    val flashLabel = when (flashState) {
                        0 -> "AUTO"
                        1 -> "ON"
                        else -> "OFF"
                    }
                    val flashTint = when (flashState) {
                        1 -> Color(0xFFFFD600)
                        0 -> CyanAccent
                        else -> Color.Gray
                    }

                    PixelButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            val nextFlash = (flashState + 1) % 3
                            flashState = nextFlash
                            val modeStr = when (nextFlash) {
                                0 -> "Flash: AUTOMÁTICO"
                                1 -> "Flash: ACTIVADO (Luz de antorcha)"
                                else -> "Flash: APAGADO"
                            }
                            Toast.makeText(context, modeStr, Toast.LENGTH_SHORT).show()
                        },
                        backgroundColor = DarkBackground,
                        textColor = flashTint,
                        borderColor = flashTint,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            PixelFlashIcon(tint = flashTint, size = 18.dp)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = flashLabel,
                                color = flashTint,
                                fontSize = 8.sp,
                                fontFamily = PressStart2PFontFamily
                            )
                        }
                    }
                }
            }
        }

        // 5. Epistemological Axioms Modal Sheet
        if (showPrinciplesSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPrinciplesSheet = false },
                containerColor = DarkBackground,
                shape = RectangleShape,
                dragHandle = null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground, RectangleShape)
                        .border(2.dp, CyanAccent, RectangleShape)
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "INSTRUCTOR PNG 32-BIT",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontFamily = PressStart2PFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "AXIOMAS EPISTEMOLÓGICOS DE CERTEZA ÓPTICA",
                                color = EmeraldAccent,
                                fontSize = 7.sp,
                                fontFamily = SilkscreenFontFamily
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(DarkSurface, RectangleShape)
                                .border(1.dp, DarkBorder, RectangleShape)
                                .clickable { showPrinciplesSheet = false },
                            contentAlignment = Alignment.Center
                        ) {
                            PixelCloseIcon(tint = Color.White, size = 12.dp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF102030), RectangleShape)
                            .border(1.dp, CyanAccent, RectangleShape)
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "1. Una computadora no puede darte certeza si no tiene la información necesaria.",
                                color = CyanAccent,
                                fontSize = 8.sp,
                                fontFamily = SilkscreenFontFamily,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 12.sp
                            )
                            Text(
                                text = "2. Si Pinocho está convencido de que algo es cierto, aunque sea falso, su nariz no reacciona.",
                                color = AmberAccent,
                                fontSize = 8.sp,
                                fontFamily = SilkscreenFontFamily,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 12.sp
                            )
                            Text(
                                text = "3. Lo representado puede tener infinitas representaciones posibles sin dejar de ser exactamente lo mismo.",
                                color = Color(0xFF69F0AE),
                                fontSize = 8.sp,
                                fontFamily = SilkscreenFontFamily,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 12.sp
                            )
                            Text(
                                text = "4. Si Pinocho falla, sea únicamente porque le falta información necesaria.",
                                color = Color(0xFFFF5252),
                                fontSize = 8.sp,
                                fontFamily = SilkscreenFontFamily,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "¿Qué conserva cada formato?\n" +
                               "El editor recibe el último fotograma del flujo ImageAnalysis en RGBA_8888 (R,G,B,A de 8 bits por canal). Los bytes se copian antes de cerrar ImageProxy y la rotación solo reordena píxeles. El Bitmap de Android se construye canal por canal; su configuración pública se denomina ARGB_8888, aunque la fuente conservada en memoria es RGBA.\n\n" +
                               "La fotografía fija RAW/JPEG es independiente: el DNG se conserva aparte cuando la lente lo admite y el JPEG auxiliar nunca sustituye al fotograma RGBA del editor. La resolución del flujo RGBA puede ser menor que la de ImageCapture. El PNG evita pérdidas adicionales al guardar, pero no recupera información que el sensor/procesamiento ya haya descartado.",
                        color = TextSecondary,
                        fontSize = 8.sp,
                        fontFamily = SilkscreenFontFamily,
                        lineHeight = 13.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    PixelButton(
                        onClick = { showPrinciplesSheet = false },
                        backgroundColor = CyanAccent,
                        textColor = Color.Black,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(
                            text = "ENTENDIDO [CONTINUAR]",
                            color = Color.Black,
                            fontSize = 8.sp,
                            fontFamily = SilkscreenFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
