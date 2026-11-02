package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class GalleryDateLayout(val preferenceValue: Int, val label: String) {
    RELATIVE_TODAY_YESTERDAY(0, "FECHA + HOY/AYER"),
    RELATIVE_TODAY_ONLY(1, "FECHA + HOY"),
    FULL_DATES(2, "FECHA COMPLETA"),
    NO_DATE_HEADERS(3, "SIN FECHAS");

    companion object {
        fun fromPreference(value: Int): GalleryDateLayout =
            values().firstOrNull { it.preferenceValue == value } ?: RELATIVE_TODAY_YESTERDAY
    }
}

private data class DatedGalleryGroup(val date: LocalDate, val photos: List<GalleryMediaItem>)

@Composable
fun FullDeviceGalleryDialog(
    photos: List<GalleryMediaItem>,
    selectedItem: GalleryMediaItem?,
    onSelectPhoto: (GalleryMediaItem) -> Unit,
    onRefresh: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("pixel_puro_gallery", android.content.Context.MODE_PRIVATE)
    }
    var dateLayout by remember {
        mutableStateOf(GalleryDateLayout.fromPreference(preferences.getInt("date_layout", 0)))
    }
    var isLayoutMenuExpanded by remember { mutableStateOf(false) }
    var photoColumnCount by remember { mutableStateOf(preferences.getInt("full_gallery_column_count", 3).coerceIn(1, 16)) }
    var isColumnMenuExpanded by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale("es", "PY"))
    }
    val sortedPhotos = remember(photos) { photos.sortedByDescending { it.dateAdded } }
    val groups = remember(sortedPhotos, today) {
        sortedPhotos.groupBy { photo ->
            Instant.ofEpochSecond(photo.dateAdded).atZone(zone).toLocalDate()
        }.map { (date, datePhotos) ->
            DatedGalleryGroup(date, datePhotos.sortedByDescending { it.dateAdded })
        }.sortedByDescending { it.date }
    }

    fun headerFor(date: LocalDate): String {
        val formatted = date.format(dateFormatter)
        val capitalized = formatted.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale("es", "PY")) else it.toString()
        }
        return when (dateLayout) {
            GalleryDateLayout.RELATIVE_TODAY_YESTERDAY -> when (date) {
                today -> "$capitalized (Hoy)"
                today.minusDays(1) -> "$capitalized (Ayer)"
                else -> capitalized
            }
            GalleryDateLayout.RELATIVE_TODAY_ONLY -> if (date == today) "$capitalized (Hoy)" else capitalized
            GalleryDateLayout.FULL_DATES -> capitalized
            GalleryDateLayout.NO_DATE_HEADERS -> ""
        }
    }

    val gridState = rememberLazyGridState()

    Column(
        modifier = Modifier.fillMaxSize()
            .background(DarkBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().background(DarkSurface)
                .border(1.dp, CyanAccent, RectangleShape)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("GALERÍA COMPLETA", color = Color.White, fontSize = 10.sp, fontFamily = PressStart2PFontFamily, fontWeight = FontWeight.Bold)
                    Text("${photos.size} IMÁGENES DISPONIBLES", color = EmeraldAccent, fontSize = 8.sp, fontFamily = SilkscreenFontFamily)
                }
                Text("CERRAR", color = Color.White, fontSize = 8.sp, fontFamily = SilkscreenFontFamily, modifier = Modifier.clickable { onClose() }.padding(start = 8.dp, top = 6.dp, bottom = 6.dp))
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box {
                    Text("VISTAS: $photoColumnCount", color = CyanAccent, fontSize = 7.sp, fontFamily = SilkscreenFontFamily, modifier = Modifier.clickable { isColumnMenuExpanded = true })
                    DropdownMenu(expanded = isColumnMenuExpanded, onDismissRequest = { isColumnMenuExpanded = false }, modifier = Modifier.background(DarkSurface)) {
                        (1..16).forEach { count ->
                            DropdownMenuItem(
                                text = { Text("$count COLUMNA${if (count == 1) "" else "S"}", color = if (count == photoColumnCount) CyanAccent else Color.White, fontSize = 11.sp, fontFamily = SilkscreenFontFamily) },
                                onClick = { photoColumnCount = count; preferences.edit().putInt("full_gallery_column_count", count).apply(); isColumnMenuExpanded = false }
                            )
                        }
                    }
                }
                Box {
                    Text("ORDEN: ${dateLayout.label}", color = CyanAccent, fontSize = 7.sp, fontFamily = SilkscreenFontFamily, modifier = Modifier.clickable { isLayoutMenuExpanded = true })
                    DropdownMenu(expanded = isLayoutMenuExpanded, onDismissRequest = { isLayoutMenuExpanded = false }, modifier = Modifier.background(DarkSurface)) {
                        GalleryDateLayout.values().forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label, color = if (option == dateLayout) CyanAccent else Color.White, fontSize = 11.sp, fontFamily = SilkscreenFontFamily) },
                                onClick = { dateLayout = option; preferences.edit().putInt("date_layout", option.preferenceValue).apply(); isLayoutMenuExpanded = false }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text("RECARGAR", color = CyanAccent, fontSize = 8.sp, fontFamily = SilkscreenFontFamily, modifier = Modifier.clickable { onRefresh() })
            }
        }

        if (photos.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "NO HAY IMÁGENES VISIBLES CON EL PERMISO ACTUAL",
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontFamily = SilkscreenFontFamily
                )
            }
        } else {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(photoColumnCount),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(end = 18.dp, top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (dateLayout == GalleryDateLayout.NO_DATE_HEADERS) {
                        items(sortedPhotos, key = { it.id }) { photo ->
                            GalleryPhotoTile(photo, selectedItem?.id == photo.id, onSelectPhoto)
                        }
                    } else {
                        groups.forEach { group ->
                            item(key = "date-${group.date}", span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    text = headerFor(group.date),
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 14.dp, bottom = 8.dp)
                                )
                            }
                            items(items = group.photos, key = { photo -> photo.id }) { photo ->
                                GalleryPhotoTile(photo, selectedItem?.id == photo.id, onSelectPhoto)
                            }
                        }
                    }
                }
                FastGalleryScrollbar(
                    state = gridState,
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().zIndex(2f)
                )
            }
        }
    }
}

@Composable
private fun FastGalleryScrollbar(state: LazyGridState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    BoxWithConstraints(
        modifier = modifier.fillMaxHeight().width(16.dp)
            .background(Color.Transparent)
            .pointerInput(state) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val layout = state.layoutInfo
                        val maxIndex = (layout.totalItemsCount - layout.visibleItemsInfo.size).coerceAtLeast(0)
                        val target = ((offset.y / size.height.coerceAtLeast(1)) * maxIndex).toInt()
                        scope.launch { state.scrollToItem(target.coerceIn(0, maxIndex)) }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val layout = state.layoutInfo
                        val maxIndex = (layout.totalItemsCount - layout.visibleItemsInfo.size).coerceAtLeast(0)
                        val target = ((change.position.y / size.height.coerceAtLeast(1)) * maxIndex).toInt()
                        scope.launch { state.scrollToItem(target.coerceIn(0, maxIndex)) }
                    }
                )
            }
    ) {
        val layout = state.layoutInfo
        val total = layout.totalItemsCount
        val visible = layout.visibleItemsInfo.size.coerceAtLeast(1)
        val maxIndex = (total - visible).coerceAtLeast(1)
        val thumbHeight = (maxHeight * (visible.toFloat() / total.coerceAtLeast(1))).coerceIn(30.dp, maxHeight)
        val scrollFraction = (state.firstVisibleItemIndex.toFloat() / maxIndex).coerceIn(0f, 1f)
        val thumbTop = (maxHeight - thumbHeight) * scrollFraction

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
            Box(modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(3.dp).background(DarkBorder))
            Box(
                modifier = Modifier.align(Alignment.TopEnd).offset(y = thumbTop).size(width = 7.dp, height = thumbHeight)
                    .background(CyanAccent, RectangleShape).border(1.dp, Color.White.copy(alpha = 0.6f), RectangleShape)
            )
        }
    }
}

@Composable
private fun GalleryPhotoTile(
    photo: GalleryMediaItem,
    isSelected: Boolean,
    onSelectPhoto: (GalleryMediaItem) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth().size(112.dp).background(DarkSurface)
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
            modifier = Modifier.fillMaxSize()
        )
        if (photo.isScreenshot) {
            Text(
                text = "CAP",
                color = EmeraldAccent,
                fontSize = 7.sp,
                fontFamily = PressStart2PFontFamily,
                modifier = Modifier.align(Alignment.BottomStart)
                    .background(Color.Black.copy(alpha = 0.8f)).padding(horizontal = 3.dp, vertical = 2.dp)
            )
        }
    }
}
