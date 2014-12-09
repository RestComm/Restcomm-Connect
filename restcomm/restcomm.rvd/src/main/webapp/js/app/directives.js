angular.module('Rvd').directive('rvdModule', function (projectModules) {
	return {
		restrict: "A",
		link: function (scope, element, attrs) {
			//console.log("creating rvdModule directive");
			//console.log(scope.node);
			projectModules.addModule(scope.node);
			
			scope.$on('$destroy', function() {
				//console.log("removing module");
				//console.log(scope.node);
				projectModules.removeModule(scope.node);
			});
		}
	}
});

angular.module('Rvd').directive('stepHeading', function () {
	return {
		restrict: 'E',
		transclude:true,
		templateUrl: 'templates/steps/stepHeading.html'
	}
});

/*
 * Usage:
 * 
 * <module-picker ng-model='step.exceptionNext'></module-picker>
 * 
 * Notes
 * 
 *  - A new inherited scope is created. The moduleVar variable is set in this scope.
 *  - The 'required' attribute is also propagated to the internal select
 */
angular.module('Rvd').directive('modulePicker', [function () {
	return {
		restrict: 'E',
		require: '?ngModel',
		templateUrl: 'templates/directive/modulePicker.html',
		scope: true,
		
		compile: function compile(element, attrs) {
			if ( attrs.hasOwnProperty('required' ) )
				$(element).find("select").attr("required","required")
			
			// this is the link function
			return function (scope,el,attrs,ngModel) {			
				ngModel.$render = function () {
					scope.moduleVar = ngModel.$viewValue;
					//console.log('internal model updated');
				}
				
				scope.$watch('moduleVar', function (newValue, oldValue) {
					ngModel.$setViewValue(newValue);
					//console.log('internal model has changed. Change propagates to external model');
				});
			}
		}
	};
}]);

angular.module('Rvd').directive('rvdVariable', ['variableRegistry', function (variableRegistry) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			var varInfo = {};
			varInfo.id = variableRegistry.newId();
			//var varNameExpression = scope.$eval(attrs.rvdVariable);
			scope.$watch(attrs.rvdVariable, function (newValue, oldValue) {
				varInfo.name = newValue;
			});
			element.on('$destroy', function () {
				variableRegistry.removeVariable(varInfo);
			});
			variableRegistry.addVariable(varInfo);
		}
	}
}]);

angular.module('Rvd').directive('lookupContext', [function () {
	return {
		restrict: 'A',
		scope: true,
		link: function (scope) {
			scope.$on("variable-name-clicked", function (event,args) {
				event.stopPropagation();
				scope.$broadcast("inject-variable", args);
			})
		},
		controller: function ($scope) {}
	}
}]);

angular.module('Rvd').directive('lookupTarget', [function () {
	return {
		restrict: 'A',
		//scope:true,
		require: "^lookupContext",
		link: function (scope, element, attrs, ctrls) {
			scope.$on("inject-variable", function (event, args) {
				if ( element[0].selectionStart >= 0 ) {
					var selStart = element[0].selectionStart;
					var value = scope.$eval(attrs.ngModel) || "";
					value = value.substring(0,selStart) + "$"+args+ (value.substring(selStart) == "" ? "" : ("" + value.substring(selStart)));
					scope.$eval(attrs.ngModel + "='"+value+"'");
				}
				var selStart = element[0].selectionStart;
				var selEnd = element[0].selectionEnd;
				
				//console.log("lookupTarget received event");
			});
		} 
	}
}]);


angular.module('Rvd').directive('variableLookup', ['variableRegistry', function (variableRegistry) {
	return {
		restrict: 'E',
		scope: true,
		replace: true,
		require: "^lookupContext",
		templateUrl: 'templates/directive/variableLookup.html',
		link: function (scope, element, attrs, lookupContextCtrl) {
			scope.view = attrs.view;
			scope.variables = variableRegistry.listAll();
			scope.selectVariable = function (variable) {
				scope.$emit("variable-name-clicked", variable.name);
			}
		} 
	}
}]);


