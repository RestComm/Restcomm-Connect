'use strict';

angular.module('rcApp').controller('IdentityRegistrationCtrl', function ($scope, $state, $location, RCommIdentityInstances, Notifications) {

    $scope.details = {
		RedirectUrl: parseRootUrl($location.absUrl(), $location.url()),
		InitialAccessToken: ""
	}

    $scope.submitRegistrationDetails = function (details) {
		RCommIdentityInstances.register(details).then(function (response) {
		    Notifications.info('Registered Identity Instance with ID ' );
		}, function (response) {
			if (response.status == 500)
				Notifications.error("Internal server error");
			else
			if (response.status == 409)
				Norifications.error("Already registered!");
		});
	}

	$scope.resetRegistrationToken = function(clientSuffix, newtoken) {
	    console.log("resetting registation token for client " + clientSuffix);
	}

    function parseRootUrl(locationAbsUrl, locationUrl) {
		var redirectUrl = locationAbsUrl.substring(0, locationAbsUrl.indexOf(locationUrl));
		if (redirectUrl && redirectUrl.endsWith("#")) // trim last # character if present
			redirectUrl = redirectUrl.substring(0, redirectUrl.length-1)

		return redirectUrl;
	}
});

angular.module('rcApp').controller('IdentityEditCtrl', function ($scope, $state, $location, RCommIdentityInstances, Notifications, IdentityConfig) {

    $scope.identity = IdentityConfig.getIdentity();

	$scope.resetRegistrationToken = function(clientSuffix, newtoken) {
	    console.log("resetting registation token for client " + clientSuffix);
	}
});

