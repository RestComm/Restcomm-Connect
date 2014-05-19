angular.module('Rvd')
.controller('packagingCtrl', function ($scope, $routeParams, PackageInfo, ConfigOption) {
	$scope.protos = {
			configOption: {name:'', label:'', type:'value', description:'', defaultValue:'', required: true }
	}
	$scope.projectName = $routeParams.projectName;
	$scope.packageInfo = null;
	$scope.ConfigOption = 
	
	$scope.getPackageInfo = function (projectName) {
		// retrieve package information from server
		// ...
		$scope.packageInfo = new PackageInfo().init({config: {options: []} });
	} 
	
	$scope.addConfigurationOption = function(type) {
		console.log("Adding configuration option");
		$scope.packageInfo.addOption( ConfigOption.getTypeByLabel(type));
	}
	
	$scope.removeConfigurationOption = function (option) {
		$scope.packageInfo.removeOption(option);
	}
	
	
	// initialization stuff
	$scope.getPackageInfo($scope.projectName);
	
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
.factory('PackageInfo', ['rvdModel', 'ConfigOption', function (rvdModel, ConfigOption) {
	function PackageInfo() {
		this.config = { options: []};
	};
	PackageInfo.prototype = new rvdModel();
	PackageInfo.prototype.constructor = PackageInfo;
	PackageInfo.prototype.addOption = function (type) {
		this.config.options.push( new ConfigOption(type) );
	}
	PackageInfo.prototype.removeOption = function (option) {
		this.config.options.splice(this.config.options.indexOf(option,1));
	}
	return PackageInfo;
}])
;

