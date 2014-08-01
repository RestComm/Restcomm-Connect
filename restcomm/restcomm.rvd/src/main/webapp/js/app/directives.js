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
