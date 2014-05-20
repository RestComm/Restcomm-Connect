angular.module('Rvd')
.controller('packagingCtrl', function ($scope, $routeParams, RappConfig, ConfigOption) {
	$scope.protos = {
			configOption: {name:'', label:'', type:'value', description:'', defaultValue:'', required: true }
	}
	$scope.projectName = $routeParams.projectName;
	$scope.rappConfig = null;
	$scope.ConfigOption = 
	
	$scope.getRappConfig = function (projectName) {
		// retrieve package information from server
		// ...
		$scope.rappConfig = new RappConfig().init({config: {options: []} });
	} 
	
	$scope.addConfigurationOption = function(type) {
		console.log("Adding configuration option");
		$scope.rappConfig.addOption( ConfigOption.getTypeByLabel(type));
	}
	
	$scope.removeConfigurationOption = function (option) {
		$scope.rappConfig.removeOption(option);
	}
	
	
	// initialization stuff
	$scope.getRappConfig($scope.projectName);
	
	console.log("Initializing packaging controller");
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

