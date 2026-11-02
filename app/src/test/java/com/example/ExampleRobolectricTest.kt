package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.PixelEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PixelPuro", appName)
    }

    @Test
    fun `two pixel hardware test creates exactly two pure pixels`() {
        val bitmap = PixelEngine.createTwoPixelBitmap()
        assertEquals(2, bitmap.width)
        assertEquals(1, bitmap.height)

        // Pixel 0: Pure Red
        val p0 = bitmap.getPixel(0, 0)
        assertEquals(255, (p0 ushr 24) and 0xFF)
        assertEquals(255, (p0 ushr 16) and 0xFF)
        assertEquals(0, (p0 ushr 8) and 0xFF)
        assertEquals(0, p0 and 0xFF)

        // Pixel 1: Pure Blue
        val p1 = bitmap.getPixel(1, 0)
        assertEquals(255, (p1 ushr 24) and 0xFF)
        assertEquals(0, (p1 ushr 16) and 0xFF)
        assertEquals(0, (p1 ushr 8) and 0xFF)
        assertEquals(255, p1 and 0xFF)
    }

    @Test
    fun `pixel integrity verification yields 100 percent for identical hardware buffer`() = runBlocking {
        val bitmap = PixelEngine.createTwoPixelBitmap()
        val copy = bitmap.copy(bitmap.config!!, false)
        val report = PixelEngine.verifyIntegrity(bitmap, copy)

        assertEquals(2, report.totalTestedPixels)
        assertEquals(2, report.identicalPixels)
        assertEquals(0, report.alteredPixels)
        assertTrue(report.isBitPerfect)
        assertEquals(100f, report.integrityPercentage, 0.001f)
    }

    @Test
    fun `test PixelEngine cropBitmap`() {
        val original = PixelEngine.createPalette16Bitmap()
        assertEquals(32, original.width)
        assertEquals(32, original.height)

        val cropped = PixelEngine.cropBitmap(original, 4, 4, 10, 12)
        assertEquals(10, cropped.width)
        assertEquals(12, cropped.height)
        assertEquals(original.getPixel(4, 4), cropped.getPixel(0, 0))
    }

    @Test
    fun `test PixelEngine calibrateSensorOffset deterministically`() {
        val original = PixelEngine.createTwoPixelBitmap()
        val calibrated = PixelEngine.calibrateSensorOffset(original, -10, 20, 5, 0)
        val p0 = calibrated.getPixel(0, 0)

        val r = (p0 ushr 16) and 0xFF
        val g = (p0 ushr 8) and 0xFF
        val b = p0 and 0xFF
        val a = (p0 ushr 24) and 0xFF

        assertEquals(245, r)
        assertEquals(20, g)
        assertEquals(5, b)
        assertEquals(255, a)
    }

    @Test
    fun `test analyzeScreenVsBuffer bounds fidelity strictly between 0 and 100 percent`() {
        val testPhoto = PixelEngine.createPalette16Bitmap()
        val rect = com.example.model.NormalizedRect(0.1f, 0.1f, 0.9f, 0.9f)
        val report = PixelEngine.analyzeScreenVsBuffer(testPhoto, rect)

        assertTrue("Fidelity must be > 0.0%", report.fidelityPercentage > 0.0f)
        assertTrue("Fidelity must be < 100.0%", report.fidelityPercentage < 100.0f)
        assertEquals(6, report.factors.size)
        assertTrue(report.factors.any { it.id == "HOMOGRAPHY" })
        assertTrue(report.factors.any { it.id == "GAMMA_EOTF" })
        assertTrue(report.factors.any { it.id == "CROSSTALK_CTM" })
        assertTrue(report.factors.any { it.id == "MOIRE_SUBPIXELS" })
        assertTrue(report.factors.any { it.id == "AMBIENT_FLARE" })
        assertTrue(report.factors.any { it.id == "ALPHA_32BIT" })
    }

    @Test
    fun `test simulateReconstructedBuffer produces pristine 32-bit RGBA bitmap`() {
        val testPhoto = PixelEngine.createPalette16Bitmap()
        val rect = com.example.model.NormalizedRect(0.1f, 0.1f, 0.9f, 0.9f)
        val report = PixelEngine.analyzeScreenVsBuffer(testPhoto, rect)
        val reconstructed = PixelEngine.simulateReconstructedBuffer(testPhoto, rect, report, 64, 64)

        assertEquals(64, reconstructed.width)
        assertEquals(64, reconstructed.height)
        val p = reconstructed.getPixel(10, 10)
        val alpha = (p ushr 24) and 0xFF
        assertEquals(255, alpha)
    }
}

