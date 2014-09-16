App.controller('AppCtrl', function ($rootScope, $location) {
	$rootScope.$on("$routeChangeError", function(event, current, previous, rejection) {
        //console.log('on $routeChangeError');
        if ( rejection == "AUTHENTICATION_ERROR" ) {
			console.log("AUTHENTICATION_ERROR");
			$location.path("/login");
		} else {
			$rootScope.rvdError = rejection;
		}
    });
    
    $rootScope.$on("resourceNotFound", function(p1, p2) {
    	console.log("resourceNotFound event caught");
    	$rootScope.rvdError = {message: "The requested resource was not found. Sorry about that."};
    });
    
    $rootScope.$on('$routeChangeStart', function(){
    	$rootScope.rvdError = undefined;
	});
});

var loginCtrl = angular.module('Rvd')
.controller('loginCtrl', ['authentication', '$scope', '$http', 'notifications', '$location', function (authentication, $scope, $http, notifications, $location) {
//	console.log("run loginCtrl ");
	authentication.clearTicket();
	
	$scope.doLogin = function (username, password) {
		authentication.doLogin(username,password).then(function () {
			$location.path("/home");
		}, function () {
			notifications.put({message:"Login failed", type:"danger"});
		})
		
		/*$http({	url:'services/auth/login', method:'POST', data:{ username: username, password: password}})
		.success ( function () {
			console.log("login successful");
			
		})
		.error( function () {
			console.log("error logging in");
			notifications.put({message:"Login failed", type:"danger"});
		});*/
	}
}]);


App.controller('homeCtrl', function ($scope, authInfo) {
});

angular.module('Rvd').controller('projectLogCtrl', ['$scope', '$routeParams', 'projectLogService', function ($scope, $routeParams, projectLogService) {
	console.log('in projectLogCtrl');
	$scope.projectName = $routeParams.projectName;
	$scope.logData = '';
	
	function retrieveLog() {
		projectLogService.retrieve().then(function (logData) {$scope.logData = logData;})
	}
	$scope.retrieveLog = retrieveLog;
	
	function resetLog() {
		projectLogService.reset().then(function () {$scope.logData = "";});
	}
	$scope.resetLog = resetLog;
	
	retrieveLog($scope.projectName);
}]);

App.controller('mainMenuCtrl', ['$scope', 'authentication', '$location', function ($scope, authentication, $location) {
	$scope.authInfo = authentication.getAuthInfo();
	//$scope.username = authentication.getTicket(); //"Testuser@test.com";
	
	function logout() {
		console.log("logging out");
		authentication.doLogout().then(function () {
			$location.path("/login");
		}, function () {
			$location.path("/login");
		});
	}
	$scope.logout = logout;
}]);
