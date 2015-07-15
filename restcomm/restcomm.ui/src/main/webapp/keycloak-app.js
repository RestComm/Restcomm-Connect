// var module = angular.module('product', []);

var auth = {};
var logout = function(){
    console.log('*** LOGOUT');
    auth.loggedIn = false;
    auth.authz = null;
    window.location = auth.logoutUrl;
};


angular.element(document).ready(function ($http) {
	$http.get("/restcomm/keycloak/config/mode")
	.success(function (data, status) {
		angular.module("rcApp").constant("authMode",data.mode);
		if (data.mode == "cloud") {
    
			var keycloakAuth = new Keycloak('/restcomm/keycloak/config/restcomm-ui.json');
			auth.loggedIn = false;

			keycloakAuth.init({ onLoad: 'login-required' }).success(function () {
				auth.loggedIn = true;
				auth.authz = keycloakAuth;
				auth.logoutUrl = keycloakAuth.authServerUrl + "/realms/restcomm/tokens/logout?redirect_uri=" + window.location.origin + "/restcomm-management/index.html";
				angular.module('rcApp').factory('Auth', function() {
					return auth;
				});
				
				keycloakAuth.loadUserProfile().success(function () {
					// try importing the logged user into Restcomm 
					var initInjector = angular.injector(["ng"]);
					var $myhttp = initInjector.get("$http");
					
					$myhttp({
						method: 'GET',
						url: '/restcomm/2012-04-24/Accounts.json/' + keycloakAuth.profile.username ,
						headers: {
							Authorization: 'Bearer ' + keycloakAuth.token
						}
					}).success(function(response) {
						console.log("Retrieved account info for user " + keycloakAuth.profile.username);
						auth.restcommAccount = response;
						angular.bootstrap(document, ["rcApp"]);
					}).error(function(errorResponse) {
						// Handle error case
						console.log("Error account info for user " + keycloakAuth.profile.username);
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
