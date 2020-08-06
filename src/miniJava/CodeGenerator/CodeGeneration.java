package miniJava.CodeGenerator;

import java.util.ArrayList;
import mJAM.Machine;
import mJAM.Machine.Op;
import mJAM.Machine.Prim;
import mJAM.Machine.Reg;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.TokenKind;
//									Arg	   Result
public class CodeGeneration implements Visitor<Object, Object>{

	int mainAddr;
	int staticFieldsAddr;
	MethodDecl currMethod;
	int localVarDisplacement;
	int numBlockVars;
	ArrayList<MethodPatch> patchList = new ArrayList<MethodPatch>();
	
	public void generateCode(AST ast) {
		Machine.initCodeGen();		
		ast.visit(this, null);
	}

	@Override
	public Object visitPackage(Package prog, Object arg) {
		//get fieldDecl and ClassDecl REDs
		int staticFieldOffset = 0;
		for (ClassDecl c : prog.classDeclList) {
			int instanceFieldOffset = 0;
			for (FieldDecl f : c.fieldDeclList) {
				if (f.isStatic) {
					Machine.emit(Op.PUSH, 1);
					f.RED = staticFieldOffset;
					staticFieldOffset++;
				} else {
					f.RED = instanceFieldOffset;
					instanceFieldOffset++;
				}
			}
			c.RED = instanceFieldOffset;
		}
		
		//Preamble code: call main, HALT after main runs
		Machine.emit(Op.LOADL, 0);
		Machine.emit(Prim.newarr); 
		mainAddr = Machine.nextInstrAddr(); 
		Machine.emit(Op.CALL, Reg.CB, -1);
		Machine.emit(Op.HALT, 0, 0, 0);
		
		for (ClassDecl c: prog.classDeclList) {
			c.visit(this, null);
		}
		
		for (MethodPatch mp: patchList) {
			Machine.patch(mp.addr, mp.method.RED);
		}
		
		return null;
	}

	@Override
	public Object visitClassDecl(ClassDecl cd, Object arg) {
		for (MethodDecl m: cd.methodDeclList) {
			m.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitFieldDecl(FieldDecl fd, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private boolean isMain(MethodDecl m) {
		if (m.name.equals("main") && !m.isPrivate && m.isStatic && m.type.typeKind == TypeKind.VOID) {
			if (m.parameterDeclList.size() == 1) {
				ParameterDecl p = m.parameterDeclList.get(0);
				if (p.type.typeKind == TypeKind.ARRAY) {
					ArrayType at = (ArrayType) p.type;
					if (at.eltType.typeKind == TypeKind.CLASS) {
						ClassType ct = (ClassType) at.eltType;
						if (ct.className.spelling.equals("String")) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	@Override
	public Object visitMethodDecl(MethodDecl md, Object arg) {
		currMethod = md;
		localVarDisplacement = 3;
		md.RED = Machine.nextInstrAddr();
		
		if (isMain(md)) {
			Machine.patch(mainAddr, Machine.nextInstrAddr());
		}
		
		int paramDisplacement = -md.parameterDeclList.size();
		for (ParameterDecl p: md.parameterDeclList) {
			p.RED = paramDisplacement;
			paramDisplacement++;
		}
		
		for (Statement s: md.statementList) {
			s.visit(this, null);
		}
		
		if (md.type.typeKind == TypeKind.VOID) {
			Machine.emit(Op.RETURN, 0, 0, md.parameterDeclList.size());
		} else {
			Machine.emit(Op.RETURN, 1, 0, md.parameterDeclList.size());
		}
		
		return null;
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl decl, Object arg) {
		decl.RED = localVarDisplacement;
		localVarDisplacement++;
		
		return null;
	}

	@Override
	public Object visitBaseType(BaseType type, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitClassType(ClassType type, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitArrayType(ArrayType type, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitBlockStmt(BlockStmt stmt, Object arg) {
		numBlockVars = 0;
		for (Statement s: stmt.sl) {
			s.visit(this, null);
		}
		Machine.emit(Op.POP, numBlockVars);
		localVarDisplacement -= numBlockVars;
		
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		numBlockVars++;
		stmt.varDecl.visit(this, null);
		stmt.initExp.visit(this, null);
		
		return null;
	}
	

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		if (stmt.ref instanceof IdRef) {
			//idref
            int offset = stmt.ref.decl.RED;
            stmt.val.visit(this, null);
            if (stmt.ref.decl instanceof FieldDecl && ((FieldDecl) (stmt.ref.decl)).isStatic) {
                Machine.emit(Op.STORE, Reg.SB, offset);
            } else if (stmt.ref.decl instanceof FieldDecl) { //non static fieldDecl
                Machine.emit(Op.STORE, Reg.OB, offset); // store into var
            } else {
                Machine.emit(Op.STORE, Reg.LB, offset); // store into var
            }
        } else if (stmt.ref instanceof QualRef) {
            if (stmt.ref.decl instanceof FieldDecl && ((FieldDecl) (stmt.ref.decl)).isStatic) {
            	//static field QRef
                int offset = stmt.ref.decl.RED;
                stmt.val.visit(this, null);
                Machine.emit(Op.STORE, Reg.SB, offset);
            }  else {
                //stmt.ref.visit(this, null);
            	QualRef qr = (QualRef) stmt.ref;
            	qr.ref.visit(this, null);
            	Machine.emit(Op.LOADL, qr.id.decl.RED);
            	//end of stmt.ref.visit
                stmt.val.visit(this, null);
                Machine.emit(Prim.fieldupd); // store into var
            }
        }
        return null;
	}
	

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {		
		if (stmt.ref instanceof IdRef) {
			IdRef idr = (IdRef) stmt.ref;
			idr.id.visit(this, null);
			stmt.ix.visit(this, null);
		} else if (stmt.ref instanceof QualRef) {
			QualRef qr = (QualRef) stmt.ref;
			if (qr.id.decl instanceof FieldDecl && ((FieldDecl) qr.id.decl).isStatic) {
				stmt.ix.visit(this, null);
			} else {
				qr.ref.visit(this, null);
				Machine.emit(Op.LOADL, qr.id.decl.RED);
				Machine.emit(Prim.fieldref);
				stmt.ix.visit(this, null);
			}
		}
		stmt.exp.visit(this, null);
		Machine.emit(Prim.arrayupd);
		return null;
	}
	
	private boolean isPrintln(CallStmt stmt) {
		if (stmt.methodRef instanceof QualRef && stmt.argList.size()==1 && stmt.methodRef.decl.name.equals("println")) {
			QualRef qr = (QualRef) stmt.methodRef;
			if (qr.ref instanceof QualRef) {
				QualRef qr2 = (QualRef) qr.ref;
				if (qr2.ref.decl instanceof ClassDecl && qr2.ref.decl.name.equals("System")) {
					return true;
				}
			}
		}
		return false;
	}
	

	@Override
	public Object visitCallStmt(CallStmt stmt, Object arg) {
		for (Expression e: stmt.argList) {
			e.visit(this, null);
		}
		
		if (isPrintln(stmt)) {
            Machine.emit(Prim.putintnl);
        } else if (stmt.methodRef instanceof IdRef) {
            // IdRef method call
            if (((MethodDecl) stmt.methodRef.decl).isStatic) {
                //static method
                patchList.add(new MethodPatch(Machine.nextInstrAddr(), (MethodDecl) stmt.methodRef.decl));
                Machine.emit(Op.CALL, Reg.CB, -1);
            } else {
                //non-static method
                Machine.emit(Op.LOAD, Reg.LB, 0);
                patchList.add(new MethodPatch(Machine.nextInstrAddr(), (MethodDecl) stmt.methodRef.decl));
                Machine.emit(Op.CALLI, Reg.CB, -1);
            }
        } else {
            //qualified method call
            stmt.methodRef.visit(this, null);
            patchList.add(new MethodPatch(Machine.nextInstrAddr(), (MethodDecl) stmt.methodRef.decl));
            Machine.emit(Op.CALLI, Reg.CB, -1);
        }
        //pop return
        if (stmt.methodRef.decl.type.typeKind != TypeKind.VOID) {
        	Machine.emit(Op.POP, 0, 0, 1);
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
        int addrIf = Machine.nextInstrAddr();
        Machine.emit(Op.JUMPIF, 0, Reg.CB, -1);

        stmt.thenStmt.visit(this, null);
        int addrThen = Machine.nextInstrAddr();
        Machine.emit(Op.JUMP, Reg.CB, -1);

        int addrElse = Machine.nextInstrAddr();
        Machine.patch(addrIf, addrElse);
        if (stmt.elseStmt != null) {
            stmt.elseStmt.visit(this, null);
        }

        int addrEnd = Machine.nextInstrAddr();
        Machine.patch(addrThen, addrEnd);
        return null;
	}
	

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		int addrWhile = Machine.nextInstrAddr();
        stmt.cond.visit(this, null);
        int addrBody = Machine.nextInstrAddr();
        Machine.emit(Op.JUMPIF, 0, Reg.CB, -1);

        stmt.body.visit(this, arg);
        Machine.emit(Op.JUMP, Reg.CB, addrWhile);

        int addrEnd = Machine.nextInstrAddr();
        Machine.patch(addrBody, addrEnd);
        return null;
	}
	

	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		expr.expr.visit(this, null);
		if (expr.operator.kind == TokenKind.MINUS) {
			Machine.emit(Prim.neg);
		} else {
			expr.operator.visit(this, null);
		}
		
		return null;
	}
	

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
		if (expr.operator.kind == TokenKind.AND) {
			//short circuit AND
			expr.left.visit(this, null);
			
			int addrAnd = Machine.nextInstrAddr();
            Machine.emit(Op.JUMPIF, Machine.falseRep, Reg.CB, -1);

            Machine.emit(Op.LOADL, 1);
            expr.right.visit(this, null);
            Machine.emit(Prim.and);
            int addrEnd = Machine.nextInstrAddr();
            Machine.emit(Op.JUMP, Reg.CB, -1);

            Machine.patch(addrAnd, Machine.nextInstrAddr());
            Machine.emit(Op.LOADL, 0);
            Machine.patch(addrEnd, Machine.nextInstrAddr());
		} else if (expr.operator.kind == TokenKind.OR) {
			//short circuit OR
			expr.left.visit(this, null);
			
			int addrOr = Machine.nextInstrAddr();
			Machine.emit(Op.JUMPIF, Machine.trueRep, Reg.CB, -1);
			
			Machine.emit(Op.LOADL, 0);
			expr.right.visit(this, null);
			Machine.emit(Prim.or);
			int addrEnd = Machine.nextInstrAddr();
			Machine.emit(Op.JUMP, Reg.CB, -1);
			
			Machine.patch(addrOr, Machine.nextInstrAddr());
			Machine.emit(Op.LOADL, 1);
			Machine.patch(addrEnd, Machine.nextInstrAddr());
		} else {
			expr.left.visit(this, null);
			expr.right.visit(this, null);
			expr.operator.visit(this, null);
		}
		
		return null;
	}
	

	@Override
	public Object visitRefExpr(RefExpr expr, Object arg) {
		expr.ref.visit(this, null);
		return null;
	}
	

	@Override
	public Object visitIxExpr(IxExpr expr, Object arg) {		
		if (expr.ref instanceof IdRef) {
			IdRef idr = (IdRef) expr.ref;
			idr.id.visit(this, null);
			expr.ixExpr.visit(this, null);
			Machine.emit(Prim.arrayref);
		} else if (expr.ref instanceof QualRef) {
			QualRef qr = (QualRef) expr.ref;
			if (qr.id.decl instanceof FieldDecl && ((FieldDecl) qr.id.decl).isStatic) {
				Machine.emit(Op.LOAD, Reg.SB, qr.id.decl.RED);
				expr.ixExpr.visit(this, null);
				Machine.emit(Prim.arrayref);
			} else if (qr.id.decl instanceof FieldDecl) {
				qr.ref.visit(this, null);
				Machine.emit(Op.LOADL, qr.id.decl.RED);
				Machine.emit(Prim.fieldref);
				expr.ixExpr.visit(this, null);
				Machine.emit(Prim.arrayref);
			}
		}
		return null;
	}
	

	private boolean isPrintln(CallExpr expr) {
		if (expr.functionRef instanceof QualRef && expr.argList.size()==1 && expr.functionRef.decl.name.equals("println")) {
			QualRef qr = (QualRef) expr.functionRef;
			if (qr.ref instanceof QualRef) {
				QualRef qr2 = (QualRef) qr.ref;
				if (qr2.ref.decl instanceof ClassDecl && qr2.ref.decl.name.equals("System")) {
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public Object visitCallExpr(CallExpr expr, Object arg) {
		for (Expression a: expr.argList) {
			a.visit(this, null);
		}
		
		if (isPrintln(expr)) {
			Machine.emit(Prim.putintnl);
		} else if (expr.functionRef instanceof IdRef) {
			if (((MethodDecl) expr.functionRef.decl).isStatic) {
                patchList.add(new MethodPatch(Machine.nextInstrAddr(), (MethodDecl) expr.functionRef.decl));
                Machine.emit(Op.CALL, Reg.CB, -1);
            } else {
                Machine.emit(Op.LOADA, Reg.OB, 0);
                patchList.add(new MethodPatch(Machine.nextInstrAddr(), (MethodDecl) expr.functionRef.decl));
                Machine.emit(Op.CALLI, Reg.CB, -1);
            }
		} else {
			expr.functionRef.visit(this, null);

            patchList.add(new MethodPatch(Machine.nextInstrAddr(), (MethodDecl) expr.functionRef.decl));
            Machine.emit(Op.CALLI, Machine.Reg.CB, -1);
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
		Machine.emit(Op.LOADL, -1);
		Machine.emit(Op.LOADL, expr.classtype.className.decl.RED);
		Machine.emit(Prim.newobj);
		
		return null;
	}
	

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		expr.sizeExpr.visit(this, null);
		Machine.emit(Prim.newarr);
		return null;
	}
	

	@Override
	public Object visitThisRef(ThisRef ref, Object arg) {
		Machine.emit(Op.LOADA, Reg.OB, 0);
		return null;
	}
	

	@Override
	public Object visitIdRef(IdRef ref, Object arg) {
		if (ref.decl instanceof FieldDecl && ((FieldDecl) ref.decl).isStatic) {
			Machine.emit(Op.LOAD, Reg.SB, ref.decl.RED);
		} else if (ref.decl instanceof FieldDecl) {
			Machine.emit(Op.LOAD, Reg.OB, ref.decl.RED);
		} else {
			Machine.emit(Op.LOAD, Reg.LB, ref.decl.RED);
		}
		
		return null;
	}
	

	@Override
	public Object visitQRef(QualRef ref, Object arg) {
		if (ref.ref.decl.type instanceof ArrayType && ref.id.spelling.equals("length")) {
			ref.ref.visit(this, null);
			Machine.emit(Prim.arraylen);
		} else if (ref.id.decl instanceof FieldDecl && ((FieldDecl) ref.id.decl).isStatic) {
			Machine.emit(Op.LOAD, Reg.SB, ref.id.decl.RED);
		} else if (ref.id.decl instanceof FieldDecl) {
			ref.ref.visit(this, null);
			Machine.emit(Op.LOADL, ref.id.decl.RED);
			Machine.emit(Prim.fieldref);
		} else if (ref.id.decl instanceof MethodDecl) {
			ref.ref.visit(this, null);
		}
		
		return null;
	}
	

	@Override
	public Object visitIdentifier(Identifier id, Object arg) {
		if (id.decl instanceof FieldDecl && ((FieldDecl) id.decl).isStatic) {
            Machine.emit(Op.LOAD, Reg.SB, id.decl.RED);
        } else if (id.decl instanceof FieldDecl) {
            Machine.emit(Op.LOAD, Reg.OB, id.decl.RED);
        } else {
            Machine.emit(Op.LOAD, Reg.LB, id.decl.RED);
        }
		
		return null;
	}
	

	@Override
	public Object visitOperator(Operator op, Object arg) {
		switch (op.kind) {
		case LESSTHAN:
			Machine.emit(Prim.lt);
			break;
		case GREATERTHAN:
			Machine.emit(Prim.gt);
			break;
		case EQUALS:
			Machine.emit(Prim.eq);
			break;
		case LESSEQUAL:
			Machine.emit(Prim.le);
			break;
		case GREATEREQUAL:
			Machine.emit(Prim.ge);
			break;
		case NOTEQUALS:
			Machine.emit(Prim.ne);
			break;
		case AND:
			Machine.emit(Prim.and);
			break;
		case OR:
			Machine.emit(Prim.or);
			break;
		case PLUS:
			Machine.emit(Prim.add);
			break;
		case MINUS:
			Machine.emit(Prim.sub);
			break;
		case MULTIPLY:
			Machine.emit(Prim.mult);
			break;
		case DIVIDE:
			Machine.emit(Prim.div);
			break;
		case NOT:
			Machine.emit(Prim.not);
			break;
		default:
			break;
		}
		
		return null;
	}
	

	@Override
	public Object visitIntLiteral(IntLiteral num, Object arg) {
		int numVal = Integer.parseInt(num.spelling);
		Machine.emit(Op.LOADL, numVal);
		return null;
	}
	

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		if (bool.kind == TokenKind.TRUE) {
			Machine.emit(Op.LOADL, Machine.trueRep);
		} else {
			Machine.emit(Op.LOADL, Machine.falseRep);
		}
		return null;
	}
	

	@Override
	public Object visitNullLiteral(NullLiteral nullLit, Object arg) {
		Machine.emit(Op.LOADL, Machine.nullRep);
		return null;
	}
	
}
