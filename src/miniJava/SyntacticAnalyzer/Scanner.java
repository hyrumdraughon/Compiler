package miniJava.SyntacticAnalyzer;

import java.io.*;

import miniJava.ErrorReporter;

public class Scanner {
	private InputStream inputStream;
	private ErrorReporter reporter;
	private char currChar;
	private StringBuilder currentSpelling;
	private boolean eot = false;
	private int lineNum;
	
	public Scanner(InputStream inputStream, ErrorReporter reporter) {
		this.inputStream = inputStream;
		this.reporter = reporter;
		this.lineNum = 1;
		
		readChar();
	}
	
	public Token scan() {
		//skip whitespace, tabs, newlines, and carriage returns
		while (!eot && (currChar == ' ' || currChar == '\t' || currChar == '\n' || currChar == '\r' || currChar == '/')) {
			if (currChar == '\n' || currChar == '\r') {
				lineNum++;//increase lineNum
			}
			
			if (currChar != '/') {
				skipIt();
			} else {
				skipIt();
				if (currChar == '/') {
					skipIt();
					while (!eot && currChar != '\n') {
						skipIt();
					}
					if (currChar == '\n') {
						lineNum++;
					}
					skipIt();
				} else if (currChar == '*') {
					skipIt();
					skipComments();
				} else {
					currentSpelling = new StringBuilder();
					currentSpelling.append('/');
					TokenKind kind = TokenKind.DIVIDE;
					String spelling = currentSpelling.toString();
					SourcePosition posn = new SourcePosition(lineNum);
					return new Token (kind, spelling, posn);
				}
			}
		}
		
		currentSpelling = new StringBuilder();
		TokenKind kind = scanToken();
		String spelling = currentSpelling.toString();
		SourcePosition posn = new SourcePosition(lineNum);
		return new Token(kind, spelling, posn);
	}
	
	public TokenKind scanToken() {
		if (eot) {
			return TokenKind.EOT;
		}
		
		switch (currChar) {
		//IDs and keywords
		case 'a': case 'b': case 'c': case 'd': case 'e': case 'f': case 'g': case 'h': case 'i': case 'j': case 'k': case 'l': case 'm': case 'n': case 'o': case 'p': case 'q': case 'r': case 's': case 't': case 'u': case 'v': case 'w': case 'x': case 'y': case 'z':
		case 'A': case 'B': case 'C': case 'D': case 'E': case 'F': case 'G': case 'H': case 'I': case 'J': case 'K': case 'L': case 'M': case 'N': case 'O': case 'P': case 'Q': case 'R': case 'S': case 'T': case 'U': case 'V': case 'W': case 'X': case 'Y': case 'Z':
			while (isAlphaNum(currChar)) {
				takeIt();
			}
			switch (currentSpelling.toString()) {//or spelling?????
			case "class":
				return TokenKind.CLASS;
			case "void":
				return TokenKind.VOID;
			case "public":
				return TokenKind.PUBLIC;
			case "private":
				return TokenKind.PRIVATE;
			case "static":
				return TokenKind.STATIC;
			case "int":
				return TokenKind.INT;
			case "boolean":
				return TokenKind.BOOLEAN;
			case "this":
				return TokenKind.THIS;
			case "return":
				return TokenKind.RETURN;
			case "if":
				return TokenKind.IF;
			case "else":
				return TokenKind.ELSE;
			case "while":
				return TokenKind.WHILE;
			case "true":
				return TokenKind.TRUE;
			case "false":
				return TokenKind.FALSE;
			case "new":
				return TokenKind.NEW;
			case "null":
				return TokenKind.NULL;
			default:
				return TokenKind.ID;
			}
		//num
		case '0': case '1': case '2': case '3': case '4':
		case '5': case '6': case '7': case '8': case '9':
			while (isDigit(currChar)) {
				takeIt();
			}
			return(TokenKind.NUM);
		//parens, curlies, brackets
		case '(':
			takeIt();
			return TokenKind.LPAREN;	
		case ')':
			takeIt();
			return TokenKind.RPAREN;	
		case '{':
			takeIt();
			return TokenKind.LCURLY;
		case '}':
			takeIt();
			return TokenKind.RCURLY;
		case '[':
			takeIt();
			return TokenKind.LBRACKET;
		case ']':
			takeIt();
			return TokenKind.RBRACKET;
		//punctuation
		case '.':
			takeIt();
			return TokenKind.DOT;
		case ',':
			takeIt();
			return TokenKind.COMMA;
		case ';':
			takeIt();
			return TokenKind.SEMICOLON;
		//relational ops plus NOT
		case '<':
			takeIt();
			if (currChar == '=') {
				takeIt();
				return TokenKind.LESSEQUAL;
			} else {
				return TokenKind.LESSTHAN;
			}
		case '>':
			takeIt();
			if (currChar == '=') {
				takeIt();
				return TokenKind.GREATEREQUAL;
			} else {
				return TokenKind.GREATERTHAN;
			}
		case '=':
			takeIt();
			if (currChar == '=') {
				takeIt();
				return TokenKind.EQUALS;
			} else {
				return TokenKind.SETEQUAL;
			}
		case '!':
			takeIt();
			if (currChar == '=') {
				takeIt();
				return TokenKind.NOTEQUALS;
			} else {
				return TokenKind.NOT;
			}
		//arith ops minus DIVIDE
		case '+':
			takeIt();
			return TokenKind.PLUS;
		case '-':
			takeIt();
			return TokenKind.MINUS;
		case '*':
			takeIt();
			return TokenKind.MULTIPLY;
		//logical ops minus NOT
		case '&':
			skipIt();
			if (currChar == '&') {
				currentSpelling.append('&');
				takeIt();
				return TokenKind.AND;
			} else {
				scanError("Unrecognized character '"+currChar+"' in input");
				return TokenKind.ERROR;
			}
		case '|':
			skipIt();
			if (currChar == '|') {
				currentSpelling.append('|');
				takeIt();
				return TokenKind.OR;
			} else {
				scanError("Unrecognized character '"+currChar+"' in input");
				return TokenKind.ERROR;
			}
		//default = scanError	
		default:
//			if (eot) {
//				return TokenKind.EOT;
//			}
			scanError("Unrecognized character '"+currChar+"' in input");
			return TokenKind.ERROR;
		}
	}
	
	private void takeIt() {
		currentSpelling.append(currChar);
		nextChar();
	}

	private void skipIt() {
		nextChar();
	}
	
	private void skipComments() {
		while (!eot && currChar != '*') {
			if (currChar == '\n' || currChar == '\r') {
				lineNum++;
			}
			skipIt();
		}
		//currChar == '*'
		skipIt();
		if (eot) {
			scanError("Unterminated comment");
		} else if (currChar == '/') {
			skipIt();
		} else {
			skipComments();
		}
	}
	
	private boolean isDigit(char c) {
		return (c >= '0') && (c <= '9');
	}
	
	private boolean isAlphaNum(char c) {
		switch (c) {
		case 'a': case 'b': case 'c': case 'd': case 'e': case 'f': case 'g': case 'h': case 'i': case 'j': case 'k': case 'l': case 'm': case 'n': case 'o': case 'p': case 'q': case 'r': case 's': case 't': case 'u': case 'v': case 'w': case 'x': case 'y': case 'z':
		case 'A': case 'B': case 'C': case 'D': case 'E': case 'F': case 'G': case 'H': case 'I': case 'J': case 'K': case 'L': case 'M': case 'N': case 'O': case 'P': case 'Q': case 'R': case 'S': case 'T': case 'U': case 'V': case 'W': case 'X': case 'Y': case 'Z':
		case '0': case '1': case '2': case '3': case '4':
		case '5': case '6': case '7': case '8': case '9':
		case '_':
			return true;
		default:
			return false;
		}
	}
	
	private void scanError(String m) {
		reporter.reportError("Scan Error: " + m);
	}
	
	//private final static char
	private void nextChar() {
		if (!eot) {
			readChar();
		}
	}
	
	private void readChar() {
		try {
			int c = inputStream.read();
			currChar = (char) c;
			if (c == -1) {
				this.eot = true;
			}
		} catch (IOException e) {
			scanError("I/O Exception!");
			eot = true;
		}
	}
}
