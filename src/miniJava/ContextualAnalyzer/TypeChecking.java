package miniJava.ContextualAnalyzer;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;
//											  Arg		Result
public class TypeChecking implements Visitor<Object, TypeDenoter> {
	public ErrorReporter errors = new ErrorReporter();
	MemberDecl currMethod;
	ClassDecl currClass;
	
	private void typeError(SourcePosition s, String m) {
		errors.reportError("*** line " + s.lineNum + ": Type Error- " + m);
	}

	public void TypeCheck(AST ast) {
		ast.visit(this, null);
	}

	@Override
	public TypeDenoter visitPackage(Package prog, Object arg) {
		for (ClassDecl c: prog.classDeclList) {
			currClass = c;
			if (currClass.name.equals("String")) {
				c.type = new BaseType(TypeKind.UNSUPPORTED, null);
			}
			c.visit(this, null);
		}
		return null;
	}

	@Override
	public TypeDenoter visitClassDecl(ClassDecl cd, Object arg) {
		for (FieldDecl f: cd.fieldDeclList) {
			f.visit(this, null);
		}
		for (MethodDecl m: cd.methodDeclList) {
			currMethod = m;
			m.visit(this, null);
		}
		return null;
	}

	@Override
	public TypeDenoter visitFieldDecl(FieldDecl fd, Object arg) {
		fd.type.visit(this, null);
		return null;
	}

	@Override
	public TypeDenoter visitMethodDecl(MethodDecl md, Object arg) {
		for (Statement s: md.statementList) {
			s.visit(this, null);
		}
		//PA4: last statement in a non-void method must be a return stmt
		if (md.type.typeKind != TypeKind.VOID) {
			if (md.statementList.size() <= 0) {
				typeError(md.posn, "MethodDecl: non-void method must have a return stmt as last stmt");
			} else if (!md.statementList.get(md.statementList.size()-1).toString().equals("ReturnStmt")) {
				typeError(md.posn, "MethodDecl: last statement in method must be a return statement");
			}
		}
		return null;
	}

	@Override
	public TypeDenoter visitParameterDecl(ParameterDecl pd, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TypeDenoter visitVarDecl(VarDecl decl, Object arg) {
		return decl.type.visit(this, null);
	}

	@Override
	public TypeDenoter visitBaseType(BaseType type, Object arg) {
		return type;
	}

	@Override
	public TypeDenoter visitClassType(ClassType type, Object arg) {
		return type;
	}

	@Override
	public TypeDenoter visitArrayType(ArrayType type, Object arg) {
		return type.eltType;
	}

	@Override
	public TypeDenoter visitBlockStmt(BlockStmt stmt, Object arg) {
		for (Statement s: stmt.sl) {
			s.visit(this, null);
		}
		return null;
	}

	@Override
	public TypeDenoter visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		TypeDenoter vdType = stmt.varDecl.visit(this, null);
		TypeDenoter eType = stmt.initExp.visit(this, null);
		
		if (!vdType.equals(eType)) {
			typeError(stmt.initExp.posn, "expr type does not equal varDecl type");
			return new BaseType(TypeKind.ERROR, null);
		} else {
			return vdType;
		}
	}

	@Override
	public TypeDenoter visitAssignStmt(AssignStmt stmt, Object arg) {
		TypeDenoter refType = stmt.ref.visit(this, null);
		TypeDenoter valType = stmt.val.visit(this, null);
		
		if (!refType.equals(valType)) {
			typeError(stmt.val.posn, "expr type does not equal ref type");
			return new BaseType(TypeKind.ERROR, null);
		} else {
			return refType;
		}
	}

	@Override
	public TypeDenoter visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		TypeDenoter refType = stmt.ref.visit(this, null);
		TypeDenoter ixType = stmt.ix.visit(this, null);
		if (!ixType.equals(new BaseType(TypeKind.INT, null))) {
			typeError(stmt.ix.posn, "index expr does not resolve to INT");
		}
		TypeDenoter exprType = stmt.exp.visit(this, null);
		
		if (!refType.equals(exprType)) {
			typeError(stmt.exp.posn, "expr type does not equal ref type");
			return new BaseType(TypeKind.ERROR, null);
		} else {
			return refType;
		}
	}

	@Override
	public TypeDenoter visitCallStmt(CallStmt stmt, Object arg) {
		MethodDecl m = (MethodDecl) stmt.methodRef.decl;
		if (m.parameterDeclList.size() != stmt.argList.size()) {
			typeError(stmt.posn, "CallStmt- method call has wrong number of arguments");
		}
		
		for (int i = 0; i<m.parameterDeclList.size(); i++) {
			TypeDenoter argType = stmt.argList.get(i).visit(this, null);
			TypeDenoter paramType = m.parameterDeclList.get(i).type;
			if (m.parameterDeclList.get(i).type instanceof ArrayType) {
				ArrayType at = (ArrayType) paramType;
				paramType = at.eltType;
			}
			if (!paramType.equals(argType)) {
				typeError(stmt.argList.get(i).posn, "CallStmt- "+i+"th arg in argList does not match type of parameter");
			}
		}
		return stmt.methodRef.decl.type;
	}

	@Override
	public TypeDenoter visitReturnStmt(ReturnStmt stmt, Object arg) {
		if (currMethod.type.typeKind == TypeKind.VOID) {
			if (stmt.returnExpr != null) {
				typeError(stmt.posn, "method return type is void, cannot return a type");
			} else {
				return null;
			}
		} else if (stmt.returnExpr == null) {
			typeError(stmt.posn, "method return type is not void but returnExpr is null");
		} else {
			TypeDenoter returnType = stmt.returnExpr.visit(this, null);
			//check return type correct
			if (!currMethod.type.equals(returnType)) {
				typeError(stmt.returnExpr.posn, "return statement type does not match method's return type");
			}
		}
		return null;
	}

	@Override
	public TypeDenoter visitIfStmt(IfStmt stmt, Object arg) {
		TypeDenoter condType = stmt.cond.visit(this, null);
		if (!condType.equals(new BaseType(TypeKind.BOOLEAN, null))) {
			typeError(stmt.cond.posn, "condition expr does not resolve to boolean");
		}
		
		stmt.thenStmt.visit(this, null);
		if (stmt.elseStmt != null) {
			stmt.elseStmt.visit(this, null);
		}
		return null;
	}

	@Override
	public TypeDenoter visitWhileStmt(WhileStmt stmt, Object arg) {
		TypeDenoter condType = stmt.cond.visit(this, null);
		if (!condType.equals(new BaseType(TypeKind.BOOLEAN, null))) {
			typeError(stmt.cond.posn, "condition expr does not resolve to boolean");
		}
		
		stmt.body.visit(this, null);
		return null;
	}

	@Override
	public TypeDenoter visitUnaryExpr(UnaryExpr expr, Object arg) {
		TypeDenoter exprType = expr.expr.visit(this, null);
		
		switch (expr.operator.kind) {
		case MINUS:
			if (!exprType.equals(new BaseType(TypeKind.INT, null))) {
				typeError(expr.expr.posn, "expr is not a boolean");
			} else {
				return exprType;
			}
		case NOT:
			if (!exprType.equals(new BaseType(TypeKind.BOOLEAN, null))) {
				typeError(expr.expr.posn, "expr is not a boolean");
			} else {
				return exprType;
			}
		default:
			return new BaseType(TypeKind.ERROR, null);
		}
	}

	@Override
	public TypeDenoter visitBinaryExpr(BinaryExpr expr, Object arg) {
		TypeDenoter left = expr.left.visit(this, null);
		TypeDenoter right = expr.right.visit(this, null);
		
		switch (expr.operator.kind) {
		case PLUS: case MINUS: case MULTIPLY: case DIVIDE:
			if (!left.equals(new BaseType(TypeKind.INT, null))) {
				typeError(expr.left.posn, "left expr is not an int");
			} else if (!right.equals(new BaseType(TypeKind.INT, null))) {
				typeError(expr.right.posn, "right expr is not an int");
			}
			return new BaseType(TypeKind.INT, null);
		case LESSTHAN: case LESSEQUAL: case GREATERTHAN: case GREATEREQUAL:
			if (!left.equals(new BaseType(TypeKind.INT, null))) {
				typeError(expr.left.posn, "left expr is not an int");
			} else if (!right.equals(new BaseType(TypeKind.INT, null))) {
				typeError(expr.right.posn, "right expr is not an int");
			}
			return new BaseType(TypeKind.BOOLEAN, null);
		case AND: case OR:
			if (!left.equals(new BaseType(TypeKind.BOOLEAN, null))) {
				typeError(expr.left.posn, "left expr is not a boolean");
			} else if (!right.equals(new BaseType(TypeKind.BOOLEAN, null))) {
				typeError(expr.right.posn, "right expr is not a boolean");
			}
			return new BaseType(TypeKind.BOOLEAN, null);
		case EQUALS: case NOTEQUALS:
			if (!left.equals(right)) {
				typeError(expr.operator.posn, "left expr and right expr do not have same type");
			}
			return new BaseType(TypeKind.BOOLEAN, null);
		default:
			return new BaseType(TypeKind.ERROR, null);
		}
	}

	@Override
	public TypeDenoter visitRefExpr(RefExpr expr, Object arg) {
		return expr.ref.visit(this, null);
	}

	@Override
	public TypeDenoter visitIxExpr(IxExpr expr, Object arg) {
		TypeDenoter ixType = expr.ixExpr.visit(this, null);
		if (!ixType.equals(new BaseType(TypeKind.INT, null))) {
			typeError(expr.ixExpr.posn, "index expr does not resolve to INT");
		}
		
		//return expr.ref.visit(this, null);
		try {
			ArrayType at = (ArrayType) expr.ref.decl.type;
			return at.eltType;
		} catch (Exception e) {
			typeError(expr.ref.posn, "IxExpr- reference is not an array");
			return new BaseType(TypeKind.ERROR, null);
		}
		
	}

	@Override
	public TypeDenoter visitCallExpr(CallExpr expr, Object arg) {
		MethodDecl m = (MethodDecl) expr.functionRef.decl;
		if (m.parameterDeclList.size() != expr.argList.size()) {
			typeError(expr.posn, "CallExpr- method call has wrong number of arguments");
		}
		
		for (int i = 0; i<m.parameterDeclList.size(); i++) {
			TypeDenoter argType = expr.argList.get(i).visit(this, null);
			if (!m.parameterDeclList.get(i).type.equals(argType)) {
				typeError(expr.argList.get(i).posn, "CallExpr- "+i+"th arg in argList does not match type of parameter");
			}
		}
		return expr.functionRef.decl.type;
	}

	@Override
	public TypeDenoter visitLiteralExpr(LiteralExpr expr, Object arg) {
		return expr.lit.visit(this, null);
	}

	@Override
	public TypeDenoter visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		return expr.classtype.visit(this, null);
	}

	@Override
	public TypeDenoter visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		TypeDenoter sizeType = expr.sizeExpr.visit(this, null);
		if (!sizeType.equals(new BaseType(TypeKind.INT, null))) {
			typeError(expr.sizeExpr.posn, "index expr does not resolve to INT");
		}
		
		return expr.eltType.visit(this, null);
	}

	@Override
	public TypeDenoter visitThisRef(ThisRef ref, Object arg) {
		//return ref.decl.type;****classdecl has a null type
		Token t = new Token(TokenKind.ID, ref.decl.name, ref.decl.posn);
		Identifier id = new Identifier(t);
		return new ClassType(id, ref.posn);
	}

	@Override
	public TypeDenoter visitIdRef(IdRef ref, Object arg) {
		return ref.id.decl.type.visit(this, null);
	}

	@Override
	public TypeDenoter visitQRef(QualRef ref, Object arg) {
		return ref.id.decl.type.visit(this, null);
	}

	@Override
	public TypeDenoter visitIdentifier(Identifier id, Object arg) {
		return id.decl.type.visit(this, null);
	}

	@Override
	public TypeDenoter visitOperator(Operator op, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TypeDenoter visitIntLiteral(IntLiteral num, Object arg) {
		return new BaseType(TypeKind.INT, null);
	}

	@Override
	public TypeDenoter visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		return new BaseType(TypeKind.BOOLEAN, null);
	}

	@Override
	public TypeDenoter visitNullLiteral(NullLiteral nullLit, Object arg) {
		return new BaseType(TypeKind.NULL, null);
	}

}
