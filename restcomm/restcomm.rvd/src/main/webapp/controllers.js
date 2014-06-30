App.controller('AppCtrl', function ($rootScope, $scope) {
	console.log("Started AppCtrl");
	$rootScope.$on("$routeChangeError", function(event, current, previous, rejection) {
        //console.log(event);
        console.log('on $routeChangeError');
        $scope.rvdError = rejection;
    });
    
    $rootScope.$on("resourceNotFound", function(p1, p2) {
    	console.log("resourceNotFound event caught");
    	$scope.rvdError = {message: "The requested resource was not found. Sorry about that."};
    });
    
    $scope.$on('$routeChangeStart', function(){
        $scope.rvdError = undefined;
	});
});

var loginCtrl = angular.module('Rvd')
.controller('loginCtrl', ['$scope', '$http', 'notifications', '$location', function ($scope, $http, notifications, $location) {
	console.log("run loginCtrl ");
	
	$scope.doLogin = function (username, password) {
		$http({	url:'services/auth/login', method:'POST', data:{ username: username, password: password}})
		.success ( function () {
			console.log("login successful");
			$location.path("/home");
		})
		.error( function () {
			console.log("error logging in");
			notifications.put({message:"Login failed", type:"danger"});
		});
	}
}]);


App.controller('homeCtrl', function ($scope) {
});



