package miniJava.SyntacticAnalyzer;

public enum TokenKind {ID, 
	//keywords:
	CLASS, VOID, PUBLIC, PRIVATE, STATIC, INT, BOOLEAN,
	THIS, RETURN, IF, ELSE, WHILE, TRUE, FALSE, NEW,
	NULL,
	//end of keywords
	NUM, 
	LPAREN, RPAREN, LCURLY, RCURLY, LBRACKET, RBRACKET,
	DOT, COMMA, SEMICOLON,
	LESSTHAN, LESSEQUAL, GREATERTHAN, GREATEREQUAL, EQUALS, NOTEQUALS, SETEQUAL,
	PLUS, MINUS, MULTIPLY, DIVIDE, 
	AND, OR, NOT, 
	EOT, ERROR}