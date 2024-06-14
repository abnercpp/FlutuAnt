package com.fatecsp.abnerferreira.flutuant

import java.math.BigDecimal
import java.math.BigInteger
import java.text.DecimalFormatSymbols

private const val F32_qNaN_BITS = 0x7FC00000u
private const val F32_INFINITY_BITS = 0x7F800000u
private const val F32_MANTISSA_MASK = 0x7FFFFFu
private const val F32_SIGN_BIT_MASK = 0x80000000u
private const val F32_SIGN_BIT_INDEX = Float.SIZE_BITS - 1
private const val F32_EXPONENT_BIT_LEN = 23
private const val F32_EXPONENT_BIAS = 127
private const val F32_PADDING_BITS = ULong.SIZE_BITS // Needs to be larger than 32 bits!

@JvmInline
value class AdHocFloat32(val bits: UInt) {
    companion object {
        @JvmStatic
        private val qNaN = AdHocFloat32(F32_qNaN_BITS)

        // `BigDecimal.TWO` is not supported on Android
        @JvmStatic
        private val BIG_TWO = BigDecimal("2")

        @JvmStatic
        fun fromText(
            text: String, symbols: DecimalFormatSymbols = DecimalFormatSymbols.getInstance()
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

            val sign = if (normalizedText.startsWith('-', ignoreCase = true)) 1u else 0u
            val shiftedSignBit = sign shl F32_SIGN_BIT_INDEX

            if (absDecimal.stripTrailingZeros() == BigDecimal.ZERO) {
                return AdHocFloat32(shiftedSignBit)
            }

            val whole = absDecimal.toBigInteger()
            var fraction = absDecimal % BigDecimal.ONE

            var fractionBits = 0uL
            var fractionBitLen = 0

            while (fractionBitLen < F32_PADDING_BITS
                && fraction.stripTrailingZeros() != BigDecimal.ZERO
            ) {
                val mul = fraction * BIG_TWO
                fraction = mul % BigDecimal.ONE
                val currentFractionBit = mul.toInt().toUInt()
                fractionBitLen++
                val shiftedBit = currentFractionBit.toULong() shl F32_PADDING_BITS - fractionBitLen
                fractionBits = fractionBits or shiftedBit
            }

            fractionBits = fractionBits shr (F32_PADDING_BITS - fractionBitLen)

            val normalizedBits = (whole shl fractionBitLen) or fractionBits.toLong().toBigInteger()

            if (normalizedBits == BigInteger.ZERO) {
                // Truncate after 44 leading zeros. We ran out of patience. And space. Mostly space.
                return AdHocFloat32(shiftedSignBit)
            }

            var mantissaBitLen = normalizedBits.bitLength() - 1

            val exponent = mantissaBitLen - fractionBitLen
            if (exponent > F32_EXPONENT_BIAS) {
                return AdHocFloat32(shiftedSignBit or F32_INFINITY_BITS) // Overflow.
            }
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
            val f32Bits = shiftedSignBit or (exponentBits shl F32_EXPONENT_BIT_LEN) or mantissaBits
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
