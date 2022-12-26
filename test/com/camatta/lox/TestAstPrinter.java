package com.camatta.lox;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestAstPrinter {
    @Test
    public void testAstPrinter() {
        Expr expression = new Expr.Binary(
                new Expr.Unary(
                        new Token(TokenType.MINUS, "-", null, 1),
                        new Expr.Literal(123)),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Grouping(
                        new Expr.Literal(45.67)));

        String output = new AstPrinter().print(expression);

        assertEquals("(* (- 123) (group 45.67))", output);
    }
}
