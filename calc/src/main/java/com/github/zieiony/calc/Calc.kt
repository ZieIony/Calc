package com.github.zieiony.calc

import java.text.DecimalFormat

enum class DelimiterType {
    ADDSUB, MUL, DIVMOD, POW
}

enum class BraceType {
    OPENING, CLOSING
}

sealed class Token
object StartOfExpression : Token()
class Delimiter(val type: DelimiterType, var action: (Double, Double) -> Double) : Token()
class Brace(val type: BraceType) : Token()
class Number(val number: Double) : Token()
object EndOfExpression : Token()
class Function(var action: (Double) -> Double) : Token()

sealed class CalcException(s: String) : RuntimeException(s)
object MissingBracketException : CalcException("missing bracket")
object EndOfExpressionException : CalcException("end of exception")
object NotANumberException : CalcException("not a number")
class UnknownFunctionException(s: String) : CalcException(s)
object UnknownTokenException : CalcException("unknown token")
object DivisionByZeroException : CalcException("division by zero")

class Calc {
    private var i = 0
    private lateinit var token: Token
    private lateinit var expr: String

    private var delimiterMap = mapOf(
            '+' to Delimiter(DelimiterType.ADDSUB, { a: Double, b: Double -> a + b }),
            '-' to Delimiter(DelimiterType.ADDSUB, { a: Double, b: Double -> a - b }),
            '*' to Delimiter(DelimiterType.MUL, { a: Double, b: Double -> a * b }),
            '/' to Delimiter(DelimiterType.DIVMOD, { a: Double, b: Double -> a / b }),
            '%' to Delimiter(DelimiterType.DIVMOD, { a: Double, b: Double -> a % b }),
            '^' to Delimiter(DelimiterType.POW, { a: Double, b: Double -> Math.pow(a, b) }),
            '(' to Brace(BraceType.OPENING),
            ')' to Brace(BraceType.CLOSING)
    )
    private var functionMap = mapOf(
            "sin" to { a: Double -> Math.sin(a / 180 * Math.PI) },
            "cos" to { a -> Math.sin(a / 180 * Math.PI) },
            "tan" to { a -> Math.sin(a / 180 * Math.PI) }
    )

    var decimalFormat = DecimalFormat()

    fun evaluate(e: String): Double {
        expr = e
        i = 0
        token = StartOfExpression
        nextToken()
        return evaluateAdd()
    }

    private fun evaluateAdd(): Double {
        var result = evaluateMul()
        while (true) {
            val t = token
            if (t is Delimiter && t.type == DelimiterType.ADDSUB) {
                nextToken()
                val result2 = evaluateMul()
                result = t.action(result, result2)
            } else {
                break
            }
        }
        return result
    }

    private fun evaluateMul(): Double {
        var result = evaluatePow()
        while (true) {
            val t = token
            if (t is Delimiter && (t.type == DelimiterType.MUL || t.type == DelimiterType.DIVMOD)) {
                nextToken()
                val result2 = evaluatePow()
                if (result2 == 0.0 && t.type == DelimiterType.DIVMOD)
                    throw DivisionByZeroException
                result = t.action(result, result2)
            } else {
                break
            }
        }
        return result
    }

    private fun evaluatePow(): Double {
        var result = evaluateSign()
        while (true) {
            val t = token
            if (t is Delimiter && t.type == DelimiterType.POW) {
                nextToken()
                val result2 = evaluateSign()
                result = t.action(result, result2)
            } else {
                break
            }
        }
        return result
    }

    private fun evaluateSign(): Double {
        val t = token
        if (t is Delimiter && t.type == DelimiterType.ADDSUB) {
            nextToken()
            val result = evaluateFunction()
            return t.action(0.0, result)
        }
        return evaluateFunction()
    }

    private fun evaluateFunction(): Double {
        val t = token
        if (t is Function) {
            nextToken()
            var result = evaluateBraces()
            result = t.action(result)
            return result
        }
        return evaluateBraces()
    }

    private fun evaluateBraces(): Double {
        val result: Double
        var t = token
        if (t is Brace && t.type == BraceType.OPENING) {
            nextToken()
            result = evaluateAdd()
            t = token
            if (t !is Brace || t.type != BraceType.CLOSING)
                throw MissingBracketException
            nextToken()
        } else {
            result = evaluateValue()
        }
        return result
    }

    private fun evaluateValue(): Double {
        val t = token
        if (t is Number) {
            val result = t.number
            nextToken()
            return result
        } else if (t is EndOfExpression) {
            throw EndOfExpressionException
        } else if (t is Brace && t.type == BraceType.CLOSING) {
            throw MissingBracketException
        }
        throw NotANumberException
    }

    private fun nextToken() {
        if (token is EndOfExpression)
            throw EndOfExpressionException

        while (true) {
            if (i == expr.length) {
                token = EndOfExpression
                return
            } else if (Character.isWhitespace(expr[i])) {
                i++
            } else {
                break
            }
        }

        if (delimiterMap.containsKey(expr[i])) {
            token = delimiterMap.getValue(expr[i])
            i++
            return
        } else if (Character.isDigit(expr[i])) {
            val start = i
            while (i < expr.length && (Character.isDigit(expr[i]) || expr[i] == decimalFormat.decimalFormatSymbols.decimalSeparator)) i++
            val number = decimalFormat.parse(expr.substring(start, i)).toDouble()
            token = Number(number)
            return
        } else if (Character.isLetter(expr[i])) {
            val start = i
            while (i < expr.length && (Character.isLetter(expr[i]))) i++
            val functionName = expr.substring(start, i)
            if (functionMap.containsKey(functionName)) {
                token = Function(functionMap.getValue(functionName))
                return
            } else {
                throw UnknownFunctionException("unknown function: $functionName")
            }
        }

        throw UnknownTokenException
    }
}
