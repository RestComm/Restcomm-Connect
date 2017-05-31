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

rcDirectives.directive('rcEndpointUrl', function(PublicConfig) {
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
        $scope.appNameVar = "";
        $scope.setApp = function() {
            if ($scope.appNameVar === 'create_new_project') {
                window.open(PublicConfig.rvdUrl);
            } else
            // if this is an application SID, populate the sidVar
            if ($scope.appNameVar && $scope.appNameVar.substr(0,2) == "AP") {
                $scope.sidVar = $scope.appNameVar;
            } else
            if (!$scope.appNameVar) {
                $scope.sidVar = null;
            }

        };

		$scope.setApplication = function (app) {
            $scope.appNameVar = app.projectName;
            $scope.sidVar = app.sid;
		};
		
		$scope.setMethod = function(method) {
			$scope.methodVar = method;
		};

        $scope.onTargetChanged = function(target) {
            if (target == "URL") {
                $scope.sidVar = null;
            } else if (target == "Application") {
                $scope.urlVar = null;
            }
        }

        $scope.initTarget = function() {
			if ($scope.urlOnlyVar)
				$scope.targetVar = 'URL';
			else {
				if($scope.sidVar){
				    $scope.targetVar = "Application";
				} else
				if ($scope.urlVar) {
					$scope.targetVar = "URL";
				} else {
				    $scope.targetVar = "Application";
				}
			}
			// initialize the application name if needed
			if ($scope.targetVar == "Application") {
			    $scope.appNameVar = $scope.sidVar; //name;
			}
        }

        // initialize control when numberDetails actually arrives.
        // NOTE: numberDetails is only used to signal when data has arrived and not to carry the actual data
        var clearWatch = $scope.$watch("detailsLoaded", function (newValue, oldValue) {
            if ($scope.detailsLoaded) {
                clearWatch();
                $scope.initTarget();
            }
        });

        $scope.targetVar = 'URL'; // default value until all data is in place (i.e. detailsLoaded == true)
	},
    templateUrl: 'templates/rc-endpoint-url.html'
  };
});

