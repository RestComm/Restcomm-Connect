angular.module('Rvd')
.service('notifications', ['$rootScope', '$timeout', function($rootScope, $timeout) {
	var notifications = {data:[]};
	
	$rootScope.notifications = notifications;
	
	notifications.put = function (notif) {
		notifications.data.push(notif);
		
		$timeout(function () { 
			if (notifications.data.indexOf(notif) != -1)
				notifications.data.splice(notifications.data.indexOf(notif),1); 
		}, 3000);
	}
	
	notifications.remove = function (removedIndex) {
		notifications.data.splice(removedIndex, 1);
	}
	
	return notifications;
}]);

angular.module('Rvd').service('projectModules', [function () {
	var serviceInstance = {moduleData: []};
	
	serviceInstance.addModule = function (module) {
		serviceInstance.moduleData.push({name:module.name, label:module.label});
	}
	
	serviceInstance.removeModule = function (module) {
		serviceInstance.moduleData.splice(serviceInstance.moduleData.indexOf(module),1);
	}
	
	serviceInstance.getModuleSummary = function () {
		return serviceInstance.moduleData;
	}
	
	serviceInstance.log = function () {
		for (var i = 0; i < serviceInstance.moduleData.length; i++) {
			console.log(serviceInstance.moduleData[i]);
		}
	}
	
	return serviceInstance;
}]);

angular.module('Rvd').service('authentication', ['$browser', '$q', function ($browser, $q) {
	//console.log("Creating authentication service");
	var serviceInstance = {};
	
	serviceInstance.looksAuthenticated = function () {
		var currentCookies = $browser.cookies();
		//console.log("checking $browser cookies...");
		//console.log( currentCookies );

		if ( !currentCookies.rvdticket )
			return false;
		return true;
	}
	
	serviceInstance.authResolver = function() {
		var deferred = $q.defer();
		if ( !this.looksAuthenticated() ) {
			deferred.reject("AUTHENTICATION_ERROR");
		} else {
			deferred.resolve({status:"authenticated"});
		}
		return deferred.promise;
	}
	
	return serviceInstance;
	
	
}]);

angular.module('Rvd').service('projectSettingsService', ['$http','$q','$modal', function ($http,$q,$modal) {
	console.log("Creating projectSettigsService");
	var service = {};
	service.retrieve = function (name) {
		var deferred = $q.defer();
		$http({method:'GET', url:'services/projects/'+name+'/settings'})
		.success(function (data,status) {deferred.resolve(data)})
		.error(function (data,status) {
			if (status == 404)
				deferred.resolve({logging:false});
			else
				deferred.reject("ERROR_RETRIEVING_PROJECT_SETTINGS");
		});
		return deferred.promise;
	}
	
	service.save = function (name, projectSettings) {
		var deferred = $q.defer();
		$http({method:'POST',url:'services/projects/'+name+'/settings',data:projectSettings})
		.success(function (data,status) {deferred.resolve()})
		.error(function (data,status) {deferred.reject('ERROR_SAVING_PROJECT_SETTINGS')});
		return deferred.promise;
	}
	
	function projectSettingsModelCtrl ($scope, projectSettings, projectName, $modalInstance, notifications) {
		console.log("in projectSettingsModelCtrl");
		$scope.projectSettings = projectSettings;
		$scope.projectName = projectName;
		
		$scope.save = function (name, data) {
			console.log("saving projectSettings for " + name);
			service.save(name, data).then(
				function () {$modalInstance.close()}, 
				function () {notifications.put("Error saving project settings")}
			);
		}
		$scope.cancel = function () {
			$modalInstance.close();
		}
	}
	
	service.showModal = function(projectName) {
		var modalInstance = $modal.open({
			  templateUrl: 'templates/projectSettingsModal.html',
			  controller: projectSettingsModelCtrl,
			  size: 'lg',
			  resolve: {
				projectSettings: function () {
					var deferred = $q.defer()
					$http.get("services/projects/"+projectName+"/settings")
					.then(function (response) {
						deferred.resolve(response.data);
					}, function (response) {
						if ( response.status == 404 )
							deferred.resolve({});
						else {
							deferred.reject();
						}
					});
					return deferred.promise;
				},
				projectName: function () {return projectName;}
			  }
			});

			modalInstance.result.then(function (projectSettings) {
				console.log(projectSettings);
			}, function () {});	
	}
	
	return service;
}]);


angular.module('Rvd').service('projectLogService', ['$http','$q','$routeParams', 'notifications', function ($http,$q,$routeParams,notifications) {
	var service = {};
	service.retrieve = function () {
		var deferred = $q.defer();
		$http({method:'GET', url:'services/apps/'+$routeParams.projectName+'/log'})
		.success(function (data,status) {
			console.log('retrieved log data');
			deferred.resolve(data);
		})
		.error(function (data,status) {
			deferred.reject();
		});
		return deferred.promise;
	}
	service.reset = function () {
		var deferred = $q.defer();
		$http({method:'DELETE', url:'services/apps/'+$routeParams.projectName+'/log'})
		.success(function (data,status) {
			console.log('reset log data');
			notifications.put({type:'success',message:$routeParams.projectName+' log reset'});
			deferred.resolve();
		})
		.error(function (data,status) {
			notifications.put({type:'danger',message:'Cannot reset '+$routeParams.projectName+' log'});
			deferred.reject();
		});
		return deferred.promise;		
	}
	
	return service;
}]); 
