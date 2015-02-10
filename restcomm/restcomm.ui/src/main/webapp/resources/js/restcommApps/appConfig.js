var rappManagerConfigCtrl = angular.module("rcApp.restcommApps").controller('RappManagerConfigCtrl', function($scope, $upload, $routeParams, rappConfig, bootstrapObject, $http, Notifications, $window, rappService, $sce) {
	
	$scope.initRappConfig = function (rappConfig) {
		var i;
		
		for ( i=0; i < rappConfig.options.length; i++ ) {
			if ( bootstrapObject != null && bootstrapObject[ rappConfig.options[i].name ] )
				rappConfig.options[i].value = bootstrapObject[ rappConfig.options[i].name ];
			if ( bootstrapObject == null ) {
				if ( rappConfig.options[i].defaultValue )
					rappConfig.options[i].value = rappConfig.options[i].defaultValue;
			}
		}
	}
	
	$scope.enableConfiguration = function (rappConfig) {
		//console.log("Applying application parameters");
		var bootstrapObject = $scope.generateBootstrap(rappConfig);
		//console.log(bootstrapObject);
		$http({
			url: '/restcomm-rvd/services/ras/apps/' + $scope.projectName + '/bootstrap',
			method: 'POST',
			data: bootstrapObject,
			headers: {'Content-Type': 'application/data'}
		}).success(function (data) {
			if ( data.rvdStatus == 'OK') {
				//console.log("Application parameters saved");
				Notifications.success('Application parameters saved');
				rappService.provisionApplicationParameters(rappConfig, bootstrapObject);
			}
			else
				console.log("Rvd error while saving application parameters");
		}).error(function () {
			console.log("HTTP error while saving application parameters");
		});
	}
	// Creates a bootstrap object out of current configuration options
	$scope.generateBootstrap = function (rappConfig) {
		var bootstrapObject = {};
		var i;
		for (i=0; i < rappConfig.options.length; i ++ ) {
			bootstrapObject[rappConfig.options[i].name] = rappConfig.options[i].value;
		}
		return bootstrapObject;
	}
	$scope.shouldShowHowToConfigure = function () {
		if ( rappConfig.howTo )
			return true;
		return false;
	}
	
	$scope.filterUserOptions = function(option) {
		if ( option.name == 'instanceToken' || option.name == 'backendRootURL' )
			return false
		return true;
	}

	$scope.filterESOptions = function(option) {
		if ( option.name == 'instanceToken' || option.name == 'backendRootURL' )
			return true
		return false;
	}	
	
	$scope.filterInitOptions = function(option) {
		if (option.isInitOption || option.name == 'instanceToken' )
			return true;
		return false;
	}
	function getOptionByName(name, options) {
		for (var i = 0; i < options.length; i++)
			if (options[i].name == name)
				return options[i];
	};
	$scope.getOptionByName = getOptionByName;	
	$scope.buildBackendBoostrapUrl = function(rappConfig) {
		var value = rappConfig.bootstrapUrl;
		return $sce.trustAsResourceUrl(value);
	}	
	$scope.watchOptionFormValidity = function (status) {
		//console.log(status);
		$scope.optionsFormValid = status;
	}
	
	
	
	$scope.projectName = $routeParams.projectName;
	$scope.rappConfig = rappConfig;
	
	$scope.initRappConfig($scope.rappConfig);
});
