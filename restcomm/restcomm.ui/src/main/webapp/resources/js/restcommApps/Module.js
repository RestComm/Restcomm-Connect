angular.module("rcApp.restcommApps", []).config([ '$routeProvider',  function($routeProvider) {
	
	$routeProvider
	.when('/ras', {
		templateUrl: 'modules/rappmanager.html', 
		controller: 'RappManagerCtrl', 
		resolve: {
			products: rappManagerCtrl.getProducts, 
			localApps: function (rappService) { return rappService.refreshLocalApps();}
		} 
	})
	.when('/ras/config/:projectSid=:projectName/:mode?', {
		templateUrl: 'modules/rappmanager-config.html', 
		controller: 'RappManagerConfigCtrl', 
		resolve: { 
			rappConfig : function (rappService, $route) { return rappService.getAppConfig($route.current.params.projectSid);}, //, $route.current.params.mode); },
			rapp: function (rappService, $route) {return rappService.getApp($route.current.params.projectSid);},
			bootstrapObject : function (rappService, $route) { return rappService.getBoostrapObject($route.current.params.projectSid); }
		}
	});
}]);

	

