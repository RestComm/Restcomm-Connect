App.controller('AppCtrl', function ($rootScope, $location) {
	$rootScope.$on("$routeChangeError", function(event, current, previous, rejection) {
        //console.log('on $routeChangeError');
        if ( rejection == "AUTHENTICATION_ERROR" ) {
			console.log("AUTHENTICATION_ERROR");
			$location.path("/login");
		} else {
			$rootScope.rvdError = rejection;
		}
    });
    
    $rootScope.$on("resourceNotFound", function(p1, p2) {
    	//console.log("resourceNotFound event caught");
    	$rootScope.rvdError = {message: "The requested resource was not found. Sorry about that."};
    });
    
    $rootScope.$on('$routeChangeStart', function(){
    	$rootScope.rvdError = undefined;
	});
});

/*
var loginCtrl = angular.module('Rvd')
.controller('loginCtrl', ['authentication', '$scope', '$http', 'notifications', '$location', function (authentication, $scope, $http, notifications, $location) {
//	console.log("run loginCtrl ");
	authentication.clearTicket();
	
	$scope.doLogin = function (username, password) {
		authentication.doLogin(username,password).then(function () {
			$location.path("/home");
		}, function () {
			notifications.put({message:"Login failed", type:"danger"});
		})
	}
}]);
*/


App.controller('homeCtrl', function ($scope, authInfo) {
});

angular.module('Rvd').controller('projectLogCtrl', ['$scope', '$routeParams', 'projectLogService', function ($scope, $routeParams, projectLogService) {
	//console.log('in projectLogCtrl');
	$scope.projectName = $routeParams.projectName;
	$scope.logData = '';
	
	function retrieveLog() {
		projectLogService.retrieve().then(function (logData) {$scope.logData = logData;})
	}
	$scope.retrieveLog = retrieveLog;
	
	function resetLog() {
		projectLogService.reset().then(function () {$scope.logData = "";});
	}
	$scope.resetLog = resetLog;
	
	retrieveLog($scope.projectName);
}]);

App.controller('mainMenuCtrl', ['$scope', 'authentication', '$location', '$modal','$q', '$http', function ($scope, authentication, $location, $modal, $q, $http) {
	$scope.authInfo = authentication.getAuthInfo();
	//$scope.username = authentication.getTicket(); //"Testuser@test.com";
	
	function logout() {
		console.log("logging out - NOT IMPLEMENTED");
		/*
		authentication.doLogout().then(function () {
			$location.path("/login");
		}, function () {
			$location.path("/login");
		});*/
	}
	$scope.logout = logout;
	
	function settingsModalCtrl ($scope, $timeout, $modalInstance, settings, rvdSettings) {
		$scope.settings = settings;
		$scope.rvdSettings = rvdSettings;
		$scope.defaultSettings = rvdSettings.getDefaultSettings();
		
		$scope.ok = function () {
			rvdSettings.saveSettings($scope.settings).then(function () {
				$modalInstance.close($scope.settings);
			}, function () {
				notifications.put("Cannot save settings");
			});
		};

		$scope.cancel = function () {
			$modalInstance.dismiss('cancel');
		};
		
		// watch form validation status and copy to outside scope so that the OK
		// button (which is outside the form's scope) status can be updated
		$scope.watchForm = function (formValid) {
			$scope.preventSubmit = !formValid;
		}
	};
	
	$scope.showSettingsModal = function (settings) {
		var modalInstance = $modal.open({
		  templateUrl: 'templates/designerSettingsModal.html',
		  controller: settingsModalCtrl,
		  size: 'lg',
		  resolve: {
			settings: function (rvdSettings) {	return rvdSettings.refresh();}
		  }
		});

		modalInstance.result.then(function (rvdSettings) {
			//console.log(settings);
			// $scope.settings
		}, function () {
		  // $log.info('Modal dismissed at: ' + new Date());
		});		
	}
	
	
	
	
}]);

App.controller('translateController', function($translate, $scope) {
  $scope.changeLanguage = function (langKey) {
    $translate.use(langKey);
  };
  $scope.getCurrentLanguage = function () {
	return $translate.use();
  }
});

angular.module('Rvd').controller('wavManagerController', function ($rootScope, $scope, $http, $upload) {
	$scope.deleteWav = function (wavItem) {
		$http({url: 'services/projects/' + $scope.projectName + '/wavs?filename=' + wavItem.filename, method: "DELETE"})
		.success(function (data, status, headers, config) {
			console.log("Deleted " + wavItem.filename);
			throwRemoveWavEvent(wavItem.filename);
		}).error(function (data, status, headers, config) {
			console.log("Error deleting " + wavItem.filename);
		});
	}
	
	// File upload stuff for play verbs
	$scope.onFileSelect = function($files) {
		    // $files: an array of files selected, each file has name, size, and
			// type.
		    for (var i = 0; i < $files.length; i++) {
		      var file = $files[i];
		      $scope.upload = $upload.upload({
		        url: 'services/projects/' + $scope.projectName + '/wavs',
		        file: file,
		      }).success(function(data, status, headers, config) {
		        // file is uploaded successfully
		    	  console.log('file uploaded successfully');
		    	  $rootScope.$broadcast("fileupload");
		      }).progress(function () {});
		      // .error(...)
		      // .then(success, error, progress);
		    }
	};
	
	function throwRemoveWavEvent(wavname) {
		$rootScope.$broadcast("project-wav-removed", wavname);
	} 
});

angular.module('Rvd').controller('playStepController', function ($scope) {
	$scope.$on('project-wav-removed', function (event, data) {
		if ( data == $scope.step.local.wavLocalFilename )
			$scope.step.local.wavLocalFilename = "";
	});
});
