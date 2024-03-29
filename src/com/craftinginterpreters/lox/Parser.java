package com.craftinginterpreters.lox;

import static com.craftinginterpreters.lox.TokenType.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
            if (match(CLASS))
                return classDeclaration();
            if (match(FUN))
                return function("function");
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

    private Stmt classDeclaration() {
        Token name = consume(IDENTIFIER, "Expected class name.");

        Expr.Variable superclass = null;
        if (match(LESS)) {
            consume(IDENTIFIER, "Expected superclass name.");
            superclass = new Expr.Variable(previous());
        }

        consume(LEFT_BRACE, "Expected '{' before class body.");

        List<Stmt.Function> methods = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            methods.add(function("method"));
        }

        consume(RIGHT_BRACE, "Expected '}' after class body.");

        return new Stmt.Class(name, superclass, methods);
    }

    private Stmt statement() {
        if (match(FOR))
            return forStatement();
        if (match(IF))
            return ifStatement();
        if (match(PRINT))
            return printStatement();
        if (match(RETURN))
            return returnStatement();
        if (match(WHILE))
            return whileStatement();
        if (match(LEFT_BRACE))
            return new Stmt.Block(block());

        return expressionStatement();
    }

    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expected '(' after 'for'.");

        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expected ';' after loop condition.");

        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expected ')' after for clauses.");

        Stmt body = statement();

        // "Desugar" a for loop into a while loop

        // If there is an increment, execute it after the body in each iteration
        if (increment != null) {
            body = new Stmt.Block(
                    Arrays.asList(
                            body,
                            new Stmt.Expression(increment)));
        }

        // If there's no condition, make it an infinite loop
        if (condition == null)
            condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        // If there is an initializer, run it once before the loop.
        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expected '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expected ')' after 'if'.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        // The 'else' is bound to the nearest 'if' that precedes it
        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expected ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        // If there is a value, we parse it. Otherwise return nil.
        if (!check(SEMICOLON)) {
            value = expression();
        }

        consume(SEMICOLON, "Expected ';' after return value.");
        return new Stmt.Return(keyword, value);
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

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expected '( after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expected ') after 'while'.");
        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    /**
     * Function parser. The "kind" parameter differentiates functions and methods.
     */
    private Stmt.Function function(String kind) {
        // Parse name
        Token name = consume(IDENTIFIER, "Expected " + kind + " name.");

        // Parse parameters
        consume(LEFT_PAREN, "Expected '(' after " + kind + "name.");
        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }

                parameters.add(
                        consume(IDENTIFIER, "Expected parameter name."));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expected ')' after parameters.");

        // Parse body
        consume(LEFT_BRACE, "Expected '{' before " + kind + " body.");
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
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
        Expr expr = or();

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
            } else if (expr instanceof Expr.Get) { // Properties
                Expr.Get get = (Expr.Get) expr;
                return new Expr.Set(get.object, get.name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr or() {
        Expr expr = and();

        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    /* AND has higher precedence than OR. */
    private Expr and() {
        Expr expr = equality();

        while (match(AND)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
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

        return call();
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();

        // Parse arguments if there are any
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    // We don't need to go into panic mode
                    error(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(expression());
            } while (match(COMMA));
        }

        Token paren = consume(RIGHT_PAREN, "Expected ')' after arguments.");

        return new Expr.Call(callee, paren, arguments);
    }

    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (match(DOT)) {
                Token name = consume(IDENTIFIER, "Expected property name after '.'.");
                expr = new Expr.Get(expr, name);
            } else {
                break;
            }
        }

        return expr;
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

        if (match(SUPER)) {
            Token keyword = previous();
            consume(DOT, "Expected '.' after 'super.");
            Token method = consume(IDENTIFIER, "Expected superclass method name.");
            return new Expr.Super(keyword, method);
        }

        if (match(THIS))
            return new Expr.This(previous());

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

            // Other keyword also denotes the start of another statement
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
                default:
                    break;
            }

            advance();
        }
    }
}