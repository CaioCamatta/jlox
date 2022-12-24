package com.camatta.lox;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

public class TestScanner {

    @Test
    public void testAllIdentifiers() throws IOException{
        String actual = Lox.runToString("/home/caio/Documents/JLox/jlox/test/com/camatta/lox/input/all_identifiers.lox");

        byte[] bytes = Files.readAllBytes(Paths.get("/home/caio/Documents/JLox/jlox/test/com/camatta/lox/output/all_identifiers.txt"));
        String expected = new String(bytes, Charset.defaultCharset()); 

        assertEquals(expected, actual);
    }
    
}
