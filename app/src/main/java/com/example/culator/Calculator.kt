package com.example.culator

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.random.Random

internal fun BigDecimal.display(): String = stripTrailingZeros().toPlainString()

/** Recursive descent keeps normal multiplication/division precedence. */
internal fun evaluate(expression: String): BigDecimal? = runCatching {
    var position = 0
    fun number(): BigDecimal {
        var negative = false
        if (position < expression.length && expression[position] == '-') {
            negative = true
            position++
        }
        val start = position
        while (position < expression.length && (expression[position].isDigit() || expression[position] == '.')) position++
        require(position > start)
        val value = expression.substring(start, position).toBigDecimal()
        return if (negative) -value else value
    }
    fun term(): BigDecimal {
        var value = number()
        while (position < expression.length && expression[position] in "×÷") {
            val operator = expression[position++]
            val next = number()
            value = if (operator == '×') value.multiply(next) else value.divide(next, 12, RoundingMode.HALF_UP)
        }
        return value
    }
    var value = term()
    while (position < expression.length && expression[position] in "+-") {
        val operator = expression[position++]
        val next = term()
        value = if (operator == '+') value + next else value - next
    }
    require(position == expression.length)
    value
}.getOrNull()

/** Whole shares and balanced integer offsets preserve the exact total. */
internal fun randomAverage(total: BigDecimal, count: Int, random: Random = Random.Default, offsetRange: Int = 20): List<BigDecimal> {
    require(count in 1..1000)
    require(offsetRange in 1..100)
    require(total.stripTrailingZeros().scale() <= 0) { "Enter a whole-number total" }
    val unit = BigDecimal.ONE
    val base = total.divide(count.toBigDecimal(), 0, RoundingMode.FLOOR)
    val extra = total.subtract(base.multiply(count.toBigDecimal())).intValueExact()
    val values = MutableList(count) { base + if (it < extra) unit else BigDecimal.ZERO }
    for (index in 0 until count - 1 step 2) {
        // Leave one unit of rounding headroom to guarantee the configured bound.
        val offset = random.nextInt(1 - offsetRange, offsetRange).toBigDecimal()
        values[index] += offset
        values[index + 1] -= offset
    }
    return values.shuffled(random)
}
