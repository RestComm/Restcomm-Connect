'use strict';

var rcMod = angular.module('rcApp');

rcMod.controller('RappManagerCtrl', function($scope, $upload, $location) {
	console.log("running RappManagerCtrl");
	$scope.test = "this is test var";
	 
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
	    	  $location.path("/ras/config/" + data[0].projectName);
	      });
	    }
	};
	
});

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
