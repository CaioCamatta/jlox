package com.camatta.lox;

import java.util.HashMap;
import java.util.Map;

class Environment {
    final Environment enclosing;

    /* Create the global environment */
    Environment() {
        enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    private final Map<String, Object> values = new HashMap<>();

    /* Define (or redefine!) a variable */
    void define(String name, Object value) {
        values.put(name, value);
    }

    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        // If the variable isn't found in this environment, try the enclosing one
        if (enclosing != null)
            return enclosing.get(name);

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    /* Unlike define(), assign() cannot create a new variable */
    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        // If the variable isn't in this env, check outer one recursively
        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
}
