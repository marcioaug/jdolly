module javametamodel_simple

open javametamodel_primitives
open javametamodel_method


sig Package {}

sig ClassId extends Id {}

sig Class extends Type {
	id: one ClassId,
	package: one Package,
	methods: set Method
}

fact {
	thereShouldBeNoEmptyPackages[]
	thereShouldBeNoUnusedClassId[]
	thereMustBeAtLeastOneClass[]
	everyMethodMustBelongToAClass[]
	allClassesMustHaveAtLeastOneMethod[]	
}

pred thereShouldBeNoEmptyPackages[] {
    Package in Class.package
}

pred thereShouldBeNoUnusedClassId[] {
	all classId:ClassId | some c:Class | c.id = classId
}

pred thereMustBeAtLeastOneClass[] {
	one Class
}

pred everyMethodMustBelongToAClass[] {
	Method in Class.methods
}

pred allClassesMustHaveAtLeastOneMethod[] {
	#Class.methods > 0
}

pred show[] {}
