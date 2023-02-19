package com.craftinginterpreters.lox;

class Return extends RuntimeException {
    final Object value;

    Return(Object value) {
        // Disable some JVM machinery (like stack traces).
        // We're using Return for flow control, not actual error handling.
        super(null, null, false, false);
        this.value = value;
    }
}
