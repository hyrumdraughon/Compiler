package miniJava.ContextualAnalyzer;

import java.util.HashMap;
import java.util.Stack;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;
//												Arg	   Result
public class Identification implements Visitor<Object, Object> {
	Stack<HashMap<String, Declaration>> ScopedIDTable = new Stack<HashMap<String, Declaration>>();
	HashMap<String, Declaration> ClassDecls = new HashMap<String, Declaration>();
	HashMap<String, Declaration> allMemberDecls = new HashMap<String, Declaration>();
	HashMap<String, Declaration> currMembers = new HashMap<String, Declaration>();
	ClassDecl currClass;
	MethodDecl currMethod;
	VarDecl currVarDecl;
	boolean initExprError;
	public ErrorReporter errors = new ErrorReporter();
	
	class idError extends Error {
		private static final long serialVersionUID = 1L;
	}
	
	private void idError(SourcePosition s, String m) throws idError {
		errors.reportError("*** line " + s.lineNum + ": Identification Error- " + m);
		throw new idError();
		
	}
	
	private void imports(Package p) {
		//class System { public static _PrintStream out; }
		Token t = new Token(TokenKind.ID, "_PrintStream", null);
		Identifier id = new Identifier(t);
		ClassType typeD = new ClassType(id, null);
		FieldDecl f = new FieldDecl(false, true, typeD, "out", null);
		allMemberDecls.put(f.name, f);
		FieldDeclList fdl = new FieldDeclList();
		fdl.add(f);
		//p.classDeclList.add(new ClassDecl("System", fdl, new MethodDeclList(), null));
		ClassDecl sys = new ClassDecl("System", fdl, new MethodDeclList(), null);
		ClassDecls.put(sys.name, sys);
		
		//class _PrintStream { public void println(int n){}; }
		BaseType btype = new BaseType(TypeKind.VOID, null);
		FieldDecl mb = new FieldDecl(false, false, btype, "println", null);
		ParameterDeclList pdl = new ParameterDeclList();
		ParameterDecl pd = new ParameterDecl(new BaseType(TypeKind.INT, null), "n", null);
		pdl.add(pd);
		MethodDecl m = new MethodDecl(mb, pdl, new StatementList(), null);
		allMemberDecls.put(m.name, m);
		MethodDeclList mdl = new MethodDeclList();
		mdl.add(m);
		//p.classDeclList.add(new ClassDecl("_PrintStream", new FieldDeclList(), mdl, null));
		ClassDecl ps = new ClassDecl("_PrintStream", new FieldDeclList(), mdl, null);
		ClassDecls.put(ps.name, ps);
		
		//class String { }
		//p.classDeclList.add(new ClassDecl("String", new FieldDeclList(), new MethodDeclList(), null));
		ClassDecl str = new ClassDecl("String", new FieldDeclList(), new MethodDeclList(), null);
		str.type = new BaseType(TypeKind.UNSUPPORTED, null);
		ClassDecls.put(str.name, str);
		//p.classDeclList.get(p.classDeclList.size()-1).type = new BaseType(TypeKind.UNSUPPORTED, null);
	}
	
	public void Identify(AST ast) {
		imports((Package) ast);
		try {
			ast.visit(this, null);
		}
		catch (idError e) {	}
	}

	@Override
	public Object visitPackage(Package prog, Object arg) {
		ScopedIDTable.push(ClassDecls);
		//System.out.println("after push classDecls, size = "+ScopedIDTable.size());
		//scope level 1
		for (ClassDecl c: prog.classDeclList){//get all ClassDecls
            c.visit(this, null);
        }
		//scope level 2
		ScopedIDTable.push(allMemberDecls);//MemberDecls hashmap
		//System.out.println("after push allMemberDecls, size = "+ScopedIDTable.size());
		for (ClassDecl c: prog.classDeclList) {//get all MemberDecls in all Classes
			for (FieldDecl f: c.fieldDeclList) {
				f.visit(this, null);
			}
			for (MethodDecl m: c.methodDeclList) {
				m.visit(this, null);
			}
			currMembers.clear();
		}
		for (ClassDecl c: prog.classDeclList) {
			currClass = c;
			for (FieldDecl f: c.fieldDeclList) {
				currMembers.put(f.name, f);
			}
			for (MethodDecl m: c.methodDeclList) {
				currMembers.put(m.name, m);
			}
			for (MethodDecl m: c.methodDeclList) {
				currMethod = m;
				//scope level 3
				HashMap<String, Declaration> parameters = new HashMap<String, Declaration>();
				ScopedIDTable.push(parameters);
				//System.out.println("after push params, size = "+ScopedIDTable.size());
				for (ParameterDecl p: m.parameterDeclList) {
					p.visit(this, null);
				}
				//scope level 4+
				HashMap<String, Declaration> stmts = new HashMap<String, Declaration>();
				ScopedIDTable.push(stmts);
				//System.out.println("after push stmts, size = "+ScopedIDTable.size());
				for (Statement s: m.statementList) {
					s.visit(this, null);
				}
				ScopedIDTable.pop();//pop scope4 stmts off the stack
				ScopedIDTable.pop();//pop scope3 parameters for this method off the stack
			}
		}
		return null;
	}

	@Override
 	public Object visitClassDecl(ClassDecl cd, Object arg) {
		if (ClassDecls.containsKey(cd.name)) {
			//error- duplicate class decl
			idError(cd.posn, "duplicate classDecl");
		} else {
			ClassDecls.put(cd.name, cd);
		}
		return null;
	}

	@Override
	public Object visitFieldDecl(FieldDecl fd, Object arg) {
		if (currMembers.containsKey(fd.name)) {
			//error- duplicate member decl
			idError(fd.posn, "duplicate memberDecl");
		} else {
			allMemberDecls.put(fd.name, fd);
			currMembers.put(fd.name, fd);
		}
		//check if ClassType, link ider to decl
		fd.type.visit(this, null);
		return null;
	}

	@Override
	public Object visitMethodDecl(MethodDecl md, Object arg) {
		if (currMembers.containsKey(md.name)) {
			//error- duplicate member decl
			idError(md.posn, "duplicate memberDecl");
		} else {
			allMemberDecls.put(md.name, md);
			currMembers.put(md.name, md);
		}
		//check if ClassType, link ider to decl
		md.type.visit(this, null);
		return null;
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, Object arg) {
		if (ScopedIDTable.peek().containsKey(pd.name)) {
			//error- duplicate parameter decl
			idError(pd.posn, "duplicate parameterDecl");
		} else {
			ScopedIDTable.peek().put(pd.name, pd);
		}
		//check for ClassType IDers and link to decls
		pd.type.visit(this, null);
		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl decl, Object arg) {
		boolean allowed = true;
		for (int i = 2; i<ScopedIDTable.size(); i++) {
			if (ScopedIDTable.get(i).containsKey(decl.name)) {
				allowed = false;
			}
		}
		if (allowed) {
			ScopedIDTable.peek().put(decl.name, decl);
		} else {
			//error*******
			idError(decl.posn, "duplicate VarDecl");
		}
		//check for ClassType and link to decls
		decl.type.visit(this, null);
		return null;
	}

	@Override
	public Object visitBaseType(BaseType type, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitClassType(ClassType type, Object arg) {
		if (ClassDecls.containsKey(type.className.spelling)) {
			type.className.decl = ClassDecls.get(type.className.spelling);
		} else {
			//error***********Class Not Found
			idError(type.posn, "ClassDecl for ClassType "+type.className.spelling+" not found");
		}
		return null;
	}

	@Override
	public Object visitArrayType(ArrayType type, Object arg) {
		type.eltType.visit(this, null);
		return null;
	}

	@Override
	public Object visitBlockStmt(BlockStmt stmt, Object arg) {
		HashMap<String, Declaration> block = new HashMap<String, Declaration>();
		ScopedIDTable.push(block);//adds new scope level to the stack
		for (Statement s: stmt.sl) {
			s.visit(this, null);
		}
		ScopedIDTable.pop();//pop scope level off the stack
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		stmt.varDecl.visit(this, null);
		currVarDecl = stmt.varDecl;
		
		stmt.initExp.visit(this, null);
		if (initExprError) {
			idError(stmt.initExp.posn, "cannot reference variable within its own initializing expression");
		}
		initExprError = false;
		currVarDecl = null;
		
		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		stmt.ref.visit(this, null);
		//PA4: check against assigning to length field of arrays
		if (stmt.ref.toString().equals("QualRef")) {
			try { 
				QualRef qr = (QualRef) stmt.ref;
				if (qr.ref.decl.type.toString().equals("ArrayType") && qr.id.spelling.equals("length")) {
					idError(stmt.ref.posn, "AssignStmt: cannot assign to array.length");
				}
			} catch (Exception e) { }
		}
		stmt.val.visit(this, null);
		return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		stmt.ref.visit(this, null);
		//PA4: check against assigning to length field of arrays
		if (stmt.ref.toString().equals("QualRef")) {
			QualRef qr = (QualRef) stmt.ref;
			if (qr.ref.decl.type.toString().equals("ArrayType") && qr.id.spelling.equals("length")) {
				idError(stmt.ref.posn, "AssignStmt: cannot assign to array.length");
			}
		}
		stmt.ix.visit(this, null);
		stmt.exp.visit(this, null);
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, Object arg) {
		if (stmt.methodRef.toString().equals("ThisRef")) {
			idError(stmt.methodRef.posn, "CallStmt: identifier 'this' does not denote a method");
		}
		
		stmt.methodRef.visit(this, null);
		for (Expression e: stmt.argList) {
			e.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
		if (stmt.returnExpr != null) {
			stmt.returnExpr.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, Object arg) {
		stmt.cond.visit(this, null);
		if (stmt.thenStmt.toString().equals("VarDeclStmt")) {
			idError(stmt.thenStmt.posn, "IfStmt- solitary VarDeclStmt not permitted in thenStmt");
		}
		stmt.thenStmt.visit(this, null);
		
		if (stmt.elseStmt != null) {
			if (stmt.elseStmt.toString().equals("VarDeclStmt")) {
				idError(stmt.elseStmt.posn, "IfStmt- solitary VarDeclStmt not permitted in elseStmt");
			}
			stmt.elseStmt.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		stmt.cond.visit(this, null);
		if (stmt.body.toString().equals("VarDeclStmt")) {
			idError(stmt.body.posn, "WhileStmt- solitary VarDeclStmt not permitted in WhileStmt body");
		}
		stmt.body.visit(this, null);
		return null;
	}

	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		expr.operator.visit(this, null);
		expr.expr.visit(this, null);
		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
		expr.left.visit(this, null);
		expr.operator.visit(this, null);
		expr.right.visit(this, null);
		return null;
	}

	@Override
	public Object visitRefExpr(RefExpr expr, Object arg) {
		expr.ref.visit(this, null);
		if (expr.ref.toString().equals("IdRef")) {
			if (expr.ref.decl.toString().equals("ClassDecl")) {
				idError(expr.ref.posn, "RefExpr: ref does not denote a field or variable");
			} else if (expr.ref.decl.toString().equals("MethodDecl")) {
				idError(expr.ref.posn, "RefExpr: ref does not denote a field or a variable");
			}
		}
		
		if (expr.ref.toString().equals("QualRef")) {
			QualRef qr = (QualRef) expr.ref;
			if (!(qr.decl.toString().equals("FieldDecl") || (qr.id.spelling.equals("length") && qr.ref.decl.type.toString().equals("ArrayType")))) {
				idError(expr.ref.posn, "RefExpr: ref does not denote a variable");
			}
		}
		
		return null;
	}

	@Override
	public Object visitIxExpr(IxExpr expr, Object arg) {
		expr.ref.visit(this, null);
		expr.ixExpr.visit(this, null);
		
		try {
			RefExpr re = (RefExpr) expr.ixExpr;
			if (re.ref.decl.type.toString().equals("ArrayType")) {
				idError(expr.ixExpr.posn, "index Expr does not have type int");
			}
		} catch (Exception e) {
			return null;
		}
		return null;
	}

	@Override
	public Object visitCallExpr(CallExpr expr, Object arg) {
		if (expr.functionRef.toString().equals("ThisRef")) {
			idError(expr.functionRef.posn, "CallStmt: identifier 'this' does not denote a method");
		}
		
		expr.functionRef.visit(this, null);
		for (Expression e: expr.argList) {
			e.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
		expr.lit.visit(this, null);
		return null;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		expr.classtype.visit(this, null);
		return null;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		expr.eltType.visit(this, null);
		expr.sizeExpr.visit(this, null);
		return null;
	}

	@Override
	public Object visitThisRef(ThisRef ref, Object arg) {
		if (currMethod.isStatic) {
			//error- cannot reference this in a static method
			idError(ref.posn, "cannot reference THIS in a static method");
		} else {
			ref.decl = currClass;
		}
		return null;
	}

	@Override
	public Object visitIdRef(IdRef ref, Object arg) {	
		boolean IDinTable = false;
		for (int i = 2; i<ScopedIDTable.size(); i++) {
			if (ScopedIDTable.get(i).containsKey(ref.id.spelling)) {
				IDinTable = true;
			}
		}
		//check if ref is direct to className
		if (ClassDecls.containsKey(ref.id.spelling) && !currMembers.containsKey(ref.id.spelling) && !IDinTable) {			
			//if yes, link ref and id to that classDecl
			ClassDecl c = (ClassDecl) ClassDecls.get(ref.id.spelling);
			ref.id.decl = c;
			ref.decl = c;
		} else {
			
			ref.id.visit(this, null);
			ref.decl = ref.id.decl;
		}
		
		if (currVarDecl != null && ref.id.decl == currVarDecl) {
			initExprError = true;
		}
		
		return null;
	}

	@Override
	public Object visitQRef(QualRef ref, Object arg) {
		ref.ref.visit(this, null);
		if (ref.ref.decl.type != null && ref.ref.decl.type.toString().equals("ArrayType")) {
			//PA4: adding length field to arraytypes
			if (ref.id.spelling.equals("length")) {
				BaseType bt = new BaseType(TypeKind.INT, ref.id.posn);
				ref.id.decl = new VarDecl(bt, "length", ref.id.posn);
				ref.decl = ref.id.decl;
				return null;
			} else {
				idError(ref.ref.posn, "QRef: qualifying ref cannot be an IxRef");
			}
		}
		
		//is ref.id a member of ref.ref.visit's classDecl???
		ClassDecl c;
		try {
			c = (ClassDecl) ref.ref.decl;
		} catch (Exception e) {
			//if (ref.ref.decl.)
			try {
				ClassType ct = (ClassType) ref.ref.decl.type;
				c = (ClassDecl) ClassDecls.get(ct.className.spelling);
			} catch (Exception e2) {
				try {
					ArrayType at = (ArrayType) ref.ref.decl.type;
					ClassType ct = (ClassType) at.eltType;
					c = (ClassDecl) ClassDecls.get(ct.className.spelling);
				} catch (Exception e3) {
					c = null;
					idError(ref.posn, "QRef: ref is a BaseType, so it cannot be qualified");
				}
			}
		}
		
		boolean IDisMember = false;
		MemberDecl md = null;
		try {
			for (FieldDecl f: c.fieldDeclList) {
				if (f.name.equals(ref.id.spelling)) {
					IDisMember = true;
					md = f;
				}
			}
			for (MethodDecl m: c.methodDeclList) {
				if (ref.ref.decl.equals(m)) {
					idError(ref.ref.posn, "QRef: ref is a method and cannot be qualified");
				}
				if (m.name.equals(ref.id.spelling)) {
					IDisMember = true;
					md = m;
				}
			}
		} catch (Exception e) {
			//error- 
			idError(ref.posn, "QRef: ref is not a reference to a class, so ID can't be a member");
		}
		
		if (IDisMember && c.name.equals(ref.ref.decl.name) && ref.ref.decl instanceof ClassDecl && !ref.ref.toString().equals("ThisRef")) {
			if (!md.isStatic) {
				//error- 
				idError(ref.id.posn, "QRef: ID does not have static access");
			}
		}
		
		if (IDisMember) {
			if (!c.equals(currClass) && md.isPrivate) {
				idError(ref.id.posn, "Qref: ID is a private member of another class");
			} else {
				ref.id.decl = md;
				ref.decl = md;
			}
			//ref.id.visit(this, null);//if id is not accessible then throw error******
		} else {
			//error- 
			idError(ref.id.posn, "QRef: ID is not a member of ref");
		}
		
		return null;
	}

	@Override
	public Object visitIdentifier(Identifier id, Object arg) {
		//id cannot be a classType, it is a variable name		
		boolean IDinTable = false;
		int declScopeLevel = 0;
		for (int i = 2; i<ScopedIDTable.size(); i++) {
			if (ScopedIDTable.get(i).containsKey(id.spelling)) {
				IDinTable = true;
				declScopeLevel = i;
			}
		}
		if (IDinTable) {
			id.decl = ScopedIDTable.get(declScopeLevel).get(id.spelling);
		} else if (allMemberDecls.containsKey(id.spelling)) {
			MemberDecl md = (MemberDecl) allMemberDecls.get(id.spelling);
			boolean idMemInClass = false;
			for (FieldDecl f: currClass.fieldDeclList) {
				if (f.equals(md)) {
					idMemInClass = true;
				}
			}
			for (MethodDecl m: currClass.methodDeclList) {
				if (m.equals(md)) {
					idMemInClass = true;
				}
			}
			if(currMethod.isStatic && idMemInClass && !md.isStatic) {
				idError(id.posn, "cannot access non-static member from within a static method");
			} else if (md.isPrivate && !idMemInClass){
				idError(id.posn, "cannot access private member of a different class");
			} else {
				id.decl = md;
			}
		} else if (ClassDecls.containsKey(id.spelling)) {
			idError(id.posn, "cannot reference a decl/non-instance of a class");
		} else {
			idError(id.posn, "ID is not declared");
		}
		
		return null;
	}

	@Override
	public Object visitOperator(Operator op, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitIntLiteral(IntLiteral num, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitNullLiteral(NullLiteral nullLit, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

}
