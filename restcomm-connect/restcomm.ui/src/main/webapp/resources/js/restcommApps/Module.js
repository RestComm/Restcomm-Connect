angular.module("rcApp.restcommApps", ['ui.router']).config([ '$stateProvider',  function($stateProvider) {

	$stateProvider.state('restcomm.ras',{
	    url:'/ras',
	    templateUrl: 'modules/rappmanager.html',
        controller: 'RappManagerCtrl',
        resolve: {
            products: function (rappService) {return rappService.getProducts()}, //  no need to wait for authorization here since we're getting products from AppStore
            localApps: function (rappService,authorize) { return rappService.refreshLocalApps();}
        },
        parent:'restcomm'
	});
	$stateProvider.state('restcomm.ras-config',{
	    url:'/ras/config/:applicationSid=:projectName/:mode?',
		templateUrl: 'modules/rappmanager-config.html',
		controller: 'RappManagerConfigCtrl',
		resolve: {
			rappConfig : function (rappService, $stateParams, authorize) {
			    return rappService.getAppConfig($stateParams.applicationSid);
			},
			rapp: function (rappService, $stateParams, authorize) {
			    return rappService.getApp($stateParams.applicationSid);
			},
			bootstrapObject : function (rappService, $stateParams, authorize) { return rappService.getBoostrapObject($stateParams.applicationSid); }
		},
		parent:'restcomm'
	});

}]);

	

