package com.camatta.lox;

import java.util.List;

interface LoxCallable {
    int arity();

    // Pass in the interpreter in case the class implementing call needs it
    // The implementer's job is to return the value that the call expression
    // produces
    Object call(Interpreter interpreter, List<Object> arguments);
}
