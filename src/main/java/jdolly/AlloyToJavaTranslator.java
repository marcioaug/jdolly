package jdolly;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import jdolly.util.StrUtil;
import jdolly.visitors.ImportCheckerVisitor;
import jdolly.visitors.ImportVisitor;
import org.eclipse.jdt.core.dom.*;
import sun.misc.Signal;

import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;


import edu.mit.csail.sdg.alloy4.SafeList;
import edu.mit.csail.sdg.alloy4compiler.ast.Sig;
import edu.mit.csail.sdg.alloy4compiler.ast.Sig.Field;
import edu.mit.csail.sdg.alloy4compiler.translator.A4Solution;
import sun.security.util.DisabledAlgorithmConstraints;

public class AlloyToJavaTranslator {
	// private final String ALLOY_MODULE_NAME = "javametamodel";
	
	public AlloyToJavaTranslator(A4Solution ans) {
		this.ans = ans;
	}
 
	private static AST ast = AST.newAST(AST.JLS3); 

	private A4Solution ans;

	public List<CompilationUnit> getJavaCode() {

		List<CompilationUnit> result = new ArrayList<CompilationUnit>();

		PackageDeclaration packageDeclaration;
		TypeDeclaration typeDeclaration;
		CompilationUnit compilationUnit;
		for (String c : getClasses()) {

			packageDeclaration = getPackageByClassId(c);

			// List<ImportDeclaration> importDeclarations = getImports(c);

			typeDeclaration = getClass(c);

			compilationUnit = ast.newCompilationUnit();

			compilationUnit.setPackage(packageDeclaration);

			// for (ImportDeclaration importDeclaration : importDeclarations) {
			// compilationUnit.imports().add(importDeclaration);
			// }

			compilationUnit.types().add(typeDeclaration);

			result.add(compilationUnit);
		}
		ImportVisitor cv = new ImportVisitor();
		PackageDeclaration packageOfCompUnit;
		Name packageName;
		// colocar imports
		for (CompilationUnit cu : result) {
			cu.accept(cv);
			packageOfCompUnit = cu.getPackage();
			packageName = packageOfCompUnit.getName();
			// System.out.println("Superclass: " + cv.getSuperClassName());

			List<String> importedPackages = new ArrayList<String>();

			for (CompilationUnit cu2 : result) {
				Name packageToCompare = cu2.getPackage().getName();
				ImportDeclaration importDeclaration = null;
				if (packageName.getFullyQualifiedName().equals(
						packageToCompare.getFullyQualifiedName()))
					continue;
				ImportCheckerVisitor iv = new ImportCheckerVisitor();
				cu2.accept(iv);

				// adiciona o import para super class
				if (iv.getClassName().equals(cv.getSuperClassName())) {
					importDeclaration = importPacakge(packageToCompare);

				}
				// adiciona o import para fields
				else {
					for (String fieldType : cv.getFieldTypes()) {
						if (iv.getClassName().equals(fieldType)) {
							importDeclaration = importPacakge(packageToCompare);
							break;
						}
					}
					if (importDeclaration == null) {
						for (String instanceType : cv.getInstanceTypes()) {
							if (iv.getClassName().equals(instanceType)) {
								importDeclaration = importPacakge(packageToCompare);
								break;
							}
						}
					}

				}

				if (importDeclaration != null
						&& !importedPackages.contains(packageToCompare
								.getFullyQualifiedName())) {
					importedPackages.add(packageToCompare
							.getFullyQualifiedName());
					cu.imports().add(importDeclaration);

				}

			}

		}

		return result;
	}

	private ImportDeclaration importPacakge(Name packageToCompare) {
		ImportDeclaration importDeclaration = ast.newImportDeclaration();
		importDeclaration.setName(ast.newSimpleName(packageToCompare
				.getFullyQualifiedName()));
		importDeclaration.setOnDemand(true);
		return importDeclaration;
	}

	private List<String> getClasses() {
		List<String> result = new ArrayList<String>();

		Sig sig = getSig(StrUtil.CLASS_ENTITY);
		List<String> classInstances = extractInstances(ans.eval(sig).toString());
		result.addAll(classInstances);

		return result;
	}
	
	private TypeDeclaration getClass(String classId) {
		TypeDeclaration result = ast.newTypeDeclaration();

		Sig classSig = getSig(StrUtil.CLASS_ENTITY);
		SafeList<Field> classFields = classSig.getFields();
		
		Map<String, List<String>> classIdRel = getClassRelationsBy("id");
		Map<String, List<String>> extendRel = getClassRelationsBy("extend");
		
		Field classIsInterfaceRel = getClassFieldsByCriteria("isInterface");
		Field classIsAbstractRel = getClassFieldsByCriteria("isAbstract");
		
		//MOD1
		Field classImplementRel = getClassFieldsByCriteria("implement");
		if (classImplementRel != null) {
			Map<String, List<String>> implementRel = getClassRelationsBy("implement");

			//MOD2
			List<String> interface_ = implementRel.get(classId);
			if (interface_ != null) {
				String interfaceName = classIdRel.get(interface_.get(0)).get(0);
				result.superInterfaceTypes().add(ast.newSimpleType(ast
						.newSimpleName(interfaceName)));
			}
		}
		//isInterface -> extract method: checkIfClassInterfRelIsNull
		if (classIsInterfaceRel != null) {
			Map<String, List<String>> IsInterfacRel = getMethodsRelationsBy("isInterface");
			String isInterface = IsInterfacRel.get(classId).get(0);
			boolean isInterface_ = isInterface.contains("True");
			if (isInterface_) {
				result.setInterface(true);
			}
			// methods
			List<MethodDeclaration> methods = getClassMethods(classFields, classId,isInterface_);
			for (MethodDeclaration methodDeclaration : methods) {
				result.bodyDeclarations().add(methodDeclaration);
			}
		} else {
			// methods
			List<MethodDeclaration> methods = getClassMethods(classFields, classId,false);
			for (MethodDeclaration methodDeclaration : methods) {
				result.bodyDeclarations().add(methodDeclaration);
			}
		}

		//isInterface
		if (classIsAbstractRel != null) {
			Map<String, List<String>> IsAbstractRel = getMethodsRelationsBy("isAbstract");
			String isAbstract = IsAbstractRel.get(classId).get(0);
			boolean isAbstract_ = isAbstract.contains("True");
			if (isAbstract_) {
				result.modifiers().add(ast.newModifier(ModifierKeyword.ABSTRACT_KEYWORD));
			}
		}
	
		// id
		String className = classIdRel.get(classId).get(0);
		result.setName(ast.newSimpleName(className));

		// visibilidade publica

		result.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));

		// heran�a
		List<String> superClass = extendRel.get(classId);
		if (superClass != null) {
			String superClassName = classIdRel.get(superClass.get(0)).get(0);
			result.setSuperclassType(ast.newSimpleType(ast
					.newSimpleName(superClassName)));
		}

		// atributos
		List<FieldDeclaration> fields = getClassFields(classFields, classId);
		for (FieldDeclaration fieldDeclaration : fields) {
			result.bodyDeclarations().add(fieldDeclaration);
		}



		return result;
	}

	private List<MethodDeclaration> getClassMethods(
			SafeList<Field> classFields, String classId, boolean isInterface) {
		List<MethodDeclaration> result = new ArrayList<MethodDeclaration>();

		Field methodsRel = getField("methods", classFields);

		Map<String, List<String>> methodsMap = getRelations(methodsRel);

		List<String> methods = methodsMap.get(classId);

		if (methods != null) {
			Map<String, List<String>> idRel = getMethodsRelationsBy("id");
			Field mIsAbstractRelations = getMethodsFieldsByCriteria("isAbstract");
			Map<String, List<String>> argRel = getMethodsRelationsBy("param");
			Map<String, List<String>> visRel = getMethodsRelationsBy("acc");
			Map<String, List<String>> bodyRel = getMethodsRelationsBy("b");
            Map<String, List<String>> returnRel = getMethodsRelationsBy("return");

            for (String method : methods) {
				String id = idRel.get(method).get(0);
				String arg = setVisValue(argRel, method);

				String vis = setVisValue(visRel, method);

				String isAbstract = StrUtil.EMPTY_STRING;
				if (mIsAbstractRelations != null) {
					Map<String, List<String>> isAbstractRel = getRelations(mIsAbstractRelations);
					
					if (isAbstractRel.containsKey(method))
						isAbstract = isAbstractRel.get(method).get(0);
				} else {
					isAbstract = "False";
				}
				
				MethodDeclaration methodDeclaration = ast
						.newMethodDeclaration();
				methodDeclaration.setName(ast.newSimpleName(id.toLowerCase()));
				Modifier m = getAccessModifier(vis);
				if (m != null)
					methodDeclaration.modifiers().add(m);

				if (isAbstract.contains("True") && !isInterface) {
					Modifier mAbstrct = getAccessModifier("abstract");
					methodDeclaration.modifiers().add(mAbstrct);
				}
				
				if (arg.length() > 0) {
					SingleVariableDeclaration parameter = ast
							.newSingleVariableDeclaration();
					parameter.setName(ast.newSimpleName("a"));
					parameter.setType(getType(arg));
					methodDeclaration.parameters().add(parameter);
				}

				methodDeclaration.setReturnType2(getType(returnRel.get(method).get(0)));

				List<String> list = bodyRel.get(method);
				if (list != null) {
					String body = bodyRel.get(method).get(0);

					Map<String, List<String>> variablesRel = getBodyRelationsBy("variables");
					Block block = ast.newBlock();


					if (variablesRel != null && variablesRel.containsKey(body)) {
						for (String variable : variablesRel.get(body)) {

							String variableId = variable.toLowerCase();

							Type type = getType(getEntityRelatByCriteria("type", "Variable").get(variable).get(0));


							VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
							fragment.setName(ast.newSimpleName(variableId));

							if (getEntityRelatByCriteria("initializer", "Variable").containsKey(variable)) {
								String initializer = getEntityRelatByCriteria("initializer", "Variable").get(variable).get(0);
								fragment.setInitializer(ast.newNumberLiteral(getNumberLiteral(initializer)));
							}

							VariableDeclarationStatement variableDeclaration = ast.newVariableDeclarationStatement(fragment);
							variableDeclaration.setType(type);

							block.statements().add(variableDeclaration);
						}
					}


                    String infixArithmeticExpression = getEntityRelatByCriteria("infixArithmeticExpression", "BodyMethod").get(body).get(0);
                    String exprOperator = getEntityRelatByCriteria("operator", "InfixArithmeticExpression").get(infixArithmeticExpression).get(0);
                    String leftOperand = getEntityRelatByCriteria("leftOperand", "InfixArithmeticExpression").get(infixArithmeticExpression).get(0);
                    String rightOperand = getEntityRelatByCriteria("rightOperand", "InfixArithmeticExpression").get(infixArithmeticExpression).get(0);





                    System.out.println(leftOperand);
                    System.out.println(rightOperand);

					InfixExpression infixExpression = ast.newInfixExpression();
					Assignment assignment = ast.newAssignment();

					assignment.setLeftHandSide(ast.newSimpleName(leftOperand.toLowerCase()));
					assignment.setOperator(Assignment.Operator.ASSIGN);

					infixExpression.setOperator(getInfixOperator(exprOperator));
					infixExpression.setLeftOperand(ast.newSimpleName(leftOperand.toLowerCase()));
					infixExpression.setRightOperand(ast.newNumberLiteral(getNumberLiteral(rightOperand)));

					assignment.setRightHandSide(infixExpression);

					Block ifBlock = ast.newBlock();

					block.statements().add(ast.newExpressionStatement(assignment));


					infixExpression = ast.newInfixExpression();
					infixExpression.setOperator(InfixExpression.Operator.EQUALS);
					infixExpression.setLeftOperand(ast.newNumberLiteral("0"));
					infixExpression.setRightOperand(ast.newNumberLiteral("0"));

					IfStatement ifStatement = ast.newIfStatement();
					ifStatement.setExpression(infixExpression);
//
//
					Map<String, List<String>> returns = getBodyRelationsBy("return");
//
//					if (returns.containsKey(body)) {
//
//						Assignment assignment = ast.newAssignment();
//
//						assignment.setLeftHandSide(ast.newSimpleName(returns.get(body).get(0).toLowerCase()));
//						assignment.setOperator(Assignment.Operator.ASSIGN);
//
//						infixExpression = ast.newInfixExpression();
//						infixExpression.setOperator(InfixExpression.Operator.PLUS);
//						infixExpression.setLeftOperand(ast.newSimpleName(returns.get(body).get(0).toLowerCase()));
//						infixExpression.setRightOperand(ast.newNumberLiteral("0"));
//
//						assignment.setRightHandSide(infixExpression);
//
//						Block ifBlock = ast.newBlock();
//
//						block.statements().add(ast.newExpressionStatement(assignment));
//
//						ifStatement.setThenStatement(ifBlock);
//					}
//
					block.statements().add(ifStatement);

					if (returns.containsKey(body)) {
						ReturnStatement returnStatement = ast.newReturnStatement();
						returnStatement.setExpression(ast.newSimpleName(returns.get(body).get(0).toLowerCase()));

						block.statements().add(returnStatement);
					}

					methodDeclaration.setBody(block);
				}
				result.add(methodDeclaration);
			}
		}

		return result;
	}

	private String getNumberLiteral(String numberLiteral) {

		String literal;

		switch (numberLiteral) {
			case "MinusOneInt_0":
				literal = "-1";
				break;
			case "ZeroInt_0":
				literal = "0";
				break;
			case "OneInt_0":
				literal = "1";
				break;
			default:
				throw new IllegalArgumentException("Invalid literal");
		}

		return literal;
	}

    private InfixExpression.Operator getInfixOperator(String op) {

        InfixExpression.Operator operator;

        switch (op) {
            case "TIMES_0":
                operator = InfixExpression.Operator.REMAINDER;
                break;
            case "DIVIDE_0":
                operator = InfixExpression.Operator.DIVIDE;
                break;
            case "REMAINDER_0":
                operator = InfixExpression.Operator.REMAINDER;
                break;
            case "PLUS_0":
                operator = InfixExpression.Operator.PLUS;
                break;
            case "MINUS_0":
                operator = InfixExpression.Operator.MINUS;
                break;
            default:
                throw new IllegalArgumentException("Invalid operator");
        }

        return operator;
    }

	private String getIdFromInvocation(Sig signature, String bodyId, String key){
		SafeList<Field> sFields = signature.getFields();
		Field idRelations = getField(key, sFields);
		return getFieldInstance(idRelations, bodyId);
		
	}
	
	private String getQualifier(Sig signature, String bodyId){
		SafeList<Field> sFields = signature.getFields();
		Field qualRelations = getField("q", sFields);
		return getFieldInstance(qualRelations, bodyId);
	}
	
	private Expression getMethodBody(String bodyId, String classId) {

		// String instanceName = bodyId.replaceAll("_[0-9]", "");
		Sig s = null;
		Field idClassRelations = null;
		Expression outputInvocationExp;
		String fieldId;
		String methodId;
		String className;
		String qualifier;
		int posOfClassName = 0;
		
		// pega o nome da classe que contem o metodo para usar no qualified this
		Sig classSig = getSig(StrUtil.CLASS_ENTITY);
		SafeList<Field> classFields = classSig.getFields();
		Field classIdRelations = getField("id", classFields);
		Map<String, List<String>> classIdRel = getRelations(classIdRelations);
		
		/**to do:apply factory pattern to extract all the conditions from here
		 * 
		 * outputInvocation = Factory.getInvocation(bodyId,invocationName) 
		 *  
		 *  */
		if (isInstanceOfType(bodyId, "MethodInvocation")) {
			s = getSig("MethodInvocation");
			methodId = getIdFromInvocation(s, bodyId, "id");
			qualifier = getQualifier(s, bodyId);
			className = classIdRel.get(classId).get(0);//possible violation of demeter's law
			outputInvocationExp = getMethodInvocationExpression(methodId, qualifier, className);

		} else if (isInstanceOfType(bodyId, "ConstructorMethodInvocation")) {
			s = getSig("ConstructorMethodInvocation");
			methodId = getIdFromInvocation(s, bodyId, "idMethod");
			idClassRelations = getField("idClass", s.getFields());
			className = getFieldInstance(idClassRelations, bodyId);
			outputInvocationExp = getConstructorMethodInvocationExpression(methodId, className);
			
		} else if (isInstanceOfType(bodyId, "FieldInvocation")) {
			s = getSig("FieldInvocation");
			fieldId = getIdFromInvocation(s, bodyId, "id");
			qualifier = getQualifier(s, bodyId);
			className = classIdRel.get(classId).get(0);//possible violation of demeter's law
			outputInvocationExp = getFieldInvocationExpression(fieldId, qualifier, className);
			
		} else if (isInstanceOfType(bodyId, "ConstructorFieldInvocation")) {
			s = getSig("ConstructorFieldInvocation");
			fieldId = getIdFromInvocation(s, bodyId, "id");
			idClassRelations = getField("idClass", s.getFields());
			className = getFieldInstance(idClassRelations, bodyId);
			outputInvocationExp = getConstructorFieldInvocationExpression(fieldId, className);
			
		} else {
			String value = bodyId.toString();
			value = value.replaceAll("(.)*_", "");
			outputInvocationExp = ast.newNumberLiteral(value);
		}
		return outputInvocationExp;
	}

	// the auxiliary getIdFromInvocation can be used here as well
	private boolean isInstanceOfType(String instance, String type) {
		Sig signature = getSig(type);
		boolean answer = true;
		String emptyString = "";
		
		if (signature == null){
			answer = false;
		}else{
			String methodId = getIdFromInvocation(signature, instance, "id");
			if (methodId.equals(emptyString))
				answer = false;
		}return answer;
	}

	private Expression getConstructorMethodInvocationExpression(
			String methodId, String classId) {
		Expression result = null;
		ClassInstanceCreation instExpr = ast.newClassInstanceCreation();
		instExpr.setType(ast.newSimpleType(ast.newSimpleName(classId)));

		Expression parameter = createParameter(methodId);

		SimpleName methodName = ast.newSimpleName(methodId.toLowerCase());

		MethodInvocation methInv = ast.newMethodInvocation();
		methInv.setName(methodName);
		if (parameter != null)
			methInv.arguments().add(parameter);

		methInv.setExpression(instExpr);

		result = methInv;

		return result;
	}

	private Expression getConstructorFieldInvocationExpression(String fieldId,
			String classId) {
		Expression result = null;

		ClassInstanceCreation instExpr = ast.newClassInstanceCreation();
		instExpr.setType(ast.newSimpleType(ast.newSimpleName(classId)));
		FieldAccess fieldAcc = ast.newFieldAccess();
		fieldAcc.setExpression(instExpr);
		fieldAcc.setName(ast.newSimpleName(fieldId.toLowerCase()));
		result = fieldAcc;
		return result;
	}

	private String getFieldInstance(Field f, String key) {
		String result = "";

		Map<String, List<String>> idRel = getRelations(f);
		
//		if (idRel.size() > 0) {
//			Collection<List<String>> values = idRel.values();
//			for (List<String> list : values) {
//				if (list.contains(key)) result = list.get(list.indexOf(key));
//			}
//		}
		if (idRel.size() > 0 && idRel.containsKey(key))

			result = idRel.get(key).get(0);

		return result;
	}

	private Expression getMethodInvocationExpression(String methodId,
			String qualifier, String classId) {
		Expression result = null;

		Expression parameter = createParameter(methodId, classId);

		SimpleName methodName = ast.newSimpleName(methodId.toLowerCase());

		if (qualifier.equals("super__0")) {
			SuperMethodInvocation superMethInv = ast.newSuperMethodInvocation();
			superMethInv.setName(methodName);
			if (parameter != null)
				superMethInv.arguments().add(parameter);
			result = superMethInv;
		} else {
			MethodInvocation methInv = ast.newMethodInvocation();
			methInv.setName(methodName);
			if (parameter != null)
				methInv.arguments().add(parameter);
			if (qualifier.equals("this__0")) {
				ThisExpression thisExpression = ast.newThisExpression();
				// radom
				// o this pode ser qualificado ou nao
				// Random generator = new Random();
				// int value = generator.nextInt(2);
				// if (value == 1)
				// thisExpression.setQualifier(ast.newSimpleName(classId));
				methInv.setExpression(thisExpression);
			}
			if (qualifier.equals("qthis__0")) {
				ThisExpression thisExpression = ast.newThisExpression();
				thisExpression.setQualifier(ast.newSimpleName(classId));
				methInv.setExpression(thisExpression);
			}
			result = methInv;
		}

		return result;
	}

	private Expression createParameter(String methodId, String classId) {
		
		String parameterType = defParamType(methodId);

		Expression parameter = initializerParam(parameterType);
		
		return parameter;
	}

	private String defParamType(String methodId) {
		Map<String, List<String>> idRel = getMethodsRelationsBy("id");
		Map<String, List<String>> argRel = getMethodsRelationsBy("param");

		String parameterType = "";
		boolean someMethodHasNotParam = false;
		for (String s : idRel.keySet()) {
//			System.out.println(s);
			String methodId2 = idRel.get(s).get(0);
//			System.out.println(methodId2);
			
			if (idRel.get(s).size() > 0 && methodId2.equals(methodId)) {
				//HACK
				if (argRel.containsKey(s)) {
//					System.out.println("tem");
					parameterType = argRel.get(s).get(0);
					//break;
				} else {
					someMethodHasNotParam = true;
				}

			}
		}
		if (someMethodHasNotParam) parameterType = "";
//		System.out.println("=================");
		return parameterType;
	}
	
	private Expression createParameter(String methodId) {

		String parameterType = defineParamType(methodId);
		Expression parameter = initializerParam(parameterType);
		
		return parameter;
	}

	private String defineParamType(String methodId) {
		Map<String, List<String>> idRel = getMethodsRelationsBy("id");
		Map<String, List<String>> argRel = getMethodsRelationsBy("param");

		String parameterType = "";
		for (String s : idRel.keySet()) {
			boolean isSizeOfRelGreaterThanZero = idRel.get(s).size() > 0;
			String firstRel = idRel.get(s).get(0);
			if (isSizeOfRelGreaterThanZero && firstRel.equals(methodId)) {
				if (argRel.containsKey(s)) {
					parameterType = argRel.get(s).get(0);
					break;
				}
			}
		}
		return parameterType;
	}

	private Expression initializerParam(String parameterType) {
		Expression parameter = null;
		if (parameterType.equals("Int__0") || parameterType.equals("Long__0")) {
			parameter = ast.newNumberLiteral("2");
		} else if (parameterType.length() > 0)
			parameter = ast.newNullLiteral();
		return parameter;
	}

	private Expression getFieldInvocationExpression(String fieldId,
			String qualifier, String classId) {

		Expression result = null;

		SimpleName fieldName = ast.newSimpleName(fieldId.toLowerCase());
		result = fieldName;
		
		if (qualifier.equals("super__0")) {
			SuperFieldAccess superFieldAcc = ast.newSuperFieldAccess();
			superFieldAcc.setName(fieldName);
			result = superFieldAcc;
		} else {
			if (qualifier.equals("this__0")) {
				FieldAccess fieldAcc = ast.newFieldAccess();			
				fieldAcc.setName(fieldName);
				ThisExpression thisExpression = ast.newThisExpression();				
				fieldAcc.setExpression(thisExpression);
				result = fieldAcc;
			}
			else if (qualifier.equals("qthis__0")) {
				FieldAccess fieldAcc = ast.newFieldAccess();			
				fieldAcc.setName(fieldName);
				ThisExpression thisExpression = ast.newThisExpression();
				thisExpression.setQualifier(ast.newSimpleName(classId));
				fieldAcc.setExpression(thisExpression);
				result = fieldAcc;
			}
		}
		return result;
		
	}

	private BodyType setBodyType(String bodySig) {
		BodyType result = null; // se n�o for nenhuma das condi��es haver� NullPointerException

		if (bodySig.startsWith("MethodInvocation"))
			result = BodyType.METHOD_INVOCATION;
		else if (bodySig.startsWith("FieldInvocation"))
			result = BodyType.FIELD_INVOCATION;
		else if (bodySig.startsWith("ConstructorFieldInvocation"))
			result = BodyType.CONSTRUCTOR_FIELD_INVOCATION;
		else if (bodySig.startsWith("ConstructorMethodInvocation"))
			result = BodyType.CONSTRUCTOR_METHOD_INVOCATION;

		// TODO
		// Isso vai ser aleat�rio
		// else if (bodySig.startsWith("ClassMethodInvocation"))
		// result = BodyType.CLASS_METHOD_INVOCATION;
		// else if (bodySig.startsWith("ClassFieldInvocation"))
		// result = BodyType.CLASS_FIELD_INVOCATION;
		else if (bodySig.startsWith("LiteralValue"))
			result = BodyType.INT_CONSTANT_VALUE;
		return result;
	}

	private List<FieldDeclaration> getClassFields(SafeList<Field> classFields,
			String td) {
		
		List<FieldDeclaration> result = new ArrayList<FieldDeclaration>();

		Field fieldsRel = getField("fields", classFields);

		Map<String, List<String>> fieldsMap = getRelations(fieldsRel);

		List<String> fields = fieldsMap.get(td);

		if (fields != null) {
			Map<String, List<String>> idRel = getFieldsRelationsBy("id");
			Map<String, List<String>> typeRel = getFieldsRelationsBy("type");
			Map<String, List<String>> visRel = getFieldsRelationsBy("acc");

			for (String field : fields) {
				String id = idRel.get(field).get(0);
				String type = typeRel.get(field).get(0);
				String vis = setVisValue(visRel, field);

				FieldDeclaration fieldDeclaration = getFieldDeclaration(id);

				Modifier m = getAccessModifier(vis);
				if (m != null)
					fieldDeclaration.modifiers().add(m);

				Type t = getType(type);
				fieldDeclaration.setType(t);

				// inicializa o field
				VariableDeclarationFragment variable = (VariableDeclarationFragment) fieldDeclaration
						.fragments().get(0);
				
				Expression inializer = getInitializer(type,field);
				variable.setInitializer(inializer);

				result.add(fieldDeclaration);
			}
		}
		return result;
	}

	private Expression getInitializer(String type, String field) {
		Expression inializer;
		if (type.equals("Int__0") || type.equals("Long__0")) {
			// pega o valor que sera inicializado a partir da inst�ncia
			String value = "1" + field.substring(field.length() - 1);
			// Random generator = new Random();
			// int value = generator.nextInt(100);
			inializer = ast.newNumberLiteral(value);
		} else
			inializer = ast.newNullLiteral();
		return inializer;
	}

	private FieldDeclaration getFieldDeclaration(String id) {
		VariableDeclarationFragment fieldId = ast.newVariableDeclarationFragment();
		
		fieldId.setName(ast.newSimpleName(id.toLowerCase()));

		FieldDeclaration fieldDeclaration = ast.newFieldDeclaration(fieldId);
		return fieldDeclaration;
	}

	private String setVisValue(Map<String, List<String>> visRel, String field) {
		String vis = StrUtil.EMPTY_STRING;
		if (visRel.containsKey(field))
			vis = visRel.get(field).get(0);
		return vis;
	}

	private Type getType(String type) {
		Type result = null;

		if (type.equals("Long__0"))
			result = ast.newPrimitiveType(PrimitiveType.LONG);
		
		else if (type.equals("Int__0"))
			result = ast.newPrimitiveType(PrimitiveType.INT);
		else if (type.equals("Void__0"))
            result = ast.newPrimitiveType(PrimitiveType.VOID);
		else {
			Map<String, List<String>> classRel = getClassRelationsBy("id");
			List<String> id = classRel.get(type);
			if (id != null) {
				String nameToNewType = id.get(0);
				result = ast.newSimpleType(ast.newName(nameToNewType));
			}
		}
		return result;
	}

	private Modifier getAccessModifier(String vis) {
		Modifier result = null;

		if (vis.equals("private__0"))
			result = ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD);
		else if (vis.equals("protected_0"))
			result = ast
					.newModifier(Modifier.ModifierKeyword.PROTECTED_KEYWORD);
		else if (vis.equals("public_0"))
			result = ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
		else if (vis.equals("abstract"))
			result = ast.newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD);
		return result;
	}

	private List<ImportDeclaration> getImports(String classId) {
		List<ImportDeclaration> result = new ArrayList<ImportDeclaration>();
		
		Map<String, List<String>> relationsByImport = getClassRelationsBy("imports");
		List<String> imported = relationsByImport.get(classId);

		if (imported != null) {
			for (String importedPack : imported) {
				ImportDeclaration importDeclaration = defImportDeclaration(importedPack);
				result.add(importDeclaration);
			}
		}
		return result;
	}

	private ImportDeclaration defImportDeclaration(String importedPackage) {
		ImportDeclaration importDeclaration = ast
				.newImportDeclaration();
		importDeclaration.setName(ast.newSimpleName(importedPackage));
		importDeclaration.setOnDemand(true);
		
		return importDeclaration;
	}

	private Map<String, List<String>> getEntityRelatByCriteria(String criteria, String entity){
		Sig entitySig = getSig(entity);
		SafeList<Field> entityFields = entitySig.getFields();
		Field entityRelatByCriteria = getField(criteria, entityFields);
		
		return getRelations(entityRelatByCriteria);
	}
	
	private Field getEntityFieldsByCriteria(String criteria, String entity){
		Sig entitySig = getSig(entity);
		SafeList<Field> entityFields = entitySig.getFields();
		Field entityFieldsByCriteria = getField(criteria, entityFields);
		return entityFieldsByCriteria;
	}

	private Map<String, List<String>> getBodyRelationsBy(String criteria) {
		return getEntityRelatByCriteria(criteria, "BodyMethod");
	}
		
	private Map<String, List<String>> getMethodsRelationsBy(String criteria){		
		return getEntityRelatByCriteria(criteria,StrUtil.METHOD_ENTITY);
	}
	
	private Map<String, List<String>> getClassRelationsBy(String criteria){		
		return getEntityRelatByCriteria(criteria,StrUtil.CLASS_ENTITY);
	}
	
	private Map<String, List<String>> getFieldsRelationsBy(String criteria){		
		return getEntityRelatByCriteria(criteria,StrUtil.FIELD_ENTITY);
	}
	
	private Field getClassFieldsByCriteria(String criteria){
		return getEntityFieldsByCriteria(criteria, StrUtil.CLASS_ENTITY);
	}
	
	private Field getMethodsFieldsByCriteria(String criteria){
		return getEntityFieldsByCriteria(criteria, StrUtil.METHOD_ENTITY);
	}
	
	private Field getFieldsOfFieldByCriteria(String criteria){
		return getEntityFieldsByCriteria(criteria, StrUtil.FIELD_ENTITY);
	}
	
	private PackageDeclaration getPackageByClassId(String classId) {
		PackageDeclaration result = ast.newPackageDeclaration();

		Map<String, List<String>> relations = getClassRelationsBy("package");
		List<String> relationsByClassId = relations.get(classId);
		String packageName = relationsByClassId.get(0);
		
		result.setName(ast.newSimpleName(packageName));

		return result;
	}

	private Field getField(String key, SafeList<Field> compUnitFields) {
		Field result = null; //compUnitFields.get(0); // poss�vel null pointer exception

		for (Field field : compUnitFields) {
			String strRepresOfField = field.toString();
			if (strRepresOfField.contains(key)) {
				result = field;
				break;
			}
		}
		return result;
	}

	private Map<String, List<String>> getRelations(Field field) {
		Map<String, List<String>> result = new HashMap<String, List<String>>();

		if (field == null)
			return result;

		String relations = ans.eval(field).toString();
		String relationsCleaned = cleanName(relations);

		boolean relationsIsNotEmpty = relationsCleaned.length() > 0;	
		if (relationsIsNotEmpty) {
			String[] arrayRelation = relationsCleaned.split(StrUtil.COMMA_SYMBOL);
			addRelationsToResult(result, arrayRelation);
		}
		return result;
	}

	private void addRelationsToResult(Map<String, List<String>> result, String[] arrayRelation) {
		String[] relatSplitted = {};
		
		for (String relation : arrayRelation) {
			relatSplitted = relation.split(StrUtil.ARROW_RIGHT_SYMBOL);
			relatSplitted[0] = relatSplitted[0].replaceAll(StrUtil.REGEX_JAVA_METAMODEL, StrUtil.EMPTY_STRING);
			relatSplitted[1] = relatSplitted[1].replaceAll(StrUtil.REGEX_JAVA_METAMODEL, StrUtil.EMPTY_STRING);

			if (!result.containsKey(relatSplitted[0])) {
				List<String> values = new ArrayList<String>();
				values.add(relatSplitted[1]);
				result.put(relatSplitted[0], values);
			} else {
				List<String> relatSplittedValues = result.get(relatSplitted[0]);
				relatSplittedValues.add(relatSplitted[1]);
			}
		}
	}

	private Sig getSig(String signature) {
		Sig result=null;
		SafeList<Sig> sigs = ans.getAllReachableSigs();
		
		//Sig result = sigs.get(0);
		
		for (Sig sig : sigs) {
			String sigName = removeCrap(sig.toString());
			if (sigName.equals(signature)) {
				result = sig;
				break;
			}
		}
		return result;
	}

	private String removeCrap(String instance) {
		String aux = instance.replaceAll(StrUtil.REGEX_CRAP_SYMBOLS, StrUtil.EMPTY_STRING);
		return aux;
	}

	private List<String> extractInstances(String labels) {
		List<String> result = new ArrayList<String>();
		
		String instances = cleanName(labels);
		
		boolean instancesIsNotEmpty = instances.length() > 0;
		
		if (instancesIsNotEmpty) {
			String[] types = instances.split(StrUtil.COMMA_SYMBOL);
			result = addTypesToResult(types);
		}
		return result;
	}
	
	private List<String> addTypesToResult(String[] types){
		List<String> result = new ArrayList<String>();
		
		final char empty = ' ';
		
		final int beginIndex = 1;
		final int firstPosition = 0;
		
		for (String typeName : types) {
			if (typeName.charAt(firstPosition) == empty)
				typeName = typeName.substring(beginIndex);
			typeName = typeName.replaceAll("javametamodel(.)*/", StrUtil.EMPTY_STRING);
			result.add(typeName);
		}
		return result;
	}

	public String cleanName(final String name) {
		final int beginIndex = 1;
		final int endIndex = name.length() - 1;
		
		String removeBraces = name.substring(beginIndex, endIndex);
		String replaceDollar = removeBraces.replace(StrUtil.DOLLAR_SYMBOL, StrUtil.UNDERSCORE_SYMBOL);
		String removeSpaces = replaceDollar.replaceAll(StrUtil.SPACE_STRING, StrUtil.EMPTY_STRING);
		
		return removeSpaces;
	}

}

	
