module javametamodel_method

open javametamodel_primitives

sig MethodId extends Id {}

sig Method {
    id: one MethodId,
    b: one BodyMethod,
    params: set Var,
    return: one PrimitiveType,
    visibility: one Visibility
}

fact {
    thereShouldBeNoUnusedMethodId[]
    aBodyBelongsToOnlyOneMethod[]
    allMethodsMustHaveABody[]
    all p:Method.params | p.type = Int_
    all m:Method | m.return = Int_
    #Method.params <= 1
    all m:Method | m.visibility = PUBLIC
    all v:Var | some m:Method | v in m.b.variables or v in m.params
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

sig BodyMethod extends Body {
    variables: set Var,
    ifStatement: lone IfStatement,
    whileStatement: lone WhileStatement,
    return: lone Var
}

fact {
    all m:Method | all p:m.params | !p in m.b.variables
    all m:Method | m.return = Void_ => no m.b.return
    all m:Method | m.return != Void_ => m.b.return in m.b.variables
    all m:Method | m.return != Void_ => some m.b.return
    // all m:Method | one m.b.whileStatement | m.b.whileStatement.body.assignment.leftHandSide = m.b.return
    all b:BodyMethod | one b.whileStatement <=> no b.ifStatement
    all b:BodyMethod | all v:b.variables | one v.initializer
    //all b:BodyMethod | b.whileStatement.body != b.ifStatement.thenStatement
    all m:Method | no m.b.ifStatement => m.b.whileStatement.body.assignment.leftHandSide = m.b.return
    all m:Method | no m.b.whileStatement => m.b.ifStatement.thenStatement.assignment.leftHandSide = m.b.return
    all m:Method | m.b.whileStatement.expression.leftOperand in m.params or m.b.whileStatement.expression.leftOperand in m.b.variables
    all m:Method | m.b.ifStatement.expression.leftOperand in m.params or m.b.ifStatement.expression.leftOperand in m.b.variables
}


abstract sig InfixExpression extends Expression {}


sig InfixArithmeticExpression extends InfixExpression {
    leftOperand: one Var,
    rightOperand: one VarOrLiteral,
    operator: one InfixArithmeticOperator
}

fact {
    all i:InfixArithmeticExpression | i.leftOperand != i.rightOperand
    all i:InfixArithmeticExpression | i.rightOperand = ZERO => i.operator != DIVIDE
    all i:InfixArithmeticExpression | i.rightOperand = ZERO => i.operator != REMAINDER

}

sig InfixLogicalExpression extends InfixExpression {
    leftOperand: one Var,
    rightOperand: one VarOrLiteral,
    operator: one InfixLogicalOperator
}

fact {
    all i:InfixLogicalExpression | i.leftOperand != i.rightOperand
}


abstract sig AssignmentOperator {}
one sig ASSIGN extends AssignmentOperator {}

sig Assignment extends Expression {
    leftHandSide: one Var,
    operator: one AssignmentOperator,
    rightHandSide: one InfixArithmeticExpression
}

sig IfStatement extends Statement {
    expression: one InfixLogicalExpression,
    thenStatement: one IfBlock,
}

sig WhileStatement extends Statement {
    expression: one InfixLogicalExpression,
    body: one WhileBlock,
}

// one sig Block extends Statement {
//     expression: one InfixArithmeticExpression
// }

sig IfBlock {
    assignment: one Assignment
}

sig WhileBlock {
    assignment: one Assignment
}
