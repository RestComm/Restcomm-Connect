'use strict';

var rcMod = angular.module('rcApp');

rcMod.controller('RappManagerCtrl', function($scope, $upload) {
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
	    	  $scope.$emit("fileupload");
	      });
	    }
	};
	
});