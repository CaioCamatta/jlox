package com.camatta.tool;
// This is a tool that we are using to generate syntax tree classes. So, it isn't part of the interpreter itself

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/**
 * GenerateAst is a little tool to help write Expr.java.
 * Without this tool, I'd have to manualy write tens of classes.
 */
public class GenerateAst {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }
        String outputDir = args[0];

        // Expressions (e.g. 2 + 3)
        defineAst(outputDir, "Expr", Arrays.asList(
                "Binary     : Expr left, Token operator, Expr right",
                "Assign     : Token name, Expr value",
                "Grouping   : Expr expression",
                "Literal    : Object value",
                "Unary      : Token operator, Expr right",
                "Variable   : Token name"));

        // Statements (e.g. if)
        // Statements have their own base class because statement and expression
        // syntaxes are disjoint.
        defineAst(outputDir, "Stmt", Arrays.asList(
                "Expression : Expr expression",
                "Print      : Expr expression",
                "Var        : Token name, Expr initializer")); // Optional initializer, e.g. var a = 0
    }

    private static void defineAst(
            String outputDir,
            String baseName,
            List<String> types)
            throws IOException {
        String path = outputDir + "/" + baseName + ".java";
        PrintWriter writer = new PrintWriter(path, "UTF-8");

        writer.println("package com.camatta.lox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + baseName + " {");

        defineVisitor(writer, baseName, types);

        // AST classes
        for (String type : types) {
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            defineType(writer, baseName, className, fields);
        }

        // base accept() method
        writer.println();
        writer.println("    abstract <R> R accept(Visitor<R> visitor);");

        writer.println("}\n");
        writer.close();
    }

    private static void defineType(
            PrintWriter writer,
            String baseName,
            String className,
            String fieldList) {
        writer.println("    static class " + className + " extends " + baseName + " {");

        // Constructor
        writer.println("        " + className + "(" + fieldList + ") {");

        // Store parameters in fields
        String[] fields = fieldList.split(", ");
        for (String field : fields) {
            String name = field.split(" ")[1];
            writer.println("            this." + name + " = " + name + ";");
        }

        writer.println("        }");

        // Visitor pattern. Each subclass implements accept()
        // This lets us define operations on expressions without having to add anything
        // to the classes.
        writer.println();
        writer.println("        @Override");
        writer.println("        <R> R accept(Visitor<R> visitor) {");
        writer.println("            return visitor.visit" + className + baseName + "(this);");
        writer.println("        }");

        // Fields
        writer.println();
        for (String field : fields) {
            writer.println("        final " + field + ";");
        }

        writer.println("    }\n");
    }

    private static void defineVisitor(
            PrintWriter writer,
            String baseName,
            List<String> types) {
        writer.println("    interface Visitor<R> {");

        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.println(
                    "        R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");
        }

        writer.println("    }\n");
    }
}