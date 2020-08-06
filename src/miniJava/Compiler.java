package miniJava;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import mJAM.*;
import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ArrayType;
import miniJava.AbstractSyntaxTrees.ClassDecl;
import miniJava.AbstractSyntaxTrees.ClassType;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.ParameterDecl;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.CodeGenerator.CodeGeneration;
//import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.ContextualAnalyzer.Identification;
import miniJava.ContextualAnalyzer.TypeChecking;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;

public class Compiler {
	private static AST ast;

	public static void main(String[] args) {
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(args[0]);
		} catch (FileNotFoundException e) {
			System.out.println("Input file " +args[0]+ " not found");
			System.exit(3);
		}
		
		ErrorReporter errorReporter = new ErrorReporter();
		Scanner scanner = new Scanner(inputStream, errorReporter);
		Parser parser = new Parser(scanner, errorReporter);		
		
		//System.out.println("Syntactic analysis ... ");
		ast = parser.parse();
		//System.out.println("Syntactic analysis complete: ");
		
		
		
		if (errorReporter.hasErrors()) {
			//System.out.println("INVALID miniJava program");
			// return code for invalid input
			System.exit(4);
		}

		//PA3
		Identification identification = new Identification();
		identification.Identify(ast);
		if (identification.errors.hasErrors()) {
			System.exit(4);
		}
		
		TypeChecking typeChecking = new TypeChecking();
		typeChecking.TypeCheck(ast);
		if (typeChecking.errors.hasErrors()) {
			System.exit(4);
		}
		
		//PA4
		int isMain = checkMain((Package) ast);
		if (isMain == 2) {
			System.out.println("*** Error: multiple main methods found");
			System.exit(4);
		} else if (isMain == 0) {
			System.out.println("*** Error: no main method found");
			System.exit(4);
		}
		CodeGeneration codeGenerator = new CodeGeneration();
		codeGenerator.generateCode(ast);
		
		StringBuilder sb = new StringBuilder();
		int i = 0;
		while (args[0].charAt(i) != '.') {
			sb.append(args[0].charAt(i));
			i++;
		}
		String ocFileName = sb.toString();
		ObjectFile objF = new ObjectFile(ocFileName+".mJAM");
		if (objF.write()) {
			System.out.println("*** Error: problem while writing object file");
			System.exit(4);
		} else {
			System.exit(0);
		}
		
	}
	
	private static int checkMain(Package prog) {
		int numMains = 0;
		for (ClassDecl c: prog.classDeclList) {
			for (MethodDecl m: c.methodDeclList) {
				if (m.name.equals("main") && !m.isPrivate && m.isStatic && m.type.typeKind == TypeKind.VOID) {
					if (m.parameterDeclList.size() == 1) {
						ParameterDecl p = m.parameterDeclList.get(0);
						if (p.type.typeKind == TypeKind.ARRAY) {
							ArrayType at = (ArrayType) p.type;
							if (at.eltType.typeKind == TypeKind.CLASS) {
								ClassType ct = (ClassType) at.eltType;
								if (ct.className.spelling.equals("String")) {
									numMains++;
								}
							}
						}
					}
				}
			}
		}
		if (numMains < 1) {
			//error no main method found
			return 0;
		} else if (numMains > 1) {
			//error multiple main methods found
			return 2;
		} else {
			return 1;
		}
	}
}
