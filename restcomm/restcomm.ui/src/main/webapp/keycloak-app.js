// var module = angular.module('product', []);

var auth = {};
var logout = function(){
    console.log('*** LOGOUT');
    auth.loggedIn = false;
    auth.authz = null;
    window.location = auth.logoutUrl;
};


angular.element(document).ready(function ($http) {
	$http.get("/restcomm/identity/config/mode")
	.success(function (data, status) {
		angular.module("rcApp").constant("authMode",data.mode);
		if (data.mode == "cloud") {
    
			var keycloakAuth = new Keycloak('/restcomm/identity/config/restcomm-ui.json');
			auth.loggedIn = false;

			keycloakAuth.init({ onLoad: 'login-required' }).success(function () {
				auth.loggedIn = true;
				auth.authz = keycloakAuth;
				auth.logoutUrl = keycloakAuth.authServerUrl + "/realms/restcomm/tokens/logout?redirect_uri=" + window.location.origin + "/index.html";
				angular.module('rcApp').factory('Auth', function() {
					return auth;
				});
				
				keycloakAuth.loadUserProfile().success(function () {
					var initInjector = angular.injector(["ng"]);
					var $myhttp = initInjector.get("$http");
					
					$myhttp({
						method: 'GET',
						url: '/restcomm/2012-04-24/Accounts.json/' + keycloakAuth.profile.username ,
						headers: {
							Authorization: 'Bearer ' + keycloakAuth.token
						}
					}).success(function(response) {
						//console.log("Retrieved account info for user " + keycloakAuth.profile.username);
						auth.restcommAccount = response;
						auth.authStatus = "instance"; // instance | realm | failed
						angular.bootstrap(document, ["rcApp"]);
					}).error(function(errorResponse, status) {
						// Handle error case
						if (status == 401)
							auth.authStatus = "realm"; // access to realm but not to this instance
						console.log("Error retrieving account for user '" + keycloakAuth.profile.username + "'");
						angular.bootstrap(document, ["rcApp"]);
					});
					
				});
				console.log(keycloakAuth.profile);      
			}).error(function (a, b) {
					window.location.reload();
			});
		} else {
			angular.module("rcApp").constant("authMode","init");
			angular.module('rcApp').factory('Auth', function() {
				return auth;
			});
			angular.bootstrap(document, ["rcApp"]);
		}
		
		
	})
	.error(function (response) {
		console.log("Internal server error: cannot retrieve identity mode.");
	});

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
