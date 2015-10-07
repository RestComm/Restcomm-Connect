'use strict';

/**
 * A general purpose global controller bound to <body/> element. It can be used for things that affect all parts of the 
 * application like setting root level scope variables 
 */
angular.module('rcApp').controller('AppCtrl', function ($rootScope,$scope,AuthService,$location) {
	$scope.auth = AuthService;
	
	// handle authorization errors for users with 'realm' auth status (valid token but no access to this instance) 
	$rootScope.$on("$routeChangeError", function (event, current, previous, rejection) {
		console.log("failed to change routes: " + rejection);
		if (rejection == "AUTH_STATUS_REALM")
			$location.path("/unauthorized");
	});
});
