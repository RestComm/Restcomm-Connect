angular.module("rcApp.restcommApps", []).config([ '$routeProvider',  function($routeProvider) {
	
	$routeProvider
	.when('/ras', {
		templateUrl: 'modules/rappmanager.html', 
		controller: 'RappManagerCtrl', 
		resolve: {
			products: rappManagerCtrl.getProducts, 
			localApps: function (rappService) { return rappService.getLocalApps();}
		} 
	})
	.when('/ras/config/:projectName/:mode?', {
		templateUrl: 'modules/rappmanager-config.html', 
		controller: 'RappManagerConfigCtrl', 
		resolve: { 
			rappConfig : function (rappService, $route) { return rappService.getAppConfig($route.current.params.projectName, $route.current.params.mode); },
			bootstrapObject : function (rappService, $route) { return rappService.getBoostrapObject($route.current.params.projectName); }
		}
	});
}]);

angular.module("rcApp.restcommApps").service("rappService", function ($http, $q, Notifications) {
	var service = {};
	//var localApps = undefined;
	var deferred;
	
	// return "a promise to return local apps"
	function getLocalApps() {
		if (!deferred)
			return refreshLocalApps();
		return deferred.promise;
	}
	// fetch apps from remote site
	function fetchLocalApps(deferred) {
		//var deferred = $q.defer();
		$http({
			method:"GET",
			url:"/restcomm-rvd/services/ras/apps"
		}).success(function (data) {
			console.log("successfully received local apps");
			//localApps = data.payload;
			deferred.resolve(data.payload);
		}).error(function () {
			console.log("Error receiving local apps");
			deferred.reject("error");
		});
	}
	// returns "a promise to return local apps"
	function refreshLocalApps() {
		console.log("refreshing local apps");
		//localApps = undefined;
		deferred = $q.defer();
		fetchLocalApps(deferred);
		return deferred.promise;		
	}
	function getAppByUrl(apps, appUrl) {
		for (var i=0; i < apps.length; i++) {
			var app = apps[i];
			var matches = RegExp(encodeURIComponent(app.projectName) + "/controller") .exec(appUrl);
			if (matches != null) {
				return app;
			}
		}
		//return undefined;
	}	
	
	function getAppConfig(appName, mode) {
		var defer = $q.defer();
		$http({url: '/restcomm-rvd/services/ras/apps/' + appName + '/config' + (mode ? ("/"+mode) : "") , method: "GET" })
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
	}
	
	function getBoostrapObject(appName) {
		var deferred = $q.defer();
		$http.get('/restcomm-rvd/services/ras/apps/' + appName + '/bootstrap' )
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
	// Notifies the provisioningUrl (if present) of the app about the number assignment
	function notifyIncomingNumberProvisioning(app, phoneNumber) {
		if (app.wasImported) {
			// retrieve app configuration
			getAppConfig(app.projectName).then(function (appConfig) {
				console.log("successfully retrieved configuration for application " + app.projectName);
				console.log(appConfig);
				if (appConfig.provisioningUrl) {
					if ( validateProvisioningUrl(appConfig.provisioningUrl, app) ) {
						$http({ 
							method: "POST", 
							url: appConfig.provisioningUrl + "/number",
							data: {phoneNumber: phoneNumber} // the instance id should be added here as well
						}).error(function (data, status) {
							Notifications.warn("Number provisioning failed with status " + status + " - " + appConfig.provisioningUrl);
						}).success(function() {
							console.log("Succesfully provisioned number " + phoneNumber);
						});
					} else {
						Notifications.warn("Cannot provision incoming number. Provisioning URL does not seem safe and has been rejected");
					}
				}
			});
		}
	}
	// Makes sure the provisioningUrl is safe to call. In the future each vendor should provide something like official domain where his services will reside
	// For now all urls are accepted
	function validateProvisioningUrl(provisioningUrl, app) {
		return true;
	}
	

	
	// Public interface
	service.getLocalApps = getLocalApps;
	service.refreshLocalApps = refreshLocalApps;
	service.getAppByUrl = getAppByUrl;
	service.getAppConfig = getAppConfig;
	service.getBoostrapObject = getBoostrapObject;
	service.notifyIncomingNumberProvisioning = notifyIncomingNumberProvisioning;
	
	
	return service;
});
	

