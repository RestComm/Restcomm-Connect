angular.module('Rvd').directive('stepHeading', function () {
	return {
		restrict: 'E',
		transclude:true,
		templateUrl: 'templates/steps/stepHeading.html'
	}
});
