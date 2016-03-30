'use strict';

/* Services */
var rcServices = angular.module('rcApp.services', []);

rcServices.factory('SessionService', function() {
  return {
    get: function(key) {
      return sessionStorage.getItem(key);
    },
    set: function(key, val) {
      return sessionStorage.setItem(key, val);
    },
    unset: function(key) {
      sessionStorage.removeItem(key);
    },
    rename: function(oldKey, newKey) {
      sessionStorage.setItem(newKey, sessionStorage.getItem(oldKey));
      this.unset(oldKey);
    }
  }
});

// IdentityConfig "service"
function IdentityConfig(server, instance) {
    this.server = server;
    this.instance = instance;

    // is an identity server configured in Restcomm ?
    function identityServerConfigured () {
        return !!this.server && (!!this.server.authServerUrl);
    }
    this.identityServerConfigured = identityServerConfigured;
    // is the instance secured by keyloak ?
    function securedByKeycloak () {
        return identityServerConfigured && (!!this.instance) && (!!this.instance.name);
    }
    this.securedByKeycloak = securedByKeycloak;
}

// Identity service
function Identity(IdentityConfig, KeycloakAuth) {
  this.config = IdentityConfig;
  this.keycloakAuth = KeycloakAuth; //
  this.account = null; // restcomm account
  this.user = null; // keycloak user profile
}
angular.module('rcApp').factory('Identity', function (IdentityConfig, KeycloakAuth) {
  return new Identity(IdentityConfig, KeycloakAuth);
});

rcServices.service('Authorization', function(KeycloakAuth,Identity,md5,Notifications,$q) {
	var serviceInstance = {};

		/*
	serviceInstance.isLoggedIn = function() {
		return KeycloakAuth.loggedIn;
    }
	serviceInstance.getAuthStatus = function () {
		return KeycloakAuth.authStatus;
	}
	serviceInstance.getLoggedSid = function() {
		return Auth.restcommAccount.sid;
	}
	serviceInstance.getLoggedAccount = function () {
		return Auth.restcommAccount;
	}
	serviceInstance.getUsername = function() {
		return Auth.authz.profile.username;
	}
	serviceInstance.getProfile = function() {
		return Auth.authz.profile;
	}
	serviceInstance.logout = function() {
		Auth.authz.logout();
	}
	*/
	
	serviceInstance.secureAny = function(roles) {
		var deferred = $q.defer();
		for (var i=0; i<roles.length; i++) {
			if ( KeycloakAuth.authz.hasResourceRole(roles[i], KeycloakAuth.authz.clientId ) ) {
				deferred.resolve("AUTH_STATUS_INSTANCE");
				return deferred.promise;
			}
		}
		deferred.reject("AUTH_STATUS_REALM");
		Notifications.error("You are not authorized to access this resource");
		return deferred.promise;
	}
	serviceInstance.secureAll = function(roles) {
		var deferred = $q.defer();
		for (var i=0; i<roles.length; i++) {
			if ( ! KeycloakAuth.authz.hasResourceRole(roles[i], KeycloakAuth.authz.clientId) ) {
				deferred.reject("AUTH_STATUS_REALM");
				Notifications.error("You are not authorized to access this resource");
				return deferred.promise;
			}
		}
		deferred.resolve("AUTH_STATUS_INSTANCE");
		return deferred.promise;
	}
	serviceInstance.secure = function(role) {
			return serviceInstance.secureAny([role]);
	}
	serviceInstance.hasRole = function(role) {
		return KeycloakAuth.authz.hasResourceRole(role, KeycloakAuth.authz.clientId);
	}
	serviceInstance.hasAccount = function() {
		var deferred = $q.defer();
		if (!!identity.account)
			deferred.resolve(true);
		else
			deferred.reject("AUTH_STATUS_NOACCOUNT");
		return deferred.promise;
	}

    
    return serviceInstance;
		
});

rcServices.factory('Notifications', function($rootScope, $timeout, $log) {
  // time (in ms) the notifications are shown
  var delay = 5000;

  var notifications = {};

  $rootScope.notifications = {};
  $rootScope.notifications.data = [];

  $rootScope.notifications.remove = function(index){
    $rootScope.notifications.data.splice(index,1);
  };

  var scheduleMessagePop = function() {
    $timeout(function() {
      $rootScope.notifications.data.splice(0,1);
    }, delay);
  };

  if (!$rootScope.notifications) {
    $rootScope.notifications.data = [];
  }

  notifications.message = function(type, header, message) {
    $rootScope.notifications.data.push({
      type : type,
      header: header,
      message : message
    });

    scheduleMessagePop();
  };

  notifications.info = function(message) {
    notifications.message('info', 'Info!', message);
    $log.info(message);
  };

  notifications.success = function(message) {
    notifications.message('success', 'Success!', message);
    $log.info(message);
  };

  notifications.error = function(message) {
    notifications.message('danger', 'Error!', message);
    $log.error(message);
  };

  notifications.warn = function(message) {
    notifications.message('warning', 'Warning!', message);
    $log.warn(message);
  };

  return notifications;
});

/**
 * Usage: Add 'ui.bootstrap.modal.dialog' to app dependencies, and then '$dialog' to module dependencies.
 * Use as:
 *   $dialog.messageBox('Title', 'Message', [{result:'cancel', label: 'Cancel'}, {result:'yes', label: 'Yes', cssClass: 'btn-primary'}])
 *     .open()
 *     .then(function(result) {
 *
 *     });
 *
 * Or just:
 *
 *  $dialog.prompt('Title', 'Message').then(function(result) { } );
 */

'use strict';

var  uiModalDialog = angular.module('ui.bootstrap.modal.dialog', []);
uiModalDialog.factory('$dialog', ['$rootScope', '$modal', function ($rootScope, $modal) {

  var prompt = function(title, message, buttons) {

    if(typeof buttons === 'undefined') {
      buttons = [
        {result:'cancel', label: 'Cancel'},
        {result:'yes', label: 'Yes', cssClass: 'btn-primary'}
      ];
    }

    var ModalCtrl = function($scope, $modalInstance) {
      $scope.title = title;
      $scope.message = message;
      $scope.buttons = buttons;

      $scope.close = function(result) {
        $modalInstance.close(result);
      };
    };

    return $modal.open({
      templateUrl: 'template/dialog/message.html',
      controller: ModalCtrl
    }).result;
  };

  return {
    prompt:     prompt,
    messageBox: function(title, message, buttons) {
      return {
        open: function() {
          return prompt(title, message, buttons);
        }
      };
    }
  };
}]);

/**
 * Easily make use of Angular UI Modal as Dialogs.
 */
uiModalDialog.run(["$templateCache", function (e) {
  e.put("template/dialog/message.html", '<div class="modal-header">   <h3 class="no-margins">{{ title }}</h3></div><div class="modal-body">  <p>{{ message }}</p></div><div class="modal-footer">    <button ng-repeat="btn in buttons" ng-click="close(btn.result)" class=btn ng-class="btn.cssClass">{{ btn.label }}</button></div>')
}]);


rcServices.factory('RCommAccounts', function($resource) {
  return $resource('/restcomm/2012-04-24/Accounts/:accountSid.:format', {
      accountSid: '@accountSid',
      format:'json'
    },
    {
	  all: {
		method: 'GET',
		url: '/restcomm/2012-04-24/Accounts.:format',
		isArray: true
	  },
      view: {
        method: 'GET',
        url: '/restcomm/2012-04-24/Accounts.:format/:accountSid'
      },
      register: {
        method:'POST',
        url: '/restcomm/2012-04-24/Accounts.:format',
        headers : {
          'Content-Type': 'application/x-www-form-urlencoded'
        }
      },
      update: {
        method:'PUT',
        headers : {
          'Content-Type': 'application/x-www-form-urlencoded'
        }
      },
      remove: {
		  method:'DELETE',
		  url: '/restcomm/2012-04-24/Accounts.:format/:accountSid.:format'
	  }
    });
});

rcServices.factory('RCommAccountOperations', function($resource) {
	return $resource('/restcomm/2012-04-24/Accounts/:accountSid/operations/', {}, {
		linkUser: { 
			method: 'POST', 
			url: '/restcomm/2012-04-24/Accounts/:accountSid/operations/link',
			headers : {
				'Content-Type': 'application/x-www-form-urlencoded'
			}
		},
		unlinkUser: {
			method: 'DELETE',
			url: '/restcomm/2012-04-24/Accounts/:accountSid/operations/link'
		},
		revokeKey: {
			method: 'DELETE',
			url: '/restcomm/2012-04-24/Accounts/:accountSid/operations/key'
		},
		assignKey: {
			method: 'GET',
			url: '/restcomm/2012-04-24/Accounts/:accountSid/operations/key/assign'
		}
	});
});

rcServices.factory('RCommNumbers', function($resource) {
  return $resource('/restcomm/2012-04-24/Accounts/:accountSid/IncomingPhoneNumbers.:format', {
      accountSid: '@accountSid',
      format:'json'
    },
    {
      get: {
        method: 'GET',
        url: '/restcomm/2012-04-24/Accounts/:accountSid/IncomingPhoneNumbers/:phoneSid.:format'
      },
      delete: {
        method:'DELETE',
        url: '/restcomm/2012-04-24/Accounts/:accountSid/IncomingPhoneNumbers/:phoneSid.:format'
      },
      register: {
        method:'POST',
        headers : {
          'Content-Type': 'application/x-www-form-urlencoded'
        }
      },
      update: {
        method:'POST',
        url: '/restcomm/2012-04-24/Accounts/:accountSid/IncomingPhoneNumbers/:phoneSid.:format',
        headers : {
          'Content-Type': 'application/x-www-form-urlencoded'
        }
      }
    });
});

rcServices.factory('RCommClients', function($resource) {
  return $resource('/restcomm/2012-04-24/Accounts/:accountSid/Clients.:format', {
      accountSid: '@accountSid',
      format:'json'
    },
    {
      get: {
        method: 'GET',
        url: '/restcomm/2012-04-24/Accounts/:accountSid/Clients/:clientSid.:format'
      },
      delete: {
        method:'DELETE',
        url: '/restcomm/2012-04-24/Accounts/:accountSid/Clients/:clientSid.:format'
      },
      register: {
        method:'POST',
        headers : {
          'Content-Type': 'application/x-www-form-urlencoded'
        }
      },
      update: {
        method:'POST',
        url: '/restcomm/2012-04-24/Accounts/:accountSid/Clients/:clientSid.:format',
        headers : {
          'Content-Type': 'application/x-www-form-urlencoded'
        }
      }
    });
});

rcServices.factory('RCommOutgoingCallerIDs', function($resource) {
  return $resource('/restcomm/2012-04-24/Accounts/:accountSid/OutgoingCallerIds.:format', {
      accountSid: '@accountSid',
      format:'json'
    },
    {
      delete: {
        method:'DELETE',
        url: '/restcomm/2012-04-24/Accounts/:accountSid/OutgoingCallerIds/:phoneSid.:format'
      },
      register: {
        method:'POST',
        headers : {
          'Content-Type': 'application/x-www-form-urlencoded'
        }
      }
    });
});

rcServices.factory('RCommLogsCalls', function($resource) {
  return $resource('/restcomm/2012-04-24/Accounts/:accountSid/Calls.:format', {
      accountSid: '@accountSid',
      format:'json'
    },
    {
      view: {
        method: 'GET',
        url: '/restcomm/2012-04-24/Accounts/:accountSid/Calls/:callSid.:format'
      },
      call: {
        method:'POST',
        headers : {
          'Content-Type': 'application/x-www-form-urlencoded'
        }
      },
      search: {
        method:'GET',
        url: '/restcomm/2012-04-24/Accounts/:accountSid/Calls/:callSid.:format',
      }
    }
  );
});

rcServices.factory('RCommLogsMessages', function($resource) {
  return $resource('/restcomm/2012-04-24/Accounts/:accountSid/SMS/Messages.:format', {
      accountSid: '@accountSid',
      format:'json'
    },
    {
      view: {
        method: 'GET',
        url: '/restcomm/2012-04-24/Accounts/:accountSid/SMS/Messages/:smsMessageSid.:format'
      },
      send: {
        method:'POST',
        headers : {
          'Content-Type': 'application/x-www-form-urlencoded'
        }
      }
    }
  );
});

rcServices.factory('RCommLogsRecordings', function($resource) {
  return $resource('/restcomm/2012-04-24/Accounts/:accountSid/Recordings.:format',
    {
      accountSid:'@accountSid',
      format:'json'
    },
    {
      view: {
        method: 'GET',
        url: '/restcomm/2012-04-24/Accounts/:accountSid/Recordings/:recordingSid.:format'
      },
      delete: {
        method:'DELETE',
        url: '/restcomm/2012-04-24/Accounts/:accountSid/Recordings/:recordingSid.:format'
      }
    }
  );
});

rcServices.factory('RCommLogsNotifications', function($resource) {
  return $resource('/restcomm/2012-04-24/Accounts/:accountSid/Notifications.:format',
    {
      accountSid: '@accountSid',
      format:'json'
    },
    {
      view: {
        method: 'GET',
        url: '/restcomm/2012-04-24/Accounts/:accountSid/Notifications/:notificationSid.:format'
      },
      delete: {
        method:'DELETE',
        url: '/restcomm/2012-04-24/Accounts/:accountSid/Notifications/:notificationSid.:format'
      }
    }
  );
});

rcServices.factory('RCommLogsTranscriptions', function($resource) {
  return $resource('/restcomm/2012-04-24/Accounts/:accountSid/Transcriptions.:format',
    {
      accountSid: '@accountSid',
      format:'json'
    },
    {
      view: {
        method: 'GET',
        url: '/restcomm/2012-04-24/Accounts/:accountSid/Transcriptions/:transcriptionSid.:format'
      },
      delete: {
        method:'DELETE',
        url: '/restcomm/2012-04-24/Accounts/:accountSid/Transcriptions/:transcriptionSid.:format'
      }
    }
  );
});

rcServices.factory('RCommApps', function($resource) {
	  return $resource('/restcomm-rvd/services/projects');
});

rcServices.factory('RCommAvailableNumbers', function($resource) {
  return $resource('/restcomm/2012-04-24/Accounts/:accountSid/AvailablePhoneNumbers/:countryCode/Local.:format',
    {
      accountSid: '@accountSid',
      countryCode: '@countryCode',
      format:'json'
    },
    {
      getCountries: {
        method: 'GET',
        isArray: true,
        url: '/resources/json/countries.:format'
      },
      getAreaCodes: {
        method: 'GET',
        isArray: true,
        url: '/resources/json/area-codes-:countryCode.:format'
      },
      getAvailableCountries: {
        method: 'GET',
        isArray: true,
        //url: '/resources/json/available.:format'
        url: '/restcomm/2012-04-24/Accounts/:accountSid/IncomingPhoneNumbers/AvailableCountries.:format'
      }
    }
  );
});

rcServices.factory('RCommStatistics', function($resource) {
  return $resource('/restcomm/2012-04-24/Accounts/:accountSid/Usage/Records/:statName.:format',
    {
      accountSid: '@accountSid',
      statName: '@statName',
      format:'json'
    },
    {
    }
  );
});

rcServices.factory('IdentityInstances', function($resource) {
    return $resource('/restcomm/2012-04-24/Identity/Instances', {}, {
        register: {
            method:'POST',
            url: '/restcomm/2012-04-24/Identity/Instances',
            headers : {
              'Content-Type': 'application/x-www-form-urlencoded'
            }
        }
    });
});

rcServices.factory('RCommJMX', function($resource) {
  return $resource('/jolokia/:op/:path',
    {
      op: 'read'
    }
  );
});

/* 
 * RAS Services
 */
// RAS related configuration options. At some point this this should be returned from the server
// rasApiKey: the Public key
// rasToken: the Token
rcServices.value("rappManagerConfig", {rasHost: "apps.restcomm.com", rasApiKey:"dae21e48184703e41ec0e42929800ed3", rasToken:"c7ba2a69395eb7b05a291f58bb75402f"});


