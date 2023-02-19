package com.craftinginterpreters.lox;

class Token {
    final TokenType type;
    final String lexeme; // A blob of characters
    final Object literal;
    final int line; // Note which line the token appears on (for error-handling). This could be more
                    // sophisticated

    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}
