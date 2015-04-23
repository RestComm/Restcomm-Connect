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

rcServices.service('AuthService', function(Auth) {
	console.log("creating AuthService");
	
	var serviceInstance = {};
		
	serviceInstance.isLoggedIn = function() {
		return Auth.loggedIn;
      //return SessionService.get('authenticated');
    }
    /*
    getLoggedUser: function() {
      return SessionService.get('logged_user');
    },*/
    
    return serviceInstance;
		
});

/*
rcServices.service('AuthService', function($http, $location, SessionService, md5) {
  var cacheSession = function(account, first) {
    var prefix = first ? '_' : '';
    SessionService.set('sid', account.sid);
    SessionService.set(prefix + 'authenticated', true);
    SessionService.set(prefix + 'logged_user', account.friendly_name);
  };

  var passwordUpdated = function() {
    SessionService.rename('_authenticated', 'authenticated');
    SessionService.rename('_logged_user', 'logged_user');
  };

  var uncacheSession = function() {
    SessionService.unset('sid');
    SessionService.unset('authenticated');
    SessionService.unset('logged_user');
    SessionService.unset('_sid');
    SessionService.unset('_authenticated');
    SessionService.unset('_logged_user');
  };


  return {
    login: function(credentials) {
      // TEMPORARY... FIXME!
      var apiPath = "http://" + credentials.sid.replace("@", "%40") + ":" + md5.createHash(credentials.token) + "@" + credentials.host + "/restcomm/2012-04-24/Accounts" + ".json/" + credentials.sid ;


      var login = $http.get(apiPath).
        success(function(data, status, headers, config) {
          if (status == 200) {
            //if(data.date_created && data.date_created == data.date_updated) {
            if(data.status) {
              if(data.status == 'uninitialized') {
                cacheSession(data, true);
              }
              else if(data.status == 'suspended') {
                // no-op
              }
              else if (data.status == 'active') {
                cacheSession(data, false);
              }
            }
          }
          else {
            uncacheSession();
          }
        }).
        error(function(data) {
          alert("Login failed! Please confirm your credentials.");
        }
      );
      return login;
    },
    logout: function() {
      // TODO: Logout from restcomm ?
      uncacheSession();
      // FIXME: return logout;
    },
    updatePassword: function(credentials, newPassword) {
      // TEMPORARY... FIXME!
      var apiPath = "http://" + credentials.sid.replace("@", "%40") + ":" + md5.createHash(credentials.token) + "@" + credentials.host + "/restcomm/2012-04-24/Accounts/" + this.getAccountSid() + ".json";
      http://127.0.0.1:8080/restcomm/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf.json
        var params = {};
      params["Auth_Token"] = md5.createHash(newPassword);

      var update = $http({method: 'PUT', url: apiPath, data: $.param(params), headers: {'Content-Type': 'application/x-www-form-urlencoded'}}).
        success(function(data) {
          passwordUpdated();
        }).
        error(function(data) {
          alert("Failed to update password. Please try again.");
        }
      );
      return update;
    },
    isLoggedIn: function() {
      return SessionService.get('authenticated');
    },
    getLoggedUser: function() {
      return SessionService.get('logged_user');
    },
    getAccountSid: function() {
      var sid = SessionService.get('sid')
      return sid ? sid : SessionService.get('_sid');
    },
    getWaitingReset: function() {
      return SessionService.get('_authenticated');
    }
  }
});
*/

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
  return $resource('/restcomm/keycloak/Accounts/:accountSid.:format', {
      accountSid: '@accountSid',
      format:'json'
    },
    {
      view: {
        method: 'GET',
        url: '/restcomm/keycloak/Accounts.:format/:accountSid'
      },
      register: {
        method:'POST',
        url: '/restcomm/keycloak/Accounts.:format',
        headers : {
          'Content-Type': 'application/x-www-form-urlencoded'
        }
      },
      update: {
        method:'PUT',
        headers : {
          'Content-Type': 'application/x-www-form-urlencoded'
        }
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
	  return $resource('/restcomm-rvd/services/apps');
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
        url: '/restcomm-management/resources/json/countries.:format'
      },
      getAreaCodes: {
        method: 'GET',
        isArray: true,
        url: '/restcomm-management/resources/json/area-codes.:format'
      },
      getAvailableCountries: {
        method: 'GET',
        isArray: true,
        //url: '/restcomm-management/resources/json/available.:format'
        url: '/restcomm/2012-04-24/Accounts/:accountSid/IncomingPhoneNumbers/AvailableCountries.:format'
      }
    }
  );
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


