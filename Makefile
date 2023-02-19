BUILD_DIR := build

generate_ast:
	@ $(MAKE) -f java.mk DIR=src PACKAGE=tool
	@ java -cp build/src com.craftinginterpreters.tool.GenerateAst \
			src/com/craftinginterpreters/lox

build: ./src/com/craftinginterpreters/lox/*
	@ $(MAKE) -f java.mk DIR=src PACKAGE=lox
