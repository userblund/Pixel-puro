package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PixelColor
import com.example.ui.pixel.PixelArrowDownIcon
import com.example.ui.pixel.PixelArrowLeftIcon
import com.example.ui.pixel.PixelArrowRightIcon
import com.example.ui.pixel.PixelArrowUpIcon
import com.example.ui.pixel.PixelButton
import com.example.ui.pixel.PixelCloseIcon
import com.example.ui.pixel.PixelContainer
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

@Composable
fun SurgicalEyedropperPanel(
    selectedColor: PixelColor?,
    pixelX: Int,
    pixelY: Int,
    bitmapWidth: Int,
    bitmapHeight: Int,
    onJumpToPixel: (Int, Int) -> Unit,
    onClose: () -> Unit,
    onIsolateColor: (PixelColor) -> Unit,
    onOpenColorReplacer: (PixelColor) -> Unit,
    onOpenRealWorldCalibration: ((PixelColor) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copiado: $text", Toast.LENGTH_SHORT).show()
    }

    val composeColor = selectedColor?.toComposeColor() ?: CyanAccent

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pixelShadow(0.dp, (-3).dp, Color.Black)
            .background(DarkSurface, RectangleShape)
            .border(2.dp, CyanAccent, RectangleShape)
            .pixelBevel(Color.White.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.8f), 2.dp)
            .padding(10.dp)
            .testTag("surgical_eyedropper_panel")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(composeColor, RectangleShape)
                            .border(1.dp, Color.White, RectangleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CUENTAGOTAS PIXEL",
                        color = CyanAccent,
                        fontSize = 10.sp,
                        fontFamily = PressStart2PFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(DarkBackground, RectangleShape)
                        .border(1.dp, DarkBorder, RectangleShape)
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    PixelCloseIcon(tint = Color.White, size = 10.dp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Color Readout Banner
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Large Square Pixel Swatch
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .pixelShadow(2.dp, 2.dp, Color.Black)
                        .background(composeColor, RectangleShape)
                        .border(2.dp, Color.White, RectangleShape)
                        .pixelBevel(Color.White.copy(alpha = 0.5f), Color.Black.copy(alpha = 0.7f), 2.dp)
                        .clickable {
                            selectedColor?.let { copyToClipboard("HEX8", it.hex8Rgba) }
                        }
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = selectedColor?.hex8Rgba ?: "#--------",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SilkscreenFontFamily
                        )
                        Box(
                            modifier = Modifier
                                .background(DarkBackground, RectangleShape)
                                .border(1.dp, DarkBorder, RectangleShape)
                                .clickable { selectedColor?.let { copyToClipboard("HEX8 RGBA", it.hex8Rgba) } }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("COPIAR", color = CyanAccent, fontSize = 7.sp, fontFamily = SilkscreenFontFamily)
                        }
                    }

                    Text(
                        text = "RGBA: ${selectedColor?.r ?: 0}, ${selectedColor?.g ?: 0}, ${selectedColor?.b ?: 0}, ${selectedColor?.a ?: 0}",
                        color = EmeraldAccent,
                        fontSize = 8.sp,
                        fontFamily = SilkscreenFontFamily
                    )

                    Text(
                        text = "PIXEL [$pixelX, $pixelY] DE [$bitmapWidth x $bitmapHeight]",
                        color = TextSecondary,
                        fontSize = 8.sp,
                        fontFamily = SilkscreenFontFamily
                    )
                }

                // Precision 1px Step D-Pad with Pixel Icons
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(DarkBackground, RectangleShape)
                        .border(1.dp, DarkBorder, RectangleShape)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(DarkSurface, RectangleShape)
                            .border(1.dp, DarkBorder, RectangleShape)
                            .clickable {
                                val newY = (pixelY - 1).coerceIn(0, bitmapHeight - 1)
                                onJumpToPixel(pixelX, newY)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        PixelArrowUpIcon(tint = Color.White, size = 10.dp)
                    }
                    Row {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(DarkSurface, RectangleShape)
                                .border(1.dp, DarkBorder, RectangleShape)
                                .clickable {
                                    val newX = (pixelX - 1).coerceIn(0, bitmapWidth - 1)
                                    onJumpToPixel(newX, pixelY)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            PixelArrowLeftIcon(tint = Color.White, size = 10.dp)
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(DarkSurface, RectangleShape)
                                .border(1.dp, DarkBorder, RectangleShape)
                                .clickable {
                                    val newX = (pixelX + 1).coerceIn(0, bitmapWidth - 1)
                                    onJumpToPixel(newX, pixelY)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            PixelArrowRightIcon(tint = Color.White, size = 10.dp)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(DarkSurface, RectangleShape)
                            .border(1.dp, DarkBorder, RectangleShape)
                            .clickable {
                                val newY = (pixelY + 1).coerceIn(0, bitmapHeight - 1)
                                onJumpToPixel(pixelX, newY)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        PixelArrowDownIcon(tint = Color.White, size = 10.dp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Corner Jump Buttons
            Text(
                text = "ESQUINAS DE HARDWARE:",
                color = TextSecondary,
                fontSize = 7.sp,
                fontFamily = SilkscreenFontFamily,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            val maxW = (bitmapWidth - 1).coerceAtLeast(0)
            val maxH = (bitmapHeight - 1).coerceAtLeast(0)

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    val isHere = pixelX == 0 && pixelY == 0
                    Box(
                        modifier = Modifier
                            .background(if (isHere) CyanAccent else DarkBackground, RectangleShape)
                            .border(1.dp, if (isHere) Color.White else DarkBorder, RectangleShape)
                            .clickable { onJumpToPixel(0, 0) }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "[0,0] SUP-IZQ",
                            color = if (isHere) Color.Black else Color.White,
                            fontSize = 7.sp,
                            fontFamily = SilkscreenFontFamily
                        )
                    }
                }
                item {
                    val isHere = pixelX == maxW && pixelY == 0
                    Box(
                        modifier = Modifier
                            .background(if (isHere) CyanAccent else DarkBackground, RectangleShape)
                            .border(1.dp, if (isHere) Color.White else DarkBorder, RectangleShape)
                            .clickable { onJumpToPixel(maxW, 0) }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "[$maxW,0] SUP-DER",
                            color = if (isHere) Color.Black else Color.White,
                            fontSize = 7.sp,
                            fontFamily = SilkscreenFontFamily
                        )
                    }
                }
                item {
                    val isHere = pixelX == 0 && pixelY == maxH
                    Box(
                        modifier = Modifier
                            .background(if (isHere) CyanAccent else DarkBackground, RectangleShape)
                            .border(1.dp, if (isHere) Color.White else DarkBorder, RectangleShape)
                            .clickable { onJumpToPixel(0, maxH) }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "[0,$maxH] INF-IZQ",
                            color = if (isHere) Color.Black else Color.White,
                            fontSize = 7.sp,
                            fontFamily = SilkscreenFontFamily
                        )
                    }
                }
                item {
                    val isHere = pixelX == maxW && pixelY == maxH
                    Box(
                        modifier = Modifier
                            .background(if (isHere) CyanAccent else DarkBackground, RectangleShape)
                            .border(1.dp, if (isHere) Color.White else DarkBorder, RectangleShape)
                            .clickable { onJumpToPixel(maxW, maxH) }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "[$maxW,$maxH] INF-DER",
                            color = if (isHere) Color.Black else Color.White,
                            fontSize = 7.sp,
                            fontFamily = SilkscreenFontFamily
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Actions row: Pure Pixel Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PixelButton(
                    onClick = { selectedColor?.let { onIsolateColor(it) } },
                    backgroundColor = DarkBackground,
                    textColor = Color.White,
                    borderColor = CyanAccent,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "AISLAR COLOR",
                        fontSize = 8.sp,
                        fontFamily = SilkscreenFontFamily,
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold
                    )
                }

                PixelButton(
                    onClick = { selectedColor?.let { onOpenColorReplacer(it) } },
                    backgroundColor = EmeraldAccent,
                    textColor = Color.Black,
                    borderColor = Color.White,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "REEMPLAZAR",
                        fontSize = 8.sp,
                        fontFamily = SilkscreenFontFamily,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            PixelButton(
                onClick = { selectedColor?.let { onOpenRealWorldCalibration?.invoke(it) } },
                backgroundColor = Color(0xFF102030),
                textColor = CyanAccent,
                borderColor = CyanAccent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🎯 ¿ES EL MÁS CERCANO AL MUNDO REAL?",
                        fontSize = 7.sp,
                        fontFamily = SilkscreenFontFamily,
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "[CALIBRAR 2^32 RGBA]",
                        fontSize = 6.sp,
                        fontFamily = PressStart2PFontFamily,
                        color = EmeraldAccent
                    )
                }
            }
        }
    }
}
