angular.module('Rvd')
.controller('packagingCtrl', function ($scope, $routeParams) {
	$scope.protos = {
			configOption: {name:'', label:'', type:'value', description:'', defaultValue:'', required: true }
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
	
	$scope.addConfigurationOption = function(selectedOption) {
		console.log("Adding configuration option");
		if ( selectedOption == "Add value" )
			$scope.packageInfo.config.options.push(angular.copy( $scope.protos.configOption ));
	}
	
	$scope.removeConfigurationOption = function (option) {
		$scope.packageInfo.config.options.splice($scope.packageInfo.config.options.indexOf(option), 1);
	}
	
	
	// initialization stuff
	$scope.getPackageInfo($scope.projectName);
	
	console.log("Initializing packaging controller");
});

