package com.camatta.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.camatta.lox.TokenType.*;

public class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0; // first character in the lexeme beign considered
    private int current = 0; // current character in the lexeme beign considered
    private int line = 1; // tracks

    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            // we are at the beginning of the next lexeme
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(':
                addToken(LEFT_PAREN);
                break;
            case ')':
                addToken(RIGHT_PAREN);
                break;
            case '{':
                addToken(LEFT_BRACE);
                break;
            case '}':
                addToken(RIGHT_BRACE);
                break;
            case ',':
                addToken(COMMA);
                break;
            case '.':
                addToken(DOT);
                break;
            case '-':
                addToken(MINUS);
                break;
            case '+':
                addToken(PLUS);
                break;
            case ';':
                addToken(SEMICOLON);
                break;
            case '*':
                addToken(STAR);
                break;
            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;
            case '/':
                if (match('/')) {
                    // A comments goes until end of line
                    while (peek() != '\n' && !isAtEnd())
                        advance();
                } else {
                    addToken(SLASH);
                }
            case ' ':
            case '\r':
            case '\t':
                break;
            case '\n':
                line++;
                break;
            case '"':
                string();
                break;
            default:
                if (isDigit(c)) {
                    number();
                } else {
                    Lox.error(line, "Unexpected character.");
                }
                break;
        }
    }

    private void number() {
        while (isDigit(peek()))
            advance();

        // Look for fraction
        if (peek() == '.' && isDigit(peekNext())) {
            advance(); // consume "."

            while (isDigit(peek()))
                advance();
        }

        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n')
                line++;
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.");
            return;
        }

        advance(); // closing ".

        String value = source.substring(start + 1, current - 1); // trim the actual "s
        addToken(STRING, value);
    }

    private boolean match(char expected) {
        // Similar to a conditional advance()
        if (isAtEnd())
            return false;
        if (source.charAt(current) != expected)
            return false;
        // Only consume character if it is what we're looking for
        current++;
        return true;
    }

    // Look at current unconsumed character
    private char peek() {
        if (isAtEnd())
            return '\0';
        return source.charAt(current);
    }

    // Second character of lookahead
    private char peekNext() {
        if (current + 1 >= source.length())
            return '\0';
        return source.charAt(current + 1);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        return source.charAt(current++);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    // Tokens with literals, like (STRING, "turkey")
    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
}
