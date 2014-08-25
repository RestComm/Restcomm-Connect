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
	var serviceInstance = {};
	
	serviceInstance.looksAuthenticated = function () {
		var currentCookies = $browser.cookies();
		if ( !currentCookies.rvdticket )
			return false;
		return true;
	}
	
	serviceInstance.authResolver = function() {
		//var deferred = $q.defer();
		if ( !this.looksAuthenticated() ) {
			console.log("authResolver: not authenticated");
			$location.path("/login");
		}	
		//return deferred.promise;
	}
	
	return serviceInstance;
	
	
}]);
