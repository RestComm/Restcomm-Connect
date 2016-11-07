var UnauthorizedResponseInterceptor = angular.module('Rvd').factory('UnauthorizedResponseInterceptor', function($q, $location, $rootScope, notifications) {
    var responseInterceptor = {
        responseError: function(response) {
        	//console.log("run UnauthorizedResponseInterceptor");
        	//console.log(response);
        	
        	if (response.status === 401) {
				if ( response.data && response.data.exception ) {
					var exceptionName = response.data.exception.className;
					//console.log("exceptionName " + exceptionName);
					if ( exceptionName == 'UserNotAuthenticated' ) {
						//authentication.clearTicket();
						$location.path("/login");
						return $q.reject(response);
					}
				}
        	} else
        	if (response.status === 404) {
				if ( response.data && response.data.exception ) {
					var exceptionName = response.data.exception.className;
					console.log("exceptionName " + exceptionName);
					/*
					if ( exceptionName == 'ProjectDoesNotExist' ) {
						//$location.path("/notfound");
						$rootScope.$emit('resourceNotFound');
						return $q.reject(response);
					}*/
				}
        	} else
            if (response.status == 403) {
                notifications.put({type:'danger',message:"Unauthorized access"});
            }
        	return $q.reject(response); //response;
        }
    };

    return responseInterceptor;
});

angular.module('Rvd').config(['$httpProvider', function($httpProvider) {
    $httpProvider.interceptors.push('UnauthorizedResponseInterceptor');
}]);

// injects authentication credentials when Restcomm authentication is used
angular.module('Rvd').factory('RestcommAuthenticationInterceptor', function ($injector) {
    function request(config) {
        return $injector.invoke(function ($state, authentication) {
            if (config.url.startsWith('services/') || config.url.startsWith('/restcomm-rvd/services/')) {
                if (authentication.getAuthHeader()) {
                    // if the request is targeted towards RVD add authorization header and there is no authorization header already
                    delete config.headers.authorization; // mind the case insensitivity of headers
                    delete config.headers.Authorization;
                    config.headers.Authorization = authentication.getAuthHeader();
                }
            } else
            if (config.url.startsWith('/restcomm/2012-04-24/')) {
                if (authentication.getAuthHeader()) {
                    delete config.headers.authorization;   // mind the case insensitivity of headers
                    delete config.headers.Authorization;
                    config.headers.Authorization = authentication.getAuthHeader();
                }
            }
            return config;
        });
    } // returns the $injector.invokve() return value

    // public interface
    return {
        request: request
    }
});
// enable the interceptor above only if we do use Restcomm for authentication
angular.module('Rvd').config( function($httpProvider, IdentityConfig) {
    if (IdentityConfig.securedByRestcomm())
        $httpProvider.interceptors.push('RestcommAuthenticationInterceptor');
});
