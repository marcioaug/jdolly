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
    type: one Int_,
    initializer: lone IntConstant
}

fact {
    all variableId:VariableId | some v:Variable | v.id = variableId
    all v:Variable | v.type != Void_
}


sig BodyMethod extends Body {
    variables: set Variable,
    infixArithmeticExpression: one InfixArithmeticExpression,
    return: lone Variable
}

fact {
    #BodyMethod.variables = 2
    all m:Method | m.return = Void_ => no m.b.return
    all m:Method | m.return != Void_ => m.b.return in m.b.variables
    all m:Method | m.return != Void_ => #m.b.return = 1
    all m:Method | m.b.infixArithmeticExpression.leftOperand in m.b.variables
}


abstract sig InfixOperator {}
abstract sig InfixArithmeticOperator extends InfixOperator {}
// abstract sig InfixLogicalOperator {}
one sig TIMES, DIVIDE, REMAINDER, PLUS, MINUS extends InfixArithmeticOperator {}
// one sig LESS_, GREATER_, LESS_EQUALS, GREATER_EQUALS, EQUALS, NOT_EQUALS, CONDITIONAL_OR, CONDITIONAL_AND extends InfixLogicalOperator {}

one sig MinusOneInt, ZeroInt, OneInt extends IntConstant {}

abstract sig Expression {}
// abstract sig Statement {}
abstract sig Constant extends Expression {}
abstract sig IntConstant extends Constant {}


// abstract sig InfixExpression extends Expression {
//     leftOperand: one Variable,
//     rightOperand: one IntConstant
// }

one sig InfixArithmeticExpression {
    leftOperand: one Variable,
    rightOperand: one IntConstant,
    operator: one InfixArithmeticOperator
}

// one sig InfixLogicalExpression extends InfixExpression {
//     operator: one InfixLogicalOperator
// }

// abstract sig AssigmentOperator {}
// one sig ASSIGN_ extends AssigmentOperator {}

// one sig Assigment extends Expression {
//     leftHandSide: one Variable,
//     operator: one AssigmentOperator,
//     rightHandSide: one InfixExpression
// }

// one sig IfStatement extends Statement {
//     expression: one InfixLogicalExpression,
//     thenStatement: one Statement,
//     elseStatement: one Statement
// }
