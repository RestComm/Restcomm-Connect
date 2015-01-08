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
			rappConfig : rappManagerConfigCtrl.loadRappConfig, 
			bootstrapObject : rappManagerConfigCtrl.loadBootstapObject 
		}
	});
}]);

