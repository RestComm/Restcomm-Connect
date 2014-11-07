'use strict';

var rcMod = angular.module('rcApp');

var rappManagerCtrl = rcMod.controller('RappManagerCtrl', function($scope, $upload, $location, products, localApps, $sce, $route, Notifications, rappManagerConfig) {	
	function getLocalApp (appId, localApps) {
		if ( appId )
			for ( var i=0; i<localApps.length; i++ ) {
				if (localApps[i].rappInfo.id == appId)
					return localApps[i];
			}
		// return undefined;
	}	
	function getOnlineApp (appId, onlineApps) {
		if ( appId )
			for ( var i=0; i<onlineApps.length; i++ ) {
				if ( onlineApps[i].info.appId == appId )
					return onlineApps[i];
			}
		// return undefined
	}
	function selectedListItemView (app) {
		if ( app.local && !app.online )
			return "local";
		return "online";
	}
	$scope.selectedListItemView = selectedListItemView;
	
	function mergeOnlineWithLocalApps(onlineApps, localApps) {
		var mergedApps = [];
		// first iterate over localApps
		for ( var i=0; i<localApps.length; i++ ) {
			var appId = localApps[i].rappInfo.id;
			if ( getOnlineApp(appId, onlineApps) )
				continue; // will populate the list when processing online apps
			var app = {online: undefined, local: localApps[i]};
			mergedApps.push(app);
		}
		// then iterate over onlineApps
		for ( i=0; i<onlineApps.length; i++ ) {
			var appId = onlineApps[i].info.appId;
			var app = {online: onlineApps[i], local: getLocalApp(appId, localApps)};
			mergedApps.push(app);
		}
		
		return mergedApps;
	}
	
	$scope.apps = mergeOnlineWithLocalApps( products, localApps );
	
	
	$scope.buildStatusMessage = function(app) {
		if ( !(app.local && app.local.status) )
			return "Not installed";
			
		var statuses = app.local.status;
		var displayedStatus = "";
		if ( statuses.indexOf('Unconfigured') != -1 )
			displayedStatus = "Installed, Needs configuration";
		else
		if ( statuses.indexOf('Active') != -1 )
			displayedStatus = "Active";
		else
			displayedStatus = statuses;
		
		if ( ! app.online )
			displayedStatus = "Local, " + displayedStatus;
		
		return displayedStatus;
	}
		
	// merge AppStore and local information
	for ( var i=0; i<products.length; i++ ) {
		products[i].localApp = getLocalApp(products[i].info.appId, localApps);
	}

	$scope.onFileSelect = function($files) {
	    for (var i = 0; i < $files.length; i++) {
	      var file = $files[i];
	      $scope.upload = $upload.upload({
	        url: '/restcomm-rvd/services/ras/apps' + "/testname", // upload.php
	        file: file,
	      }).progress(function(evt) {
	        console.log('percent: ' + parseInt(100.0 * evt.loaded / evt.total));
	      }).success(function(data, status, headers, config) {
			  if (status == 409) {
	    		  console.log(data.exception.message);
	    		  Notifications.warn("This application is already installed");
	    	  } else {
				  //console.log('Application uploaded successfully');
				  //$location.path("/ras/apps/" + data[0].projectName + "/config");
				  //$location.path("/ras/config/" + data[0].projectName);
	    		  Notifications.success("Application installed");
				  $route.reload();
			  }
	      })
	      .error( function (data, status, headers) {
	    	  if (status == 409)
	    		  Notifications.warn("This application is already installed");
	    	  else
	    		  Notifications.error("Cannot import application package");
	      });
	    }
	};
	
	$scope.formatHtml = function (markup) {
		return $sce.trustAsHtml(markup);
	}
	$scope.rappManagerConfig = rappManagerConfig;
	
});

rappManagerCtrl.getProducts = function ($q, $http, rappManagerConfig) {
	var deferred = $q.defer();
	
	console.log("retrieving products from AppStore");
	$http({
		method:"GET", 
		//url:"https://restcommapps.wpengine.com/edd-api/products/?key=" + apikey + "&token=" + token + "&cacheInvalidator=" + new Date().getTime()
		url:"http://" + rappManagerConfig.rasHost + "/edd-api/products/?key=" + rappManagerConfig.rasApiKey + "&token=" + rappManagerConfig.rasToken + "&cacheInvalidator=" + new Date().getTime()
	}).success(function (data) {
		console.log("succesfully retrieved " + data.products.length + " products from AppStore");
		deferred.resolve(data.products);
	}).error(function () {
		console.log("http error while retrieving products from AppStore");
		deferred.reject("http error");
	});
	
	return deferred.promise;
}

rappManagerCtrl.getLocalApps = function ($q, $http) {
	var deferred = $q.defer();
	$http({
		method:"GET",
		url:"/restcomm-rvd/services/ras/apps"
	}).success(function (data) {
		//console.log("successfully received local apps");
		deferred.resolve(data.payload);
	}).error(function () {
		console.log("Error receiving local apps");
		deferred.reject("error");
	});
	
	return deferred.promise;
}

// Will need this controller when resolving its dependencies. 
var rappManagerConfigCtrl = rcMod.controller('RappManagerConfigCtrl', function($scope, $upload, $routeParams, rappConfig, bootstrapObject, $http, Notifications) {
	
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
		console.log("enabling configuration");
		var bootstrapObject = $scope.generateBootstrap(rappConfig);
		console.log(bootstrapObject);
		$http({
			url: '/restcomm-rvd/services/ras/apps/' + $scope.projectName + '/bootstrap',
			method: 'POST',
			data: bootstrapObject,
			headers: {'Content-Type': 'application/data'}
		}).success(function (data) {
			if ( data.rvdStatus == 'OK') {
				console.log("successfully saved bootstrap information");
				Notifications.success('Application configured');
			}
			else
				console.log("Rvd error while saving bootstrap information");
		}).error(function () {
			console.log("http error while saving bootstrap info");
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
	}
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
	
	$scope.projectName = $routeParams.projectName;
	$scope.rappConfig = rappConfig;
	
	$scope.initRappConfig($scope.rappConfig);
});

rappManagerConfigCtrl.loadRappConfig = function ($q, $http, $route) {
	var defer = $q.defer();
	
	$http({url: '/restcomm-rvd/services/ras/apps/' + $route.current.params.projectName + '/config', method: "GET" })
	.success(function (data, status, headers, config) {
		if (data.rvdStatus == "OK") {
			console.log("succesfull retrieved app config");
			defer.resolve(data.payload);
		} else {
			defer.reject("error getting app config")
		}
	})
	.error(function () {
		console.log("error getting app config"); 
		defer.reject("bad response");
	});
	
	return defer.promise;
};
rappManagerConfigCtrl.loadBootstapObject = function ($q, $http, $route) {
	var deferred = $q.defer();
	$http.get('/restcomm-rvd/services/ras/apps/' + $route.current.params.projectName + '/bootstrap' )
	.success(function (data, status) {
		deferred.resolve(data);
	})
	.error(function (data,status) {
		if (status == 404)
			deferred.resolve(null);
		else
			deferred.reject(status);
	});
	
	return deferred.promise;
}
