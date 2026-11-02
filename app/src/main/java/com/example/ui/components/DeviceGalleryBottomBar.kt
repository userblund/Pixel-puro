package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.GalleryMediaItem
import com.example.ui.pixel.PixelButton
import com.example.ui.pixel.PixelCameraIcon
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

@Composable
fun DeviceGalleryBottomBar(
    photos: List<GalleryMediaItem>,
    selectedItem: GalleryMediaItem?,
    hasPermission: Boolean,
    onSelectPhoto: (GalleryMediaItem) -> Unit,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onOpenPresetGallery: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenFullGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .pixelShadow(0.dp, (-3).dp, Color.Black)
            .background(DarkSurface, RectangleShape)
            .border(2.dp, DarkBorder, RectangleShape)
            .pixelBevel(Color.White.copy(alpha = 0.2f), Color.Black.copy(alpha = 0.8f), 2.dp)
            .padding(vertical = 6.dp)
            .testTag("device_gallery_bottom_bar")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PixelImageIcon(tint = CyanAccent, size = 12.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "GALERIA DISPOSITIVO",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontFamily = PressStart2PFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                    if (photos.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "[${photos.size}]",
                            color = EmeraldAccent,
                            fontSize = 8.sp,
                            fontFamily = SilkscreenFontFamily
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(DarkBackground, RectangleShape)
                            .border(1.dp, DarkBorder, RectangleShape)
                            .clickable { onRefresh() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("RECARGAR", color = TextSecondary, fontSize = 7.sp, fontFamily = SilkscreenFontFamily)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .background(DarkBackground, RectangleShape)
                            .border(1.dp, CyanAccent, RectangleShape)
                            .clickable { onOpenPresetGallery() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("PATRONES", color = CyanAccent, fontSize = 7.sp, fontFamily = SilkscreenFontFamily)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (!hasPermission) {
                // Permission Request Prompt
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .background(DarkBackground, RectangleShape)
                        .border(1.dp, DarkBorder, RectangleShape)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Auto-escaneo de fotos y capturas",
                        color = TextSecondary,
                        fontSize = 8.sp,
                        fontFamily = SilkscreenFontFamily,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    PixelButton(
                        onClick = onRequestPermission,
                        backgroundColor = CyanAccent,
                        textColor = Color.Black
                    ) {
                        Text(
                            text = "ACTIVAR",
                            color = Color.Black,
                            fontSize = 8.sp,
                            fontFamily = SilkscreenFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (photos.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Sin fotos en memoria externa",
                        color = TextMuted,
                        fontSize = 8.sp,
                        fontFamily = SilkscreenFontFamily
                    )
                }
            } else {
                // Horizontal scrolling photo strip
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // First item: Snap photo with Camera
                    item {
                        Box(
                            modifier = Modifier
                                .size(width = 60.dp, height = 60.dp)
                                .pixelShadow(2.dp, 2.dp, Color.Black)
                                .background(DarkSurface, RectangleShape)
                                .border(1.dp, CyanAccent, RectangleShape)
                                .clickable { onOpenCamera() },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                PixelCameraIcon(tint = CyanAccent, size = 16.dp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "CAM",
                                    color = CyanAccent,
                                    fontSize = 7.sp,
                                    fontFamily = PressStart2PFontFamily
                                )
                            }
                        }
                    }

                    items(photos, key = { it.id }) { photo ->
                        val isSelected = selectedItem?.id == photo.id

                        Box(
                            modifier = Modifier
                                .size(width = 60.dp, height = 60.dp)
                                .pixelShadow(2.dp, 2.dp, Color.Black)
                                .background(DarkBackground, RectangleShape)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) CyanAccent else DarkBorder,
                                    shape = RectangleShape
                                )
                                .clickable { onSelectPhoto(photo) }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(photo.uri)
                                    .crossfade(false)
                                    .build(),
                                contentDescription = photo.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )

                            // Tiny pixel badge if it's a screenshot
                            if (photo.isScreenshot) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .background(Color.Black.copy(alpha = 0.85f), RectangleShape)
                                        .border(1.dp, EmeraldAccent, RectangleShape)
                                        .padding(horizontal = 2.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "CAP",
                                        color = EmeraldAccent,
                                        fontSize = 6.sp,
                                        fontFamily = PressStart2PFontFamily
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .background(DarkBackground, RectangleShape)
                    .border(1.dp, CyanAccent, RectangleShape)
                    .clickable { onOpenFullGallery() }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "VER TODA LA GALERÍA",
                    color = CyanAccent,
                    fontSize = 8.sp,
                    fontFamily = PressStart2PFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
