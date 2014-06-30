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
