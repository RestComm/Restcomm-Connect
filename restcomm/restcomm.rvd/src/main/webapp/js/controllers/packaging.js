angular.module('Rvd')
.controller('packagingCtrl', function ($scope, $routeParams, RappConfig, ConfigOption, $http, rappConfigWrap) {

	$scope.addConfigurationOption = function(type) {
		console.log("Adding configuration option");
		$scope.rappConfig.addOption( ConfigOption.getTypeByLabel(type));
	}
	
	$scope.removeConfigurationOption = function (option) {
		$scope.rappConfig.removeOption(option);
	}
	
	$scope.saveRappConfig = function (projectName,rappConfig) {
		var packed = rappConfig.pack();
		$http({
			url: 'services/manager/projects/package/config?name=' + projectName,
			method:'POST',
			data: packed,
			headers: {'Content-Type': 'application/data'}
		})
		.success(function () {console.log("App config saved")});
	}
	
	$scope.preparePackage = function (projectName) {
		$http({
			url: 'services/manager/projects/package/prepare?name=' + projectName,
			method: 'GET'
		})
		.success(function () {console.log("Package is ready for download")});
	}
	
	// initialization stuff
	$scope.projectName = $routeParams.projectName;
	$scope.rappConfig = rappConfigWrap.rappConfig;
	$scope.configExists = rappConfigWrap.exists;
})
.factory('RappConfigService', ['$http', '$q', 'RappConfig', '$route', '$location', function ($http, $q, RappConfig,$route, $rootScope) {
	var serviceFunctions = {
		getRappConfig : function () {
			var deferred = $q.defer();
			$http({
				url:  'services/manager/projects/package/config?name=' + $route.current.params.projectName,
				method: 'GET',
			})
			.success(function (data, status, headers, config) {
				var rappConfig = new RappConfig().init(data);
				deferred.resolve({exists:true, rappConfig: rappConfig});
			})
			.error(function (data, status, headers,config) {
				if ( status == 404 ) {
					var rappConfig = new RappConfig();
					deferred.resolve({exists:false, rappConfig: rappConfig});
				} else {
					console.log("server error occured");
					deferred.reject({statusCode: status, message:'Sorry, the resource you were looking for could not be found'});
				}
			});
			return deferred.promise;
		}
	}
	return serviceFunctions;
}])
.factory('ConfigOption', ['rvdModel', function (rvdModel) {
	var types = ['value'];
	var typesByLabel = {'Add value': 'value'};
	
	function ConfigOption() {
		// {name:'', label:'', type:'value', description:'', defaultValue:'', required: true }
	};
	ConfigOption.prototype = new rvdModel();
	ConfigOption.prototype.constructor = ConfigOption;
	ConfigOption.getTypeByLabel = function(type) { return typesByLabel[type];	}
	ConfigOption.getTypeLabels = function() {	return labels;	}
	return ConfigOption;
}])
.factory('RappConfig', ['rvdModel', 'ConfigOption', function (rvdModel, ConfigOption) {
	function RappConfig() {
		this.options = [];
	};
	RappConfig.prototype = new rvdModel();
	RappConfig.prototype.constructor = RappConfig;
	RappConfig.prototype.addOption = function (type) {
		this.options.push( new ConfigOption(type) );
	}
	RappConfig.prototype.removeOption = function (option) {
		this.options.splice(this.options.indexOf(option,1));
	}
	return RappConfig;
}])
;

