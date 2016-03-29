var keycloakAuth = {};
var logout = function(){
    console.log('*** LOGOUT');
    keycloakAuth.loggedIn = false;
    keycloakAuth.authz = null;
    window.location = keycloakAuth.logoutUrl;
};


angular.element(document).ready(['$http',function ($http) {
  // manually inject $q since it's not available
  var initInjector = angular.injector(["ng"]);
  var $q = initInjector.get("$q");
  // try to retrieve Identity server configuration
  var serverPromise = $q.defer();
  $http.get("/restcomm/2012-04-24/Identity/Server").success(function (serverConfig) {
    console.log(serverConfig);
    serverPromise.resolve(serverConfig);
  }).error( function (response) {
    serverPromise.reject();
  });
  // try to retrieve IdentityInstance
  var instancePromise = $q.defer();
  $http.get("/restcomm/2012-04-24/Identity/Instances/current").success(function (instance) {
    instancePromise.resolve(instance);
  }).error(function (response) {
    if (response.status == 404)
      instancePromise.resolve(null);
    else
      instancePromise.reject();
  });
  // when both responses are received do sth...
  $q.all([serverPromise.promise,instancePromise.promise]).then(function (responses) {
    console.log("SuCCESS");
    var identityConfig = responses[0];
    var identityInstance = responses[1];
    // is this instance supposed to be secured by an auth server ?
    angular.module('rcApp').value('IdentityConfig', function () {
      return identityServerConfig;
    });
    angular.module('rcApp').value('IdentityInstance', function () {
      return identityInstance;
    });
    angular.module('rcApp').factory('KeycloakAuth', function() {
      return keycloakAuth;
    });
    if ( !! identityConfig.authServerUrl && !! identityInstance ) {
      // if the instance is already secured by keycloak
      var keycloak = new Keycloak({ url: identityConfig.authServerUrl, realm: identityConfig.realm, identityInstance.name + "-restcomm-ui" });
			keycloakAuth.loggedIn = false;
			keycloak.init({ onLoad: 'login-required' }).success(function () {
				keycloakAuth.loggedIn = true;
				keycloakAuth.authz = keycloak;
				keycloakAuth.logoutUrl = identityConfig.authServerUrl + "/realms/" + identityConfig.realm + "/tokens/logout?redirect_uri=" + window.location.origin + "/index.html";
        angular.bootstrap(document, ["rcApp"]);
			}).error(function (a, b) {
					window.location.reload();
			});
    } else
    if ( !! identityConfig.authServerUrl && ! identityInstance ){
      // keycloak is already configured but no identity instance yet
      angular.bootstrap(document, ["rcApp"]);
    } else {
      // no identity configuration. We should run in compatibility authorization mode
      angular.bootstrap(document, ["rcApp"]);
    }

  }, function () {
    console.log("Internal server error");
  });
}]);

function Identity(identityConfig, identityInstance, KeycloakAuth) {
  this.identityConfig = identityConfig;
  this.identityInstance = identityInstance;
  this.keycloakAuth = keycloakAuth;

  this.restcommAccount = null;
  this.userProfile = null;

  // is an identity server configured in Restcomm ?
  function identityServerConfigured () {
    return !!this.identityConfig && (!!this.identityConfig.authServerUrl);
  }
  this.identityServerConfigured = identityServerConfigured;
  // is the instance secured by keyloak ?
  function securedByKeycloak () {
    return identityServerConfigured && (!!this.identityInstance) && (!!this.identityInstance.name);
  }
  this.securedByKeycloak = securedByKeycloak;
  // is the user logged in keycloak ?
  function loggedInKeycloak() {
    throw "NOT IMPLEMENTED";
  }
  this.loggedInKeycloak = loggedInKeycloak;
  //

}

angular.module('rcApp').factory('identity', function (IdentityConfig, IdentityInstance, KeycloakAuth) {
  return new Identity(IdentityConfig, IdentityInstance, KeycloakAuth);
});

angular.module('rcApp').factory('authInterceptor', function($q, Auth) {
    return {
        request: function (config) {
            var deferred = $q.defer();
            if (Auth.authz.token) {
                Auth.authz.updateToken(5).success(function() {
                    config.headers = config.headers || {};
                    config.headers.Authorization = 'Bearer ' + Auth.authz.token;

                    deferred.resolve(config);
                }).error(function() {
                        deferred.reject('Failed to refresh token');
                    });
            }
            return deferred.promise;
        }
    };
});




angular.module('rcApp').config(function($httpProvider, authMode) {
    $httpProvider.responseInterceptors.push('errorInterceptor');
    if (authMode != 'init') {
    	// if the instance is not bound to keycloak do not add token
    	$httpProvider.interceptors.push('authInterceptor');
	}

});

angular.module('rcApp').factory('errorInterceptor', function($q) {
    return function(promise) {
        return promise.then(function(response) {
            return response;
        }, function(response) {
            if (response.status == 401) {
                console.log('session timeout?');
                logout();
            } else if (response.status == 403) {
                alert("Forbidden");
            } else if (response.status == 404) {
                alert("Not found");
            } else if (response.status) {
                if (response.data && response.data.errorMessage) {
                    alert(response.data.errorMessage);
                } else {
                    alert("An unexpected server error has occurred");
                }
            }
            return $q.reject(response);
        });
    };
});
