package com.example.util

import android.content.Context
import android.graphics.Bitmap
import com.example.model.NormalizedRect
import com.example.model.ScreenBufferAnalysisReport

data class CameraCalibrationProfile(
    val profileName: String = "Calibración Pantalla PC",
    val colorCorrectionMatrix: FloatArray = floatArrayOf(
        1.22f, -0.16f, -0.06f,
        -0.08f, 1.15f, -0.07f,
        -0.03f, -0.12f, 1.15f
    ),
    val inverseGamma: Float = 0.455f,
    val ambientFlareOffset: Int = 8,
    val rectLeft: Float = 0.12f,
    val rectTop: Float = 0.15f,
    val rectRight: Float = 0.88f,
    val rectBottom: Float = 0.85f,
    val fidelityPercentage: Float = 85.4f,
    val opticalCertainty: Float = 91.2f,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CameraCalibrationProfile
        return colorCorrectionMatrix.contentEquals(other.colorCorrectionMatrix) &&
                inverseGamma == other.inverseGamma &&
                ambientFlareOffset == other.ambientFlareOffset &&
                rectLeft == other.rectLeft &&
                rectTop == other.rectTop &&
                rectRight == other.rectRight &&
                rectBottom == other.rectBottom
    }

    override fun hashCode(): Int {
        var result = colorCorrectionMatrix.contentHashCode()
        result = 31 * result + inverseGamma.hashCode()
        result = 31 * result + ambientFlareOffset
        result = 31 * result + rectLeft.hashCode()
        result = 31 * result + rectTop.hashCode()
        result = 31 * result + rectRight.hashCode()
        result = 31 * result + rectBottom.hashCode()
        return result
    }
}

object CameraCalibrationManager {
    private const val PREFS_NAME = "pixelpuro_calibration_prefs"
    private const val KEY_IS_CALIBRATED = "is_calibrated"
    private const val KEY_INVERSE_GAMMA = "inv_gamma"
    private const val KEY_FLARE_OFFSET = "flare_offset"
    private const val KEY_FIDELITY = "fidelity_pct"
    private const val KEY_CERTAINTY = "certainty"
    private const val KEY_RECT_LEFT = "rect_left"
    private const val KEY_RECT_TOP = "rect_top"
    private const val KEY_RECT_RIGHT = "rect_right"
    private const val KEY_RECT_BOTTOM = "rect_bottom"
    private const val KEY_CTM = "ctm_matrix"

    fun saveProfile(context: Context, profile: CameraCalibrationProfile) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ctmStr = profile.colorCorrectionMatrix.joinToString(",")
        prefs.edit()
            .putBoolean(KEY_IS_CALIBRATED, true)
            .putFloat(KEY_INVERSE_GAMMA, profile.inverseGamma)
            .putInt(KEY_FLARE_OFFSET, profile.ambientFlareOffset)
            .putFloat(KEY_FIDELITY, profile.fidelityPercentage)
            .putFloat(KEY_CERTAINTY, profile.opticalCertainty)
            .putFloat(KEY_RECT_LEFT, profile.rectLeft)
            .putFloat(KEY_RECT_TOP, profile.rectTop)
            .putFloat(KEY_RECT_RIGHT, profile.rectRight)
            .putFloat(KEY_RECT_BOTTOM, profile.rectBottom)
            .putString(KEY_CTM, ctmStr)
            .apply()
    }

    fun loadProfile(context: Context): CameraCalibrationProfile? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_IS_CALIBRATED, false)) return null

        val invGamma = prefs.getFloat(KEY_INVERSE_GAMMA, 0.455f)
        val flare = prefs.getInt(KEY_FLARE_OFFSET, 8)
        val fidelity = prefs.getFloat(KEY_FIDELITY, 85.4f)
        val certainty = prefs.getFloat(KEY_CERTAINTY, 91.2f)
        val l = prefs.getFloat(KEY_RECT_LEFT, 0.12f)
        val t = prefs.getFloat(KEY_RECT_TOP, 0.15f)
        val r = prefs.getFloat(KEY_RECT_RIGHT, 0.88f)
        val b = prefs.getFloat(KEY_RECT_BOTTOM, 0.85f)
        val ctmStr = prefs.getString(KEY_CTM, null)

        val ctm = if (!ctmStr.isNullOrEmpty()) {
            ctmStr.split(",").mapNotNull { it.toFloatOrNull() }.toFloatArray()
        } else {
            floatArrayOf(1.22f, -0.16f, -0.06f, -0.08f, 1.15f, -0.07f, -0.03f, -0.12f, 1.15f)
        }

        return CameraCalibrationProfile(
            colorCorrectionMatrix = if (ctm.size == 9) ctm else floatArrayOf(1.22f, -0.16f, -0.06f, -0.08f, 1.15f, -0.07f, -0.03f, -0.12f, 1.15f),
            inverseGamma = invGamma,
            ambientFlareOffset = flare,
            rectLeft = l,
            rectTop = t,
            rectRight = r,
            rectBottom = b,
            fidelityPercentage = fidelity,
            opticalCertainty = certainty
        )
    }

    fun clearProfile(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun applyProfileToPhoto(cameraPhoto: Bitmap, profile: CameraCalibrationProfile): Bitmap {
        val rect = NormalizedRect(profile.rectLeft, profile.rectTop, profile.rectRight, profile.rectBottom)
        val report = ScreenBufferAnalysisReport(
            fidelityPercentage = profile.fidelityPercentage,
            opticalCertainty = profile.opticalCertainty,
            estimatedGamma = 1.0f / profile.inverseGamma.coerceAtLeast(0.01f),
            inverseGamma = profile.inverseGamma,
            ambientFlareOffset = profile.ambientFlareOffset,
            contrastRatio = 12.0f,
            moireInterferenceScore = 0.2f,
            colorCorrectionMatrix = profile.colorCorrectionMatrix,
            factors = emptyList()
        )
        return PixelEngine.simulateReconstructedBuffer(
            cameraPhoto = cameraPhoto,
            normalizedRect = rect,
            report = report,
            targetWidth = cameraPhoto.width,
            targetHeight = cameraPhoto.height
        )
    }
}
