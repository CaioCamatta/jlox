package com.camatta.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
    private static final Interpreter interpreter = new Interpreter();
    static boolean hadError = false;
    static boolean hadRuntimeError = false;
    static boolean verbose = false; // Whether to print debugging info

    public static void main(String[] args) throws IOException {
        if (args.length > 2) {
            System.out.println("Usage: jlox [-v]? [script]");
            System.exit(64);
        } else if (args.length == 2 && args[0].contains("-v")) {
            verbose = true;
            runFile(args[1]);
        } else if (args.length == 1 && !args[0].contains("-v")) {
            runFile(args[0]);
        } else {
            verbose = args.length > 0 ? args[0].contains("-v") : false;
            runPrompt();
        }
    }

    public static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        // Indicate an error in the exit code
        if (hadError)
            System.exit(65);
        if (hadRuntimeError)
            System.exit(70);
    }

    public static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.println("> ");
            String line = reader.readLine();
            // If user presses CTRL D (end of file), exit loop
            if (line == null)
                break;
            run(line);

            // IF user make a mistake, we shouldn't end their session4
            hadError = false;
        }
    }

    public static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        if (verbose) {
            for (Token token : tokens) {
                System.out.println("[scanner] token: " + token);
            }
        }

        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        // Stop if there was a syntax error
        if (hadError)
            return;

        if (verbose)
            new AstPrinter().print(statements);

        interpreter.interpret(statements);
    }

    protected static String runToString(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));

        Scanner scanner = new Scanner(new String(bytes, Charset.defaultCharset()));
        List<Token> tokens = scanner.scanTokens();

        String result = "";

        for (Token token : tokens) {
            result = result + token.toString() + "\n";
        }

        return result;
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    /* Report error at given token */
    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    static void runtimeError(RuntimeError error) {
        System.err.println("[line " + error.token.line + "] " + error.getMessage());
        hadRuntimeError = true;
    }

}
