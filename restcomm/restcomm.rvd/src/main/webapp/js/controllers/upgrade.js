App.controller('upgradeCtrl', function ($scope, $routeParams, $http, $q, $location) {
	$scope.projectName = $routeParams.projectName;
	$scope.upgradeStatus = "pending";
	
	$scope.onUpgradePressed = function(name) {
		console.log("Upgrading project " + name);
		$scope.upgradeProject(name)
		.then( 
				function () { 
					console.log("Project upgraded succesfully");
					$location.path("/designer/" + name);
				},
				function () { 
					console.log("Error upgrading project");
					$scope.upgradeStatus = "upgradeError";
				}
		);
	}
	
	$scope.upgradeProject = function(name) {
		var deferred = $q.defer();
		
		$http({url: 'api/projects/' + $scope.projectName + '/upgrade',	method: "PUT" })
		.success(function (data, status, headers, config) { deferred.resolve('Project upgraded'); })
		.error(function (data, status, headers, config) { deferred.reject({type:'upgradeError', data:data}); });	
		
		return deferred.promise;
	}
	
	
	// Controller bootstrap
	$http({	url:'api/projects/' + $scope.projectName + '/info',	method:'GET' })
	.success(function (data, status, headers, config) {
		if ( data && data.version == "1.0" ) {
			$scope.upgradeStatus = "alreadyUpgraded";
		} else
			$scope.upgradeStatus = "needsUpgrade";
		
	})
	.error(function () {
		$scope.severeError = true;
	});
});