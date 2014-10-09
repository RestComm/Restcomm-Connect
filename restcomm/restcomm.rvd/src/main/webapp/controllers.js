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
    	//console.log("resourceNotFound event caught");
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
	//console.log('in projectLogCtrl');
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

App.controller('mainMenuCtrl', ['$scope', 'authentication', '$location', '$modal','$q', '$http', function ($scope, authentication, $location, $modal, $q, $http) {
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
	
	function settingsModalCtrl ($scope, $timeout, $modalInstance, settings) {
		$scope.settings = settings;

		$scope.ok = function () {
			$http.post("services/settings", settings, {headers: {'Content-Type': 'application/data'}})
			.success( function () { 
				$modalInstance.close(settings);
			})
			.error( function () {
				notifications.put("Cannot save settings");
			});
		};

		$scope.cancel = function () {
			$modalInstance.dismiss('cancel');
		};
		
		// watch form validation status and copy to outside scope so that the OK
		// button (which is outside the form's scope) status can be updated
		$scope.watchForm = function (formValid) {
			$scope.preventSubmit = !formValid;
		}
	};
	
	$scope.showSettingsModal = function (settings) {
		var modalInstance = $modal.open({
		  templateUrl: 'templates/designerSettingsModal.html',
		  controller: settingsModalCtrl,
		  size: 'lg',
		  resolve: {
			settings: function () {
				var deferred = $q.defer()
				$http.get("services/settings")
				.then(function (response) {
					deferred.resolve(response.data);
				}, function (response) {
					if ( response.status == 404 )
						deferred.resolve({});
					else {
						// console.log("BEFORE reject");
						deferred.reject();
						// console.log("AFTER reject");
					}
				});
				return deferred.promise;
			}
		  }
		});

		modalInstance.result.then(function (settings) {
			//console.log(settings);
			// $scope.settings
		}, function () {
		  // $log.info('Modal dismissed at: ' + new Date());
		});		
	}
	
	
	
	
}]);
