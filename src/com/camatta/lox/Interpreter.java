package com.camatta.lox;

import java.util.List;

import javax.lang.model.element.ExecutableElement;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    // Variables will stay in memory as long as the interpreer is running.
    private Environment environment = new Environment();

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        // Post-order traversal
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                // We must cast bcause we don't statically know the type. This is the core of
                // what makes this language dynamically typed.
                checkNumberOperand(expr.operator, right);
                return -(double) right;
        }

        // Shouldn't be reached
        return null;
    }

    /* Check of operand is a numbers */
    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double)
            return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    /* Check of operands are numbers */
    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double)
            return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    /* False and nil are falsey. Everything else is truthy. */
    private boolean isTruthy(Object object) {
        if (object == null)
            return false;
        if (object instanceof Boolean)
            return (boolean) object;
        return true;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    /* Send the expression back into the interpreter's visitor implementation. */
    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    /* Analogue to evaluate(). */
    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    /* Execute statements in the context of a given environment by */
    void executeBlock(List<Stmt> statements, Environment environment) {
        // There are cleaner ways to do this than to change and restore the enviroment
        Environment previous = this.environment;

        try {
            // Update the environment
            this.environment = environment;

            // Visit all statements
            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            // Restore previous environment
            this.environment = previous;
        }
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        // Evaluate left-to-right (!)
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case PLUS:
                // '+' is overloaded; it can be used for strings or doubles, so we dynamically
                // check the type
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }

                if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                }

                // If either operand is a string, convert the other to a string
                if (left instanceof String && right instanceof Double) {
                    return (String) left + doubleAsString((Double) right);
                }
                if (left instanceof Double && right instanceof String) {
                    return doubleAsString((Double) left) + (String) right;
                }

                // We already have type checks, so we throw if none of the cases match
                throw new RuntimeError(expr.operator,
                        "Operands must be two numbers or two strings.");
            case SLASH:
                checkNumberOperands(expr.operator, left, right);

                // Check if the denominator is zero and raise an error if so
                if ((Double) right == 0)
                    throw new RuntimeError(expr.operator,
                            "Division by zero is undefined.");

                return (double) left / (double) right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;

        }

        // Shouldn't be reached
        return null;
    }

    /*
     * Lox's equality is the same as Java: no implicit conversions; null == null.
     */
    private boolean isEqual(Object a, Object b) {
        // Handle nil/null specially to prevent throwing a NullPointerException when
        // calling equals()
        if (a == null && b == null)
            return true;
        if (a == null)
            return false;

        return a.equals(b);
    }

    private String stringify(Object object) {
        if (object == null)
            return "nil";

        if (object instanceof Double) {
            return doubleAsString((Double) object);
        }

        return object.toString();
    }

    /*
     * Converts a double to a string and removes trailing '.0' if it's a whole
     * number (an int)
     */
    private String doubleAsString(Double number) {
        String text = number.toString();
        if (text.endsWith(".0")) {
            text = text.substring(0, text.length() - 2);
        }
        return text;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;

        if (stmt.initializer != null) {
            // Process the value / initializer
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }
}