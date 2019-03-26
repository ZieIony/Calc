package com.github.zieiony.calc

import java.text.DecimalFormat

enum class DelimiterType {
    ADDSUB, MULDIV, POW
}

enum class BraceType {
    OPENING, CLOSING
}

sealed class Token {
    object StartOfExpression : Token()
    class Delimiter(val type: DelimiterType, var action: (Double, Double) -> Double) : Token()
    class Brace(val type: BraceType) : Token()
    class Number(val number: Double) : Token()
    object EndOfExpression : Token()
    class Function(var action: (Double) -> Double) : Token()
}

sealed class CalcException(s: String) : RuntimeException(s)
class MissingBracketException : CalcException("missing bracket")
class EndOfExpressionException : CalcException("end of exception")
class NotANumberException : CalcException("not a number")
class UnknownFunctionException(s: String) : CalcException(s)
class UnknownTokenException : CalcException("unknown token")
class DivisionByZeroException : CalcException("division by zero")

class Calc {
    private var i = 0
    private lateinit var token: Token
    private lateinit var expr: String

    private var delimiterMap = mapOf(
            '+' to Token.Delimiter(DelimiterType.ADDSUB, { a: Double, b: Double -> a + b }),
            '-' to Token.Delimiter(DelimiterType.ADDSUB, { a: Double, b: Double -> a - b }),
            '*' to Token.Delimiter(DelimiterType.MULDIV, { a: Double, b: Double -> a * b }),
            '/' to Token.Delimiter(DelimiterType.MULDIV, { a: Double, b: Double -> a / b }),
            '%' to Token.Delimiter(DelimiterType.MULDIV, { a: Double, b: Double -> a % b }),
            '^' to Token.Delimiter(DelimiterType.POW, { a: Double, b: Double -> Math.pow(a, b) }),
            '(' to Token.Brace(BraceType.OPENING),
            ')' to Token.Brace(BraceType.CLOSING)
    )
    private var functionMap = mapOf(
            "sin" to { a: Double -> Math.sin(a / 180 * Math.PI) },
            "cos" to { a -> Math.sin(a / 180 * Math.PI) },
            "tan" to { a -> Math.sin(a / 180 * Math.PI) }
    )

    var decimalformat = DecimalFormat()

    fun evaluate(e: String): Double {
        expr = e
        i = 0
        token = Token.StartOfExpression
        nextToken()
        return evaluateAdd()
    }

    private fun evaluateAdd(): Double {
        var result = evaluateMul()
        while (true) {
            val t = token
            if (t is Token.Delimiter && t.type == DelimiterType.ADDSUB) {
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
            if (t is Token.Delimiter && t.type == DelimiterType.MULDIV) {
                nextToken()
                val result2 = evaluatePow()
                if (result2 == 0.0)
                    throw DivisionByZeroException()
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
            if (t is Token.Delimiter && t.type == DelimiterType.POW) {
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
        if (t is Token.Delimiter && t.type == DelimiterType.ADDSUB) {
            nextToken()
            val result = evaluateFunction()
            return t.action(0.0, result)
        }
        return evaluateFunction()
    }

    private fun evaluateFunction(): Double {
        val t = token
        if (t is Token.Function) {
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
        if (t is Token.Brace && t.type == BraceType.OPENING) {
            nextToken()
            result = evaluateAdd()
            t = token
            if (t !is Token.Brace || t.type != BraceType.CLOSING)
                throw MissingBracketException()
            nextToken()
        } else {
            result = evaluateValue()
        }
        return result
    }

    private fun evaluateValue(): Double {
        val t = token
        if (t is Token.Number) {
            val result = t.number
            nextToken()
            return result
        } else if (t is Token.EndOfExpression) {
            throw EndOfExpressionException()
        } else if (t is Token.Brace && t.type == BraceType.CLOSING) {
            throw MissingBracketException()
        }
        throw NotANumberException()
    }

    private fun nextToken() {
        if (token is Token.EndOfExpression)
            throw EndOfExpressionException()

        while (true) {
            if (i == expr.length) {
                token = Token.EndOfExpression
                return
            } else if (Character.isWhitespace(expr[i])) {
                i++
            } else {
                break
            }
        }

        if (delimiterMap.containsKey(expr[i])) {
            token = delimiterMap[expr[i]]!!
            i++
            return
        } else if (Character.isDigit(expr[i])) {
            val start = i
            while (i < expr.length && (Character.isDigit(expr[i]) || expr[i] == decimalformat.decimalFormatSymbols.decimalSeparator)) i++
            val number = decimalformat.parse(expr.substring(start, i)).toDouble()
            token = Token.Number(number)
            return
        } else if (Character.isLetter(expr[i])) {
            val start = i
            while (i < expr.length && (Character.isLetter(expr[i]))) i++
            val functionName = expr.substring(start, i)
            if (functionMap.containsKey(functionName)) {
                token = Token.Function(functionMap[functionName]!!)
                return
            } else {
                throw UnknownFunctionException("unknown function: $functionName")
            }
        }
        throw UnknownTokenException()
    }
}
