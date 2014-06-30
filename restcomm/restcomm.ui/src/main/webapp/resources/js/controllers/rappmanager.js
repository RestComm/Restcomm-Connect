'use strict';

var rcMod = angular.module('rcApp');

var rappManagerCtrl = rcMod.controller('RappManagerCtrl', function($scope, $upload, $location, products, localApps, $sce, $route) {
	console.log("running RappManagerCtrl");
	$scope.test = "this is test var";
	$scope.products = products;
	
	$scope.localApps = localApps;
	
	
	
	$scope.getLocalApp = function(appId) {
		for ( var i=0; i<localApps.length; i++ ) {
			if (localApps[i].rappInfo.id == appId)
				return localApps[i];
		}
		// return undefined;
	}
	
	$scope.buildStatusMessage = function(statuses) {
		if ( !statuses )
			return "";
		else
		if ( statuses.indexOf('Unconfigured') != -1 )
			return "Installed, needs configuration";
		else
		if ( statuses.indexOf('Active') != -1 )
			return "Active";
		else
			return statuses;
	}
		
	// merge AppStore and local information
	for ( var i=0; i<products.length; i++ ) {
		products[i].localApp = $scope.getLocalApp(products[i].info.appId);
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
	    	  console.log('file uploaded successfully');
	    	  //$location.path("/ras/apps/" + data[0].projectName + "/config");
	    	  //$location.path("/ras/config/" + data[0].projectName);
	    	  $route.reload();
	      });
	    }
	};
	
	$scope.formatHtml = function (markup) {
		return $sce.trustAsHtml(markup);
	}
	
});

rappManagerCtrl.getProducts = function ($q, $http) {
	var deferred = $q.defer();
	
	var apikey = "f837224433386cbc24e647f6292c12a7";
	var token = "bcdcd32f1e860cdca3d1f674e204dd0f";
	
	console.log("retrieving products from AppStore");
	$http({
		method:"GET", 
		url:"https://restcommapps.wpengine.com/edd-api/products/?key=" + apikey + "&token=" + token
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
		console.log("successfully received local apps");
		deferred.resolve(data.payload);
	}).error(function () {
		console.log("Error receiving local apps");
		deferred.reject("error");
	});
	
	return deferred.promise;
}

// Will need this controller when resolving its dependencies. 
var rappManagerConfigCtrl = rcMod.controller('RappManagerConfigCtrl', function($scope, $upload, $routeParams, rappConfig, $http) {
	
	$scope.initRappConfig = function (rappConfig) {
		var i;
		for ( i=0; i < rappConfig.options.length; i++ ) {
			if ( rappConfig.options[i].defaultValue )
				rappConfig.options[i].value = rappConfig.options[i].defaultValue;
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
			if ( data.rvdStatus == 'OK')
				console.log("successfully saved bootstrap information");
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
		
	console.log("running RappManagerConfigCtrl");
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
