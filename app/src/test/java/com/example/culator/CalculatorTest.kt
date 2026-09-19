package com.example.culator

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class CalculatorTest {
    @Test fun arithmeticAndPrecedence() {
        assertEquals("14", evaluate("2+3×4")?.display())
        assertEquals("-3", evaluate("-8+10÷2")?.display())
        assertEquals("0.3", evaluate("0.1+0.2")?.display())
        assertEquals("2.5", evaluate("10÷4")?.display())
        assertEquals("5", evaluate("8-3")?.display())
        listOf("", "1÷0", "2+", ".", "1.2.3").forEach { assertNull(evaluate(it)) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun fractionalTotalsCannotBeSplitIntoWholeNumbers() {
        randomAverage("10.5".toBigDecimal(), 3)
    }

    @Test fun randomSharesAlwaysPreserveSumAndBounds() {
        for (input in listOf("100", "0", "-80", "1", "101", "-1", "999999999999999999999")) {
            val total = input.toBigDecimal()
            for (count in listOf(1, 2, 3, 7, 1000)) {
                for (range in listOf(1, 2, 20, 50, 100)) repeat(20) { seed ->
                    val values = randomAverage(total, count, Random(seed), offsetRange = range)
                    assertEquals(count, values.size)
                    assertTrue(values.all { it.stripTrailingZeros().scale() <= 0 })
                    assertEquals(0, total.compareTo(values.fold(BigDecimal.ZERO, BigDecimal::add)))
                    val mean = total.divide(count.toBigDecimal(), 20, RoundingMode.HALF_UP)
                    assertTrue(values.all { (it - mean).abs() <= range.toBigDecimal() })
                }
            }
        }
    }
}
