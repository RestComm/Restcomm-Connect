angular.module('Rvd')
.controller('packagingCtrl', function ($scope, $routeParams, RappConfig, ConfigOption, $http) {
	$scope.protos = {
			configOption: {name:'', label:'', type:'value', description:'', defaultValue:'', required: true }
	}
	
	$scope.getRappConfig = function (projectName) {
		// retrieve package information from server
		// ...
		$scope.rappConfig = new RappConfig().init({options: []});
	} 
	
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
	
	// initialization stuff
	$scope.projectName = $routeParams.projectName;
	$scope.rappConfig = new RappConfig();
	$scope.getRappConfig($scope.projectName);
})
.factory('ConfigOption', ['rvdModel', function (rvdModel) {
	var types = ['value'];
	var typesByLabel = {'Add value': 'value'};
	
	function ConfigOption() {};
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

