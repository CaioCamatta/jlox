BUILD_DIR := build

generate_ast:
	@ $(MAKE) -f java.mk DIR=src PACKAGE=tool
	@ java -cp build/src com.camatta.tool.GenerateAst \
			src/com/camatta/lox

build: ./src/com/camatta/lox/*
	@ $(MAKE) -f java.mk DIR=src PACKAGE=lox
