'use strict';

var rcMod = angular.module('rcApp');

rcMod.controller('RappManagerCtrl', function($scope, $upload, $location) {
	console.log("running RappManagerCtrl");
	$scope.test = "this is test var";
	 
	$scope.onFileSelect = function($files) {
	    for (var i = 0; i < $files.length; i++) {
	      var file = $files[i];
	      $scope.upload = $upload.upload({
	        url: '/restcomm-rvd/services/ras/app/new?name=' + "testname" , // upload.php
	        file: file,
	      }).progress(function(evt) {
	        console.log('percent: ' + parseInt(100.0 * evt.loaded / evt.total));
	      }).success(function(data, status, headers, config) {
	    	  console.log('file uploaded successfully');
	    	  $location.path("/ras/config/" + data[0].projectName);
	      });
	    }
	};
	
});

// Will need this controller when resolving its dependencies. 
var rappManagerConfigCtrl = rcMod.controller('RappManagerConfigCtrl', function($scope, $upload, $routeParams, rappConfig) {
	console.log("running RappManagerConfigCtrl");
	$scope.projectName = $routeParams.projectName;
	$scope.rappConfig = rappConfig;
});

rappManagerConfigCtrl.loadRappConfig = function ($q, $http, $route) {
	var defer = $q.defer();
	
	$http({url: '/restcomm-rvd/services/ras/app/getconfig?name=' + $route.current.params.projectName, method: "GET" })
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
