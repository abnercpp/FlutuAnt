package com.fatecsp.abnerferreira.flutuant

import java.math.BigDecimal
import java.math.BigInteger
import java.text.DecimalFormatSymbols

private const val F32_qNaN_BITS = 0x7FC00000u
private const val F32_MANTISSA_MASK = 0x7FFFFFu
private const val F32_SIGN_BIT_MASK = 0x80000000u
private const val F32_SIGN_BIT_INDEX = Float.SIZE_BITS - 1
private const val F32_EXPONENT_BIT_LEN = 23
private const val F32_EXPONENT_BIAS = 127

@JvmInline
value class AdHocFloat32(val bits: UInt) {
    companion object {
        @JvmStatic
        private val qNaN = AdHocFloat32(F32_qNaN_BITS)

        // `BigDecimal.TWO` is not supported on Android
        @JvmStatic
        private val BIG_TWO = BigInteger.TWO.toBigDecimal()

        @JvmStatic
        fun fromText(
            text: String, symbols: DecimalFormatSymbols = DecimalFormatSymbols.getInstance()
        ): AdHocFloat32 {
            val trimmedText = text.trim()
            val absDecimal = runCatching { BigDecimal(trimmedText).abs() }.getOrNull()

            if (absDecimal == null) {
                return qNaN // Should not happen.
            }

            val sign = if (trimmedText.startsWith(symbols.minusSign, ignoreCase = true)) 1u else 0u
            val signBit = sign shl F32_SIGN_BIT_INDEX

            if (absDecimal.stripTrailingZeros() == BigDecimal.ZERO) {
                return AdHocFloat32(sign shl F32_SIGN_BIT_INDEX)
            }

            val whole = absDecimal.toBigInteger()
            var fraction = absDecimal % BigDecimal.ONE

            var fractionBits = 0u
            var fractionBitLen = 0

            while (fractionBitLen < Float.SIZE_BITS && fraction.stripTrailingZeros() != BigDecimal.ZERO) {
                fractionBitLen++
                val mul = fraction * BIG_TWO
                fraction = mul % BigDecimal.ONE
                val currentFractionBit = mul.toInt().toUInt() shl (Float.SIZE_BITS - fractionBitLen)
                fractionBits = fractionBits or currentFractionBit
            }

            fractionBits = fractionBits shr Float.SIZE_BITS - fractionBitLen

            val normalizedBits =
                (whole shl fractionBitLen) or fractionBits.toULong().toLong().toBigInteger()
            var mantissaBitLen = normalizedBits.bitLength() - 1

            val exponent = mantissaBitLen - fractionBitLen
            val exponentBits = (exponent + F32_EXPONENT_BIAS).toUInt()

            var mantissa =
                normalizedBits xor (BigInteger.ONE shl (mantissaBitLen)) // Remove implicit one.

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
            val f32Bits = signBit or (exponentBits shl F32_EXPONENT_BIT_LEN) or mantissaBits
            return AdHocFloat32(f32Bits)
        }
    }
}

val AdHocFloat32.sign
    get() = (bits shr F32_SIGN_BIT_INDEX).toInt()

val AdHocFloat32.biasedExponent
    get() = ((bits and F32_SIGN_BIT_MASK.inv()) shr F32_EXPONENT_BIT_LEN).toInt()

val AdHocFloat32.exponent
    get() = biasedExponent - F32_EXPONENT_BIAS

val AdHocFloat32.mantissa
    get() = (bits and F32_MANTISSA_MASK).toInt()

val AdHocFloat32.stdFloat
    get() = Float.fromBits(bits.toInt())
