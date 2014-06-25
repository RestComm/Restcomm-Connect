angular.module('Rvd')
.service('notifications', ['$rootScope', '$timeout', function($rootScope, $timeout) {
	console.log("running notifications servivce");
	var notifications = {data:[]};
	
	
	$rootScope.notifications = notifications.data;
	
	notifications.put = function (notif) {
		notifications.data.push(notif);
		
		$timeout(function () { 
			notifications.data.splice(notifications.data.indexOf(notif),1); 
		}, 5000);
	}
	
	return notifications;
}]);
