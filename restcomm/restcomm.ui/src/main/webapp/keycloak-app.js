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
    // create a constant with keycloak server and instance identity configuration
    var identityConfig = new IdentityConfig(responses[0],responses[1]);
    angular.module('rcApp').constant('IdentityConfig', identityConfig);
    angular.module('rcApp').factory('KeycloakAuth', function() {
      return keycloakAuth;
    });
    if ( identityConfig.securedByKeycloak() ) {
      // if the instance is already secured by keycloak
      var keycloak = new Keycloak({ url: identityConfig.server.authServerUrl, realm: identityConfig.server.realm, clientId: identityConfig.instance.name + "-restcomm-ui" });
			keycloakAuth.loggedIn = false;
			keycloak.init({ onLoad: 'login-required' }).success(function () {
				keycloakAuth.loggedIn = true;
				keycloakAuth.authz = keycloak;
				keycloakAuth.logoutUrl = identityConfig.server.authServerUrl + "/realms/" + identityConfig.server.realm + "/tokens/logout?redirect_uri=" + window.location.origin + "/index.html";
        angular.bootstrap(document, ["rcApp"]);
			}).error(function (a, b) {
					window.location.reload();
			});
    } else
    if (identityConfig.identityServerConfigured() && !identityConfig.securedByKeycloak()){
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

angular.module('rcApp').config(function($httpProvider) {
    // TODO -fix this!
    /*
    $httpProvider.responseInterceptors.push('errorInterceptor');
    if (authMode != 'init') {
    	// if the instance is not bound to keycloak do not add token
    	$httpProvider.interceptors.push('authInterceptor');
	}
	*/

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
