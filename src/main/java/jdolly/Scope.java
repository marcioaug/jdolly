package jdolly;

public class Scope {

	private int maxPackage = 2;
	private int maxClass = 3;
	private int maxMethod = 3;
	private int maxField = 2;
	private String theoryFile = "alloyTheory/default.als";
	private int skipSize = 25;
	private String output = "";

	public Scope() { }
	
	public Scope(final int maxPackage, final int maxClass, final int maxMethod, final int maxField) {
		this.maxPackage = maxPackage;
		this.maxClass = maxClass;
		this.maxMethod = maxMethod;
		this.maxField = maxField;
	}

	public int getMaxPackage() {
		return maxPackage;
	}

	public void setMaxPackage(final int maxPackage) {
		this.maxPackage = maxPackage;
	}

	public int getMaxClass() {
		return maxClass;
	}

	public void setMaxClass(final int maxClass) {
		this.maxClass = maxClass;
	}

	public int getMaxMethod() {
		return maxMethod;
	}

	public void setMaxMethod(final int maxMethod) {
		this.maxMethod = maxMethod;
	}

	public int getMaxField() {
		return maxField;
	}

	public void setMaxField(final int maxField) {
		this.maxField = maxField;
	}


	public String getTheoryFile() {
		return this.theoryFile;
	}

	public void setTheoryFile(final String theoryFile) {
		this.theoryFile = theoryFile;
	}

	public int getSkipSize() {
		return this.skipSize;
	}

	public void setSkipSize(final int skipSize) {
		this.skipSize = skipSize;
	}

	public String getOutput() {
		return this.output;
	}

	public void setOutput(String output) {
		this.output = output;
	}

	@Override
	public String toString() {
		return "Scope: maxPackage=" + maxPackage + ", maxClass=" + maxClass + ", maxMethod=" + maxMethod + ", maxField="
				+ maxField + "]";
	}
	
}
