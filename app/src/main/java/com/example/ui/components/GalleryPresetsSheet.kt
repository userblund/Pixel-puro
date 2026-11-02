package com.example.ui.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PresetType
import com.example.model.SamplePreset
import com.example.ui.pixel.PixelButton
import com.example.ui.pixel.PixelCloseIcon
import com.example.ui.pixel.PixelImageIcon
import com.example.ui.pixel.pixelBevel
import com.example.ui.pixel.pixelShadow
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.PressStart2PFontFamily
import com.example.ui.theme.SilkscreenFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryPresetsSheet(
    activePresetType: PresetType,
    onSelectPreset: (PresetType) -> Unit,
    onImportImage: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val presets = listOf(
        SamplePreset(
            id = "two_pixel",
            name = "TEST 2 PIXELES HARDWARE [2x1]",
            description = "Exactamente 2 pixeles (Rojo #FF0000 y Azul #0000FF). Usa estrictamente 2 pixeles de hardware.",
            type = PresetType.TWO_PIXEL_HARDWARE_TEST
        ),
        SamplePreset(
            id = "nyquist",
            name = "TABLERO NYQUIST 1-PIXEL [16x16]",
            description = "Ajedrez alternado 1px blanco y 1px negro. Calibracion de hardware sin blur.",
            type = PresetType.NYQUIST_CHECKERBOARD
        ),
        SamplePreset(
            id = "palette_16",
            name = "PALETA 16 COLORES [32x32]",
            description = "Colores indexados exactos sin compresion.",
            type = PresetType.PALETTE_16_TEST
        ),
        SamplePreset(
            id = "retro_art",
            name = "PIXEL ART ESPADA HD [32x32]",
            description = "Ilustracion con gemas, brillos RGB y transparencias.",
            type = PresetType.RETRO_PIXEL_ART
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkBackground,
        shape = RectangleShape,
        dragHandle = null,
        modifier = modifier.testTag("gallery_presets_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBackground, RectangleShape)
                .border(2.dp, CyanAccent, RectangleShape)
                .padding(12.dp)
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
                    PixelImageIcon(tint = CyanAccent, size = 16.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PATRONES Y ARCHIVOS",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontFamily = PressStart2PFontFamily,
                        fontWeight = FontWeight.Bold
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
                    PixelCloseIcon(tint = Color.White, size = 10.dp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action: Import custom file
            PixelButton(
                onClick = {
                    onImportImage()
                    onDismiss()
                },
                backgroundColor = CyanAccent,
                textColor = Color.Black,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "SELECCIONAR FOTO MANUAL",
                    color = Color.Black,
                    fontSize = 8.sp,
                    fontFamily = SilkscreenFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "PATRONES DE PRUEBA 1:1:",
                color = TextSecondary,
                fontSize = 8.sp,
                fontFamily = SilkscreenFontFamily,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(presets) { preset ->
                    val isSelected = activePresetType == preset.type
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pixelShadow(2.dp, 2.dp, Color.Black)
                            .background(if (isSelected) DarkSurface else DarkBackground, RectangleShape)
                            .border(1.dp, if (isSelected) CyanAccent else DarkBorder, RectangleShape)
                            .clickable {
                                onSelectPreset(preset.type)
                                onDismiss()
                            }
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = preset.name,
                                color = if (isSelected) CyanAccent else Color.White,
                                fontSize = 8.sp,
                                fontFamily = PressStart2PFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = preset.description,
                                color = TextMuted,
                                fontSize = 8.sp,
                                fontFamily = SilkscreenFontFamily
                            )
                        }
                    }
                }
            }
        }
    }
}
