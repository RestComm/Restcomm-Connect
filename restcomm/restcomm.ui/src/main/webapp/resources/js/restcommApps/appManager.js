var rappManagerCtrl = angular.module("rcApp.restcommApps").controller('RappManagerCtrl', function($scope, $upload, $location, products, localApps, $sce, $route, Notifications, rappManagerConfig, $filter) {	
	
	/* Sample list item
	{
		title: "ABCauto",
		projectName: "project ABCauto",
		description: "bla bla bla",
		wasImported: true,
		hasPackaging: true,
		hasBootstrap: false,
		appId: [may exist or not],
		status: ??
		isOnline: true/false
		isLocal: true/false
		onlineApp: {} [may exist or not]
		localApp: {} [may exist or not]
		link: [exist only in onlineApp]
	}
	*/
	
	/*
	Application Classification:
	 * local
	 * local (exported)
	 * imported 
	 * imported (RAS)
	 * 
	 * project
	 * local app (under development)
	 * local app (installed from package)
	 * remote app (available from application store)
	 * remote app (purchased by user and available for download and installation)
	
	applications created locally without packaging info:
	 - returned by getLocalApps()
	 - wasImported = false
	 - hasPackaging = false
	 - no .rappInfo member (not returned although it may exist)
	 - display project name as title
	 - no uniqueId
	  
	applications created locally with packaging info
	 - returned by getLocalApps()
	 - wasImported = false
	 - hasPackaging = true
	 - .rappInfo does not exist
	 - no uniqueId
	 
	application downloaded from RAS or imported from package:
	 - returned by getLocalApps()
	 - wasImported = true
	 - hasPackaging = false
	 - .rappInfo exists
	 - unieuqId should be present
	 
	uninstalled applications only available on RAS
	 - returned by getProducts()
	 - 
	 
	 Resolving conflicts
	 
	 - When a RAS/packaged application with uniqueID is installed and there 
	   is already an existing application with the same ID, the application
	   should be updated (under confirmation of course).
	 - When an application package is imported that does not contain an ID
	   (such packages are the packages that are under development or acquired
	   through non-RAS means such as directly through email etc.)
	   a new project should be created. In case of project name collision
	   an indexed name (project 1,2...) should be created
	 
	 Populating application list
	 
	 All available projects should be displayed. Those that have a uniqueID
	 that also exist in the remote products list should be populated with
	 information from the store and ordered in the stores app order.
	  */
	
	
	function populateAppList(onlineApps,localApps) {
		var appList = [];
		var installedOnlineApps = []; // keep these separately
		// go through local apps
		for (var i=0; i<localApps.length; i++) {
			var localApp = localApps[i];
			var app = {};
			app.title = localApp.projectName;
			app.projectName = localApp.projectName;
			app.isLocal = true;
			app.isOnline = false;  // assume it is not online
			// app.description = ""
			app.wasImported = localApp.wasImported;
			app.hasPackaging = localApp.hasPackaging;
			app.hasBootstrap = localApp.hasBootstrap;
			app.localApp = localApp;
			if (localApp.rappInfo) {
				app.appId = localApp.rappInfo.id;
				app.description = localApp.rappInfo.description;
			}
			
			// try to find a matching online application
			if (app.appId) {
				app.onlineApp = getOnlineApp(app.appId, onlineApps);
				if (app.onlineApp)
					app.isOnline = true;
			}
			
			if (app.isOnline)
				installedOnlineApps.push(app); // keep these separately and add it to the end of the appList later
			else
				appList.push(app);
		}
		
		// go through remote apps
		for (var j=0; j<onlineApps.length; j++) {
			var onlineApp = onlineApps[j];
			var app = {}; // assume there is no local app for this onlineApp (based on the appId)
			app.title = onlineApp.info.title;
			app.description = onlineApp.info.excerpt;
			app.wasImported = false;
			app.hasPackaging = false;
			app.appId = onlineApp.info.appId;
			app.isOnline = true;
			app.isLocal = false;
			app.onlineApp = onlineApp;
			app.link = onlineApp.info.link;
			app.thumbnail = onlineApp.info.thumbnail;
			app.category = onlineApp.info.category;
			// app.localApp = undefined;
			
			// try to find a matching local application
			var foundLocal = false;
			for (var k=0; k<installedOnlineApps.length; k++) {
				var processedApp = installedOnlineApps[k];
				if ( processedApp.isOnline && processedApp.appId == onlineApp.info.appId ) {
					// This is an installed online app. Override its properties from online data and push into appList in the right position.
					processedApp.title = app.title;
					processedApp.description = app.description;
					processedApp.link = app.link;
					processedApp.thumbnail = app.thumbnail;
					app.category = app.category;
					appList.push(processedApp);
					foundLocal = true;
					break;
				}
			}
			if (!foundLocal)
				appList.push(app);
		}
		
		// re-scan and finalize the appList
		for (i=0; i<appList.length; i++) {
			var app = appList[i];
			app.type = getTypeForApp(app);
			app.status = getStatusForApp(app);
		}
		return appList;
	}
	
	function getOnlineApp (appId, onlineApps) {
		if ( appId )
			for ( var i=0; i<onlineApps.length; i++ ) {
				if ( onlineApps[i].info.appId == appId )
					return onlineApps[i];
			}
		// return undefined
	}	
	
	
	function getTypeForApp (app) {
		if (app.isLocal && !app.wasImported)
			return "local";
		
		if (app.isLocal && app.hasPackaging )
			return "local (packaged)";
			
		if (app.wasImported && app.isOnline)
			return "imported (RAS)";
			
		if (app.isOnline && !app.isLocal)
			return "available online (RAS)";
			
		if (app.wasImported && !app.isOnline)
			return "imported";
	}
	
	
	function getStatusForApp(app) {
		if (app.isLocal) { 
			if ((app.hasPackaging || app.isOnline) && !app.hasBootstrap )
				return "needs configuration";
			else
				return "OK";
		}
	}
	
	$scope.importPackage = function($files) {
	    for (var i = 0; i < $files.length; i++) {
	      var file = $files[i];
	      $scope.upload = $upload.upload({
	        url: '/restcomm-rvd/services/ras/apps',
	        file: file,
	      }).progress(function(evt) {
	        console.log('percent: ' + parseInt(100.0 * evt.loaded / evt.total));
	      }).success(function(data, status, headers, config) {
			  if (status == 409) {
	    		  console.log(data.exception.message);
	    		  Notifications.warn("This application is already installed");
	    	  } else {
	    		  Notifications.success("Application installed");
				  $route.reload();
			  }
	      })
	      .error( function (data, status, headers) {
	    	  if (status == 409)
	    		  Notifications.warn("This application is already installed");
	    	  else if (status == 500 && data && data.exception && data.exception.className == "UnsupportedRasApplicationVersion")
				Notifications.error(data.exception.message);
	    	  else
	    		  Notifications.error("Cannot import application package");
	      });
	    }
	};
	
	function filterApplications (apps, filter, searchText) {
		return $filter("appsFilter")(apps,filter, searchText);
	}

	$scope.setFilter = function (newFilter, searchFilterText) {
		$scope.filter = newFilter;
		$scope.filteredApps = filterApplications($scope.appList, newFilter, searchFilterText);
	}
	
	$scope.searchTextClicked = function (searchFilterText) {
		$scope.filteredApps = filterApplications($scope.appList, $scope.filter, searchFilterText);
	}

	$scope.formatHtml = function (markup) {
		return $sce.trustAsHtml(markup);
	}
	
	$scope.switchOrder = function(field, sortPredicate ) {
		var newPredicate = field;
		if (newPredicate == sortPredicate)
			newPredicate = "-"+sortPredicate;
		return newPredicate;
	}
	
	$scope.getTypeForApp = getTypeForApp;
	$scope.getStatusForApp = getStatusForApp;

	
	$scope.appList = populateAppList(products,localApps);
	$scope.filterText = "";
	$scope.filter = "all";
	$scope.setFilter($scope.filter);
	$scope.sortPredicate = "title";
		
	$scope.rappManagerConfig = rappManagerConfig;
	
});

rappManagerCtrl.getProducts = function ($q, $http, rappManagerConfig) {
	var deferred = $q.defer();
	
	console.log("retrieving products from AppStore");
	$http({
		method:"GET", 
		//url:"https://restcommapps.wpengine.com/edd-api/products/?key=" + apikey + "&token=" + token + "&cacheInvalidator=" + new Date().getTime()
		url:"http://" + rappManagerConfig.rasHost + "/edd-api/products/?key=" + rappManagerConfig.rasApiKey + "&token=" + rappManagerConfig.rasToken + "&cacheInvalidator=" + new Date().getTime()
	}).success(function (data) {
		console.log("succesfully retrieved " + data.products.length + " products from AppStore");
		deferred.resolve(data.products);
	}).error(function () {
		console.log("http error while retrieving products from AppStore");
		//deferred.reject("http error");
		deferred.resolve([]);
	});
	
	return deferred.promise;
}


<<<<<<< HEAD:restcomm/restcomm.ui/src/main/webapp/resources/js/controllers/rappmanager.js
// Will need this controller when resolving its dependencies. 
var rappManagerConfigCtrl = rcMod.controller('RappManagerConfigCtrl', function($scope, $upload, $routeParams, rappConfig, bootstrapObject, $http, Notifications, $window) {
	
	$scope.initRappConfig = function (rappConfig) {
		var i;
		
		for ( i=0; i < rappConfig.options.length; i++ ) {
			if ( bootstrapObject != null && bootstrapObject[ rappConfig.options[i].name ] )
				rappConfig.options[i].value = bootstrapObject[ rappConfig.options[i].name ];
			if ( bootstrapObject == null ) {
				if ( rappConfig.options[i].defaultValue )
					rappConfig.options[i].value = rappConfig.options[i].defaultValue;
			}
		}
	}
	
	function notifyConfigurationUrl(rappConfig) {
		if ( rappConfig.configurationUrl ) {
			$http({
				url: rappConfig.configurationUrl,
				method: 'POST',
				data: rappConfig.options,
				headers: {'Content-Type': 'application/data'}
			}).success(function (data, status) {
				if ( data.status == "ok" ) {
					//console.log("contacted configurationUrl - " + status);
					if (data.message)
						Notifications.success(data.message);
					if ( data.redirectUrl ) {
						console.log("redirect to " + data.redirectUrl);
						$window.open(data.redirectUrl, '_blank');
					}
				} else {
					if (data.message)
						Notifications.warn(data.message);
				}
			})
			.error(function (data, status) {
				console.log("error contacting configurationUrl - " + rappConfig.configurationUrl + " - " + status);
				Notifications.warn("Error submitting configuration parameters to remote server");
			});
		}
	}
	
	$scope.enableConfiguration = function (rappConfig) {
		//console.log("enabling configuration");
		var bootstrapObject = $scope.generateBootstrap(rappConfig);
		console.log(bootstrapObject);
		$http({
			url: '/restcomm-rvd/services/ras/apps/' + $scope.projectName + '/bootstrap',
			method: 'POST',
			data: bootstrapObject,
			headers: {'Content-Type': 'application/data'}
		}).success(function (data) {
			if ( data.rvdStatus == 'OK') {
				console.log("successfully saved bootstrap information");
				Notifications.success('Configuration saved');
				notifyConfigurationUrl(rappConfig);
			}
			else
				console.log("Rvd error while saving bootstrap information");
		}).error(function () {
			console.log("http error while saving bootstrap info");
		});
	}
	// Creates a bootstrap object out of current configuration options
	$scope.generateBootstrap = function (rappConfig) {
		var bootstrapObject = {};
		var i;
		for (i=0; i < rappConfig.options.length; i ++ ) {
			bootstrapObject[rappConfig.options[i].name] = rappConfig.options[i].value;
		}
		return bootstrapObject;
	}
	$scope.shouldShowHowToConfigure = function () {
		if ( rappConfig.howTo )
			return true;
		return false;
	}
	
	$scope.filterUserOptions = function(option) {
		if ( option.name == 'instanceToken' || option.name == 'backendRootURL' )
			return false
		return true;
	}

	$scope.filterESOptions = function(option) {
		if ( option.name == 'instanceToken' || option.name == 'backendRootURL' )
			return true
		return false;
	}	
	
	function generateUUID(){
	    var d = new Date().getTime();
	    var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
	        var r = (d + Math.random()*16)%16 | 0;
	        d = Math.floor(d/16);
	        return (c=='x' ? r : (r&0x7|0x8)).toString(16);
	    });
	    return uuid;
	};
	$scope.generateNewInstanceToken = function (option) {
		option.value = generateUUID();
	}
	function getOptionByName(name, options) {
		for (var i = 0; i < options.length; i++)
			if (options[i].name == name)
				return options[i];
	}
	$scope.getOptionByName = getOptionByName;
	$scope.createNewInstance = function() {
		console.log("creating new instance");
		var instanceToken = getOptionByName("instanceToken", rappConfig.options).value;
		var backendRootURL = getOptionByName("backendRootURL", rappConfig.options).value;
		$http({ url:backendRootURL + "/provisioning/spawnInstance.php?instanceToken=" + instanceToken, method:"PUT" })
		.success(function () {
			console.log("created new instance");
			Notifications.success("New instance created");
		})
		.error(function () {
			console.log("error creating new instance");
			Notifications.success("Error creating new instance");
		});
	}	
	
	$scope.projectName = $routeParams.projectName;
	$scope.rappConfig = rappConfig;
	
	$scope.initRappConfig($scope.rappConfig);
});

rappManagerConfigCtrl.loadRappConfig = function ($q, $http, $route) {
	var defer = $q.defer();
	
	$http({url: '/restcomm-rvd/services/ras/apps/' + $route.current.params.projectName + '/config', method: "GET" })
	.success(function (data, status, headers, config) {
		if (data.rvdStatus == "OK") {
			console.log("succesfull retrieved app config");
			defer.resolve(data.payload);
		} else {
			defer.reject("error getting app config")
		}
	})
	.error(function () {
		console.log("error getting app config"); 
		defer.reject("bad response");
	});
	
	return defer.promise;
};
rappManagerConfigCtrl.loadBootstapObject = function ($q, $http, $route) {
	var deferred = $q.defer();
	$http.get('/restcomm-rvd/services/ras/apps/' + $route.current.params.projectName + '/bootstrap' )
	.success(function (data, status) {
		deferred.resolve(data);
	})
	.error(function (data,status) {
		if (status == 404)
			deferred.resolve(null);
		else
			deferred.reject(status);
	});
	
	return deferred.promise;
}

=======
>>>>>>> 45ec11b... Fixes #165. Moved RestcommApps related functionality to a sub-module and better organized it in files:restcomm/restcomm.ui/src/main/webapp/resources/js/restcommApps/appManager.js
rcMod.filter('appsFilter', function() {
  return function(appsList, filterType, searchFilterText) {
	  filterType = filterType || "all";
	  var filteredList = [];
	  
	  var nameRegex;
	  if ( searchFilterText )
	  		nameRegex = new RegExp(searchFilterText, "i");

	  for (var i=0; i<appsList.length; i++) {
		  var app = appsList[i];
		  // first search by text filter and drop application that do not match
		  if (nameRegex) {
			  var matched = false;
			  if ( nameRegex.exec(app.projectName) != null )
				  matched = true;
			  if ( nameRegex.exec(app.title) != null )
				  matched = true;				
			  if (!matched)
				  continue;
		  }
		  		  
		  if (filterType == "all") {
		      filteredList.push(app);
		  }
		  else
		  if (filterType == "local") {
			  if (app.isLocal && !app.wasImported)
				filteredList.push(app);
		  }
		  else
		  if (filterType == "imported") {
			  if (app.wasImported )
				filteredList.push(app);
		  }		
		  else
		  if (filterType == "packaged") {
			  if (app.isLocal && app.hasPackaging )
				filteredList.push(app);
		  }			    
		  else
		  if (filterType == "ras") {
			  if (app.isOnline )
				filteredList.push(app);
		  }
		 
	  }
	  return filteredList;
  };
})


