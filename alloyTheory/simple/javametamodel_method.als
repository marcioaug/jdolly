module javametamodel_method

open javametamodel_primitives

sig MethodId extends Id {}

sig Method {
    id: one MethodId,
    b: one BodyMethod,
    return: one PrimitiveType,
}

fact {
    thereShouldBeNoUnusedMethodId[]
    aBodyBelongsToOnlyOneMethod[]
    allMethodsMustHaveABody[]
}

pred thereShouldBeNoUnusedMethodId[] {
	all methodId:MethodId | some m:Method | m.id = methodId
}

pred aBodyBelongsToOnlyOneMethod[] {
    no m1, m2:Method | m1 != m2 && m1.b = m2.b
}

pred allMethodsMustHaveABody[] {
	BodyMethod in Method.b
}


sig VariableId extends Id {}

sig Variable {
    id: one VariableId,
    type: one PrimitiveType
}


sig BodyMethod extends Body {
    variables: set Variable
}

fact {
    #BodyMethod.variables <= 2
    all variableId:VariableId | some v:Variable | v.id = variableId
    no v:Variable | v.type = Void_
}