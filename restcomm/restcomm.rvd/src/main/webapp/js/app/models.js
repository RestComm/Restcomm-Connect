angular.module('Rvd').service('ModelBuilder', ['$injector', function ($injector) {
	var modelBuilder = {};
	modelBuilder.build = function (modelName) {
		var builtModel = $injector.invoke([modelName, function(model) {
			var newInstance = new model();
			return newInstance;
		}]);
		return builtModel;
	}
	
	return modelBuilder;
}]);

angular.module('Rvd').factory('ExceptionHandlingInfo', ['rvdModel', function(rvdModel) {
	function ExceptionHandlingInfo() {
		this.exceptionMappings = [];
		this.defaultNext = undefined;
	}
	ExceptionHandlingInfo.prototype = new rvdModel();
	ExceptionHandlingInfo.prototype.constructor = ExceptionHandlingInfo;
	ExceptionHandlingInfo.prototype.pack = function () {
		if ( this.defaultNext === undefined && this.exceptionMappings.length == 0 )
			return undefined;
		else
			return angular.copy(this);
	}

	return ExceptionHandlingInfo;
}]);
