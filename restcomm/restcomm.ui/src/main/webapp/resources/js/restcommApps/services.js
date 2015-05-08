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
			url:"/restcomm-rvd/apps"
		}).success(function (data) {
			console.log("Received apps from RVD");
			//localApps = data.payload;
			deferred.resolve(data.payload);
		}).error(function () {
			console.log("Error receiving apps from RVD");
			deferred.reject("error");
		});
	}
	// returns "a promise to return local apps"
	function refreshLocalApps() {
		//console.log("refreshing local apps");
		//localApps = undefined;
		deferred = $q.defer();
		fetchLocalApps(deferred);
		return deferred.promise;		
	}
	function getAppByUrl(apps, appUrl) {
		for (var i=0; i < apps.length; i++) {
			var app = apps[i];
			var pattern = app.projectName + "/controller";
			var decodedUrl = decodeURIComponent(appUrl);
			var matches = RegExp(pattern) .exec(decodedUrl);
			if (matches != null) {
				return app;
			}
		}
		//return undefined;
	}	
	
	function getAppConfig(appName, mode) {
		var defer = $q.defer();
		$http({url: '/restcomm-rvd/apps/' + appName + '/config' + (mode ? ("/"+mode) : "") , method: "GET" })
		.success(function (data, status, headers, config) {
			if (data.rvdStatus == "OK") {
				defer.resolve(data.payload);
			} else
			if (data.rvdStatus == "NOT_FOUND")
				defer.reject("application has no RAS capabilities");
			else
				defer.reject("error getting app config")
		})
		.error(function () {
			console.log("error getting app config"); 
			defer.reject("bad response");
		});
		return defer.promise;
	}
	
	function getApp(appName) {
		var defer = $q.defer();
		$http({url: '/restcomm-rvd/apps/' + appName, method: "GET" })
		.success(function (data, status, headers, config) {
			if (data.rvdStatus == "OK") {
				//console.log("succesfull retrieved app config");
				defer.resolve(data.payload);
			} else {
				defer.reject("error getting app config")
			}
		})
		.error(function () {
			console.log("error getting app"); 
			defer.reject("bad response");
		});
		return defer.promise		
	}
	
	function getBoostrapObject(appName) {
		var deferred = $q.defer();
		$http.get('/restcomm-rvd/apps/' + appName + '/parameters' )
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
		//if (app.wasImported) {
			// retrieve app configuration
			getAppConfig(app.projectName).then(function (appConfig) {
				//console.log("successfully retrieved configuration for application " + app.projectName);
				//console.log(appConfig);
				if (appConfig.provisioningUrl) {
					if ( validateProvisioningUrl(appConfig.provisioningUrl, app) ) {
						$http({ 
							method: "POST", 
							url: appConfig.provisioningUrl,
							data: {
								type: "number",
								payload: {
									action: "assign",
									phoneNumber: phoneNumber
								}
							} // the instance id should be added here as well
						}).error(function (data, status) {
							Notifications.warn("Number provisioning failed with status " + status + " - " + appConfig.provisioningUrl );
							if ( status == 404)
								console.log( "This maybe a CORS issue. Make sure the provisioning server supports CORS requests.");
						}).success(function(data) {
							if (data) {
								if (data.status == "ok")
									Notifications.success("Succesfully provisioned number " + phoneNumber + (data.message ? (" - "+data.message) : ""));
								else
								if (data.status == "error")
									Notifications.warn("Number provisioning failed for " + phoneNumber + (data.message ? (" - "+data.message) : ""));
							} else
								Notifications.success("Succesfully provisioned number ");
						});
					} else {
						Notifications.warn("Cannot provision incoming number. Provisioning URL does not seem safe and has been rejected");
					}
				}
			});
		//}
	}
	
	//function notifyConfigurationUrl(rappConfig) {
	function provisionApplicationParameters(rappConfig, bootstrapObject) {
		if ( rappConfig.provisioningUrl && bootstrapObject && Object.keys(bootstrapObject).length > 0 ) {
			var url = rappConfig.provisioningUrl;
			$http({
				url: url,
				method: 'POST',
				data: {
					type: "parameters",
					payload: bootstrapObject
				},
				headers: {'Content-Type': 'application/data'}
			}).success(function (data, status) {
				if ( data && data.status == "ok" ) {
					//console.log("contacted configurationUrl - " + status);
					console.log("Succesfully provisioned application parameters");
					if (data.message)
						Notifications.success(data.message);
					if ( data.redirectUrl ) {
						//console.log("redirect to " + data.redirectUrl);
						$window.open(data.redirectUrl, '_blank');
					}
				} else 
				if (data && data.status == "error") {
					var message = "Error provisioning application parameters";
					if (data.message)
						message += " - " + data.message;
					Notifications.warn(message);
				}
			})
			.error(function (data, status) {
				if ( status == 404) {
					console.log( "Error contacting provisioning url - " + url + " - " + status + ". This maybe a CORS issue. Make sure the provisioning server supports CORS requests.");
					Notifications.warn("Error provisioning application parameters. This may be a CORS issue.");
				}
				else {
					console.log("Error contacting provisioning url - " + url + " - " + status);
					Notifications.warn("Error provisioning application parameters. This may be a CORS issue.");
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
	service.getApp = getApp;
	service.getBoostrapObject = getBoostrapObject;
	service.notifyIncomingNumberProvisioning = notifyIncomingNumberProvisioning;
	service.provisionApplicationParameters = provisionApplicationParameters;
	
	
	return service;
});
