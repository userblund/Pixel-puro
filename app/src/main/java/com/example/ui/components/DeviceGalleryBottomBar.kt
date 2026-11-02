package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("pixel_puro_gallery", android.content.Context.MODE_PRIVATE)
    }
    var visiblePhotoCount by remember {
        mutableStateOf(preferences.getInt("bottom_visible_photo_count", 3).coerceIn(1, 16))
    }
    var isPhotoCountMenuExpanded by remember { mutableStateOf(false) }

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PixelImageIcon(tint = CyanAccent, size = 12.dp)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "GALERIA DISPOSITIVO",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontFamily = PressStart2PFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                    if (photos.isNotEmpty()) {
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("[${photos.size}]", color = EmeraldAccent, fontSize = 8.sp, fontFamily = SilkscreenFontFamily)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        Text(
                            text = "VISTAS: $visiblePhotoCount",
                            color = CyanAccent,
                            fontSize = 7.sp,
                            fontFamily = SilkscreenFontFamily,
                            modifier = Modifier.clickable { isPhotoCountMenuExpanded = true }.padding(3.dp)
                        )
                        DropdownMenu(
                            expanded = isPhotoCountMenuExpanded,
                            onDismissRequest = { isPhotoCountMenuExpanded = false },
                            modifier = Modifier.background(DarkSurface)
                        ) {
                            (1..16).forEach { count ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "$count IMAGEN${if (count == 1) "" else "ES"}",
                                            color = if (count == visiblePhotoCount) CyanAccent else Color.White,
                                            fontSize = 11.sp,
                                            fontFamily = SilkscreenFontFamily
                                        )
                                    },
                                    onClick = {
                                        visiblePhotoCount = count
                                        preferences.edit().putInt("bottom_visible_photo_count", count).apply()
                                        isPhotoCountMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier.background(DarkBackground, RectangleShape)
                            .border(1.dp, CyanAccent, RectangleShape)
                            .clickable { onOpenCamera() }.padding(horizontal = 5.dp, vertical = 3.dp)
                    ) {
                        Text("CAM", color = CyanAccent, fontSize = 7.sp, fontFamily = PressStart2PFontFamily)
                    }
                    Spacer(modifier = Modifier.size(4.dp))
                    Box(
                        modifier = Modifier.background(DarkBackground, RectangleShape)
                            .border(1.dp, DarkBorder, RectangleShape)
                            .clickable { onRefresh() }.padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text("RECARGAR", color = TextSecondary, fontSize = 7.sp, fontFamily = SilkscreenFontFamily)
                    }
                    Spacer(modifier = Modifier.size(4.dp))
                    Box(
                        modifier = Modifier.background(DarkBackground, RectangleShape)
                            .border(1.dp, CyanAccent, RectangleShape)
                            .clickable { onOpenPresetGallery() }.padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text("PATRONES", color = CyanAccent, fontSize = 7.sp, fontFamily = SilkscreenFontFamily)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (!hasPermission) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp)
                        .background(DarkBackground, RectangleShape).border(1.dp, DarkBorder, RectangleShape)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Auto-escaneo de fotos y capturas",
                        color = TextSecondary,
                        fontSize = 8.sp,
                        fontFamily = SilkscreenFontFamily,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    PixelButton(onClick = onRequestPermission, backgroundColor = CyanAccent, textColor = Color.Black) {
                        Text("ACTIVAR", color = Color.Black, fontSize = 8.sp, fontFamily = SilkscreenFontFamily, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (photos.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Text("Sin fotos en memoria externa", color = TextMuted, fontSize = 8.sp, fontFamily = SilkscreenFontFamily)
                }
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val tileSize = ((maxWidth - 20.dp - (visiblePhotoCount - 1) * 4.dp) / visiblePhotoCount).coerceAtLeast(12.dp)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(photos, key = { it.id }) { photo ->
                            val isSelected = selectedItem?.id == photo.id
                            Box(
                                modifier = Modifier.size(tileSize)
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
                                    model = ImageRequest.Builder(LocalContext.current).data(photo.uri).crossfade(false).build(),
                                    contentDescription = photo.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )
                                if (photo.isScreenshot && tileSize >= 34.dp) {
                                    Box(
                                        modifier = Modifier.align(Alignment.BottomStart)
                                            .background(Color.Black.copy(alpha = 0.85f), RectangleShape)
                                            .border(1.dp, EmeraldAccent, RectangleShape)
                                            .padding(horizontal = 2.dp, vertical = 1.dp)
                                    ) {
                                        Text("CAP", color = EmeraldAccent, fontSize = 6.sp, fontFamily = PressStart2PFontFamily)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp)
                    .background(DarkBackground, RectangleShape).border(1.dp, CyanAccent, RectangleShape)
                    .clickable { onOpenFullGallery() }.padding(vertical = 8.dp),
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
