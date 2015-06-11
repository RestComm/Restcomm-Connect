var rappManagerConfigCtrl = angular.module("rcApp.restcommApps").controller('RappManagerConfigCtrl', function($scope, $upload, $routeParams, rappConfig, rapp, bootstrapObject, $http, Notifications, $window, rappService, $sce) {
	
	$scope.initRappConfig = function (rappConfig, bootstrapObj) {
		var i;
		
		for ( i=0; i < rappConfig.options.length; i++ ) {
			if ( bootstrapObj != null && bootstrapObj[ rappConfig.options[i].name ] )
				rappConfig.options[i].value = bootstrapObj[ rappConfig.options[i].name ];
			if ( bootstrapObj == null ) {
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
	
	$scope.filterInitOptions = function(option) {
		if (option.isInitOption )
			return true;
		return false;
	}
	function getOptionByName(name, options) {
		for (var i = 0; i < options.length; i++)
			if (options[i].name == name)
				return options[i];
	};
	$scope.getOptionByName = getOptionByName;	
	$scope.buildBackendBoostrapUrl = function(rappConfig, rapp) {
		var value;
		if ( rapp.info.rasVersion >= 2 ) {
			value = rappConfig.bootstrapUrl;
		} else {
			var backendRootURLOption = getOptionByName("backendRootURL", rappConfig.options);
			if ( backendRootURLOption && backendRootURLOption.value)
				value = backendRootURLOption.value  + "/ui/index.php";
		}
		return $sce.trustAsResourceUrl(value);
	}	
	$scope.watchOptionFormValidity = function (status) {
		//console.log(status);
		$scope.optionsFormValid = status;
	}
	$scope.needsBootstrapping = function (rappConfig, rapp) {
		if ( rapp.info.rasVersion >= 2 ) {
			if ( rapp.config.bootstrapUrl )
				return true;
		} else {
			var backendRootURLOption = getOptionByName("backendRootURL", rappConfig.options);
			if ( backendRootURLOption && backendRootURLOption.value )
				return true;
		}
		return false;
	}
	
	
	
	$scope.projectName = $routeParams.projectName;
	$scope.rappConfig = rappConfig;
	$scope.rapp = rapp;
	$scope.bootstrapObject = bootstrapObject;
	$scope.initRappConfig($scope.rappConfig, bootstrapObject);
});
