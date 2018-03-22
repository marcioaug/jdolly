module javametamodel_primitives

abstract sig Id {}
abstract sig Body {}

abstract sig Type {}
abstract sig PrimitiveType extends Type {}
one sig Int_, Void_ extends PrimitiveType {}

abstract sig InfixOperator {}
abstract sig InfixArithmeticOperator extends InfixOperator {}
abstract sig InfixLogicalOperator  {}
one sig TIMES, DIVIDE, REMAINDER, PLUS, MINUS extends InfixArithmeticOperator {}
one sig LESS, GREATER, LESS_EQUALS, GREATER_EQUALS, EQUALS, NOT_EQUALS extends InfixLogicalOperator {}


abstract sig Expression {}
abstract sig Statement {}
abstract sig Literal extends VarOrLiteral {}
abstract sig NumberLiteral extends Literal {}
one sig MINUS_ONE, ZERO, ONE, TWO extends NumberLiteral {}

sig Var extends VarOrLiteral {
    type: one PrimitiveType,
    initializer: lone NumberLiteral
}

fact {
    all v:Var | v.type = Int_
}

abstract sig VarOrLiteral extends Expression {}


abstract sig Visibility {}
one sig PRIVATE, PROTECTED, PUBLIC extends Visibility {}
