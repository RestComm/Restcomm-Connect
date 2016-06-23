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

    function parseRootUrl(locationAbsUrl, locationUrl) {
		var redirectUrl = locationAbsUrl.substring(0, locationAbsUrl.indexOf(locationUrl));
		if (redirectUrl && redirectUrl.endsWith("#")) // trim last # character if present
			redirectUrl = redirectUrl.substring(0, redirectUrl.length-1)

		return redirectUrl;
	}
});

angular.module('rcApp').controller('IdentityEditCtrl', function ($scope, $state, $location, RCommIdentityInstances, Notifications, identity) {
    $scope.identity = identity;
    $scope.restcommUiDetails = {};
    $scope.restcommRestDetails = {};
    $scope.rvdUiDetails = {};
    $scope.rvdRestDetails = {};

	$scope.resetRegistrationToken = function(clientSuffix, newtoken) {
	    RCommIdentityInstances.resetClientToken(identity.Sid, {'client-suffix':clientSuffix, token:newtoken}).then(function () {
	        Notifications.success("Registration token was reset for " + clientSuffixToName(clientSuffix));
	    }, function () {
	        Notifications.error("Registration Token reset failed for " + clientSuffixToName(clientSuffix));
	    });
	}

	function clientSuffixToName(clientSuffix) {
	    if (clientSuffix == 'restcomm-ui')
	        return 'Admin UI';
	    else
	    if (clientSuffix == 'restcomm-rest')
	        return 'Restcomm REST';
        else
	    if (clientSuffix == 'rvd-ui')
	        return 'RVD UI';
        else
	    if (clientSuffix == 'rvd-rest')
	        return 'RVD REST';

	}

});

