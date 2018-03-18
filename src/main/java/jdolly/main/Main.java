package jdolly.main;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jdolly.JDolly;
import jdolly.JDollyFactory;
import jdolly.Scope;
import jdolly.examples.TestLogger;

import org.eclipse.jdt.core.dom.CompilationUnit;

public class Main {

	private static final String JDOLLY_FILE = "JDollyFile.yml";

	private static String theoryFile;
	private static String output;
	private static JDolly generator;
	private static Scope scope;
	private static int skipSize;
	private static ProgramDetailer programDetailer;
	private static Long maxPrograms;

	public static void main(String[] args) {
		parseArguments(args);

		File jDollyFile = getJDollyFile();

		if (jDollyFile != null) {
			try {
				scope = new ObjectMapper(new YAMLFactory()).readValue(jDollyFile, Scope.class);
			} catch (IOException e) {
				System.err.println("Não foi possível abrir o arquivo de configuração.\n" + e);
			}
		} else {
			scope = new Scope();
		}

        theoryFile = scope.getTheoryFile();
        skipSize = scope.getSkipSize();
        output = scope.getOutput();

		generator = JDollyFactory.createJDolly(scope, theoryFile);
		
		programDetailer = new ProgramDetailer(/* print the Programs? = */ true, 
											  /* print the Logfiles? = */ true, 
											  /* check for compilation errors? = */ false);
		generatePrograms(programDetailer, maxPrograms, skipSize);

	}

	public static void generatePrograms(ProgramDetailer programDetailer, Long maxPrograms, int skip) {

		long count = 0;

		TestLogger logger = new TestLogger(output);
		
		final boolean checkCompilationErrors = programDetailer.shouldCheckForCompilationErrors();
		
    		for (List<CompilationUnit> cus : generator) {
			
			count++;
			
			if (count % skip != 0){ 
				continue;
			}
			if (maxPrograms != null && count == maxPrograms){
				break;
			}	
			if (programDetailer.shouldPrintPrograms()) {
				programDetailer.printPrograms(cus);
			}
			if (programDetailer.shouldPrintLogFiles()){
				logger.logGenerated(cus, checkCompilationErrors);
			}	
		}
		if (checkCompilationErrors) {
			ProgramDetailer.printCompilationErrorsRates(logger);
		}

	}

	/**
	 * Utility function that returns the system's temporary directory.
	 */
	public static String getSystemTempDir() {
		String tempdir = System.getProperty("java.io.tmpdir");
		if (tempdir == null) {
			throw new IllegalArgumentException("Temp dir is not specified");
		}
		String separator = System.getProperty("file.separator");
		if (!tempdir.endsWith(separator)) {
			return tempdir + separator;
		}
		return tempdir;
	}

	private static void parseArguments(String[] args) {
		boolean vflag = true;
		String arg;
		int i = 0;

		while (i < args.length && args[i].startsWith("-")) {
			arg = args[i++];

			if (arg.equals("-addconstraints")) {
				if (i < args.length)
					theoryFile = args[i++];
				else
					System.err.println("-addconstraints requires a path");
				if (vflag)
					System.out.println("addconstraints path= " + theoryFile);
			} else if (arg.equals("-skip")) {
				skipSize = Integer.parseInt(args[i++]);
			} else if (arg.equals("-output")) {
				if (i < args.length)
					output = args[i++];
				else
					System.err.println("-output requires a path");
				if (vflag)
					System.out.println("output path= " + output);
			} else if (arg.equals("-maxprograms")) {
				if (i < args.length)
					maxPrograms = Long.parseLong(args[i++]);
				else
					System.err.println("-maxprograms requires a number");
				if (vflag)
					System.out.println("output path= " + output);
			} else if (arg.equals("-scope")) {
				if (i < args.length) {
					int maxPackage = Integer.parseInt(args[i++]);
					int maxClass = Integer.parseInt(args[i++]);
					int maxMethod = Integer.parseInt(args[i++]);
					int maxField = Integer.parseInt(args[i++]);
					scope = new Scope(maxPackage, maxClass, maxMethod, maxField);
				} else
					System.err
							.println("-scope requires number of packages, classes, methods, and fields");
				if (vflag)
					System.out.println(scope.toString());

			}
		}

		// if (i == args.length || i == args.length)
		// System.err
		// .println("Usage: Main [-output path] [-addconstraints path]");

	}

	public static String readInputStreamAsString(InputStream in)
			throws IOException {

		BufferedInputStream bis = new BufferedInputStream(in);
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		int result = bis.read();
		while (result != -1) {
			byte b = (byte) result;
			buf.write(b);
			result = bis.read();
		}
		return buf.toString();
	}

	private static File getJDollyFile() {

        Path path = Paths.get(JDOLLY_FILE).toAbsolutePath();
		File jDollyFile = new File(path.toString()) ;

		if (jDollyFile.exists() && !jDollyFile.isDirectory()) {
			return jDollyFile;
		}

		return null;
	}
}
