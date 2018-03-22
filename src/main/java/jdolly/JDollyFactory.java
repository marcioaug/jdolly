package jdolly;

public class JDollyFactory {
	
	private static JDollyFactory _instance = null;
	
	private JDollyFactory() {};
	
	public static JDollyFactory getInstance() {
		if (_instance == null)
			_instance = new JDollyFactory();
		return _instance;
	}
	
	
	public static JDolly createJDolly(Scope scope, String theory) {
		
		return getInstance().createJDollyAux(scope, theory);				
	}

	private JDolly createJDollyAux(Scope scope, String theory) {
		return new JDollyEasy(scope, theory);
	}

}
