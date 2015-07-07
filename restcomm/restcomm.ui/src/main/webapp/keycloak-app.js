// var module = angular.module('product', []);

var auth = {};
var logout = function(){
    console.log('*** LOGOUT');
    auth.loggedIn = false;
    auth.authz = null;
    window.location = auth.logoutUrl;
};


angular.element(document).ready(function ($http) {
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
			
			/*
			$myhttp({
				method: 'GET',
				url: '/restcomm/2012-04-24/Accounts.json/import',
				headers: {
					Authorization: 'Bearer ' + keycloakAuth.token
				}
			}).success(function(response) {
	        	console.log("Succesfully triggered user import. Starting application");
	        	angular.bootstrap(document, ["rcApp"]);
	            //myApplication.constant("config", response.data);
	        }).error(function(errorResponse) {
	            // Handle error case
	        	console.log("Error triggering user import from keycloak to restcomm");
	        });
	        * */
		});
        console.log(keycloakAuth.profile);
        //console.log(profile);
        //console.log("username: " + keycloakAuth.username);
        //console.log(keycloakAuth.token);
        //console.log("Will now bootstrap rcApp module");
        
        
    }).error(function (a, b) {
            window.location.reload();
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




angular.module('rcApp').config(function($httpProvider) {
    $httpProvider.responseInterceptors.push('errorInterceptor');
    $httpProvider.interceptors.push('authInterceptor');

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
