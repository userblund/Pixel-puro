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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.pixel.PixelButton
import com.example.ui.pixel.PixelCloseIcon
import com.example.ui.pixel.PixelLayersIcon
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
fun FloatingOverlayPromptDialog(
    hasOverlayPermission: Boolean,
    onStartService: () -> Unit,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .pixelShadow(4.dp, 4.dp, Color.Black)
                .background(DarkSurface, RectangleShape)
                .border(2.dp, CyanAccent, RectangleShape)
                .pixelBevel(Color.White.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.8f), 2.dp)
                .padding(14.dp)
                .testTag("floating_overlay_prompt_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PixelLayersIcon(tint = CyanAccent, size = 16.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MODO FLOTANTE 2%",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontFamily = PressStart2PFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(DarkBackground, RectangleShape)
                            .border(1.dp, DarkBorder, RectangleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        PixelCloseIcon(tint = Color.White, size = 8.dp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground, RectangleShape)
                        .border(1.dp, DarkBorder, RectangleShape)
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "MUESTREADOR EN PANTALLA",
                            color = EmeraldAccent,
                            fontSize = 8.sp,
                            fontFamily = PressStart2PFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "La galeria se oculta 98%, dejando solo un boton flotante del 2% y la lupa quirurgica offset para capturar pixeles y colores sobre cualquier otra app.",
                            color = TextSecondary,
                            fontSize = 8.sp,
                            fontFamily = SilkscreenFontFamily
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!hasOverlayPermission) {
                    Text(
                        text = "[!] REQUIERE PERMISO DE SUPERPOSICION",
                        color = Color(0xFFFFB300),
                        fontSize = 8.sp,
                        fontFamily = SilkscreenFontFamily,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PixelButton(
                        onClick = onRequestPermission,
                        backgroundColor = CyanAccent,
                        textColor = Color.Black,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "CONCEDER PERMISO",
                            color = Color.Black,
                            fontSize = 8.sp,
                            fontFamily = SilkscreenFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    PixelButton(
                        onClick = {
                            onStartService()
                            onDismiss()
                        },
                        backgroundColor = EmeraldAccent,
                        textColor = Color.Black,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "INICIAR MODO FLOTANTE [2%]",
                            color = Color.Black,
                            fontSize = 8.sp,
                            fontFamily = SilkscreenFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                PixelButton(
                    onClick = onDismiss,
                    backgroundColor = DarkBackground,
                    textColor = Color.White,
                    borderColor = DarkBorder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CANCELAR", color = Color.White, fontSize = 8.sp, fontFamily = SilkscreenFontFamily)
                }
            }
        }
    }
}
