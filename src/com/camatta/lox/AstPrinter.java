package com.camatta.lox;

import java.util.Arrays;
import java.util.List;

import com.camatta.lox.Expr.*;
import com.camatta.lox.Stmt.*;

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
    public String visitBinaryExpr(Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Literal expr) {
        if (expr.value == null)
            return "nil";
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    @Override
    public String visitExpressionStmt(Expression expr) {
        return parenthesize("expr", expr.expression);
    }

    @Override
    public String visitPrintStmt(Print expr) {
        return parenthesize("print", expr.expression);
    }

    @Override
    public String visitVarStmt(Var stmt) {
        return parenthesize("var " + stmt.name.lexeme, stmt.initializer);
    }

    @Override
    public String visitVariableExpr(Variable expr) {
        return parenthesize("var", expr);
    }

    @Override
    public String visitAssignExpr(Assign expr) {
        return parenthesize("assign " + expr.name.lexeme, expr.value);
    }

    @Override
    public String visitBlockStmt(Block stmt) {
        StringBuilder output = new StringBuilder();
        for (Stmt s : stmt.statements) {
            output.append("    " + s.accept(this) + "\n");
        }
        return "{\n" + output + "}";
    }

    @Override
    public String visitIfStmt(If stmt) {
        return parenthesize("if", stmt.condition) + stmt.thenBranch.accept(this) + (stmt.elseBranch != null
                ? stmt.elseBranch.accept(this)
                : "");
    }

    @Override
    public String visitWhileStmt(While stmt) {
        return "while " + "TODO print condition and body.";
    }

    @Override
    public String visitLogicalExpr(Logical expr) {
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

    @Override
    public String visitFunctionStmt(Function stmt) {
        return "fun";
    }

    @Override
    public String visitCallExpr(Call expr) {
        return "call";
    }

    @Override
    public String visitReturnStmt(Stmt.Return stmt) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String visitClassStmt(Stmt.Class stmt) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String visitGetExpr(Expr.Get expr) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String visitSetExpr(Expr.Set expr) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String visitThisExpr(This expr) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String visitSuperExpr(Super expr) {
        // TODO Auto-generated method stub
        return null;
    }
}
