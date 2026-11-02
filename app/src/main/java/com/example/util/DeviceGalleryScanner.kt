package com.example.util

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.example.model.GalleryMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DeviceGalleryScanner {

    /**
     * Automatically scans the device's storage for all photos and screenshots.
     * Orders by most recent first so the newest photo/screenshot is immediately available.
     */
    suspend fun queryDevicePhotos(context: Context): List<GalleryMediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<GalleryMediaItem>()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.DATE_ADDED
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        runCatching {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "IMG_$id"
                    val width = cursor.getInt(widthCol)
                    val height = cursor.getInt(heightCol)
                    val dateAdded = cursor.getLong(dateCol)

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val isScreenshot = name.contains("screenshot", ignoreCase = true) ||
                            name.contains("captura", ignoreCase = true)

                    items.add(
                        GalleryMediaItem(
                            id = id,
                            uri = contentUri,
                            name = name,
                            width = width,
                            height = height,
                            dateAdded = dateAdded,
                            isScreenshot = isScreenshot
                        )
                    )
                }
            }
        }

        items
    }
}
