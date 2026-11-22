package com.pricewatch.web;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Locale;

/** Parses only decimal arithmetic. Tool arguments are never evaluated as code. */
final class AskArithmetic {
    private final String input;
    private int position;

    private AskArithmetic(String input) {
        this.input = input.toLowerCase(Locale.ROOT).replace('×', '*').replace('÷', '/').replace('−', '-');
    }

    static BigDecimal calculate(String expression) {
        if (expression.isBlank() || expression.length() > 120) throw invalid();
        var parser = new AskArithmetic(expression);
        try {
            BigDecimal value = parser.sum();
            parser.skipSpaces();
            if (parser.position != parser.input.length()) throw invalid();
            return value.stripTrailingZeros();
        } catch (ArithmeticException ignored) {
            throw new IllegalArgumentException("Division by zero.");
        }
    }

    private BigDecimal sum() {
        BigDecimal value = product();
        while (true) {
            if (take("+")) value = value.add(product());
            else if (take("-")) value = value.subtract(product());
            else return value;
        }
    }

    private BigDecimal product() {
        BigDecimal value = operand();
        while (true) {
            if (take("*") || take("of")) value = value.multiply(operand());
            else if (take("/")) value = value.divide(operand(), MathContext.DECIMAL128);
            else return value;
        }
    }

    private BigDecimal operand() {
        if (take("-")) return operand().negate();
        if (take("+")) return operand();
        BigDecimal value;
        if (take("(")) {
            value = sum();
            if (!take(")")) throw invalid();
        } else {
            int start = position;
            while (position < input.length()) {
                char ch = input.charAt(position);
                if ((ch < '0' || ch > '9') && ch != '.') break;
                position++;
            }
            try {
                value = new BigDecimal(input.substring(start, position));
            } catch (NumberFormatException ignored) {
                throw invalid();
            }
        }
        return take("%") ? value.movePointLeft(2) : value;
    }

    private boolean take(String token) {
        skipSpaces();
        if (!input.startsWith(token, position)) return false;
        position += token.length();
        return true;
    }

    private void skipSpaces() {
        while (position < input.length() && Character.isWhitespace(input.charAt(position))) position++;
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("Use decimal numbers, arithmetic, parentheses, or percentages.");
    }
}
