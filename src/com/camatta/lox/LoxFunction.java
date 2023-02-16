package com.camatta.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
    private final Stmt.Function declaration;
    private final Environment closure;

    private final boolean isInitializer;

    LoxFunction(Stmt.Function declaration, Environment closure,
            boolean isInitializer) {
        this.isInitializer = isInitializer;
        this.closure = closure;
        this.declaration = declaration;
    }

    LoxFunction bind(LoxInstance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        // This is sort of a "closure-within-a-closure". WHen the method is called, that
        // will become the parent of the method body's environment
        return new LoxFunction(declaration, environment,
                isInitializer);
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        // Environment must be created dynamically, each function *call* gets its own
        // environment. Otherwise recursion wouldn't work.
        Environment environment = new Environment(closure);
        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            if (isInitializer)
                return closure.getAt(0, "this");

            return returnValue.value;
        }

        // If the function is an object initializer (init), we override and return
        // 'this'.
        if (isInitializer)
            return closure.getAt(0, "this");
        return null;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }
}