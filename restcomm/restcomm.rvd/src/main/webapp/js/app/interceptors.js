var UnauthorizedResponseInterceptor = angular.module('Rvd').factory('UnauthorizedResponseInterceptor', ['$q', '$location', '$rootScope', function($q, $location, $rootScope) {
    var responseInterceptor = {
        responseError: function(response) {
        	console.log("run UnauthorizedResponseInterceptor");
        	console.log(response);
        	
        	if (response.status === 401) {
				if ( response.data && response.data.exception ) {
					var exceptionName = response.data.exception.className;
					console.log("exceptionName " + exceptionName);
					if ( exceptionName == 'UserNotAuthenticated' ) {
						$location.path("/login");
						return $q.reject(response);
					}
				}
        	} else
        	if (response.status === 404) {
				if ( response.data && response.data.exception ) {
					var exceptionName = response.data.exception.className;
					console.log("exceptionName " + exceptionName);
					if ( exceptionName == 'ProjectDoesNotExist' ) {
						//$location.path("/notfound");
						$rootScope.$emit('resourceNotFound');
						return $q.reject(response);
					}
				}
        	}        		
        	return $q.reject(response); //response;
        }
    };

    return responseInterceptor;
}]);

angular.module('Rvd').config(['$httpProvider', function($httpProvider) {
    $httpProvider.interceptors.push('UnauthorizedResponseInterceptor');
}]);
