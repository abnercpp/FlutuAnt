package com.fatecsp.abnerferreira.flutuant

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.text.DecimalFormatSymbols
import java.util.Locale

private val SYMBOLS = DecimalFormatSymbols.getInstance(Locale.US) // Guarantee reproducibility.

@RunWith(Parameterized::class)
class AdHocFloat32Test(
    private val decimalText: String,
    private val expectedFloat: Float,
    private val expectedSign: Int,
    private val expectedMantissa: Int
) {
    private val sut
        get() = AdHocFloat32.fromText(decimalText, SYMBOLS)

    private val expectedExponent
        get() = Math.getExponent(expectedFloat)

    @Test
    fun f32Bits_areCorrect() = assertEquals(expectedFloat.toBits(), sut.stdFloat.toBits())

    @Test
    fun sign_isCorrect() = assertEquals(expectedSign, sut.sign)

    @Test
    fun exponent_isCorrect() = assertEquals(expectedExponent, sut.exponent)

    @Test
    fun mantissa_isCorrect() = assertEquals(expectedMantissa, sut.mantissa)

    companion object {
        @JvmStatic
        @Parameters
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf("CHINELO HAVAIANAS", Float.NaN, 0, 0x400000),
            arrayOf("-1", Float.fromBits((0xBF800000).toInt()), 1, 0),
            arrayOf("-0", Float.fromBits((0x80000000u).toInt()), 1, 0),
            arrayOf("0", Float.fromBits(0), 0, 0),
            arrayOf(
                "0.00000000000000000000000000000000000009403954",
                Float.fromBits(0x01ffffff),
                0,
                0x7FFFFF
            ),
            arrayOf("0.003", Float.fromBits(0x3B449BA6), 0, 0x449BA6),
            arrayOf("0.12678", Float.fromBits(0x3E01D29E), 0, 0x1D29E),
            arrayOf("0.3", Float.fromBits(0x3E99999A), 0, 0x19999A),
            arrayOf("0.5", Float.fromBits(0x3F000000), 0, 0),
            arrayOf("1", Float.fromBits(0x3F800000), 0, 0),
            arrayOf("1.3", Float.fromBits(0x3FA66666), 0, 0x266666),
            arrayOf("1.5", Float.fromBits(0x3FC00000), 0, 0x400000),
            arrayOf("12.375", Float.fromBits(0x41460000), 0, 0x460000),
            arrayOf("612.3", Float.fromBits(0x44191333), 0, 0x191333),
            arrayOf("109328139.101230812390812", Float.fromBits(0x4CD086E1), 0, 0x5086E1),
            arrayOf("999999999999999999999999999999999999991", Float.POSITIVE_INFINITY, 0, 0),
            arrayOf("-999999999999999999999999999999999999991", Float.NEGATIVE_INFINITY, 1, 0)
        )
    }
}