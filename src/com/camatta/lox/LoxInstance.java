package com.camatta.lox;

import java.util.HashMap;
import java.util.Map;

class LoxInstance {
    private LoxClass loxClass;
    private final Map<String, Object> fields = new HashMap<>();

    LoxInstance(LoxClass loxClass) {
        this.loxClass = loxClass;
    }

    LoxInstance() {
    }

    @Override
    public String toString() {
        return loxClass.name + " instance";
    }

    /* Get a property (field store on the instance or method stored on the class) */
    Object get(Token name) {
        if (fields.containsKey(name.lexeme)) {
            return fields.get(name.lexeme);
        }

        // If we don't find a field, look for a method in the class
        LoxFunction method = loxClass.findMethod(name.lexeme);
        if (method != null)
            return method.bind(this);


        // Check if user accidentaly tried to access a static method in an instance
        if (loxClass.findStaticMethod(name.lexeme) != null)
        throw new RuntimeError(name,
                "Can't access static method '" + name.lexeme + "' in an instance.");

        // Unlike javascript (which silently returns undefined), we throw an error if a
        // property isnt defined
        throw new RuntimeError(name,
                "Undefined property '" + name.lexeme + "'.");
    }

    void set(Token name, Object value) {
        fields.put(name.lexeme, value);
    }
}
