'use strict';

angular.module('rcApp').controller('IdentityRegistrationCtrl', function ($scope, $state, $location, RCommOrgIdentities, Notifications, identity) {

    $scope.details = {
		RedirectUrl: parseRootUrl($location.absUrl(), $location.url()),
		InitialAccessToken: ""
	}

    $scope.submitRegistrationDetails = function (details) {
		RCommOrgIdentities.register(details).then(function (response) {
		    Notifications.info('Registered Identity Instance ' + response.data.Name + ". Please refresh to enable SSO." );
		    $state.go('restcomm.dashboard');
		}, function (response) {
			if (response.status == 500)
				Notifications.error("Internal server error");
			else
			if (response.status == 409)
				Notifications.error("Organization Identity '" + response.data.occupiedName + "' seems already registered!");
		});
	}

    function parseRootUrl(locationAbsUrl, locationUrl) {
		var redirectUrl = locationAbsUrl.substring(0, locationAbsUrl.indexOf(locationUrl));
		if (redirectUrl && redirectUrl.endsWith("#")) // trim last # character if present
			redirectUrl = redirectUrl.substring(0, redirectUrl.length-1)

		return redirectUrl;
	}

})
.controller('IdentityEditCtrl', function ($scope, $state, $location, RCommOrgIdentities, Notifications, identity, $timeout, $window) {
    $scope.identity = identity;
    $scope.restcommDetails = {};

	$scope.resetRegistrationToken = function(clientSuffix, newtoken) {
	    RCommOrgIdentities.resetClientToken(identity.Sid, {'client-suffix':clientSuffix, token:newtoken}).then(function () {
	        Notifications.success("Registration token was reset for " + clientSuffixToName(clientSuffix));
	    }, function () {
	        Notifications.error("Registration Token reset failed for " + clientSuffixToName(clientSuffix));
	    });
	}

	$scope.unregisterInstance = function(identity) {
	    RCommOrgIdentities.unregister(identity.Sid).then(function () {
	        Notifications.info("Identity has been unregistered from authorization server. SSO disabled. Will now reload page...");
	        $state.go('restcomm.dashboard').then(function () {
                $window.location.reload();
            });
	    })
	}

	function clientSuffixToName(clientSuffix) {
	    if (clientSuffix == 'restcomm')
	        return 'Restcomm';
	}

})
.controller('AccountUnlinkedCtrl', function ($scope, $state, $stateParams, AuthService, RCommAccounts, Notifications) {
    $scope.unlinkedUsername = AuthService.getKeycloakUsername();
    $scope.accountPassword = '';
    //console.log("evaluateAccess: " + $stateParams.evaluateAccess);
    $scope.verifyAccountPassword = function(password) {
        console.log('verifying password');
        RCommAccounts.link({accountSid:AuthService.getKeycloakUsername(), format:null}, $.param({password:password}), function (a,b) {
            AuthService.reload().then(function () {
                $state.go('restcomm.dashboard');
            }, function () {
                Notifications.error("Re-authorization failed");
            });
        }, function (response) {
            if (response.status == 403 && response.data && response.data.code == 'INVALID_LINKING_PASSWORD') {
                Notifications.error("Invalid account password for " + AuthService.getKeycloakUsername());
                $scope.accountPassword = '';
            }

        });
    }
})
.controller('NoAccountCtrl', function ($scope, AuthService) {
    $scope.oauthUsername = AuthService.getKeycloakUsername();
    $scope.logoutKeycloak = function () {
        keycloakLogout(); // defined in restcomm.js
    }
})
.controller('AuthErrorCtrl', function ($scope, AuthService, errorType) {
    $scope.tokenUsername = AuthService.getKeycloakUsername();
    $scope.errorType = errorType;
    $scope.logoutKeycloak = function () {
        keycloakLogout(); // defined in restcomm.js
    }
})
;

