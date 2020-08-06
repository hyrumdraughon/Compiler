package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

public class Parser {
	private Scanner scanner;
	private ErrorReporter reporter;
	private Token token;
	private boolean trace = true;
	
	public Parser(Scanner scanner, ErrorReporter reporter) {
		this.scanner = scanner;
		this.reporter = reporter;
	}
	
	class SyntaxError extends Error {
		private static final long serialVersionUID = 1L;
	}
	
	public AST parse() {
		token = scanner.scan();
		try {
			return parseProgram();
		}
		catch (SyntaxError e) {
			return null;
		}
	}
	
	private Package parseProgram() {
		ClassDeclList cdl = new ClassDeclList();
		while (token.kind == TokenKind.CLASS) {
			ClassDecl cd = parseClassDeclaration();
			cdl.add(cd);
		}
		Package p = new Package(cdl, null);
		accept(TokenKind.EOT);
		return p;
	}
	
	private ClassDecl parseClassDeclaration() {
		FieldDeclList fdl = new FieldDeclList();
		MethodDeclList mdl = new MethodDeclList();
		//
		accept(TokenKind.CLASS);
		String className = token.spelling;//pass className in to ClassDecl later
		SourcePosition classSrc = token.posn;
		accept(TokenKind.ID);
		accept(TokenKind.LCURLY);
		while (token.kind != TokenKind.RCURLY) {
			boolean isPrivate = parseVisibility();
			boolean isStatic = parseAccess();
			if (token.kind == TokenKind.VOID) {//MethodDecl
				SourcePosition baseSrc = token.posn;
				BaseType type = new BaseType(TypeKind.VOID, baseSrc);
				acceptIt();
				String fdName = token.spelling;
				SourcePosition fdSrc = token.posn;
				accept(TokenKind.ID);
				accept(TokenKind.LPAREN);
				ParameterDeclList pdl;
				if (token.kind == TokenKind.RPAREN) {
					acceptIt();
					pdl = new ParameterDeclList();
				} else {
					pdl = parseParameterList();
					accept(TokenKind.RPAREN);
				}
				accept(TokenKind.LCURLY);
				StatementList sl = new StatementList();
				while (token.kind != TokenKind.RCURLY) {
					Statement s = parseStatement();
					sl.add(s);
				}
				accept(TokenKind.RCURLY);
				FieldDecl fd = new FieldDecl(isPrivate, isStatic, type, fdName, fdSrc);
				MethodDecl md = new MethodDecl(fd, pdl, sl, fd.posn);
				mdl.add(md);
			} else {
				TypeDenoter t = parseType();
				String fdName = token.spelling;
				SourcePosition fdSrc2 = token.posn;
				
				accept(TokenKind.ID);
				if (token.kind == TokenKind.SEMICOLON) {//FieldDecl
					acceptIt();
					FieldDecl fd = new FieldDecl(isPrivate, isStatic, t, fdName, fdSrc2);
					fdl.add(fd);
				} else {					//MethodDecl
					accept(TokenKind.LPAREN);
					ParameterDeclList pdl;
					if (token.kind == TokenKind.RPAREN) {
						acceptIt();
						pdl = new ParameterDeclList();
					} else {
						pdl = parseParameterList();
						accept(TokenKind.RPAREN);
					}
					accept(TokenKind.LCURLY);
					StatementList sl = new StatementList();
					while (token.kind != TokenKind.RCURLY) {
						Statement s = parseStatement();
						sl.add(s);
					}
					accept(TokenKind.RCURLY);
					FieldDecl fd = new FieldDecl(isPrivate, isStatic, t, fdName, fdSrc2);
					MethodDecl md = new MethodDecl(fd, pdl, sl, fd.posn);
					mdl.add(md);
				}
			}
		}
		accept(TokenKind.RCURLY);
		return new ClassDecl(className, fdl, mdl, classSrc);
	}
	
	private boolean parseVisibility() {//isPrivate
		if (token.kind == TokenKind.PUBLIC) {
			acceptIt();
			return false;
		} else if (token.kind == TokenKind.PRIVATE) {
			acceptIt();
			return true;
		} else {
			return false;//default false or null???
		}
	}
	
	private boolean parseAccess() {//isStatic
		if (token.kind == TokenKind.STATIC) {
			acceptIt();
			return true;
		} else {
			return false;//default false or null???
		}
	}
	
	private TypeDenoter parseType() {
		SourcePosition TypeSrc = token.posn;
		switch (token.kind) {
		case INT:
			acceptIt();//int
			BaseType iType = new BaseType(TypeKind.INT, TypeSrc);
			if (token.kind == TokenKind.LBRACKET) {//int[]
				acceptIt();
				accept(TokenKind.RBRACKET);
				return new ArrayType(iType, iType.posn);
			}
			return iType;
		case BOOLEAN://boolean
			acceptIt();
			return new BaseType(TypeKind.BOOLEAN, TypeSrc);
		case ID:
			Identifier id = new Identifier(token);
			acceptIt();//ID
			ClassType cType = new ClassType(id, id.posn);
			if (token.kind == TokenKind.LBRACKET) {//ID[]
				acceptIt();
				accept(TokenKind.RBRACKET);
				return new ArrayType(cType, cType.posn);
			}
			return cType;
		default:
			parseError("Invalid term- expecting INT, BOOLEAN, or ID but found "+token.kind);
			return null;//maybe cant return null
		}
	}
	
	private ParameterDeclList parseParameterList() {
		ParameterDeclList pdl = new ParameterDeclList();
		//
		TypeDenoter t = parseType();
		String name = token.spelling;
		SourcePosition pSrc = token.posn;
		accept(TokenKind.ID);
		ParameterDecl p = new ParameterDecl(t, name, pSrc);
		pdl.add(p);
		//
		while (token.kind == TokenKind.COMMA) {
			acceptIt();
			TypeDenoter t2 = parseType();
			String name2 = token.spelling;
			SourcePosition p2Src = token.posn;
			accept(TokenKind.ID);
			ParameterDecl p2 = new ParameterDecl(t2, name2, p2Src);
			pdl.add(p2);
		}
		return pdl;
	}
	
	private ExprList parseArgumentList() {
		ExprList el = new ExprList();
		//
		Expression e = parseExpression();
		el.add(e);
		while (token.kind == TokenKind.COMMA) {
			acceptIt();
			Expression e2 = parseExpression();
			el.add(e2);
		}
		return el;
	}
	
	//Reference ::= (id | this)(.id)*
	private Reference parseReference() {
		Reference r;
		if (token.kind == TokenKind.ID) {
			Identifier id = new Identifier(token);
			r = new IdRef(id, id.posn);
			acceptIt();
			while (token.kind == TokenKind.DOT) {
				acceptIt();
				Identifier idQ = new Identifier(token);
				accept(TokenKind.ID);
				r = new QualRef(r, idQ, idQ.posn);
			}
		} else if (token.kind == TokenKind.THIS) {
			r = new ThisRef(token.posn);
			acceptIt();
			while (token.kind == TokenKind.DOT) {
				acceptIt();
				Identifier idQ = new Identifier(token);
				accept(TokenKind.ID);
				r = new QualRef(r, idQ, idQ.posn);
			}
		} else {
			parseError("Invalid term- expected ID or THIS but found "+token.kind);
			r = null;//maybe cant return null???
		}
		return r;
	}
	
	private Statement parseStatement() {
		Statement s;
		switch (token.kind) {
		//::= { Statement*}
		case LCURLY://BlockStmt
			SourcePosition blockSrc = token.posn;
			acceptIt();
			
			StatementList sl = new StatementList();
			while (token.kind == TokenKind.LCURLY || 
					token.kind == TokenKind.INT ||
					token.kind == TokenKind.BOOLEAN ||
					token.kind == TokenKind.ID ||
					token.kind == TokenKind.THIS ||
					token.kind == TokenKind.RETURN || 
					token.kind == TokenKind.IF || 
					token.kind == TokenKind.WHILE) {
				Statement stmt = parseStatement();
				sl.add(stmt);
			}
			accept(TokenKind.RCURLY);
			s = new BlockStmt(sl, blockSrc);
			return s;
		//::= Type(int|boolean) id = Expression;
		case INT: case BOOLEAN://VarDeclStmt
			TypeDenoter t = parseType();
			String varName = token.spelling;
			SourcePosition varSrc = token.posn;
			accept(TokenKind.ID);
			accept(TokenKind.SETEQUAL);
			Expression e = parseExpression();
			accept(TokenKind.SEMICOLON);
			VarDecl vd = new VarDecl(t, varName, varSrc);
			s = new VarDeclStmt(vd, e, vd.posn);
			return s;
		case ID:
			Identifier id = new Identifier(token);
			ClassType type = new ClassType(id, id.posn);
			IdRef idRef = new IdRef(id, id.posn);
			//
			acceptIt();
			if (token.kind == TokenKind.LBRACKET) {
				acceptIt();
				if (token.kind == TokenKind.RBRACKET) {//type: VarDeclStmt
					acceptIt();
					String vName = token.spelling;
					SourcePosition vSrc = token.posn;
					accept(TokenKind.ID);
					accept(TokenKind.SETEQUAL);
					Expression expr = parseExpression();
					accept(TokenKind.SEMICOLON);
					ArrayType arrType = new ArrayType(type, type.posn);
					VarDecl vDecl = new VarDecl(arrType, vName, vSrc);
					s = new VarDeclStmt(vDecl, expr, vDecl.posn);
					return s;
				} else {//IxReference: IxAssignStmt
					Expression idx = parseExpression();
					accept(TokenKind.RBRACKET);
					accept(TokenKind.SETEQUAL);
					Expression expr = parseExpression();
					accept(TokenKind.SEMICOLON);
					s = new IxAssignStmt(idRef, idx, expr, idRef.posn);
					return s;
				}
			} else if (token.kind == TokenKind.DOT) {//it is a Reference OR IxReference
				Reference qr = idRef;
				while (token.kind == TokenKind.DOT) {
					acceptIt();
					Identifier idQ = new Identifier(token);
					accept(TokenKind.ID);
					qr = new QualRef(qr, idQ, idQ.posn);
				}
				if (token.kind == TokenKind.SETEQUAL) {//AssignStmt
					acceptIt();
					Expression ex = parseExpression();
					accept(TokenKind.SEMICOLON);
					s = new AssignStmt(qr, ex, qr.posn);
					return s;
				} else if (token.kind == TokenKind.LPAREN) {//CallStmt
					acceptIt();
					ExprList el;
					if (token.kind == TokenKind.RPAREN) {
						acceptIt();
						accept(TokenKind.SEMICOLON);
						el = new ExprList();
					} else {
						el = parseArgumentList();//argList is 0 or 1
						accept(TokenKind.RPAREN);
						accept(TokenKind.SEMICOLON);
					}
					s = new CallStmt(qr, el, qr.posn);
					return s;
				} else if (token.kind == TokenKind.LBRACKET) {//IxAssignStmt
					acceptIt();
					Expression idx = parseExpression();
					accept(TokenKind.RBRACKET);
					accept(TokenKind.SETEQUAL);
					Expression ex = parseExpression();
					accept(TokenKind.SEMICOLON);
					s = new IxAssignStmt(qr, idx, ex, qr.posn);
					return s;
				} else {
					parseError("Invalid term- unexpected "+token.kind);
				}
			} else if (token.kind == TokenKind.ID) {//Type: VarDeclStmt
				String vName = token.spelling;
				SourcePosition vSrc = token.posn;
				acceptIt();
				accept(TokenKind.SETEQUAL);
				Expression ex = parseExpression();
				accept(TokenKind.SEMICOLON);
				VarDecl vDecl = new VarDecl(type, vName, vSrc);
				s = new VarDeclStmt(vDecl, ex, vDecl.posn);
				return s;
			} else if (token.kind == TokenKind.SETEQUAL) {//Reference: AssignStmt
				acceptIt();
				Expression ex = parseExpression();
				accept(TokenKind.SEMICOLON);
				s = new AssignStmt(idRef, ex, idRef.posn);
				return s;
			} else if (token.kind == TokenKind.LPAREN) {//Reference: CallStmt
				acceptIt();
				ExprList el;
				if (token.kind == TokenKind.RPAREN) {
					acceptIt();
					accept(TokenKind.SEMICOLON);
					el = new ExprList();
				} else {
					el = parseArgumentList();//argList is 0 or 1
					accept(TokenKind.RPAREN);
					accept(TokenKind.SEMICOLON);
				}
				s = new CallStmt(idRef, el, idRef.posn);
				return s;
			}
			break;
		case THIS:
			Reference r = parseReference();
			if (token.kind == TokenKind.SETEQUAL) {//THIS AssignStmt
				acceptIt();
				Expression ex = parseExpression();
				accept(TokenKind.SEMICOLON);
				s = new AssignStmt(r, ex, r.posn);
				return s;
			} else if (token.kind == TokenKind.LPAREN) {//THIS CallStmt
				acceptIt();
				ExprList el;
				if (token.kind == TokenKind.RPAREN) {
					acceptIt();
					accept(TokenKind.SEMICOLON);
					el = new ExprList();
				} else {
					el = parseArgumentList();//argList is 0 or 1
					accept(TokenKind.RPAREN);
					accept(TokenKind.SEMICOLON);
				}
				s = new CallStmt(r, el, r.posn);
				return s;
			} else if (token.kind == TokenKind.LBRACKET) {//THIS IxAssignStmt
				acceptIt();
				Expression idx = parseExpression();
				accept(TokenKind.RBRACKET);
				accept(TokenKind.SETEQUAL);
				Expression ex = parseExpression();
				accept(TokenKind.SEMICOLON);
				s = new IxAssignStmt(r, idx, ex, r.posn);
				return s;
			} else {
				parseError("Invalid term- found unexpected "+token.kind);
			}
			break;
		case RETURN:
			SourcePosition retSrc = token.posn;
			acceptIt();
			Expression ex = null;
			if (token.kind == TokenKind.SEMICOLON) {
				acceptIt();
			} else {
				ex = parseExpression();
				accept(TokenKind.SEMICOLON);
			}
			s = new ReturnStmt(ex, retSrc);
			return s;
		case IF:
			SourcePosition ifSrc = token.posn;
			acceptIt();
			accept(TokenKind.LPAREN);
			Expression cond = parseExpression();
			accept(TokenKind.RPAREN);
			Statement then = parseStatement();
			Statement elseStmt = null;
			if (token.kind == TokenKind.ELSE) {
				acceptIt();
				elseStmt = parseStatement();
			}
			s = new IfStmt(cond, then, elseStmt, ifSrc);
			return s;
		case WHILE:
			SourcePosition whileSrc = token.posn;
			acceptIt();
			accept(TokenKind.LPAREN);
			Expression exp = parseExpression();
			accept(TokenKind.RPAREN);
			Statement stmt = parseStatement();
			s = new WhileStmt(exp, stmt, whileSrc);
			return s;
		default:
			parseError("Invalid term- found unexpected "+token.kind);
			s = null;
			return s;
		}
		s = null;
		return s;
	}
	
	private Expression parseExpression() {
		Expression e = parseDisjunction();
		while (token.kind == TokenKind.OR) {
			Operator op = new Operator(token);
			acceptIt();
			Expression e2 = parseDisjunction();
			e = new BinaryExpr(op, e, e2, op.posn);
		}
		return e;
	}
	
	private Expression parseDisjunction() {
		Expression e = parseConjunction();
		while (token.kind == TokenKind.AND) {
			Operator op = new Operator(token);
			acceptIt();
			Expression e2 = parseConjunction();
			e = new BinaryExpr(op, e, e2, op.posn);
		}
		return e;
	}
	
	private Expression parseConjunction() {
		Expression e = parseEquality();
		while (token.kind == TokenKind.EQUALS || token.kind == TokenKind.NOTEQUALS) {
			Operator op = new Operator(token);
			acceptIt();
			Expression e2 = parseEquality();
			e = new BinaryExpr(op, e, e2, op.posn);
		}
		return e;
	}
	
	private Expression parseEquality() {
		Expression e = parseRelational();
		while (token.kind == TokenKind.LESSEQUAL || token.kind == TokenKind.LESSTHAN 
				|| token.kind == TokenKind.GREATERTHAN || token.kind == TokenKind.GREATEREQUAL) {
			Operator op = new Operator(token);
			acceptIt();
			Expression e2 = parseRelational();
			e = new BinaryExpr(op, e, e2, op.posn);
		}
		return e;
	}
	
	private Expression parseRelational() {
		Expression e = parseAdditive();
		while (token.kind == TokenKind.PLUS || token.kind == TokenKind.MINUS) {
			Operator op = new Operator(token);
			acceptIt();
			Expression e2 = parseAdditive();
			e = new BinaryExpr(op, e, e2, op.posn);
		}
		return e;
	}
	
	private Expression parseAdditive() {
		Expression e = parseMultiplicative();
		while (token.kind == TokenKind.MULTIPLY || token.kind == TokenKind.DIVIDE) {
			Operator op = new Operator(token);
			acceptIt();
			Expression e2 = parseMultiplicative();
			e = new BinaryExpr(op, e, e2, op.posn);
		}
		return e;
	}
	
	private Expression parseMultiplicative() {
		Expression e;
		if (token.kind == TokenKind.MINUS || token.kind == TokenKind.NOT) {
			Operator op = new Operator(token);
			acceptIt();
			Expression e2 = parseMultiplicative();
			e = new UnaryExpr(op, e2, op.posn);
		} else {
			e = parseUnary();
		}
		return e;
	}
	
	private Expression parseUnary() {
		Expression e = null;
		//
		if (token.kind == TokenKind.LPAREN) {
			acceptIt();
			e = parseExpression();
			accept(TokenKind.RPAREN);
		} else {
			switch (token.kind) {
			case ID: case THIS:
				Reference r = parseReference();
				if (token.kind == TokenKind.LBRACKET) {//IxReference: IxExpr
					accept(TokenKind.LBRACKET);
					Expression ex = parseExpression();
					accept(TokenKind.RBRACKET);
					e = new IxExpr(r, ex, r.posn);
				} else if (token.kind == TokenKind.LPAREN) {//Reference(ArgList?);: CallExpr
					acceptIt();
					ExprList el = new ExprList();
					if (token.kind != TokenKind.RPAREN) {
						el = parseArgumentList();//argList is 0 or 1
					}
					accept(TokenKind.RPAREN);
					e = new CallExpr(r, el, r.posn);
				} else {
					e = new RefExpr(r, r.posn);//Reference: RefExpr
				}
				break;
			case NUM: 
				IntLiteral intLit = new IntLiteral(token);
				acceptIt();
				e = new LiteralExpr(intLit, intLit.posn);
				break;
			case TRUE: case FALSE:
				BooleanLiteral booLit = new BooleanLiteral(token);
				acceptIt();
				e = new LiteralExpr(booLit, booLit.posn);
				break;
			case NULL:
				NullLiteral nullLit = new NullLiteral(token);
				acceptIt();
				e = new LiteralExpr(nullLit, nullLit.posn);
				break;
			case NEW:
				acceptIt();
				if (token.kind == TokenKind.ID) {
					Identifier id = new Identifier(token);
					ClassType type = new ClassType(id, id.posn);
					acceptIt();
					if (token.kind == TokenKind.LPAREN) {//NEW id(): NewObjectExpr
						acceptIt();
						accept(TokenKind.RPAREN);
						e = new NewObjectExpr(type, type.posn);
					} else if (token.kind == TokenKind.LBRACKET) {//NEW id[Expr]: NewArrayExpr
						acceptIt();
						Expression ex = parseExpression();
						accept(TokenKind.RBRACKET);
						e = new NewArrayExpr(type, ex, type.posn);
					} else {
						parseError("Invalid term- expecting LPAREN or LBRACKET but found "+token.kind);
					}
				} else {//NEW int[Expr]: NewArrayExpr
					SourcePosition bSrc = token.posn;
					accept(TokenKind.INT);
					accept(TokenKind.LBRACKET);
					Expression ex = parseExpression();
					accept(TokenKind.RBRACKET);
					BaseType baseType = new BaseType(TypeKind.INT, bSrc);
					e = new NewArrayExpr(baseType, ex, baseType.posn);
				}
				break;
			default:
				parseError("Invalid term- found unexpected "+token.kind);
				break;
			}
		}
		return e;
	}
	
	private void acceptIt() throws SyntaxError {
		accept(token.kind);
	}
	
	private void accept(TokenKind expectedTokenKind) throws SyntaxError {
		if (token.kind == expectedTokenKind) {
			if (trace) {
				pTrace();
			}
			token = scanner.scan();
		} else {
			parseError("expecting '" + expectedTokenKind + 
					"' but found '" + token.kind + "'");
		}
	}
	
	private void parseError(String e) throws SyntaxError {
		reporter.reportError("Parse error: "+e);
		throw new SyntaxError();
	}
	
	private void pTrace() {
		StackTraceElement [] stl = Thread.currentThread().getStackTrace();
		for (int i = stl.length - 1; i > 0 ; i--) {
		//	if(stl[i].toString().contains("parse"))
		//		System.out.println(stl[i]);
		}
		//System.out.println("accepting: " + token.kind + " (\"" + token.spelling + "\")");
		//System.out.println();
	}
}
