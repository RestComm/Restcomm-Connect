'use strict';

angular.module('rcApp').controller('EventsCtrl', function ($rootScope, rappService, $state) {
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
	    console.log("switching states: " + fromState.name + " -> " + toState.name);
	});

	$rootScope.$on('$stateChangeError',  function(event, toState, toParams, fromState, fromParams, error){
	    console.log("stateChangeError");
	    // see AuthService.checkAccess() for error definitions
	    if (error == "MISSING_ACCOUNT_SID")
	        $state.go("public.login");
	    else
	    if (error == "MISSING_LOGGED_ACCOUNT") {
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
	    }
	});
});



