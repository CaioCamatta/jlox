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

    Scanner(String source){
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()){
            // we are at teh beginning of the next lexeme
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }
}
