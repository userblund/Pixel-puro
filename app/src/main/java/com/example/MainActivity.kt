package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.PixelColor
import com.example.service.FloatingPixelService
import com.example.ui.PixelPuroViewModel
import com.example.ui.components.ColorAnalyzerSheet
import com.example.ui.components.DeviceGalleryBottomBar
import com.example.ui.components.FloatingOverlayPromptDialog
import com.example.ui.components.GalleryPresetsSheet
import com.example.ui.components.FullDeviceGalleryDialog
import com.example.ui.components.IntegrityCheckDialog
import com.example.ui.components.PixelEditorDialog
import com.example.ui.components.ScreenVsBufferAnalyzerDialog
import com.example.ui.components.SurgicalEyedropperPanel
import com.example.ui.pixel.PixelBrushIcon
import com.example.ui.pixel.PixelCameraIcon
import com.example.ui.pixel.PixelCloseIcon
import com.example.ui.pixel.PixelDropperIcon
import com.example.ui.pixel.PixelGridIcon
import com.example.ui.pixel.PixelLayersIcon
import com.example.ui.pixel.PixelMonitorIcon
import com.example.ui.pixel.PixelPaletteIcon
import com.example.ui.pixel.PixelTuneIcon
import com.example.ui.pixel.pixelBevel
import com.example.ui.pixel.pixelShadow
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PressStart2PFontFamily
import com.example.ui.theme.SilkscreenFontFamily
import com.example.ui.theme.TextSecondary
import com.example.ui.view.PixelCanvasViewer
import java.util.Locale
import kotlin.math.abs

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                PixelPuroApp()
            }
        }
    }
}

@Composable
fun PixelPuroApp(
    viewModel: PixelPuroViewModel = viewModel()
) {
    val context = LocalContext.current

    val currentBitmap by viewModel.currentBitmap.collectAsState()
    val activePreset by viewModel.activePreset.collectAsState()
    val isEyedropperActive by viewModel.isEyedropperActive.collectAsState()
    val selectedPixelColor by viewModel.selectedPixelColor.collectAsState()
    val selectedPixelX by viewModel.selectedPixelX.collectAsState()
    val selectedPixelY by viewModel.selectedPixelY.collectAsState()
    val isGridEnabled by viewModel.isGridEnabled.collectAsState()
    val useBilinearFilter by viewModel.useBilinearFilter.collectAsState()
    val isolatedColor by viewModel.isolatedColor.collectAsState()
    val isolatedBitmap by viewModel.isolatedBitmap.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val integrityReport by viewModel.integrityReport.collectAsState()
    val devicePhotos by viewModel.devicePhotos.collectAsState()
    val selectedMediaItem by viewModel.selectedMediaItem.collectAsState()

    var currentScale by remember { mutableFloatStateOf(1.0f) }
    var isUIVisible by remember { mutableStateOf(true) }

    var showCameraScreen by remember { mutableStateOf(false) }
    var showAnalyzerSheet by remember { mutableStateOf(false) }
    var showEditorDialog by remember { mutableStateOf(false) }
    var showGallerySheet by remember { mutableStateOf(false) }
    var showFullDeviceGallery by remember { mutableStateOf(false) }
    var showFloatingDialog by remember { mutableStateOf(false) }
    var showCalibrationDialog by remember { mutableStateOf(false) }
    var showScreenAnalyzerDialog by remember { mutableStateOf(false) }
    var screenAnalyzerReferenceBuffer by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var screenAnalyzerNormalizedRect by remember {
        mutableStateOf(com.example.model.NormalizedRect(left = 0.12f, top = 0.15f, right = 0.88f, bottom = 0.85f))
    }
    var returnToScreenAnalyzerAfterCapture by remember { mutableStateOf(false) }
    var calibrationColorTarget by remember { mutableStateOf<PixelColor?>(null) }

    val mediaPermissions = when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun checkMediaAccess(): Boolean = mediaPermissions.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    var hasMediaPermission by remember {
        mutableStateOf(
            checkMediaAccess()
        )
    }

    val requestMediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasMediaPermission = checkMediaAccess()
        if (hasMediaPermission) {
            viewModel.scanDevicePhotos(context, autoSelectLatest = true)
            Toast.makeText(context, "Fotos cargadas automáticamente", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (hasMediaPermission) {
            viewModel.scanDevicePhotos(context, autoSelectLatest = true)
        } else {
            requestMediaPermissionLauncher.launch(mediaPermissions)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadFromUri(context, uri)
            Toast.makeText(context, "Imagen cargada en modo 1:1 sin compresión", Toast.LENGTH_SHORT).show()
        }
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(context)) {
            Toast.makeText(context, "Permiso de superposición concedido", Toast.LENGTH_SHORT).show()
        }
    }

    fun startFloatingService() {
        if (Settings.canDrawOverlays(context)) {
            val intent = Intent(context, FloatingPixelService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Toast.makeText(context, "PixelPuro minimizado al 2% con cuentagotas flotante", Toast.LENGTH_LONG).show()
        } else {
            showFloatingDialog = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Collapsible Top HUD (Positioned ABOVE the photo - NEVER covers the image)
            AnimatedVisibility(
                visible = isUIVisible,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Main Pixel Toolbar (Stepped Rectangles & 3D Bevels)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pixelShadow(3.dp, 3.dp, Color.Black)
                            .background(DarkSurface, RectangleShape)
                            .border(2.dp, CyanAccent, RectangleShape)
                            .pixelBevel(Color.White.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.8f), 2.dp)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("pixel_top_bar")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Logo & Dimensions
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { showGallerySheet = true }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(CyanAccent, RectangleShape)
                                        .border(1.dp, Color.White, RectangleShape)
                                        .pixelBevel(Color.White.copy(alpha = 0.6f), Color.Black.copy(alpha = 0.6f), 1.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "1:1",
                                        color = Color.Black,
                                        fontSize = 7.sp,
                                        fontFamily = PressStart2PFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "PIXELPURO",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontFamily = PressStart2PFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${currentBitmap.width}x${currentBitmap.height} PX",
                                        color = EmeraldAccent,
                                        fontSize = 7.sp,
                                        fontFamily = SilkscreenFontFamily
                                    )
                                }
                            }

                            // Pure Pixel Action Buttons
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Live Pixel Camera
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(DarkBackground, RectangleShape)
                                        .border(1.dp, EmeraldAccent, RectangleShape)
                                        .clickable { showCameraScreen = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    PixelCameraIcon(tint = EmeraldAccent, size = 14.dp)
                                }

                                // Screenshot Integrity Verification
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(DarkBackground, RectangleShape)
                                        .border(1.dp, CyanAccent, RectangleShape)
                                        .clickable { viewModel.runIntegrityCheck() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    PixelCameraIcon(tint = CyanAccent, size = 14.dp)
                                }

                                // Eyedropper
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(if (isEyedropperActive) CyanAccent else DarkBackground, RectangleShape)
                                        .border(1.dp, if (isEyedropperActive) Color.White else DarkBorder, RectangleShape)
                                        .clickable { viewModel.setEyedropperActive(!isEyedropperActive) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    PixelDropperIcon(tint = if (isEyedropperActive) Color.Black else CyanAccent, size = 14.dp)
                                }

                                // Grid
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(if (isGridEnabled) CyanAccent else DarkBackground, RectangleShape)
                                        .border(1.dp, if (isGridEnabled) Color.White else DarkBorder, RectangleShape)
                                        .clickable { viewModel.toggleGrid() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    PixelGridIcon(tint = if (isGridEnabled) Color.Black else TextSecondary, size = 14.dp)
                                }

                                // Tune Filter
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(DarkBackground, RectangleShape)
                                        .border(1.dp, if (useBilinearFilter) Color(0xFFFFB300) else DarkBorder, RectangleShape)
                                        .clickable {
                                            viewModel.toggleBilinearFilter()
                                            val mode = if (!useBilinearFilter) "Bilineal (Suavizado)" else "Nearest Neighbor (100% Puro Hardware)"
                                            Toast.makeText(context, "Render: $mode", Toast.LENGTH_SHORT).show()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    PixelTuneIcon(tint = if (useBilinearFilter) Color(0xFFFFB300) else EmeraldAccent, size = 14.dp)
                                }

                                // Palette Analyzer
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(DarkBackground, RectangleShape)
                                        .border(1.dp, EmeraldAccent, RectangleShape)
                                        .clickable {
                                            viewModel.analyzeCurrentBitmap()
                                            showAnalyzerSheet = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    PixelPaletteIcon(tint = EmeraldAccent, size = 14.dp)
                                }

                                // Pixel Editor
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(DarkBackground, RectangleShape)
                                        .border(1.dp, Color(0xFFFFD600), RectangleShape)
                                        .clickable { showEditorDialog = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    PixelBrushIcon(tint = Color(0xFFFFD600), size = 14.dp)
                                }

                                // Screen vs Buffer Cotejo (Recuadro Rojo)
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(DarkBackground, RectangleShape)
                                        .border(1.dp, Color(0xFFFF1744), RectangleShape)
                                        .clickable {
                                            screenAnalyzerReferenceBuffer = when (activePreset) {
                                                com.example.model.PresetType.TWO_PIXEL_HARDWARE_TEST -> com.example.util.PixelEngine.createTwoPixelBitmap()
                                                com.example.model.PresetType.NYQUIST_CHECKERBOARD -> com.example.util.PixelEngine.createNyquistCheckerboard()
                                                com.example.model.PresetType.PALETTE_16_TEST -> com.example.util.PixelEngine.createPalette16Bitmap()
                                                com.example.model.PresetType.RETRO_PIXEL_ART -> com.example.util.PixelEngine.createRetroPixelArt()
                                                com.example.model.PresetType.CUSTOM_IMPORTED -> currentBitmap
                                            }
                                            showScreenAnalyzerDialog = true
                                        }
                                        .testTag("screen_vs_buffer_top_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    PixelMonitorIcon(tint = Color(0xFFFF1744), size = 14.dp)
                                }

                                // Overlay 2%
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(DarkBackground, RectangleShape)
                                        .border(1.dp, Color(0xFFFF4081), RectangleShape)
                                        .clickable {
                                            if (Settings.canDrawOverlays(context)) {
                                                startFloatingService()
                                            } else {
                                                showFloatingDialog = true
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    PixelLayersIcon(tint = Color(0xFFFF4081), size = 14.dp)
                                }
                            }
                        }
                    }

                    // Pure Pixel Scale Strip
                    val isRealHardwareScale = abs(currentScale - 1.0f) < 0.05f
                    val scaleText = if (isRealHardwareScale) "TAMANO REAL: 100% [1:1]" else String.format(Locale.US, "ESCALADO: %.2fx", currentScale)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pixelShadow(2.dp, 2.dp, Color.Black)
                            .background(DarkSurface, RectangleShape)
                            .border(1.dp, if (isRealHardwareScale) EmeraldAccent else CyanAccent, RectangleShape)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .testTag("scale_status_badge")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (isRealHardwareScale) EmeraldAccent else CyanAccent, RectangleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = scaleText,
                                    color = if (isRealHardwareScale) EmeraldAccent else CyanAccent,
                                    fontSize = 8.sp,
                                    fontFamily = SilkscreenFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // FIT
                                Box(
                                    modifier = Modifier
                                        .background(DarkBackground, RectangleShape)
                                        .border(1.dp, DarkBorder, RectangleShape)
                                        .clickable {
                                            // Compute exact fit ratio
                                            currentScale = 0.95f * minOf(1.0f, 1080f / maxOf(1, currentBitmap.width).toFloat())
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("FIT", color = Color.White, fontSize = 7.sp, fontFamily = PressStart2PFontFamily)
                                }

                                // 1:1
                                Box(
                                    modifier = Modifier
                                        .background(if (isRealHardwareScale) EmeraldAccent else DarkBackground, RectangleShape)
                                        .border(1.dp, if (isRealHardwareScale) Color.White else DarkBorder, RectangleShape)
                                        .clickable { currentScale = 1.0f }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("1:1", color = if (isRealHardwareScale) Color.Black else Color.White, fontSize = 7.sp, fontFamily = PressStart2PFontFamily)
                                }

                                // 8X
                                Box(
                                    modifier = Modifier
                                        .background(DarkBackground, RectangleShape)
                                        .border(1.dp, DarkBorder, RectangleShape)
                                        .clickable { currentScale = 8.0f }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("8X", color = Color.White, fontSize = 7.sp, fontFamily = PressStart2PFontFamily)
                                }

                                // 32X
                                Box(
                                    modifier = Modifier
                                        .background(DarkBackground, RectangleShape)
                                        .border(1.dp, DarkBorder, RectangleShape)
                                        .clickable { currentScale = 32.0f }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("32X", color = Color.White, fontSize = 7.sp, fontFamily = PressStart2PFontFamily)
                                }

                                // 128X
                                Box(
                                    modifier = Modifier
                                        .background(DarkBackground, RectangleShape)
                                        .border(1.dp, DarkBorder, RectangleShape)
                                        .clickable { currentScale = 128.0f }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("128X", color = Color.White, fontSize = 7.sp, fontFamily = PressStart2PFontFamily)
                                }

                                // 1000X Extreme Zoom
                                Box(
                                    modifier = Modifier
                                        .background(DarkBackground, RectangleShape)
                                        .border(1.dp, CyanAccent, RectangleShape)
                                        .clickable { currentScale = 1000.0f }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("1000X", color = CyanAccent, fontSize = 7.sp, fontFamily = PressStart2PFontFamily)
                                }

                                // VER COMPLETO (Clean Mode)
                                Box(
                                    modifier = Modifier
                                        .background(DarkBackground, RectangleShape)
                                        .border(1.dp, EmeraldAccent, RectangleShape)
                                        .clickable { isUIVisible = false }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("VER", color = EmeraldAccent, fontSize = 7.sp, fontFamily = PressStart2PFontFamily)
                                }
                            }
                        }
                    }
                }
            }

            // 2. Pure Photo Canvas Viewport - Occupies 100% of space between top bar and bottom bar!
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                PixelCanvasViewer(
                    bitmap = currentBitmap,
                    isolatedBitmap = isolatedBitmap,
                    isEyedropperActive = isEyedropperActive,
                    isGridEnabled = isGridEnabled,
                    useBilinearFilter = useBilinearFilter,
                    currentScale = currentScale,
                    onScaleChanged = { newScale -> currentScale = newScale },
                    onToggleUIVisibility = { isUIVisible = !isUIVisible },
                    onPixelSelected = { color, x, y ->
                        viewModel.onPixelSelected(color, x, y)
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Active Color Isolation Banner (Tiny pill in canvas)
                if (isolatedColor != null && isUIVisible) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                            .pixelShadow(2.dp, 2.dp, Color.Black)
                            .background(DarkSurface, RectangleShape)
                            .border(1.dp, CyanAccent, RectangleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("color_isolation_banner")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(isolatedColor!!.toComposeColor(), RectangleShape)
                                    .border(1.dp, Color.White, RectangleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AISLANDO: ${isolatedColor!!.hex8Rgba}",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontFamily = SilkscreenFontFamily
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clickable { viewModel.clearIsolation() }
                                    .padding(2.dp)
                            ) {
                                PixelCloseIcon(tint = Color.White, size = 8.dp)
                            }
                        }
                    }
                }

                // Restore UI Pill when in clean/immersive mode
                if (!isUIVisible) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .pixelShadow(2.dp, 2.dp, Color.Black)
                            .background(DarkSurface, RectangleShape)
                            .border(1.dp, CyanAccent, RectangleShape)
                            .clickable { isUIVisible = true }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "[+UI]",
                            color = CyanAccent,
                            fontSize = 8.sp,
                            fontFamily = PressStart2PFontFamily
                        )
                    }
                }
            }

            // 3. Collapsible Bottom Controls (Positioned BELOW the photo - NEVER covers the image)
            AnimatedVisibility(
                visible = isUIVisible && isEyedropperActive,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
                modifier = Modifier.fillMaxWidth()
            ) {
                SurgicalEyedropperPanel(
                    selectedColor = selectedPixelColor,
                    pixelX = if (selectedPixelX >= 0) selectedPixelX else 0,
                    pixelY = if (selectedPixelY >= 0) selectedPixelY else 0,
                    bitmapWidth = currentBitmap.width,
                    bitmapHeight = currentBitmap.height,
                    onJumpToPixel = { x, y ->
                        val safeX = x.coerceIn(0, currentBitmap.width - 1)
                        val safeY = y.coerceIn(0, currentBitmap.height - 1)
                        val argb = currentBitmap.getPixel(safeX, safeY)
                        viewModel.onPixelSelected(PixelColor(argb), safeX, safeY)
                    },
                    onClose = { viewModel.setEyedropperActive(false) },
                    onIsolateColor = { color ->
                        viewModel.isolateColor(color)
                    },
                    onOpenColorReplacer = { color ->
                        showEditorDialog = true
                    },
                    onOpenRealWorldCalibration = { color ->
                        calibrationColorTarget = color
                        showCalibrationDialog = true
                    }
                )
            }

            AnimatedVisibility(
                visible = isUIVisible && !isEyedropperActive,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
                modifier = Modifier.fillMaxWidth()
            ) {
                DeviceGalleryBottomBar(
                    photos = devicePhotos,
                    selectedItem = selectedMediaItem,
                    hasPermission = hasMediaPermission,
                    onSelectPhoto = { photo ->
                        viewModel.selectDevicePhoto(context, photo)
                    },
                    onRequestPermission = {
                        requestMediaPermissionLauncher.launch(mediaPermissions)
                    },
                    onRefresh = {
                        viewModel.scanDevicePhotos(context, autoSelectLatest = false)
                    },
                    onOpenPresetGallery = {
                        showGallerySheet = true
                    },
                    onOpenCamera = {
                        showCameraScreen = true
                    },
                    onOpenFullGallery = {
                        if (checkMediaAccess()) {
                            viewModel.scanDevicePhotos(context, autoSelectLatest = false)
                            showFullDeviceGallery = true
                        } else {
                            requestMediaPermissionLauncher.launch(mediaPermissions)
                        }
                    }
                )
            }
        }
    }


    if (showFullDeviceGallery) {
        Dialog(
            onDismissRequest = { showFullDeviceGallery = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            FullDeviceGalleryDialog(
                photos = devicePhotos,
                selectedItem = selectedMediaItem,
                onSelectPhoto = { photo ->
                    viewModel.selectDevicePhoto(context, photo)
                    showFullDeviceGallery = false
                },
                onRefresh = {
                    viewModel.scanDevicePhotos(context, autoSelectLatest = false)
                },
                onClose = { showFullDeviceGallery = false }
            )
        }
    }

    // Full-Screen Live Pixel Camera Viewfinder
    if (showCameraScreen) {
        com.example.ui.camera.PixelCameraScreen(
            onPhotoCaptured = { bitmap ->
                viewModel.updateBitmap(bitmap)
                viewModel.scanDevicePhotos(context, autoSelectLatest = true)
                showCameraScreen = false
                if (returnToScreenAnalyzerAfterCapture) {
                    returnToScreenAnalyzerAfterCapture = false
                    showScreenAnalyzerDialog = true
                }
            },
            onClose = {
                showCameraScreen = false
                if (returnToScreenAnalyzerAfterCapture) {
                    returnToScreenAnalyzerAfterCapture = false
                    showScreenAnalyzerDialog = true
                }
            }
        )
    }

    // Modal Sheet: Color Analyzer & Histogram
    if (showAnalyzerSheet) {
        ColorAnalyzerSheet(
            analysis = analysisResult,
            isLoading = isAnalyzing,
            onDismiss = { showAnalyzerSheet = false },
            onIsolateColor = { color ->
                viewModel.isolateColor(color)
            }
        )
    }

    // Dialog: Integrity Check Report
    if (integrityReport != null) {
        IntegrityCheckDialog(
            report = integrityReport!!,
            onDismiss = { viewModel.dismissIntegrityReport() },
            onSaveScreenshot = {
                viewModel.saveVerificationScreenshot(context)
            }
        )
    }

    // Sheet: Pixel-Level Editor
    if (showEditorDialog) {
        PixelEditorDialog(
            initialBitmap = currentBitmap,
            initialSelectedColor = selectedPixelColor,
            onSaveBitmap = { updatedBitmap ->
                viewModel.saveEditedBitmapToGallery(context, updatedBitmap)
            },
            onDismiss = { showEditorDialog = false }
        )
    }

    // Sheet: Gallery & Hardware Test Presets
    if (showGallerySheet) {
        GalleryPresetsSheet(
            activePresetType = activePreset,
            onSelectPreset = { presetType ->
                viewModel.selectPreset(presetType)
            },
            onImportImage = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onDismiss = { showGallerySheet = false }
        )
    }

    // Dialog: Floating Overlay Mode Permissions & Launcher
    if (showFloatingDialog) {
        FloatingOverlayPromptDialog(
            hasOverlayPermission = Settings.canDrawOverlays(context),
            onStartService = {
                startFloatingService()
            },
            onRequestPermission = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                overlayPermissionLauncher.launch(intent)
            },
            onDismiss = { showFloatingDialog = false }
        )
    }

    // Modal Sheet: Real-World Color Truth Calibration (2^32 RGBA / Epistemología)
    if (showCalibrationDialog && calibrationColorTarget != null) {
        com.example.ui.components.RealWorldColorCalibrationDialog(
            detectedColor = calibrationColorTarget!!,
            onCalibrateColorSample = { calibratedColor ->
                calibrationColorTarget = calibratedColor
                viewModel.onPixelSelected(
                    calibratedColor,
                    if (selectedPixelX >= 0) selectedPixelX else 0,
                    if (selectedPixelY >= 0) selectedPixelY else 0
                )
                Toast.makeText(context, "Muestra calibrada a la verdad física (${calibratedColor.hex8Rgba})", Toast.LENGTH_SHORT).show()
            },
            onCalibrateGlobalSensor = { deltaR, deltaG, deltaB, deltaA ->
                viewModel.calibrateGlobalSensorOffset(context, deltaR, deltaG, deltaB, deltaA)
            },
            onDismiss = { showCalibrationDialog = false }
        )
    }

    // Dialog: Screen vs 32-bit Buffer Cotejo (Recuadro Rojo Pantalla PC)
    if (showScreenAnalyzerDialog) {
        val refBuffer = screenAnalyzerReferenceBuffer ?: when (activePreset) {
            com.example.model.PresetType.TWO_PIXEL_HARDWARE_TEST -> com.example.util.PixelEngine.createTwoPixelBitmap()
            com.example.model.PresetType.NYQUIST_CHECKERBOARD -> com.example.util.PixelEngine.createNyquistCheckerboard()
            com.example.model.PresetType.PALETTE_16_TEST -> com.example.util.PixelEngine.createPalette16Bitmap()
            com.example.model.PresetType.RETRO_PIXEL_ART -> com.example.util.PixelEngine.createRetroPixelArt()
            com.example.model.PresetType.CUSTOM_IMPORTED -> currentBitmap
        }

        ScreenVsBufferAnalyzerDialog(
            cameraPhoto = currentBitmap,
            referenceBuffer = refBuffer,
            initialNormalizedRect = screenAnalyzerNormalizedRect,
            onSaveReconstructed = { reconstructed ->
                viewModel.saveEditedBitmapToGallery(context, reconstructed)
            },
            onLoadToCanvas = { reconstructed ->
                viewModel.updateBitmap(reconstructed)
            },
            onOpenCameraForImagen2 = { selectedReference, selectedRect ->
                // Guardar las elecciones del diálogo (incluida Imagen 1 cargada desde galería)
                // antes de cerrarlo para abrir la cámara.
                screenAnalyzerReferenceBuffer = selectedReference
                screenAnalyzerNormalizedRect = selectedRect
                returnToScreenAnalyzerAfterCapture = true
                showScreenAnalyzerDialog = false
                showCameraScreen = true
            },
            onDismiss = {
                showScreenAnalyzerDialog = false
                screenAnalyzerReferenceBuffer = null
                screenAnalyzerNormalizedRect = com.example.model.NormalizedRect(
                    left = 0.12f, top = 0.15f, right = 0.88f, bottom = 0.85f
                )
                returnToScreenAnalyzerAfterCapture = false
            }
        )
    }
}


