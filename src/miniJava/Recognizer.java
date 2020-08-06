package miniJava;

import java.io.InputStream;

import mJAM.Disassembler;
import mJAM.Interpreter;
import mJAM.ObjectFile;
import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.AbstractSyntaxTrees.ArrayType;
import miniJava.AbstractSyntaxTrees.ClassDecl;
import miniJava.AbstractSyntaxTrees.ClassType;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.ParameterDecl;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.CodeGenerator.CodeGeneration;
import miniJava.ContextualAnalyzer.Identification;
import miniJava.ContextualAnalyzer.TypeChecking;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;

/**
 * Recognize whether input entered through the keyboard is a valid
 * arithmetic expression as defined by the simple CFG and scanner grammar above.  
 * 
 */
public class Recognizer {
	private static AST ast;

	public static void main(String[] args) {

		/*
		 * note that a compiler should read the file specified by args[0]
		 * instead of reading from the keyboard!
		 */
		System.out.print("Enter arithmetic expression: ");
		InputStream inputStream = System.in;
		
		ErrorReporter errorReporter = new ErrorReporter();
		Scanner scanner = new Scanner(inputStream, errorReporter);
		Parser parser = new Parser(scanner, errorReporter);		
		
		//System.out.println("Syntactic analysis ... ");
		ast = parser.parse();
		//System.out.println("Syntactic analysis complete: ");
		
		
		
		if (errorReporter.hasErrors()) {
			//System.out.println("INVALID miniJava program");
			// return code for invalid input
			System.out.println(4+": SA Error");
			System.exit(4);
		}
		ASTDisplay display = new ASTDisplay();
		display.showTree(ast);

		//PA3
		Identification identification = new Identification();
		identification.Identify(ast);
		System.out.println("ID is done");
		if (identification.errors.hasErrors()) {
			System.out.println(4+": ID Error");
			System.exit(4);
		}
		
		TypeChecking typeChecking = new TypeChecking();
		typeChecking.TypeCheck(ast);
		System.out.println("Type Check is done");
		if (typeChecking.errors.hasErrors()) {
			System.out.println(4+":Type Error");
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
		
		ObjectFile objF = new ObjectFile("Recognizer.mJAM");
		if (objF.write()) {
			System.out.println("*** Error: problem while writing object file");
			System.exit(4);
		} else {
			//System.exit(0);
		}
		
		String asmCodeFileName = "Recognizer.asm";
		System.out.print("Writing assembly file " + asmCodeFileName + " ... ");
		Disassembler d = new Disassembler("Recognizer.mJAM");
		if (d.disassemble()) {
			System.out.println("***Writing assembly file FAILED");
			return;
		} else
			System.out.println("SUCCEEDED");

		/*
		 * run code using debugger
		 * 
		 */
		
		if (true) {
			System.out.println("Running code in debugger ... ");
			Interpreter.debug("Recognizer.mJAM", asmCodeFileName);
			//System.out.println("*** mJAM execution completed");
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
