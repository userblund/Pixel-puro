package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.GalleryMediaItem
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.PressStart2PFontFamily
import com.example.ui.theme.SilkscreenFontFamily
import com.example.ui.theme.TextMuted

@Composable
fun FullDeviceGalleryDialog(
    photos: List<GalleryMediaItem>,
    selectedItem: GalleryMediaItem?,
    onSelectPhoto: (GalleryMediaItem) -> Unit,
    onRefresh: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .border(1.dp, CyanAccent, RectangleShape)
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "GALERÍA COMPLETA",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontFamily = PressStart2PFontFamily,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${photos.size} IMÁGENES DISPONIBLES",
                    color = EmeraldAccent,
                    fontSize = 8.sp,
                    fontFamily = SilkscreenFontFamily
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "RECARGAR",
                    color = CyanAccent,
                    fontSize = 8.sp,
                    fontFamily = SilkscreenFontFamily,
                    modifier = Modifier.clickable { onRefresh() }
                )
                Text(
                    text = "CERRAR",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontFamily = SilkscreenFontFamily,
                    modifier = Modifier.clickable { onClose() }
                )
            }
        }

        if (photos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NO HAY IMÁGENES VISIBLES CON EL PERMISO ACTUAL",
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontFamily = SilkscreenFontFamily
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 104.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(photos, key = { it.id }) { photo ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .size(112.dp)
                            .background(DarkSurface)
                            .border(
                                width = if (selectedItem?.id == photo.id) 2.dp else 1.dp,
                                color = if (selectedItem?.id == photo.id) CyanAccent else DarkBorder,
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
                            modifier = Modifier.fillMaxSize()
                        )
                        if (photo.isScreenshot) {
                            Text(
                                text = "CAP",
                                color = EmeraldAccent,
                                fontSize = 7.sp,
                                fontFamily = PressStart2PFontFamily,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .background(Color.Black.copy(alpha = 0.8f))
                                    .padding(horizontal = 3.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
