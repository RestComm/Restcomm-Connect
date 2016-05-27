App.controller('upgradeCtrl', function ($scope, $stateParams, $http, $q, $location) {
	$scope.projectName = $stateParams.projectName;
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
		
		$http({url: 'services/manager/projects/upgrade?name=' + $scope.projectName,	method: "PUT" })
		.success(function (data, status, headers, config) { deferred.resolve('Project upgraded'); })
		.error(function (data, status, headers, config) { deferred.reject({type:'upgradeError', data:data}); });	
		
		return deferred.promise;
	}
	
	
	// Controller bootstrap
	$http({	url:'services/manager/projects/info?name=' + $scope.projectName,	method:'GET' })
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