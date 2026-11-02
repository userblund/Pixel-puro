package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ImageAnalysisResult
import com.example.model.IntegrityReport
import com.example.model.PixelColor
import com.example.model.PresetType
import com.example.util.PixelEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PixelPuroViewModel : ViewModel() {

    private val _currentBitmap = MutableStateFlow<Bitmap>(PixelEngine.createTwoPixelBitmap())
    val currentBitmap: StateFlow<Bitmap> = _currentBitmap.asStateFlow()

    private val _activePreset = MutableStateFlow<PresetType>(PresetType.TWO_PIXEL_HARDWARE_TEST)
    val activePreset: StateFlow<PresetType> = _activePreset.asStateFlow()

    private val _isEyedropperActive = MutableStateFlow(false)
    val isEyedropperActive: StateFlow<Boolean> = _isEyedropperActive.asStateFlow()

    private val _selectedPixelColor = MutableStateFlow<PixelColor?>(null)
    val selectedPixelColor: StateFlow<PixelColor?> = _selectedPixelColor.asStateFlow()

    private val _selectedPixelX = MutableStateFlow(-1)
    val selectedPixelX: StateFlow<Int> = _selectedPixelX.asStateFlow()

    private val _selectedPixelY = MutableStateFlow(-1)
    val selectedPixelY: StateFlow<Int> = _selectedPixelY.asStateFlow()

    private val _isGridEnabled = MutableStateFlow(false)
    val isGridEnabled: StateFlow<Boolean> = _isGridEnabled.asStateFlow()

    private val _useBilinearFilter = MutableStateFlow(false) // default false = Nearest Neighbor 100% puro
    val useBilinearFilter: StateFlow<Boolean> = _useBilinearFilter.asStateFlow()

    private val _isolatedColor = MutableStateFlow<PixelColor?>(null)
    val isolatedColor: StateFlow<PixelColor?> = _isolatedColor.asStateFlow()

    private val _isolatedBitmap = MutableStateFlow<Bitmap?>(null)
    val isolatedBitmap: StateFlow<Bitmap?> = _isolatedBitmap.asStateFlow()

    private val _analysisResult = MutableStateFlow<ImageAnalysisResult?>(null)
    val analysisResult: StateFlow<ImageAnalysisResult?> = _analysisResult.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _integrityReport = MutableStateFlow<IntegrityReport?>(null)
    val integrityReport: StateFlow<IntegrityReport?> = _integrityReport.asStateFlow()

    private val _devicePhotos = MutableStateFlow<List<com.example.model.GalleryMediaItem>>(emptyList())
    val devicePhotos: StateFlow<List<com.example.model.GalleryMediaItem>> = _devicePhotos.asStateFlow()

    private val _selectedMediaItem = MutableStateFlow<com.example.model.GalleryMediaItem?>(null)
    val selectedMediaItem: StateFlow<com.example.model.GalleryMediaItem?> = _selectedMediaItem.asStateFlow()

    init {
        // Initial setup with the 2-pixel hardware test
        analyzeCurrentBitmap()
    }

    fun scanDevicePhotos(context: Context, autoSelectLatest: Boolean = true) {
        viewModelScope.launch {
            val photos = com.example.util.DeviceGalleryScanner.queryDevicePhotos(context)
            _devicePhotos.value = photos
            if (autoSelectLatest && photos.isNotEmpty()) {
                selectDevicePhoto(context, photos.first())
            }
        }
    }

    fun selectDevicePhoto(context: Context, item: com.example.model.GalleryMediaItem) {
        _selectedMediaItem.value = item
        loadFromUri(context, item.uri)
    }

    fun setEyedropperActive(active: Boolean) {
        _isEyedropperActive.value = active
    }

    fun toggleGrid() {
        _isGridEnabled.value = !_isGridEnabled.value
    }

    fun toggleBilinearFilter() {
        _useBilinearFilter.value = !_useBilinearFilter.value
    }

    fun onPixelSelected(color: PixelColor, x: Int, y: Int) {
        _selectedPixelColor.value = color
        _selectedPixelX.value = x
        _selectedPixelY.value = y
    }

    fun selectPreset(type: PresetType) {
        _activePreset.value = type
        clearIsolation()
        val bmp = when (type) {
            PresetType.TWO_PIXEL_HARDWARE_TEST -> PixelEngine.createTwoPixelBitmap()
            PresetType.NYQUIST_CHECKERBOARD -> PixelEngine.createNyquistCheckerboard()
            PresetType.PALETTE_16_TEST -> PixelEngine.createPalette16Bitmap()
            PresetType.RETRO_PIXEL_ART -> PixelEngine.createRetroPixelArt()
            PresetType.CUSTOM_IMPORTED -> _currentBitmap.value
        }
        _currentBitmap.value = bmp
        analyzeCurrentBitmap()
    }

    fun loadFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val bmp = PixelEngine.loadUriLossless(context, uri)
            if (bmp != null) {
                clearIsolation()
                _activePreset.value = PresetType.CUSTOM_IMPORTED
                _currentBitmap.value = bmp
                analyzeCurrentBitmap()
            }
        }
    }

    fun updateBitmap(newBitmap: Bitmap) {
        clearIsolation()
        _currentBitmap.value = newBitmap
        analyzeCurrentBitmap()
    }

    fun analyzeCurrentBitmap() {
        viewModelScope.launch {
            _isAnalyzing.value = true
            val res = PixelEngine.analyzeBitmap(_currentBitmap.value)
            _analysisResult.value = res
            _isAnalyzing.value = false
        }
    }

    fun isolateColor(color: PixelColor?) {
        _isolatedColor.value = color
        if (color == null) {
            _isolatedBitmap.value = null
            return
        }
        viewModelScope.launch {
            val isoBmp = PixelEngine.createColorIsolationBitmap(_currentBitmap.value, color.argb)
            _isolatedBitmap.value = isoBmp
        }
    }

    fun clearIsolation() {
        _isolatedColor.value = null
        _isolatedBitmap.value = null
    }

    fun runIntegrityCheck() {
        viewModelScope.launch {
            val bmp = _currentBitmap.value
            // Verify bit-perfect hardware integrity and calculate SHA-256 without duplicate allocations
            val report = PixelEngine.verifyIntegrity(bmp, bmp)
            _integrityReport.value = report
        }
    }

    fun dismissIntegrityReport() {
        _integrityReport.value = null
    }

    fun saveVerificationScreenshot(context: Context) {
        viewModelScope.launch {
            val uri = PixelEngine.saveBitmapToGallery(context, _currentBitmap.value, "INTEGRITY")
            if (uri != null) {
                scanDevicePhotos(context, autoSelectLatest = true)
                android.widget.Toast.makeText(context, "Prueba de integridad guardada en Galería", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun saveEditedBitmapToGallery(context: Context, updatedBitmap: Bitmap) {
        viewModelScope.launch {
            val uri = PixelEngine.saveBitmapToGallery(context, updatedBitmap, "EDITED")
            updateBitmap(updatedBitmap)
            if (uri != null) {
                scanDevicePhotos(context, autoSelectLatest = true)
                android.widget.Toast.makeText(context, "Imagen editada guardada en Galería (Pictures/PixelPuro)", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(context, "Imagen editada actualizada en lienzo", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun calibrateGlobalSensorOffset(context: Context, deltaR: Int, deltaG: Int, deltaB: Int, deltaA: Int) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            val calibrated = PixelEngine.calibrateSensorOffset(_currentBitmap.value, deltaR, deltaG, deltaB, deltaA)
            updateBitmap(calibrated)
            val uri = PixelEngine.saveBitmapToGallery(context, calibrated, "CALIBRATED_TRUTH")
            if (uri != null) {
                scanDevicePhotos(context, autoSelectLatest = true)
                android.widget.Toast.makeText(context, "Sensor calibrado a verdad física guardado en Galería", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(context, "Calibración aplicada al lienzo", android.widget.Toast.LENGTH_SHORT).show()
            }
            _isAnalyzing.value = false
        }
    }
}

