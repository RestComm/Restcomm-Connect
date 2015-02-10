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
	.when('/ras/config/:projectName/:mode?', {
		templateUrl: 'modules/rappmanager-config.html', 
		controller: 'RappManagerConfigCtrl', 
		resolve: { 
			rappConfig : function (rappService, $route) { return rappService.getAppConfig($route.current.params.projectName);}, //, $route.current.params.mode); },
			bootstrapObject : function (rappService, $route) { return rappService.getBoostrapObject($route.current.params.projectName); }
		}
	});
}]);

	

