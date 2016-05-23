rvdMod.controller('packagingCtrl', function ($scope, $stateParams, Rapp, ConfigOption, $http, rappWrap, $location, notifications, rvdSettings, $translate) {

	$scope.addConfigurationOption = function(type) {
		$scope.rapp.config.addOption(type);
	}
	
	$scope.removeConfigurationOption = function (option) {
		$scope.rapp.config.removeOption(option);
	}
	
	$scope.optionExists = function (name) {
		return $scope.rapp.config.optionExists(name);
	}
	
	$scope.saveRappClicked = function (applicationSid,rapp, submitPermitted) {
		if (submitPermitted)
			$scope.saveRapp(applicationSid,rapp);
		else {
			$translate('formSaveReguiredNotification')
			.then(function (translatedValue) {
				notifications.put({message:translatedValue, type:"warning"});
			});
			
		}
	}
	
	$scope.saveRapp = function (applicationSid,rapp) {
		var packed = rapp.pack();
		$http({
			url: 'services/ras/packaging/app/save?applicationSid=' + applicationSid,
			method:'POST',
			data: packed,
			headers: {'Content-Type': 'application/data'}
		})
		.success(function () {
			console.log("App config saved");
			$scope.isNewRapp = false;
			notifications.put({message:"Package information saved", type:"success"});
		});
	}
	
	$scope.preparePackage = function (applicationSid, projectName) {
		
		$http({
			url: 'services/ras/packaging/app/prepare?applicationSid=' + applicationSid,
			method: 'GET'
		})
		.success(function () {
			console.log("Package is ready for download");
			$location.path("/packaging/" + applicationSid + "=" + projectName + "/download");
		});
	}
	
	$scope.watchFormStatus = function (status) {
		$scope.submitPermitted = status;
	}
	
	// initialization stuff
	$scope.projectName = $stateParams.projectName;
	$scope.applicationSid = $stateParams.applicationSid;
	$scope.rapp = rappWrap.rapp;
	$scope.isNewRapp = !rappWrap.exists;
	//if ( !rappWrap.exists ) {
	//	$scope.rapp.info.name = $scope.projectName;
	//}
	$scope.showErrors = false; // show validation messages
	$scope.effectiveSettings = rvdSettings.getEffectiveSettings();
});

var packagingDownloadCtrl = rvdMod.controller('packagingDownloadCtrl', function ($scope, binaryInfo, $stateParams, fileRetriever) {
	$scope.test = binaryInfo;
	$scope.binaryInfo = binaryInfo;
	$scope.projectName = $stateParams.projectName;
	$scope.applicationSid = $stateParams.applicationSid;
	$scope.download = function(applicationSid, projectName) {
        var downloadUrl = '/restcomm-rvd/services/ras/packaging/download?applicationSid=' + applicationSid + '&projectName=' + projectName;
        fileRetriever.download(downloadUrl, projectName + ".ras.zip").catch(function () {
            notifications.put({message:'Error downloading application package', type:"danger"});
        });
	}
});

rvdMod.factory('RappService', ['$http', '$q', 'Rapp', function ($http, $q, Rapp) {
	var serviceFunctions = {
		getRapp : function (params) {
			var deferred = $q.defer();
			$http({
				url:  'services/ras/packaging/app?applicationSid=' + params.applicationSid,
				method: 'GET',
			})
			.success(function (data, status, headers, config) {
				var rapp = new Rapp().init(data);
				deferred.resolve({exists:true, rapp: rapp});
			})
			.error(function (data, status, headers,config) {
				if ( status == 404 ) {
					var rapp = new Rapp();
					deferred.resolve({exists:false, rapp: rapp});
				} else {
					console.log("server error occured");
					deferred.reject({statusCode: status, message:'Sorry, the resource you were looking for could not be found'});
				}
			});
			return deferred.promise;
		},
		getBinaryInfo : function (params) {
        	var deferred = $q.defer();
        	$http({
        		url: 'services/ras/packaging/binary/info?applicationSid=' + params.applicationSid,
        		method: 'GET'
        	})
        	.success(function (data, status) {
        		console.log("Package is ready for download");
        		deferred.resolve(data.payload); // this is binaryInfo
        	})
        	.error(function () {deferred.reject("error reading binary package information")});
        	return deferred.promise;
        }
	}
	return serviceFunctions;
}])
.factory('ConfigOption', ['rvdModel', function (rvdModel) {

	//var types = ['value'];
	
	function ConfigOption(type) {
		// {name:'', label:'', type:'value', description:'', defaultValue:'', required: true, isInitOption = false }
		//this.type = type;
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
		this.howTo = undefined;
	};
	RappConfig.prototype = new rvdModel();
	RappConfig.prototype.constructor = RappConfig;
	RappConfig.prototype.addOption = function (type) {
		this.options.push( new ConfigOption(type) );
	}
	RappConfig.prototype.removeOption = function (option) {
		this.options.splice(this.options.indexOf(option),1);
	}
	RappConfig.prototype.optionExists = function (name) {
		for (var i=0; i<this.options.length; i++) 
			if (this.options[i].name == name) return true;
		return false;
	}
	RappConfig.prototype.init = function (from) {
		angular.extend(this, from);
		for (var i=0; i<from.options.length; i++) {
			var option = new ConfigOption().init(from.options[i]);
			this.options[i] = option;
		}
		return this;
	}
	return RappConfig;
}])
.factory('RappInfo', ['rvdModel', function (rvdModel) {
	function RappInfo() {
		//this.appVersion = 1;
	};
	RappInfo.prototype = new rvdModel();
	RappInfo.prototype.constructor = RappInfo;
	return RappInfo;
}])
.factory('Rapp', ['rvdModel', 'RappConfig', 'RappInfo', function (rvdModel, RappConfig, RappInfo) {
	function Rapp() {
		this.config = new RappConfig();
		this.info = new RappInfo();
	};
	Rapp.prototype = new rvdModel();
	Rapp.prototype.constructor = Rapp;
	Rapp.prototype.init = function (from) {
		angular.extend(this, from);
		this.info = new RappInfo().init(from.info);
		this.config = new RappConfig().init(from.config);
		return this;
	}
	return Rapp;
}])

;

