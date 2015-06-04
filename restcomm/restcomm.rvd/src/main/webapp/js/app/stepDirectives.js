angular.module('Rvd')
.directive('gatherStep', function (gatherModel) {
	return {
			restrict: 'A',
			link: function (scope, element, attrs) {
				if (scope.step) {
					//gatherModel.validate(scope.step);
				}
				else
					scope.step = new gatherModel();
				
				
				function getEffectiveValidationType() {
					if ( scope.step.validation.userPattern != undefined )
						return "One of";
					else
						return "Regex"
				}
				scope.getEffectiveValidationType = getEffectiveValidationType;
				
				function setValidationTypeOneOf() {
					if ( getEffectiveValidationType() != "One of" ) {
						scope.step.validation.userPattern = "";
						scope.step.validation.regexPattern = undefined;
					}
				}
				scope.setValidationTypeOneOf = setValidationTypeOneOf;
				
				function setValidationTypeRegex() {
					if ( getEffectiveValidationType() != "Regex" ) {
						scope.step.validation.userPattern = undefined;
						scope.step.validation.regexPattern = "";
					}
				}		
				scope.setValidationTypeRegex = 	setValidationTypeRegex;
				
		}
	}
})
;
