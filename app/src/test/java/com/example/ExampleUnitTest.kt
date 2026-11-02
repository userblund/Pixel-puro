package com.example

import com.example.model.PixelColor
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.sqrt

class ExampleUnitTest {
    @Test
    fun testPixelColorHexFormatting() {
        val pureRed = PixelColor(0xFFFF0000.toInt())
        assertEquals("#FFFF0000", pureRed.hex8Argb)
        assertEquals("#FF0000FF", pureRed.hex8Rgba)
        assertEquals(255, pureRed.r)
        assertEquals(0, pureRed.g)
        assertEquals(0, pureRed.b)
        assertEquals(255, pureRed.a)

        val pureGreen = PixelColor(0xFF00FF00.toInt())
        assertEquals("#FF00FF00", pureGreen.hex8Argb)
        assertEquals("#00FF00FF", pureGreen.hex8Rgba)
    }

    @Test
    fun testDeterministicRgbaDeltaCalculations() {
        val sensor = PixelColor(0xFF102030.toInt()) // r=16, g=32, b=48, a=255
        val targetR = 20
        val targetG = 30
        val targetB = 55
        val targetA = 255

        val deltaR = targetR - sensor.r
        val deltaG = targetG - sensor.g
        val deltaB = targetB - sensor.b
        val deltaA = targetA - sensor.a

        assertEquals(4, deltaR)
        assertEquals(-2, deltaG)
        assertEquals(7, deltaB)
        assertEquals(0, deltaA)

        val dist = sqrt((deltaR * deltaR + deltaG * deltaG + deltaB * deltaB + deltaA * deltaA).toDouble())
        assertEquals(8.306, dist, 0.01)
    }
}
