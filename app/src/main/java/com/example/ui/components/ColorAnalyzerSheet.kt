package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ImageAnalysisResult
import com.example.model.PixelColor
import com.example.ui.pixel.PixelButton
import com.example.ui.pixel.PixelCloseIcon
import com.example.ui.pixel.PixelPaletteIcon
import com.example.ui.pixel.pixelBevel
import com.example.ui.pixel.pixelShadow
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.PressStart2PFontFamily
import com.example.ui.theme.SilkscreenFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorAnalyzerSheet(
    analysis: ImageAnalysisResult?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onIsolateColor: (PixelColor) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copiado", Toast.LENGTH_SHORT).show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkBackground,
        shape = RectangleShape,
        dragHandle = null,
        modifier = modifier.testTag("color_analyzer_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
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
                    PixelPaletteIcon(tint = CyanAccent, size = 16.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "INFORME PIXEL",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontFamily = PressStart2PFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ANALISIS DE PALETA Y CANALES",
                            color = TextSecondary,
                            fontSize = 7.sp,
                            fontFamily = SilkscreenFontFamily
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(DarkSurface, RectangleShape)
                        .border(1.dp, DarkBorder, RectangleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    PixelCloseIcon(tint = Color.White, size = 10.dp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (isLoading) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Text(
                        text = "EXAMINANDO PIXEL A PIXEL...",
                        color = CyanAccent,
                        fontSize = 9.sp,
                        fontFamily = PressStart2PFontFamily
                    )
                }
            } else if (analysis != null) {
                // Key Stats Cards Grid
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PixelStatBox(
                        title = "DIMENSION",
                        value = "${analysis.width}x${analysis.height}",
                        subtitle = "${analysis.totalPixels} PX",
                        accentColor = CyanAccent,
                        modifier = Modifier.weight(1f)
                    )
                    PixelStatBox(
                        title = "COLORES",
                        value = "${analysis.uniqueColorsCount}",
                        subtitle = if (analysis.uniqueColorsCount <= 256) "INDEXADO" else "COLOR REAL",
                        accentColor = EmeraldAccent,
                        modifier = Modifier.weight(1f)
                    )
                    PixelStatBox(
                        title = "ALFA",
                        value = if (analysis.hasAlpha) "ACTIVO" else "OPACO",
                        subtitle = if (analysis.hasAlpha) "${analysis.transparentPixelsCount} VACIOS" else "100% SOLIDO",
                        accentColor = AmberAccent,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurface,
                    contentColor = CyanAccent,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = CyanAccent
                        )
                    },
                    modifier = Modifier.fillMaxWidth().border(1.dp, DarkBorder, RectangleShape)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "COLORES [${analysis.topColors.size}]",
                                fontSize = 8.sp,
                                fontFamily = SilkscreenFontFamily,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "HISTOGRAMAS",
                                fontSize = 8.sp,
                                fontFamily = SilkscreenFontFamily,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                "INTEGRIDAD",
                                fontSize = 8.sp,
                                fontFamily = SilkscreenFontFamily,
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                when (selectedTab) {
                    0 -> {
                        // Palette List with Search
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text("FILTRAR HEX...", fontSize = 8.sp, fontFamily = SilkscreenFontFamily)
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
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )

                            val filteredColors = remember(analysis.topColors, searchQuery) {
                                if (searchQuery.isBlank()) analysis.topColors
                                else analysis.topColors.filter {
                                    it.hex8Rgba.contains(searchQuery, ignoreCase = true) ||
                                            it.hex8Argb.contains(searchQuery, ignoreCase = true)
                                }
                            }

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredColors) { color ->
                                    PixelColorRow(
                                        color = color,
                                        onIsolate = {
                                            onIsolateColor(color)
                                            onDismiss()
                                        },
                                        onCopy = { copyToClipboard("Color ${color.hex8Rgba}", color.hex8Rgba) }
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        // Histograms Tab
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            item {
                                PixelHistogramCard(title = "CANAL ROJO [R]", bins = analysis.redHistogram, tint = Color(0xFFFF4081))
                            }
                            item {
                                PixelHistogramCard(title = "CANAL VERDE [G]", bins = analysis.greenHistogram, tint = EmeraldAccent)
                            }
                            item {
                                PixelHistogramCard(title = "CANAL AZUL [B]", bins = analysis.blueHistogram, tint = CyanAccent)
                            }
                            item {
                                PixelHistogramCard(title = "LUMINANCIA [Y]", bins = analysis.lumaHistogram, tint = AmberAccent)
                            }
                        }
                    }

                    2 -> {
                        // Integrity / Metadata Tab
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pixelShadow(2.dp, 2.dp, Color.Black)
                                    .background(DarkSurface, RectangleShape)
                                    .border(1.dp, DarkBorder, RectangleShape)
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "HUELLA SHA-256:",
                                        color = CyanAccent,
                                        fontSize = 8.sp,
                                        fontFamily = PressStart2PFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = analysis.sha256Hash,
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontFamily = SilkscreenFontFamily,
                                        modifier = Modifier
                                            .background(DarkBackground, RectangleShape)
                                            .border(1.dp, DarkBorder, RectangleShape)
                                            .padding(6.dp)
                                            .clickable { copyToClipboard("SHA-256", analysis.sha256Hash) }
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "MEMORIA SIN COMPRIMIR:",
                                        color = TextSecondary,
                                        fontSize = 7.sp,
                                        fontFamily = SilkscreenFontFamily
                                    )
                                    val sizeMb = analysis.uncompressedSizeBytes / (1024f * 1024f)
                                    Text(
                                        text = String.format(Locale.US, "%.2f MB (%d BYTES)", sizeMb, analysis.uncompressedSizeBytes),
                                        color = EmeraldAccent,
                                        fontSize = 9.sp,
                                        fontFamily = SilkscreenFontFamily,
                                        fontWeight = FontWeight.Bold
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

@Composable
private fun PixelStatBox(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .pixelShadow(2.dp, 2.dp, Color.Black)
            .background(DarkSurface, RectangleShape)
            .border(1.dp, DarkBorder, RectangleShape)
            .padding(6.dp)
    ) {
        Column {
            Text(
                text = title,
                color = TextSecondary,
                fontSize = 7.sp,
                fontFamily = SilkscreenFontFamily,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = accentColor,
                fontSize = 10.sp,
                fontFamily = SilkscreenFontFamily,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 7.sp,
                fontFamily = SilkscreenFontFamily
            )
        }
    }
}

@Composable
private fun PixelColorRow(
    color: PixelColor,
    onIsolate: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface, RectangleShape)
            .border(1.dp, DarkBorder, RectangleShape)
            .padding(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Square Pixel Swatch
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(color.toComposeColor(), RectangleShape)
                    .border(1.dp, Color.White, RectangleShape)
                    .clickable { onCopy() }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = color.hex8Rgba,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontFamily = SilkscreenFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f%%", color.percentage),
                        color = CyanAccent,
                        fontSize = 8.sp,
                        fontFamily = SilkscreenFontFamily
                    )
                }

                Text(
                    text = "RGB(${color.r},${color.g},${color.b},${color.a}) | ${color.count} PX",
                    color = EmeraldAccent,
                    fontSize = 7.sp,
                    fontFamily = SilkscreenFontFamily
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .background(DarkBackground, RectangleShape)
                    .border(1.dp, CyanAccent, RectangleShape)
                    .clickable { onIsolate() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text("AISLAR", color = CyanAccent, fontSize = 7.sp, fontFamily = SilkscreenFontFamily)
            }
        }
    }
}

@Composable
private fun PixelHistogramCard(
    title: String,
    bins: IntArray,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val maxVal = remember(bins) { (bins.maxOrNull() ?: 1).coerceAtLeast(1) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pixelShadow(2.dp, 2.dp, Color.Black)
            .background(DarkSurface, RectangleShape)
            .border(1.dp, DarkBorder, RectangleShape)
            .padding(8.dp)
    ) {
        Column {
            Text(
                text = title,
                color = tint,
                fontSize = 8.sp,
                fontFamily = SilkscreenFontFamily,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(DarkBackground, RectangleShape)
                    .border(1f.dp, DarkBorder, RectangleShape)
            ) {
                val step = size.width / 256f
                for (i in 0 until 256) {
                    val count = bins[i]
                    val barHeight = (count.toFloat() / maxVal.toFloat()) * size.height
                    drawRect(
                        color = tint,
                        topLeft = Offset(i * step, size.height - barHeight),
                        size = Size(step.coerceAtLeast(1f), barHeight)
                    )
                }
            }
        }
    }
}
