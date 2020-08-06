package miniJava.SyntacticAnalyzer;

public class Token {
	public TokenKind kind;
	public String spelling;
	public SourcePosition posn;
	
	public Token(TokenKind kind, String spelling, SourcePosition posn) {
		this.kind = kind;
		this.spelling = spelling;
		this.posn = posn;
	}
}
