// var module = angular.module('product', []);

var auth = {};
var logout = function(){
    console.log('*** LOGOUT');
    auth.loggedIn = false;
    auth.authz = null;
    window.location = auth.logoutUrl;
};


angular.element(document).ready(function ($http) {
    var keycloakAuth = new Keycloak('keycloak.json');
    auth.loggedIn = false;

    keycloakAuth.init({ onLoad: 'login-required' }).success(function () {
        auth.loggedIn = true;
        auth.authz = keycloakAuth;
        auth.logoutUrl = keycloakAuth.authServerUrl + "/realms/restcomm/tokens/logout?redirect_uri="+ window.location.origin +"/restcomm-rvd/index.html";
        angular.module('Rvd').factory('keycloakAuth', function() {
            return auth;
        });
        keycloakAuth.loadUserProfile().success(function () {
			console.log("User profile retrieved")
		});
        angular.bootstrap(document, ["Rvd"]);
    }).error(function (a, b) {
            window.location.reload();
        });

});

/*
module.controller('GlobalCtrl', function($scope, $http) {
    $scope.products = [];
    $scope.reloadData = function() {
        $http.get("/database/products").success(function(data) {
            $scope.products = angular.fromJson(data);

        });

    };
    $scope.logout = logout;
});
*/


angular.module('Rvd').factory('authInterceptor', function($q, keycloakAuth) {
    return {
        request: function (config) {
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
});




angular.module('Rvd').config(function($httpProvider) {
    $httpProvider.responseInterceptors.push('errorInterceptor');
    $httpProvider.interceptors.push('authInterceptor');

});

angular.module('Rvd').factory('errorInterceptor', function($q) {
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
                //alert("Not found");
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
