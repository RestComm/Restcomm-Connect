var App = angular.module('Rvd', [
	'angularFileUpload',
	'ngRoute',
	'ngDragDrop',
	'ui.bootstrap',
	'ui.bootstrap.collapse',
	'ui.bootstrap.popover',
	'ui.sortable',
	'basicDragdrop',
	'pascalprecht.translate',
	'ngSanitize',
	'ngResource'
]);

var rvdMod = App;

App.config([ '$routeProvider', '$translateProvider', function($routeProvider, $translateProvider) {
	
	$routeProvider.when('/project-manager/:projectKind', {
		templateUrl : 'templates/projectManager.html',
		controller : 'projectManagerCtrl',
		resolve: {
			authInfo: function (authentication) {return authentication.authResolver();}
		}
	})
	.when('/home', {
		templateUrl : 'templates/home.html',
		controller : 'homeCtrl',
		resolve: {
			authInfo: function (authentication) {return authentication.authResolver();}
		}
	})
	.when('/designer/:projectName', {
		templateUrl : 'templates/designer.html',
		controller : 'designerCtrl',
		resolve: {
			authInfo: function (authentication) {return authentication.authResolver();},
			//projectSettings: function (projectSettingsService, $route) {return projectSettingsService.retrieve($route.current.params.projectName);},
			project: function(designerService, $route) { return designerService.openProject($route.current.params.projectName); },
			bundledWavs: function(designerService) { return designerService.getBundledWavs()}
		}
	})
	.when('/packaging/:projectName', {
		templateUrl : 'templates/packaging/form.html',
		controller : 'packagingCtrl',
		resolve: {
			rappWrap: function(RappService) {return RappService.getRapp();},
			authInfo: function (authentication) {return authentication.authResolver();},
			rvdSettingsResolver: function (rvdSettings) {return rvdSettings.refresh();} // not meant to return anything back. Just trigger the fetching of the settings
		}
	})
	.when('/packaging/:projectName/download', {
		templateUrl : 'templates/packaging/download.html',
		controller : 'packagingDownloadCtrl',
		resolve: { 
			binaryInfo: packagingDownloadCtrl.getBinaryInfo,
			authInfo: function (authentication) {return authentication.authResolver();}
		}
	})	
	.when('/upgrade/:projectName', {
		templateUrl : 'templates/upgrade.html',
		controller : 'upgradeCtrl',
		resolve: {
			authInfo: function (authentication) {return authentication.authResolver();}
		}
	})
	.when('/login', {
		templateUrl : 'templates/login.html',
		controller : 'loginCtrl'
	})
	.when('/designer/:projectName/log', {
		templateUrl : 'templates/projectLog.html',
		controller : 'projectLogCtrl'
	})	
	.otherwise({
		redirectTo : '/home'
	});
	
	$translateProvider.useStaticFilesLoader({
  		prefix: '/restcomm-rvd/languages/',
  		suffix: '.json'
	});
	$translateProvider.use('en-US');

}]);

App.factory( 'dragService', [function () {
	var dragInfo;
	var dragId = 0;
	var pDragActive = false;
	var serviceInstance = {
		newDrag: function (model) {
			dragId ++;
			pDragActive = true;
			if ( typeof(model) === 'object' )
				dragInfo = { id : dragId, model : model };
			else
				dragInfo = { id : dragId, class : model };
				
			return dragId;
		},
		popDrag:  function () {
			if ( pDragActive ) {
				var dragInfoCopy = angular.copy(dragInfo);
				pDragActive = false;
				return dragInfoCopy;
			}
		},
		dragActive: function () {
			return pDragActive; 
		}
		
	};
	return serviceInstance;
}]);

/*
App.factory('protos', function () {
	var protoInstance = { 
		nodes: {
				voice: {kind:'voice', name:'module', label:'Untitled module', steps:[], iface:{edited:false,editLabel:false}},
				ussd: {kind:'ussd', name:'module', label:'Untitled module', steps:[], iface:{edited:false,editLabel:false}},		
				sms: {kind:'sms', name:'module', label:'Untitled module', steps:[], iface:{edited:false,editLabel:false}},
		},
	};
	return protoInstance;
});
*/


App.filter('excludeNode', function() {
    return function(items, exclude_named) {
        var result = [];
        items.forEach(function (item) {
            if (item.name !== exclude_named) {
                result.push(item);
            }
        });                
        return result;
    }
});

