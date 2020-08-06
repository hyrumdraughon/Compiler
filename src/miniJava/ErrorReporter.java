package miniJava;

public class ErrorReporter {
	private int numErrors;
	
	public ErrorReporter() {
		this.numErrors = 0;
	}
	
	public boolean hasErrors() {
		return numErrors > 0;
	}

	public void reportError(String message) {
		System.out.println(message);
		numErrors++;
	}
}
