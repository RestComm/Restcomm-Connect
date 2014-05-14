angular.module('Rvd')
.directive('gatherStep', function (gatherModel) {
	return {
			restrict: 'A',
			link: function (scope, element, attrs) {
				console.log("linking gatherStep");
				if (scope.step) {
					//gatherModel.validate(scope.step);
				}
				else
					scope.step = new gatherModel();
				
				function updatePattern(scope) {
					if ( scope.step.validation.iface.userPattern == '' )
						scope.step.validation.pattern = '';
					else
					//if ( scope.step.validation.iface.userPatternType == 'Any of')
					//		scope.step.validation.pattern = '^[' + scope.step.validation.iface.userPattern + ']*$';
					//else
					if ( scope.step.validation.iface.userPatternType == 'Regex')
						scope.step.validation.pattern = scope.step.validation.iface.userPattern;
					else
					if ( scope.step.validation.iface.userPatternType == 'One of')
						scope.step.validation.pattern = '^[' + scope.step.validation.iface.userPattern + ']$';
				}
				
				scope.$watch('step.validation.iface.userPatternType',function (newValue, oldValue) {
					if ( newValue != oldValue )
						updatePattern(scope);
				});
				
				scope.$watch('step.validation.iface.userPattern',function (newValue, oldValue) {
					if ( newValue != oldValue )
						updatePattern(scope);
				});
				
			}
	}
})
;
