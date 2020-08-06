/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

abstract public class TypeDenoter extends AST {
    
    public TypeDenoter(TypeKind type, SourcePosition posn){
        super(posn);
        typeKind = type;
    }
    
    public TypeKind typeKind;
    
    public boolean equals(Object other) {
    	TypeDenoter otherType = (TypeDenoter) other;
    	if (this.typeKind == TypeKind.ERROR || otherType.typeKind == TypeKind.ERROR) {
    		return true;
    	} else if (this.typeKind == TypeKind.UNSUPPORTED || otherType.typeKind == TypeKind.UNSUPPORTED) {
    		return false;
    	} else if ((this.typeKind == TypeKind.CLASS && otherType.typeKind == TypeKind.NULL) || 
    			(this.typeKind == TypeKind.NULL && otherType.typeKind == TypeKind.CLASS)) {
    		return true;
    	} else if (this.typeKind == TypeKind.CLASS && otherType.typeKind == TypeKind.CLASS) {
    		ClassType thisC = (ClassType) this;
    		ClassType otherC = (ClassType) otherType;
    		return thisC.className.spelling.equals(otherC.className.spelling);
    	} else {
    		return this.typeKind == otherType.typeKind;
    	}
    }
    
}

        