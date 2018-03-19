module javametamodel_primitives

abstract sig Id {}
abstract sig Body {}

abstract sig Type {}
abstract sig PrimitiveType extends Type {}
one sig Int_, Void_ extends PrimitiveType {}

