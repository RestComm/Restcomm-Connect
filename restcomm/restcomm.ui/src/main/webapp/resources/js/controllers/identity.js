'use strict';

angular.module('rcApp').controller('IdentityRegistrationCtrl', function ($scope, $location, RCommIdentityInstances, Notifications) {

    $scope.details = {
		RedirectUrl: parseRootUrl($location.absUrl(), $location.url()),
		InitialAccessToken: ""
	}

    $scope.submitRegistrationDetails = function (details) {
		console.log("submitting details");
		RCommIdentityInstances.register(details).catch(function (response) {
			if (response.status == 500)
				Notifications.error("Internal server error");
			else
			if (response.status == 409)
				Norifications.error("Already registered!");
		});
	}

    function parseRootUrl(locationAbsUrl, locationUrl) {
		var redirectUrl = locationAbsUrl.substring(0, locationAbsUrl.indexOf(locationUrl));
		if (redirectUrl && redirectUrl.endsWith("#")) // trim last # character if present
			redirectUrl = redirectUrl.substring(0, redirectUrl.length-1)
		return redirectUrl;
	}
});
