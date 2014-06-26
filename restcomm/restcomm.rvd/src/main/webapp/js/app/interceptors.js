var UnauthorizedResponseInterceptor = angular.module('Rvd').factory('UnauthorizedResponseInterceptor', ['$q', '$location', function($q, $location) {
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
        	}
        	return response;
        }
    };

    return responseInterceptor;
}]);

angular.module('Rvd').config(['$httpProvider', function($httpProvider) {
    $httpProvider.interceptors.push('UnauthorizedResponseInterceptor');
}]);
