package miniJava.CodeGenerator;

import miniJava.AbstractSyntaxTrees.MethodDecl;

public class MethodPatch {
	int addr;
	MethodDecl method;
	
	public MethodPatch(int addr, MethodDecl method) {
		this.addr = addr;
		this.method = method;
	}

}
