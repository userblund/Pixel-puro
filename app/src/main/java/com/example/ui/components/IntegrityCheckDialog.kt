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
import androidx.compose.ui.window.Dialog
import com.example.model.IntegrityReport
import com.example.ui.pixel.PixelButton
import com.example.ui.pixel.PixelCameraIcon
import com.example.ui.pixel.PixelCloseIcon
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IntegrityCheckDialog(
    report: IntegrityReport,
    onDismiss: () -> Unit,
    onSaveScreenshot: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copiado", Toast.LENGTH_SHORT).show()
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .pixelShadow(4.dp, 4.dp, Color.Black)
                .background(DarkSurface, RectangleShape)
                .border(2.dp, if (report.isBitPerfect) EmeraldAccent else CyanAccent, RectangleShape)
                .pixelBevel(Color.White.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.8f), 2.dp)
                .padding(14.dp)
                .testTag("integrity_check_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PixelCameraIcon(tint = if (report.isBitPerfect) EmeraldAccent else CyanAccent, size = 16.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "INTEGRIDAD HARDWARE",
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

                // Status Banner (Pure Pixel Box)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (report.isBitPerfect) EmeraldAccent.copy(alpha = 0.15f) else CyanAccent.copy(alpha = 0.15f), RectangleShape)
                        .border(1.dp, if (report.isBitPerfect) EmeraldAccent else CyanAccent, RectangleShape)
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = if (report.isBitPerfect) "[x] 0 PIXELES ALTERADOS" else "[!] ${report.alteredPixels} MODIFICADOS",
                            color = if (report.isBitPerfect) EmeraldAccent else CyanAccent,
                            fontSize = 9.sp,
                            fontFamily = PressStart2PFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.US, "FIDELIDAD HARDWARE: %.3f%%", report.integrityPercentage),
                            color = Color.White,
                            fontSize = 8.sp,
                            fontFamily = SilkscreenFontFamily
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Detail verification stats
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DetailRow(label = "PIXELES TESTEADOS:", value = "${report.totalTestedPixels} PX", valueColor = Color.White)
                    DetailRow(label = "IDENTICOS [1:1]:", value = "${report.identicalPixels} PX", valueColor = EmeraldAccent)
                    DetailRow(label = "ALTERADOS:", value = "${report.alteredPixels} PX", valueColor = if (report.alteredPixels == 0) EmeraldAccent else Color(0xFFFF5252))
                    DetailRow(label = "TUBERIA:", value = "NEAREST NEIGHBOR", valueColor = CyanAccent)
                    DetailRow(
                        label = "HORA:",
                        value = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(report.timestamp)),
                        valueColor = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Hash verification box
                Text(
                    text = "SHA-256 HASH ORIGINAL VS RENDER:",
                    color = TextSecondary,
                    fontSize = 7.sp,
                    fontFamily = SilkscreenFontFamily,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground, RectangleShape)
                        .border(1.dp, DarkBorder, RectangleShape)
                        .padding(6.dp)
                        .clickable { copyToClipboard("Hash SHA-256", report.originalSha256) }
                ) {
                    Column {
                        Text(
                            text = "ORIG: ${report.originalSha256.take(20)}...",
                            color = EmeraldAccent,
                            fontSize = 7.sp,
                            fontFamily = SilkscreenFontFamily
                        )
                        Text(
                            text = "REND: ${report.screenCaptureSha256.take(20)}...",
                            color = if (report.originalSha256 == report.screenCaptureSha256) EmeraldAccent else CyanAccent,
                            fontSize = 7.sp,
                            fontFamily = SilkscreenFontFamily
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PixelButton(
                        onClick = onDismiss,
                        backgroundColor = DarkBackground,
                        textColor = Color.White,
                        borderColor = DarkBorder,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CERRAR", color = Color.White, fontSize = 8.sp, fontFamily = SilkscreenFontFamily)
                    }

                    PixelButton(
                        onClick = {
                            onSaveScreenshot()
                            Toast.makeText(context, "Captura PNG Lossless guardada", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        backgroundColor = EmeraldAccent,
                        textColor = Color.Black,
                        borderColor = Color.White,
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("GUARDAR PNG", color = Color.Black, fontSize = 8.sp, fontFamily = SilkscreenFontFamily, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = label, color = TextSecondary, fontSize = 8.sp, fontFamily = SilkscreenFontFamily)
        Text(
            text = value,
            color = valueColor,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = SilkscreenFontFamily
        )
    }
}
