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
