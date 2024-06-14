package com.fatecsp.abnerferreira.flutuant

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import kotlin.math.max

private const val F32_qNaN_BITS = 0x7FC00000u
private const val F32_INFINITY_BITS = 0x7F800000u
private const val F32_MANTISSA_MASK = 0x7FFFFFu
private const val F32_SIGN_BIT_MASK = 0x80000000u
private const val F32_SIGN_BIT_INDEX = Float.SIZE_BITS - 1
private const val F32_EXPONENT_BIT_LEN = 23
private const val F32_EXPONENT_BIAS = 127
private const val F32_EXPONENT_AREA = F32_EXPONENT_BIAS * 2
private const val F32_EXPONENT_SUBNORMAL_OR_ZERO = -126
private const val F32_NORMAL_SCALE_MAX = 45

// `BigDecimal.TWO` is not supported on Android
private val BIG_TWO = 2.toBigDecimal()

private val F32_NORMAL_MIN = BigDecimal(1.1754943508222875E-38)
private val F32_SUBNORMAL_MAX = BigDecimal(1.1754942106924411E-38)
private val F32_SPLIT = (F32_NORMAL_MIN + F32_SUBNORMAL_MAX).divide(BIG_TWO, RoundingMode.HALF_EVEN)

@JvmInline
value class AdHocFloat32(val bits: UInt) {
    companion object {
        @JvmStatic
        private val qNaN = AdHocFloat32(F32_qNaN_BITS)

        @JvmStatic
        fun fromText(
            text: String,
            symbols: DecimalFormatSymbols = DecimalFormatSymbols.getInstance()
        ): AdHocFloat32 {
            // We need to replace the local-specific decimal separator with the standard one.
            val normalizedText =
                text.trim()
                    .replace(symbols.decimalSeparator, '.')
                    .replace(symbols.minusSign, '-')

            val absDecimal = runCatching { BigDecimal(normalizedText).abs() }.getOrNull()

            if (absDecimal == null) {
                return qNaN
            }

            val sign = if (normalizedText.startsWith('-')) 1u else 0u
            val okSignBit = sign shl F32_SIGN_BIT_INDEX

            if (absDecimal.stripTrailingZeros() == BigDecimal.ZERO) {
                return AdHocFloat32(okSignBit)
            }

            val whole = absDecimal.toBigInteger()
            var fraction = absDecimal % BigDecimal.ONE
            val isNormalFraction = fraction.isNormalF32()
            if (isNormalFraction) {
                fraction = fraction.setScale(F32_NORMAL_SCALE_MAX, RoundingMode.HALF_EVEN)
            }
            var remainder = fraction
            var fractionBits = BigInteger.ZERO
            var fractionBitLen = 0

            while (fractionBitLen < F32_EXPONENT_AREA
                && remainder.stripTrailingZeros() != BigDecimal.ZERO
            ) {
                val mul = remainder * BIG_TWO
                remainder = mul % BigDecimal.ONE
                val currentFractionBit = mul.toInt().toUInt().toLong().toBigInteger()
                fractionBitLen++
                val shiftedBit = currentFractionBit shl F32_EXPONENT_AREA - fractionBitLen
                fractionBits = fractionBits or shiftedBit
            }

            fractionBits = fractionBits shr (F32_EXPONENT_AREA - fractionBitLen)

            if (isNormalFraction &&
                fractionBitLen - fractionBits.bitLength() == F32_EXPONENT_BIAS - 1
            ) {
                fractionBitLen = F32_EXPONENT_BIAS - 1
                fractionBits = BigInteger.ONE
            }

            val normalizedBits = (whole shl fractionBitLen) or fractionBits

            if (normalizedBits == BigInteger.ZERO) {
                // We ran out of patience. And space. Mostly space.
                return AdHocFloat32(okSignBit)
            }

            var mantissaBitLen = normalizedBits.bitLength()

            val exponent = mantissaBitLen - fractionBitLen - 1
            if (exponent > F32_EXPONENT_BIAS) {
                return AdHocFloat32(okSignBit or F32_INFINITY_BITS) // Overflow.
            }
            var biasedExponentBits = (exponent + F32_EXPONENT_BIAS).toUInt()

            var mantissa =
                // Remove implicit one for normal numbers.
                if (exponent >= F32_EXPONENT_SUBNORMAL_OR_ZERO ||
                    exponent == F32_EXPONENT_SUBNORMAL_OR_ZERO - 1 && isNormalFraction
                ) {
                    mantissaBitLen -= 1
                    normalizedBits xor (BigInteger.ONE shl mantissaBitLen)
                } else {
                    // Add implicit zero for subnormal numbers.
                    biasedExponentBits = 0u
                    val leadingZeros = F32_EXPONENT_SUBNORMAL_OR_ZERO - exponent - 1
                    val mantissaWithImplicitZero = normalizedBits shr leadingZeros
                    mantissaBitLen = mantissaWithImplicitZero.bitLength() + leadingZeros
                    mantissaWithImplicitZero
                }

            if (mantissaBitLen < F32_EXPONENT_BIT_LEN) {
                mantissa = mantissa shl (F32_EXPONENT_BIT_LEN - mantissaBitLen)
            } else if (mantissaBitLen > F32_EXPONENT_BIT_LEN) {
                val offsetTilRoundingBit = mantissaBitLen - F32_EXPONENT_BIT_LEN - 1
                val mantissaWithRoundingBit = mantissa shr offsetTilRoundingBit
                val roundingBit = mantissaWithRoundingBit and BigInteger.ONE
                mantissa = mantissaWithRoundingBit shr 1 // Truncate at the 23rd bit.

                if (roundingBit == BigInteger.ONE) {
                    mantissa += BigInteger.ONE
                    mantissaBitLen = mantissa.bitLength()
                    if (mantissaBitLen > F32_EXPONENT_BIT_LEN) {
                        mantissa = mantissa shr 1 // Truncate at the 23rd bit.
                    }
                }
            }

            val mantissaBits = mantissa.toInt().toUInt()
            val f32Bits = okSignBit or (biasedExponentBits shl F32_EXPONENT_BIT_LEN) or mantissaBits
            return AdHocFloat32(f32Bits)
        }
    }
}

val AdHocFloat32.sign
    get() = (bits shr F32_SIGN_BIT_INDEX).toInt()

val AdHocFloat32.biasedExponent
    get() = ((bits and F32_SIGN_BIT_MASK.inv()) shr F32_EXPONENT_BIT_LEN).toInt()

val AdHocFloat32.exponent
    get() = max(biasedExponent - F32_EXPONENT_BIAS, F32_EXPONENT_SUBNORMAL_OR_ZERO)

val AdHocFloat32.mantissa
    get() = (bits and F32_MANTISSA_MASK).toInt()

val AdHocFloat32.stdFloat
    get() = Float.fromBits(bits.toInt())

private fun BigDecimal.isNormalF32(): Boolean = this >= F32_SPLIT
