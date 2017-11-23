'use strict';

angular.module('rcApp').controller('EventsCtrl', function ($rootScope, rappService, $state, Notifications) {

	$rootScope.$on('$stateChangeStart', function(event, toState, toParams, fromState, fromParams, options){
	    //console.log("switching states: " + fromState.name + " -> " + toState.name);
	});

	$rootScope.$on('$stateChangeError',  function(event, toState, toParams, fromState, fromParams, error){
	    event.preventDefault();
	    console.log("Error switching state: " + fromState.name + " -> " + toState.name);
	    // see AuthService.checkAccess() for error definitions
	    if (error == "MISSING_ACCOUNT_SID")
	        $state.go("public.login");
	    else
	    if (error == 'RESTCOMM_AUTH_FAILED' || error == 'RESTCOMM_NOT_AUTHENTICATED') {
	        //Notifications.error('Unauthorized access');
	        $state.go("public.login");
	    } else
	    if (error == "KEYCLOAK_INSTANCE_NOT_REGISTERED") {
	        console.log("Identity Instance not registered.");
	        $state.go("public.identity-registration");
	    } else
	    if (error == "KEYCLOAK_INSTANCE_ALREADY_REGISTERED") {
	        console.log("Identity Instance is already registered.");
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
	    }
	});
});



