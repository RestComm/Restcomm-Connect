var rappManagerConfigCtrl = angular.module("rcApp.restcommApps").controller('RappManagerConfigCtrl', function($scope, $upload, $routeParams, rappConfig, bootstrapObject, $http, Notifications, $window, rappService) {
	
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
		if (option.isInitOption || option.name == 'instanceToken' );
			return true;
		return false;
	}
	
	function generateUUID(){
	    var d = new Date().getTime();
	    var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
	        var r = (d + Math.random()*16)%16 | 0;
	        d = Math.floor(d/16);
	        return (c=='x' ? r : (r&0x7|0x8)).toString(16);
	    });
	    return uuid;
	};
	$scope.generateNewInstanceToken = function (option) {
		option.value = generateUUID();
	}
	function getOptionByName(name, options) {
		for (var i = 0; i < options.length; i++)
			if (options[i].name == name)
				return options[i];
	};
	$scope.getOptionByName = getOptionByName;
	$scope.createNewInstance = function() {
		console.log("creating new instance");
		var instanceToken = getOptionByName("instanceToken", rappConfig.options).value;
		var backendRootURL = getOptionByName("backendRootURL", rappConfig.options).value;
		$http({ url:backendRootURL + "/provisioning/spawnInstance.php?instanceToken=" + instanceToken, method:"PUT" })
		.success(function () {
			console.log("created new instance");
			Notifications.success("New instance created");
		})
		.error(function () {
			console.log("error creating new instance");
			Notifications.success("Error creating new instance");
		});
	}	
	$scope.startApplicationUI = function (rappConfig) {
		//getOptionByName("backendRootURL",rappConfig.options).value}}/ui/index.php?instanceToken={{getOptionByName("instanceToken",rappConfig.options).value
		var newWin = window.open('about:blank', '_blank');
		var url = getOptionByName("backendRootURL",rappConfig.options).value + "/ui/index.php?instanceToken=" + getOptionByName("instanceToken",rappConfig.options).value;
		console.log("starting application UI at " + url);
		$http.post(url, bootstrapObject).then( function (response) {
			newWin.location.href = response.headers('Location');
		},
		function (response) {
				console.log(response);
		});
	}
	$scope.submitForm = function() {
		console.log("submitting form");
	}
	$scope.watchOptionFormValidity = function (status) {
		console.log(status);
		$scope.optionsFormValid = status;
	}
	
	
	
	$scope.projectName = $routeParams.projectName;
	$scope.rappConfig = rappConfig;
	
	$scope.initRappConfig($scope.rappConfig);
});
