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

rcServices.factory('AuthService',function(RCommAccounts,$http, $location, SessionService, md5, Notifications, $q, IdentityConfig, KeycloakAuth){
    var account = null;
    var uninitialized = null;

    function getAccountSid() {
        if (!!account)
            return account.sid;
        return null;
    };

    function getAccount() {
        return account;
    };

    // returns Friendly Name for the logged account. Override this in SSO to also cover users with no account mapped
    function getFriendlyName() {
        if (!!account)
            return account.friendly_name;
        return "";
    }

    // Checks access for typical restcomm operations. It resolves to a valid, authorized restcomm Account.
    // It Returns a promise.
    //
    //  - rejected:
    //      MISSING_ACCOUNT_SID,
    //      KEYCLCOAK_NO_LINKED_ACCOUNT
    //      KEYCLOAK_INSTANCE_NOT_REGISTERED
    //      MISSING_LOGGED_ACCOUNT - no Account is stored in the JS application and cookie authentication is not supported (no shiro). Applies when using Restcomm authentication, not keycloak.
    //
    //  - resolved: returns a valid Restcomm account for the logged user
    function checkAccess() {
        //var role; // undefined - it should be provided as a function parameter
        var deferred = $q.defer();

        if (IdentityConfig.securedByKeycloak()) {
            if (!KeycloakAuth.loggedIn) {
                deferred.reject("KEYCLOAK_NOT_LOGGED_IN"); // this normally won't be thrown as keycloak adapter is supposed to detect it and redirect automatically
                return deferred.promise;
            }
            var username = getUsername();  // since we're logged in, there MUST be a username available
            var promisedAccount = $q.defer();
            if (!account) {
                $http({method:'GET', url:'restcomm/2012-04-24/Accounts.json/' + encodeURIComponent(username), headers: {Authorization: 'Bearer ' + KeycloakAuth.authz.token}})
                .success(function (data,status) {
                    promisedAccount.resolve(data);
                })
                .error(function (data,status) {
                    deferred.reject('KEYCLCOAK_NO_LINKED_ACCOUNT'); // TODO is this the proper error code ? Maybe we should judge by the HTTP status code.
                    promisedAccount.reject();
                });
            } else {
                promisedAccount.resolve(account);
            }

            // when the account becomes available, make sure the username/email_address match
            promisedAccount.promise.then(function (fetchedAccount) {
                if (username.toLowerCase() == fetchedAccount.email_address.toLowerCase()) {
                    setActiveAccount(fetchedAccount);
                    deferred.resolve();
                }
            }
                // do nothing if promise is rejected
            );
        } else
        if (IdentityConfig.securedByRestcomm()) {
            if (!!getAccountSid()) // get account sid from js application (not from session storage) - if F5 is pressed this is lost
                deferred.resolve();
            else
                deferred.reject("MISSING_LOGGED_ACCOUNT");
        } else {
            // looks like the instance is not yet registered to keycloak although Restcomm is configured to use it
            deferred.reject("KEYCLOAK_INSTANCE_NOT_REGISTERED");
        }
        return deferred.promise;
    }

    // updates all necessary state
    function setActiveAccount(newAccount) {
        account = newAccount;
        SessionService.set('sid',newAccount.sid);
        if (account && account.status == 'uninitialized')
            uninitialized = true;
        else
            uninitialized = false;
    }

    function clearActiveAccount() {
        SessionService.unset('sid');
        account = null;
        uninitialized = null;
    }

    function isUninitialized() {
        return uninitialized;
    }

    // Returns a promise.
    //  - resolved: OK, UNINITIALIZED
    //  - rejected: SUSPENDED, UNKNOWN_ERROR, AUTH_ERROR
    function login(credentials) {
      var deferred = $q.defer();
      // TEMPORARY... FIXME!
      //var apiPath = $location.protocol() + "://" + credentials.sid.replace("@", "%40") + ":" + md5.createHash(credentials.token) + "@" + credentials.host + "/restcomm/2012-04-24/Accounts" + ".json/" + credentials.sid ;
      var auth_header = credentials.sid + ":" + md5.createHash(credentials.token);
      auth_header = "Basic " + btoa(auth_header);
      var login = $http({
        method:"GET",
        url:"/restcomm/2012-04-24/Accounts" + ".json/" + credentials.sid,
        headers:{authorization: auth_header}
      }).success(function(data, status, headers, config) {
          if (status == 200) {
            //if(data.date_created && data.date_created == data.date_updated) {
            if(data.status) {
              if(data.status == 'uninitialized') {
                setActiveAccount(data);
                deferred.resolve("UNINITIALIZED");
                return;
              }
              else if(data.status == 'suspended') {
                clearActiveAccount();
                deferred.reject('SUSPENDED');
                return;
              }
              else if (data.status == 'active') {
                setActiveAccount(data);
                deferred.resolve('OK');
                return;
              }
            }
          }
          // some sort of unknown error occured
          clearActiveAccount();
          deferred.reject('UNKNOWN_ERROR');
          return;
        }).
        error(function(data) {
          Notifications.error("Login failed! Please confirm your credentials.");
          clearActiveAccount();
          deferred.reject('AUTH_ERROR');
          return;
        });
      return deferred.promise;
    }

    function logout() {
          SessionService.unset('sid');
          account = null;
          // uninitialized = ???
          if (IdentityConfig.securedByKeycloak())
            keycloakLogout(); // keycloak logout - defined in restcomm.js
          else {
            $http.get('/restcomm/2012-04-24/Logout'); // TODO should we wait for a response before moving to login view ?
            $state.go("public.login");
          }
    }

    // applies to Restcomm authorization (not keycloak)
    function onAuthError() {
        if (IdentityConfig.securedByRestcomm()) {
            SessionService.unset('sid');
            account = null;
            //$state.go("public.login");
            $location.path('/login').search('returnTo', $location.path());
        }
    }

    // Returns the username (email address) for the logged  user. It's only available when keycloak is used for authorization.
    function getUsername() {
        if (IdentityConfig.securedByKeycloak() && KeycloakAuth.loggedIn)
            return KeycloakAuth.authz.tokenParsed.preferred_username;
        return null;
    }

    // public interface
    return {
        login: login,
        logout: logout,
        getAccountSid: getAccountSid,
        getAccount: getAccount,
        getFrientlyName: getFriendlyName,
        checkAccess: checkAccess,
        isUninitialized: isUninitialized,
        onAuthError: onAuthError
    }
});

// IdentityConfig service constructor. See restcomm.js. This service is created early before the rcMod angular module is initialized and is accessible as a 'constant' service.
function IdentityConfig(server, instance,$q) {
    var This = this;
    this.server = server;
    this.instance = instance;

    // is an identity server configured in Restcomm ?
    function identityServerConfigured () {
        return !!This.server && (!!This.server.authServerUrl);
    }
    // True is Restcomm is configured to use an authorization server and an identity instance is already in place
    function securedByKeycloak () {
        return identityServerConfigured() && (!!This.instance) && (!!This.instance.name);
    }
    // True if Restcomm is used for authorization (legacy mode). No keycloak needs to be present.
    function securedByRestcomm() {
        return !identityServerConfigured();
    }
    // returns identity instance if applicable (as a promise) or null if not (as null, not promise)
    // Returns:
    //  resolved:
    //      - identity instance object
    //  rejected:
    //      - KEYCLOAK_INSTANCE_NOT_REGISTERED
    //  not-applicable - Restcomm does not use keycloak for external authorization
    //      - null
    function getIdentity() {
        if (!identityServerConfigured())
            return null;
        var deferred = $q.defer();
        if (!!This.instance && !!This.instance.name)
            deferred.resolve(This.instance);
        else
            deferred.reject("KEYCLOAK_INSTANCE_NOT_REGISTERED");
        return deferred.promise;
    }

    // Public interface

    this.identityServerConfigured = identityServerConfigured;
    this.securedByKeycloak = securedByKeycloak;
    this.securedByRestcomm = securedByRestcomm;
    this.getIdentity = getIdentity;
}

// KeycloakAuth service is manually initialized in restcomm.js
//angular.module('rcApp').factory('KeycloakAuth', function() {
//  return keycloakAuth;
//});

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

rcServices.factory('RCVersion', function($resource) {
   return $resource('/restcomm/2012-04-24/Accounts/:accountSid/Version.:format', {
        accountSid: '@accountSid',
        format: 'json'
   },
        {
          get: {
            method: 'GET',
            url: '/restcomm/2012-04-24/Accounts/:accountSid/Version.:format'
          }
        }
  );
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


