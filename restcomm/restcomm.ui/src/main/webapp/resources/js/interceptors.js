
// Authorization interceptor. It's effective when restcomm is secured by Keycloak.
angular.module('rcApp').factory('authInterceptor', function($q) {
    return {
        request: function (config) {
            if (!keycloakAuth.authz)
                return config;
            var deferred = $q.defer();
            if (keycloakAuth.authz.token) {
                keycloakAuth.authz.updateToken(5).success(function() {
                    config.headers = config.headers || {};
                    config.headers.Authorization = 'Bearer ' + keycloakAuth.authz.token;
                    deferred.resolve(config);
                }).error(function() {
                    deferred.reject('Failed to refresh token');
                });
            }
            return deferred.promise;
        }
    };
}).config(['$httpProvider','IdentityConfig', function($httpProvider,IdentityConfig) {
    if ( IdentityConfig.securedByKeycloak() ) {
        $httpProvider.interceptors.push('authInterceptor');
    }
}]);


// There is a circular dependency issue when directly injecting AuthService in the function. A workaround using $injector has
// been used - http://stackoverflow.com/questions/20647483/angularjs-injecting-service-into-a-http-interceptor-circular-dependency
angular.module('rcApp').factory('authHttpResponseInterceptor',['$q','$location','$injector','IdentityConfig','Notifications','SessionService',function($q,$location,$injector,IdentityConfig, Notifications, SessionService){
    return {
      request: function(config) {
          var restcomm_prefix = "/restcomm/"
    	  var rvd_prefix = "/restcomm-rvd/";
    	  if ( ! config.headers.Authorization ) { // if no header is already present
              if ( config.url.substring(0, rvd_prefix.length) === rvd_prefix || config.url.substring(0, restcomm_prefix.length) === restcomm_prefix  ) {
                  var AuthService = $injector.get('AuthService');
                  var account = AuthService.getAccount();
                  if (!!account) {
                      var creds = SessionService.getStoredCredentials();
                      //var auth_header = account.email_address + ":" + account.auth_token;
                      var auth_header = creds.sid + ":" + creds.token;
                      auth_header = "Basic " + btoa(auth_header);
                      config.headers.Authorization = auth_header;
                  }
              }
          }
		  return config;
	    },
      response: function(response){
            var AuthService = $injector.get('AuthService');
            if (response.status === 401) {
              AuthService.onAuthError();
            } else
            if (response.status === 403) {
              AuthService.onError403();
            }
            return response || $q.when(response);
      },
      responseError: function(rejection) {
        // TODO handle all errors here
            var AuthService = $injector.get('AuthService');
            if (rejection.status === 401) {
                AuthService.onAuthError(rejection.config.noLoginRedirect);
            } else
            if (rejection.status === 403) {
              AuthService.onError403();
            }
            return $q.reject(rejection);
      }
    }
  }])
  .config(['$httpProvider','IdentityConfig', function($httpProvider, IdentityConfig) {
    if ( IdentityConfig.securedByRestcomm() ) {
        // http Intercpetor to check auth failures for xhr requests
        $httpProvider.interceptors.push('authHttpResponseInterceptor');
    }
  }]);


/*
var interceptor = ['$rootScope', '$q', '$location', function (scope, $q, $location) {

  function success(response) {
    return response;
  }

  function error(response) {
    var status = response.status;

    if (status == 401) {
      console.log("Redirecting to login due to 401 ERROR CODE (@" + $location.url() + ")");
      $location.path("/login");
      return $q.reject(response);
    }
    // otherwise
    return $q.reject(response);

  }

  return function (promise) {
    return promise.then(success, error);
  }

}];
*/