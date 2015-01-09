angular.module("rcApp.restcommApps", []).config([ '$routeProvider',  function($routeProvider) {
	
	$routeProvider
	.when('/ras', {
		templateUrl: 'modules/rappmanager.html', 
		controller: 'RappManagerCtrl', 
		resolve: {
			products: rappManagerCtrl.getProducts, 
			localApps: rappManagerCtrl.getLocalApps
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

angular.module("rcApp.restcommApps").service("rappService", function ($http, $q) {
	var service = {};
	
	service.getAppConfig = function (appName, mode) {
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
	
	service.getBoostrapObject = function (appName) {
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
	
	return service;
});
	

