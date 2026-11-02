package com.example.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PixelColor
import com.example.ui.pixel.PixelButton
import com.example.ui.pixel.PixelCloseIcon
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
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * RealWorldColorCalibrationDialog
 *
 * Implements the user's ground-truth epistemology:
 * 1. "Una computadora no puede darte certeza si no tiene la información necesaria."
 * 2. "Si Pinocho está convencido de que algo es cierto, aunque sea falso, su nariz no reacciona."
 * 3. "Lo representado puede tener infinitas representaciones posibles sin dejar de ser exactamente lo mismo."
 * 4. "Si Pinocho falla, sea únicamente porque le falta información necesaria."
 *
 * When the phone camera captures a pixel, it might assert "this is the closest color to the real world"
 * even when physical reality is different. This dialog queries the user for ground-truth reality,
 * computes deterministic delta values over the full 2^32 RGBA color space without lossy heuristics,
 * and allows calibrating either the isolated color or the entire sensor offset across the image.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealWorldColorCalibrationDialog(
    detectedColor: PixelColor,
    onCalibrateColorSample: (PixelColor) -> Unit,
    onCalibrateGlobalSensor: (deltaR: Int, deltaG: Int, deltaB: Int, deltaA: Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val sensorR = detectedColor.r
    val sensorG = detectedColor.g
    val sensorB = detectedColor.b
    val sensorA = detectedColor.a

    var targetR by remember { mutableIntStateOf(sensorR) }
    var targetG by remember { mutableIntStateOf(sensorG) }
    var targetB by remember { mutableIntStateOf(sensorB) }
    var targetA by remember { mutableIntStateOf(sensorA) }

    var hexInputText by remember {
        mutableStateOf(String.format(Locale.US, "%02X%02X%02X%02X", sensorR, sensorG, sensorB, sensorA))
    }

    var showEpistemologyInfo by remember { mutableStateOf(false) }

    LaunchedEffect(targetR, targetG, targetB, targetA) {
        hexInputText = String.format(Locale.US, "%02X%02X%02X%02X", targetR, targetG, targetB, targetA)
    }

    val deltaR = targetR - sensorR
    val deltaG = targetG - sensorG
    val deltaB = targetB - sensorB
    val deltaA = targetA - sensorA

    val distance4D = sqrt(
        (deltaR * deltaR + deltaG * deltaG + deltaB * deltaB + deltaA * deltaA).toDouble()
    )

    val targetArgb = AndroidColor.argb(targetA, targetR, targetG, targetB)
    val targetComposeColor = Color(targetArgb)
    val sensorComposeColor = detectedColor.toComposeColor()

    fun parseHexInput(raw: String) {
        val clean = raw.trim().removePrefix("#").removePrefix("0x")
        when (clean.length) {
            8 -> runCatching {
                targetR = clean.substring(0, 2).toInt(16).coerceIn(0, 255)
                targetG = clean.substring(2, 4).toInt(16).coerceIn(0, 255)
                targetB = clean.substring(4, 6).toInt(16).coerceIn(0, 255)
                targetA = clean.substring(6, 8).toInt(16).coerceIn(0, 255)
            }
            6 -> runCatching {
                targetR = clean.substring(0, 2).toInt(16).coerceIn(0, 255)
                targetG = clean.substring(2, 4).toInt(16).coerceIn(0, 255)
                targetB = clean.substring(4, 6).toInt(16).coerceIn(0, 255)
                targetA = 255
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkBackground,
        shape = RectangleShape,
        dragHandle = null,
        modifier = modifier.testTag("real_world_calibration_dialog")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBackground, RectangleShape)
                .border(2.dp, CyanAccent, RectangleShape)
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "CALIBRACIÓN MUNDO REAL",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontFamily = PressStart2PFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ESPACIO DETERMINISTA 2^32 COLORES RGBA [SIN HEURÍSTICAS]",
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
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    PixelCloseIcon(tint = Color.White, size = 12.dp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Epistemological Principles Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RectangleShape)
                    .border(1.dp, if (showEpistemologyInfo) AmberAccent else DarkBorder, RectangleShape)
                    .clickable { showEpistemologyInfo = !showEpistemologyInfo }
                    .padding(8.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "📜 PRINCIPIOS DE CERTEZA ÓPTICA",
                            color = AmberAccent,
                            fontSize = 8.sp,
                            fontFamily = SilkscreenFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (showEpistemologyInfo) "[ CERRAR ]" else "[ VER AXIOMAS ]",
                            color = CyanAccent,
                            fontSize = 7.sp,
                            fontFamily = SilkscreenFontFamily
                        )
                    }

                    if (showEpistemologyInfo) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Una computadora no puede darte certeza si no tiene la información necesaria.\n" +
                                   "• Si Pinocho está convencido de que algo es cierto, aunque sea falso, su nariz no reacciona.\n" +
                                   "• Lo representado puede tener infinitas representaciones posibles sin dejar de ser exactamente lo mismo.\n" +
                                   "• Si Pinocho falla, sea únicamente porque le falta información necesaria.",
                            color = Color.White,
                            fontSize = 7.sp,
                            fontFamily = SilkscreenFontFamily,
                            lineHeight = 12.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "El sensor de la cámara entrega datos matemáticos brutos. Para alcanzar la certeza física, tú aportas la verdad del mundo real en el espacio completo de 2^32 colores RGBA.",
                            color = TextSecondary,
                            fontSize = 7.sp,
                            fontFamily = SilkscreenFontFamily,
                            lineHeight = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Core Question Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pixelShadow(2.dp, 2.dp, Color.Black)
                    .background(Color(0xFF102030), RectangleShape)
                    .border(1.dp, CyanAccent, RectangleShape)
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = "¿ES EXACTAMENTE ESTE COLOR EL MÁS CERCANO AL DEL MUNDO REAL?",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontFamily = PressStart2PFontFamily,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Si la respuesta es no, define abajo la verdad física (#RRGGBBAA) para calcular la desviación matemática exacta.",
                        color = TextSecondary,
                        fontSize = 7.sp,
                        fontFamily = SilkscreenFontFamily
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Side by Side Comparison: Sensor vs Ground Truth
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sensor Readout
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(DarkSurface, RectangleShape)
                        .border(1.dp, DarkBorder, RectangleShape)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SENSOR CÁMARA",
                        color = TextMuted,
                        fontSize = 7.sp,
                        fontFamily = SilkscreenFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(sensorComposeColor, RectangleShape)
                            .border(2.dp, Color.White, RectangleShape)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = detectedColor.hex8Rgba,
                        color = Color.White,
                        fontSize = 8.sp,
                        fontFamily = SilkscreenFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "RGBA($sensorR,$sensorG,$sensorB,$sensorA)",
                        color = TextSecondary,
                        fontSize = 7.sp,
                        fontFamily = SilkscreenFontFamily
                    )
                }

                // Real-World Truth Readout
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(DarkSurface, RectangleShape)
                        .border(1.dp, CyanAccent, RectangleShape)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "MUNDO REAL (TRUTH)",
                        color = CyanAccent,
                        fontSize = 7.sp,
                        fontFamily = SilkscreenFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(targetComposeColor, RectangleShape)
                            .border(2.dp, Color.White, RectangleShape)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "#${String.format(Locale.US, "%02X%02X%02X%02X", targetR, targetG, targetB, targetA)}",
                        color = CyanAccent,
                        fontSize = 8.sp,
                        fontFamily = SilkscreenFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "RGBA($targetR,$targetG,$targetB,$targetA)",
                        color = EmeraldAccent,
                        fontSize = 7.sp,
                        fontFamily = SilkscreenFontFamily
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mathematical Deviation Metrics (Deterministic 4D Vector)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RectangleShape)
                    .border(1.dp, EmeraldAccent, RectangleShape)
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = "DESVIACIÓN MATEMÁTICA PURA (2^32 COLORES RGBA):",
                        color = EmeraldAccent,
                        fontSize = 7.sp,
                        fontFamily = PressStart2PFontFamily
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ΔR: ${if (deltaR >= 0) "+$deltaR" else "$deltaR"}",
                            color = Color(0xFFFF5252),
                            fontSize = 8.sp,
                            fontFamily = SilkscreenFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ΔG: ${if (deltaG >= 0) "+$deltaG" else "$deltaG"}",
                            color = Color(0xFF69F0AE),
                            fontSize = 8.sp,
                            fontFamily = SilkscreenFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ΔB: ${if (deltaB >= 0) "+$deltaB" else "$deltaB"}",
                            color = Color(0xFF40C4FF),
                            fontSize = 8.sp,
                            fontFamily = SilkscreenFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ΔA: ${if (deltaA >= 0) "+$deltaA" else "$deltaA"}",
                            color = Color(0xFFFFD600),
                            fontSize = 8.sp,
                            fontFamily = SilkscreenFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "DISTANCIA EUCLIDIANA 4D: ${String.format(Locale.US, "%.2f", distance4D)} UNIDADES",
                        color = TextSecondary,
                        fontSize = 7.sp,
                        fontFamily = SilkscreenFontFamily
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Direct HEX8 Input
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = hexInputText,
                    onValueChange = { input ->
                        hexInputText = input.uppercase(Locale.US)
                        parseHexInput(input)
                    },
                    label = {
                        Text("INGRESAR COLOR FÍSICO (#RRGGBBAA)", fontSize = 7.sp, fontFamily = SilkscreenFontFamily)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = CyanAccent
                    ),
                    modifier = Modifier.weight(1f)
                )

                PixelButton(
                    onClick = {
                        // Reset to sensor value
                        targetR = sensorR
                        targetG = sensorG
                        targetB = sensorB
                        targetA = sensorA
                    },
                    backgroundColor = DarkSurface,
                    textColor = Color.White,
                    borderColor = DarkBorder,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("ES EXACTO", fontSize = 7.sp, fontFamily = SilkscreenFontFamily)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sliders for Fine-Tuning R, G, B, A
            CalibrationSlider(
                label = "R (ROJO FÍSICO)",
                value = targetR,
                accentColor = Color(0xFFFF5252),
                onValueChange = { targetR = it.coerceIn(0, 255) }
            )
            CalibrationSlider(
                label = "G (VERDE FÍSICO)",
                value = targetG,
                accentColor = Color(0xFF69F0AE),
                onValueChange = { targetG = it.coerceIn(0, 255) }
            )
            CalibrationSlider(
                label = "B (AZUL FÍSICO)",
                value = targetB,
                accentColor = Color(0xFF40C4FF),
                onValueChange = { targetB = it.coerceIn(0, 255) }
            )
            CalibrationSlider(
                label = "A (ALFA FÍSICO)",
                value = targetA,
                accentColor = Color(0xFFFFD600),
                onValueChange = { targetA = it.coerceIn(0, 255) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action 1: Calibrate sample only
            PixelButton(
                onClick = {
                    onCalibrateColorSample(PixelColor(targetArgb))
                    onDismiss()
                },
                backgroundColor = DarkSurface,
                textColor = CyanAccent,
                borderColor = CyanAccent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(
                    text = "CALIBRAR MUESTRA DE COLOR",
                    fontSize = 8.sp,
                    fontFamily = SilkscreenFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action 2: Calibrate entire sensor across full image
            PixelButton(
                onClick = {
                    onCalibrateGlobalSensor(deltaR, deltaG, deltaB, deltaA)
                    onDismiss()
                },
                backgroundColor = EmeraldAccent,
                textColor = Color.Black,
                borderColor = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🌐 CALIBRAR SENSOR GLOBAL (TODA LA FOTO)",
                        color = Color.Black,
                        fontSize = 8.sp,
                        fontFamily = SilkscreenFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "APLICA Δ($deltaR, $deltaG, $deltaB, $deltaA) SIN COMPRESIÓN PNG",
                        color = Color(0xFF1B5E20),
                        fontSize = 7.sp,
                        fontFamily = SilkscreenFontFamily
                    )
                }
            }
        }
    }
}

@Composable
private fun CalibrationSlider(
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

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
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
            modifier = Modifier.fillMaxWidth().height(22.dp)
        )
    }
}
