package com.camatta.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class LoxClass extends LoxInstance implements LoxCallable {
    final String name;
    final LoxClass superclass;
    private final Map<String, LoxFunction> methods;
    private final Map<String, LoxFunction> staticMethods;
    private final Map<String, Object> fields = new HashMap<>();

    LoxClass(String name, LoxClass superclass, Map<String, LoxFunction> methods,
            Map<String, LoxFunction> staticMethods) {
        this.superclass = superclass;
        this.name = name;
        // Instance methods are owner by the class. Instances own fields
        this.methods = methods;
        this.staticMethods = staticMethods;
    }

    /* Get a property (class field or method) */
    @Override
    Object get(Token name) {
        if (fields.containsKey(name.lexeme)) {
            return fields.get(name.lexeme);
        }

        // If we don't find a field, look for a method in the class
        LoxFunction method = findStaticMethod(name.lexeme);
        if (method != null)
            return method;

        // Unlike javascript (which silently returns undefined), we throw an error if a
        // property isnt defined
        throw new RuntimeError(name,
                "Undefined static property '" + name.lexeme + "'.");
    }

    @Override
    void set(Token name, Object value) {
        fields.put(name.lexeme, value);
    }

    LoxFunction findStaticMethod(String name) {
        if (staticMethods.containsKey(name)) {
            return staticMethods.get(name);
        }

        if (superclass != null) {
            return superclass.findStaticMethod(name);
        }

        return null;
    }

    LoxFunction findMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }

        if (superclass != null) {
            return superclass.findMethod(name);
        }

        return null;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        LoxInstance instance = new LoxInstance(this);

        // When a class is called, after the instance is created, look for an init. If
        // we find it, bind and invoke it just like a normal method.
        LoxFunction initializer = findMethod("init");
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }

        return instance;
    }

    /* Number of arguments */
    @Override
    public int arity() {
        LoxFunction initializer = findMethod("init");
        if (initializer == null)
            return 0;
        return initializer.arity();
    }
}
