'use strict';

angular.module('rcApp').controller('EventsCtrl', function ($rootScope, AuthService, rappService, $state, Notifications) {
	//console.log("INSIDE EventsCtrl");
	$rootScope.$on("incoming-number-updated", function (event, data) {
        //console.log("new incoming-number-updated event with the following data: ");
        //console.log(data);
        // is there a voice_url ? Only voice_urls support provisioning
        if ( data.params.VoiceUrl ) {
            rappService.getLocalApps().then(function (localApps) {
                //console.log("Just received local apps. will now go through local apps and search for the voice_url " + data.params.VoiceUrl);
                //console.log(localApps);
                var app = rappService.getAppByUrl(localApps, data.params.VoiceUrl);
                if (app) {
                    //console.log("Will notify " + app.projectName + " app");
                    //console.log(app);
                    rappService.notifyIncomingNumberProvisioning(app, data.params.PhoneNumber);
                } else {
                    //console.log("no application matched");
                }
            });
        }
	});

	$rootScope.$on('$stateChangeStart', function(event, toState, toParams, fromState, fromParams, options){
	    //console.log("switching states: " + fromState.name + " -> " + toState.name);
	});

	$rootScope.$on('$stateChangeError',  function(event, toState, toParams, fromState, fromParams, error){
	    event.preventDefault();
	    console.log("Error switching state: " + fromState.name + " -> " + toState.name);
	    // see AuthService.evaluateAccess() for error definitions
	    if (error == 'LOGGED_ACCOUNT') { // attempt to navigate to a view for not-logged users while logged
	        $state.go('restcomm.dashboard');
	    } else
	    if (error == "MISSING_ACCOUNT_SID")
	        $state.go("public.login");
	    else
	    if (error == 'RESTCOMM_AUTH_FAILED' || error == 'RESTCOMM_NOT_AUTHENTICATED') {
	        //Notifications.error('Unauthorized access');
	        $state.go("public.login");
	    } else
	    if (error == "KEYCLOAK_INSTANCE_NOT_REGISTERED") {
	       Notifications.error("Identity Instance not registered.");
	        $state.go("restcomm.dashboard");
	    } else
	    if (error == "KEYCLOAK_INSTANCE_ALREADY_REGISTERED") {
	        //Notifications.warn("Identity Instance is already registered.");
	        $state.go("restcomm.dashboard");
	    } else
	    if (error == "IDENTITY_REGISTRATION_NOT_AVAILABLE") { // we are trying to access restcomm registration view while in restcomm-authentication mode
	        $state.go("restcomm.dashboard");
	    } else
	    if (error == 'RESTCOMM_ACCOUNT_NOT_INITIALIZED') {
	        $state.go('public.uninitialized');
	    } else
	    if (error == 'ACCOUNT_ALREADY_INITIALIZED') {
	        $state.go('restcomm.dashboard');
	    } else
	    if (error == 'UNKWNOWN_ERROR') {
	        console.log('internal error');
	    } else
	    if (error == 'KEYCLCOAK_NO_LINKED_ACCOUNT') {
	        $state.go('root.unlinked',{evaluateAccess:false});
	    } else
	    if (error == 'KEYCLOAK_ACCOUNT_ALREADY_LINKED') {
	        $state.go('restcomm.dashboard');
	    } else
	    if (error == 'LOCAL_LOGIN_NOT_AVAILABLE') {
	        $state.go('restcomm.dashboard');
	    } else
	    if (error == 'KEYCLOAK_NO_ACCOUNT') {
	        $state.go('root.noaccount');
	    } else
	    if (error == 'KEYCLOAK_ORGANIZATION_ACCESS_FORBIDDEN') {
	        $state.go('root.auth-error');
	    } else
	    if (error == 'GENERIC_AUTHORIZATION_ERROR') {
	        $state.go('root.auth-error');
	    }
	});

    // This error is thrown from the interceptors
	$rootScope.$on('RC_ERROR', function (event, error) {
	    if (error == 'KEYCLOAK_ACCOUNT_NOT_LINKED') {
            if ( $state.$current.name != 'unlinked' ) {
                console.log('redirecting to unlinked state');
                AuthService.clear();
                //$state.go('root.unlinked', {evaluateAccess:false});
                $state.go('root.unlinked', {evaluateAccess:false});
            }
	    } else {
	        console.log('Unknown error: ', error);
	    }
	});
});



