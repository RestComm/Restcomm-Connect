'use strict';

/* Directives */
var rcDirectives = angular.module('rcApp.directives', []);

rcDirectives.directive('passwordMatch', [function () {
  return {
    restrict: 'A',
    scope:true,
    require: 'ngModel',
    link: function (scope, elem , attrs,control) {
      var checker = function () {

        //get the value of the first password
        var e1 = scope.$eval(attrs.ngModel);

        //get the value of the other password 
        var e2 = scope.$eval(attrs.passwordMatch);
        return e1 == e2;
      };
      scope.$watch(checker, function (n) {

        //set the form control to valid if both
        //passwords are the same, else invalid
        control.$setValidity("unique", n);
      });
    }
  };
}]);

rcDirectives.directive('rcPageTitle', function() {
  return {
    restrict: 'E',
    scope: {
      icon: '@',
      title: '@',
      subTitle: '@'
    },
    templateUrl: 'templates/rc-pagetitle.html'
  };
});

rcDirectives.directive('rcNumbersPills', function() {
  return {
    restrict: 'E',
    templateUrl: 'templates/rc-numbers-pills.html'
  };
});

rcDirectives.directive('rcLogsPills', function() {
  return {
    restrict: 'E',
    templateUrl: 'templates/rc-logs-pills.html'
  };
});

rcDirectives.directive('rcListFilter', function() {
  return {
    restrict: 'E',
    scope: {
      filter: '='
    },
    templateUrl: 'templates/rc-list-filter.html'
  };
});

rcDirectives.directive('rcEndpointUrl', function() {
  return {
    restrict: 'E',
    scope: {
      // id: '@',
      methodVar: '=',
      urlVar: '=',
      appVar: "=",
      apps: '='
    },
    controller: function ($scope) {
		$scope.setApp = function (app) {
			$scope.urlVar = app.projectName;
			$scope.appVar = app;
		};
		
		$scope.setMethod = function(method) {
			$scope.methodVar = method;
		}
	},
    templateUrl: 'templates/rc-endpoint-url.html'/*,
    link: function(scope, element, attrs) {
      scope.$watch('var', function() { console.log(scope.$parent['newNumber']['voiceURL'] = 'xxx'); });
    }*/
  };
});

