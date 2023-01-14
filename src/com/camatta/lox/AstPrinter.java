package com.camatta.lox;

import java.util.Arrays;
import java.util.List;

/**
 * Prints a tree in as a Lisp-style string.
 * This class isn't strictly necessary, just a nice-to-have.
 */
class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {
    void print(List<Stmt> statements) {
        for (Stmt s : statements) {
            System.out.println("[parser] statement: " + s.accept(this));
        }
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null)
            return "nil";
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    @Override
    public String visitExpressionStmt(Stmt.Expression expr) {
        return parenthesize("expr", expr.expression);
    }

    @Override
    public String visitPrintStmt(Stmt.Print expr) {
        return parenthesize("print", expr.expression);
    }

    @Override
    public String visitVarStmt(Stmt.Var stmt) {
        return parenthesize("var " + stmt.name.lexeme, stmt.initializer);
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return parenthesize("var", expr);
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return parenthesize("assign " + expr.name.lexeme, expr.value);
    }

    @Override
    public String visitBlockStmt(Stmt.Block stmt) {
        StringBuilder output = new StringBuilder();
        for (Stmt s : stmt.statements) {
            output.append("    " + s.accept(this) + "\n");
        }
        return "{\n" + output + "}";
    }

    @Override
    public String visitIfStmt(Stmt.If stmt) {
        return parenthesize("if", stmt.condition) + stmt.thenBranch.accept(this) + (stmt.elseBranch != null
                ? stmt.elseBranch.accept(this)
                : "");
    }

    @Override
    public String visitWhileStmt(Stmt.While stmt) {
        return "while " + "TODO print condition and body.";
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    private String parenthesize(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();

        builder.append("(").append(name);
        for (Expr expr : exprs) {
            if (expr != null) {
                builder.append(" ");
                // Recurse
                builder.append(expr.accept(this));

            }
        }
        builder.append(")");

        return builder.toString();
    }
}
