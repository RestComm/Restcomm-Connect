angular.module('Rvd')
.controller('packagingCtrl', function ($scope, $routeParams) {
	$scope.protos = {
			configOption: {name:'', label:''}
	}
	$scope.projectName = $routeParams.projectName;
	$scope.packageInfo = null;
	
	$scope.getPackageInfo = function (projectName) {
		// retrieve package information from server
		// ...
		$scope.packageInfo = {
				config: {
					options: []
				}
		};
	} 
	
	$scope.addConfigurationOption = function() {
		console.log("Added configuration option");
		$scope.packageInfo.config.options.push(angular.copy( $scope.protos.configOption ));
	}
	
	
	// initialization stuff
	$scope.getPackageInfo($scope.projectName);
	
	console.log("Initializing packaging controller");
});

