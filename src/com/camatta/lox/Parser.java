package com.camatta.lox;

import java.util.ArrayList;
import java.util.List;

import static com.camatta.lox.TokenType.*;

class Parser {
    private static class ParseError extends RuntimeException {
    }

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        // Parse statements until EOF. This is the program rule
        // program -> declaration* EOF ;
        while (!isAtEnd()) {
            statements.add(declaration());
        }

        return statements;
    }

    private Expr expression() {
        return assignment();
    }

    private Stmt declaration() {
        try {
            if (match(VAR))
                return varDeclaration();

            return statement();
        } catch (ParseError error) {
            // This method is repeatedly called when parsing a series of statements, so it's
            // a good place to synchronize when the parser goes into panic mode
            synchronize();
            return null;
        }
    }

    private Stmt statement() {
        if (match(PRINT))
            return printStatement();
        if (match(LEFT_BRACE))
            return new Stmt.Block(block());

        return expressionStatement();
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expected ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt varDeclaration() {
        // At this point, the parser has already parsed VAR, so next we should have an
        // identifier with the variable name
        Token name = consume(IDENTIFIER, "Expected variable name.");

        // Initializers are optional. If nothing is provided we initialize with null.
        // Otherwise, we expect an expression
        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expected ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expected '}' after block.");
        return statements;
    }

    private Expr assignment() {
        // Parse some expression on the left
        Expr expr = equality();

        // If we find an equals,
        if (match(EQUAL)) {
            Token equals = previous();

            // Assignment is right-associative, so recursively call assignment() to parse
            // the right-hand side
            Expr value = assignment();

            // look at the left-hand sign to figure out what kind of assignment target it
            // is.
            if (expr instanceof Expr.Variable) {
                // Convert the r-value expression node into an l-value representation
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    // If equality were left recursive, the recursive descent parser
    // would call itself immediately and repeatedly, leading to a stack overflow.
    private Expr equality() {
        Expr expr = comparison();

        // The rule only repeats if we have an != or a ==. SO, if we see something
        // different we exit.
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() {
        Expr expr = term();

        // Very similar to equality, a left-associative series of binary operators
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        // Look at current operator. If we have a ! or -, it must be a unary expression.
        // In that case, grab the operator and recursively call unary().
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary(); // Recursively call unary to get operand. This will result in a call to
                                  // primary().
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    // Highest level of precedence
    private Expr primary() {
        if (match(FALSE))
            return new Expr.Literal(false);
        if (match(TRUE))
            return new Expr.Literal(true);
        if (match(NIL))
            return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        // This allows using a variable!
        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expected '(' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expected expression.");
    }

    /**
     * Check if the current token has any of the given types.
     * If so, consume the token.
     */
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    /*
     * Check if next token is of expected type and consume if so. Otherwise, throw
     * an error
     */
    private Token consume(TokenType type, String message) {
        if (check(type))
            return advance();
        throw error(peek(), message);
    }

    /* Check if current token is of given type without consuming. */
    private boolean check(TokenType type) {
        if (isAtEnd())
            return false;
        return peek().type == type;
    }

    /* Consume current token and return it */
    private Token advance() {
        if (!isAtEnd())
            current++;
        return previous();
    }

    /* Check if current, unconsumed token is EOF */
    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    /* Peek current, unconsumed token */
    private Token peek() {
        return tokens.get(current);
    }

    /* Return most recently consumed token */
    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        // Return the error instead of throwing it so we can let the calling method
        // inside the parser decide whether to unwind or not.
        return new ParseError();
    }

    /**
     * Recover from an error by clearing out the call frames and resetting the state
     * back to the start of the next statement. ("Panic mode")
     */
    private void synchronize() {
        advance();

        // Discard tokens until we find a statement boundary.
        while (!isAtEnd()) {
            // Most obvious option is a semicolon
            if (previous().type == SEMICOLON)
                return;

            // Other keywordalso denote start of another statement
            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }
    }
}