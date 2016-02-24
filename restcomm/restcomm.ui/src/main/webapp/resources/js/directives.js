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
      detailsLoaded: '=', // use this only to determine when the numberDetails object has arrived
      sidVar: '=',
      urlOnlyVar: '=',
      methodVar: '=',
      urlVar: '=',
      apps: '=',
    },
    controller: function ($scope) {
		$scope.setApplication = function (app) {
            //$scope.urlVar = app.startUrl;
            $scope.appNameVar = app.projectName;
            $scope.sidVar = app.sid;
		};
		
		$scope.setMethod = function(method) {
			$scope.methodVar = method;
		};

        $scope.setTarget = function(target) {
            $scope.targetVar = target;
            $scope.urlVar = '';
            $scope.appNameVar = '';
            $scope.sidVar = '';
        }

        $scope.initTarget = function() {
            if($scope.sidVar){
                $scope.targetVar = 'Application';
                for (var i=0; i<$scope.apps.length; i++) {
                  var app = $scope.apps[i];
                  if(app.sid == $scope.sidVar){
                    $scope.appNameVar = app.projectName;
                    $scope.urlVal = '';
                    break;
                  }
                }
            } else {
                $scope.targetVar = 'URL';
            }
        }

        $scope.clearSelectedApp = function() {
            $scope.appNameVar = '';
            $scope.sidVar = '';
        }

        // initialize control when numberDetails actually arrives.
        // NOTE: numberDetails is only used to signal when data has arrived and not to carry the actual data
        var clearWatch = $scope.$watch("detailsLoaded", function (newValue, oldValue) {
            if ($scope.detailsLoaded) {
                clearWatch();
                //console.log("numberDetails finally returned:");
                $scope.initTarget();
            }
        });

        $scope.targetVar = 'URL'; // default value until all data is in place (i.e. detailsLoaded == true)
	},
    templateUrl: 'templates/rc-endpoint-url.html'/*,
    link: function(scope, element, attrs) {
      scope.$watch('var', function() { console.log(scope.$parent['newNumber']['voiceURL'] = 'xxx'); });
    }*/
  };
});

