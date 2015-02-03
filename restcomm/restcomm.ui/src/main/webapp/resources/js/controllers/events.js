'use strict';

angular.module('rcApp').controller('EventsCtrl', function ($rootScope, rappService) {
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
});
