package com.example.model

import androidx.compose.ui.graphics.Color
import java.util.Locale

data class PixelColor(
    val argb: Int,
    val count: Int = 0,
    val percentage: Float = 0f
) {
    val a: Int get() = (argb shr 24) and 0xFF
    val r: Int get() = (argb shr 16) and 0xFF
    val g: Int get() = (argb shr 8) and 0xFF
    val b: Int get() = argb and 0xFF

    val hex8Argb: String get() = String.format(Locale.US, "#%02X%02X%02X%02X", a, r, g, b)
    val hex8Rgba: String get() = String.format(Locale.US, "#%02X%02X%02X%02X", r, g, b, a)
    val hex6Rgb: String get() = String.format(Locale.US, "#%02X%02X%02X", r, g, b)

    val rgbaCss: String
        get() = String.format(Locale.US, "rgba(%d, %d, %d, %.2f)", r, g, b, a / 255f)

    val rgbaValues: String
        get() = "R:$r  G:$g  B:$b  A:$a"

    val hslValues: String
        get() {
            val rf = r / 255f
            val gf = g / 255f
            val bf = b / 255f
            val max = maxOf(rf, gf, bf)
            val min = minOf(rf, gf, bf)
            val l = (max + min) / 2f
            val d = max - min
            val s = if (max == min) 0f else if (l > 0.5f) d / (2f - max - min) else d / (max + min)
            val h = when {
                max == min -> 0f
                max == rf -> ((gf - bf) / d + (if (gf < bf) 6f else 0f)) * 60f
                max == gf -> ((bf - rf) / d + 2f) * 60f
                else -> ((rf - gf) / d + 4f) * 60f
            }
            return String.format(Locale.US, "H:%.0f° S:%.0f%% L:%.0f%%", h, s * 100f, l * 100f)
        }

    fun toComposeColor(): Color = Color(argb)
}

data class ImageAnalysisResult(
    val width: Int,
    val height: Int,
    val totalPixels: Long,
    val uniqueColorsCount: Int,
    val topColors: List<PixelColor>,
    val hasAlpha: Boolean,
    val transparentPixelsCount: Long,
    val redHistogram: IntArray,
    val greenHistogram: IntArray,
    val blueHistogram: IntArray,
    val lumaHistogram: IntArray,
    val sha256Hash: String,
    val uncompressedSizeBytes: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageAnalysisResult

        if (width != other.width) return false
        if (height != other.height) return false
        if (totalPixels != other.totalPixels) return false
        if (uniqueColorsCount != other.uniqueColorsCount) return false
        if (topColors != other.topColors) return false
        if (hasAlpha != other.hasAlpha) return false
        if (transparentPixelsCount != other.transparentPixelsCount) return false
        if (!redHistogram.contentEquals(other.redHistogram)) return false
        if (!greenHistogram.contentEquals(other.greenHistogram)) return false
        if (!blueHistogram.contentEquals(other.blueHistogram)) return false
        if (!lumaHistogram.contentEquals(other.lumaHistogram)) return false
        if (sha256Hash != other.sha256Hash) return false
        if (uncompressedSizeBytes != other.uncompressedSizeBytes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + totalPixels.hashCode()
        result = 31 * result + uniqueColorsCount
        result = 31 * result + topColors.hashCode()
        result = 31 * result + hasAlpha.hashCode()
        result = 31 * result + transparentPixelsCount.hashCode()
        result = 31 * result + redHistogram.contentHashCode()
        result = 31 * result + greenHistogram.contentHashCode()
        result = 31 * result + blueHistogram.contentHashCode()
        result = 31 * result + lumaHistogram.contentHashCode()
        result = 31 * result + sha256Hash.hashCode()
        result = 31 * result + uncompressedSizeBytes.hashCode()
        return result
    }
}

enum class PresetType {
    TWO_PIXEL_HARDWARE_TEST,
    NYQUIST_CHECKERBOARD,
    PALETTE_16_TEST,
    RETRO_PIXEL_ART,
    CUSTOM_IMPORTED
}

data class SamplePreset(
    val id: String,
    val name: String,
    val description: String,
    val type: PresetType
)

data class IntegrityReport(
    val originalSha256: String,
    val screenCaptureSha256: String,
    val totalTestedPixels: Int,
    val identicalPixels: Int,
    val alteredPixels: Int,
    val integrityPercentage: Float,
    val isBitPerfect: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class EyedropperState(
    val isActive: Boolean = false,
    val touchX: Float = 0f,
    val touchY: Float = 0f,
    val pixelX: Int = -1,
    val pixelY: Int = -1,
    val color: PixelColor? = null
)

data class GalleryMediaItem(
    val id: Long,
    val uri: android.net.Uri,
    val name: String,
    val width: Int,
    val height: Int,
    val dateAdded: Long,
    val isScreenshot: Boolean
)

data class NormalizedRect(
    val left: Float = 0.15f,
    val top: Float = 0.20f,
    val right: Float = 0.85f,
    val bottom: Float = 0.80f
) {
    val width: Float get() = (right - left).coerceAtLeast(0.01f)
    val height: Float get() = (bottom - top).coerceAtLeast(0.01f)

    fun toPixelRect(bitmapWidth: Int, bitmapHeight: Int): android.graphics.Rect {
        val l = (left * bitmapWidth).toInt().coerceIn(0, bitmapWidth - 1)
        val t = (top * bitmapHeight).toInt().coerceIn(0, bitmapHeight - 1)
        val r = (right * bitmapWidth).toInt().coerceIn(l + 1, bitmapWidth)
        val b = (bottom * bitmapHeight).toInt().coerceIn(t + 1, bitmapHeight)
        return android.graphics.Rect(l, t, r, b)
    }
}

data class OpticalTransferFactor(
    val id: String,
    val title: String,
    val subtitle: String,
    val currentObserved: String,
    val requiredCondition: String, // "¿Qué tendría que pasar para que sea 100%?"
    val theoreticalFormula: String,
    val degradationPenaltyPercent: Float
)

data class ScreenBufferAnalysisReport(
    val fidelityPercentage: Float, // Strictly 0.0% < fidelity < 100.0%
    val opticalCertainty: Float,
    val estimatedGamma: Float,
    val inverseGamma: Float,
    val ambientFlareOffset: Int,
    val contrastRatio: Float,
    val moireInterferenceScore: Float,
    val colorCorrectionMatrix: FloatArray, // 3x3 matrix [m00, m01, m02, m10, m11, m12, m20, m21, m22]
    val factors: List<OpticalTransferFactor>,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ScreenBufferAnalysisReport
        return fidelityPercentage == other.fidelityPercentage &&
                colorCorrectionMatrix.contentEquals(other.colorCorrectionMatrix) &&
                factors == other.factors
    }

    override fun hashCode(): Int {
        var result = fidelityPercentage.hashCode()
        result = 31 * result + colorCorrectionMatrix.contentHashCode()
        result = 31 * result + factors.hashCode()
        return result
    }
}

