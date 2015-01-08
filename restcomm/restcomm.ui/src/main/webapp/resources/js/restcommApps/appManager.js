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
	/*
	function selectedListItemView (app) {
		if ( app.local && !app.online )
			return "local";
		return "online";
	}
	*/
	//$scope.selectedListItemView = selectedListItemView;
	

	
	//$scope.apps = mergeOnlineWithLocalApps( products, localApps );
	
	
	
	/*
	$scope.buildStatusMessage = function(app) {
		if ( !(app.local && app.local.status) )
			return "Not installed";
			
		var statuses = app.local.status;
		var displayedStatus = "";
		if ( statuses.indexOf('Unconfigured') != -1 )
			displayedStatus = "Installed, Needs configuration";
		else
		if ( statuses.indexOf('Active') != -1 )
			displayedStatus = "Active";
		else
			displayedStatus = statuses;
		
		if ( ! app.online )
			displayedStatus = "Local, " + displayedStatus;
		
		return displayedStatus;
	}
	*/
		
	// merge AppStore and local information
	/*
	for ( var i=0; i<products.length; i++ ) {
		products[i].localApp = getLocalApp(products[i].info.appId, localApps);
	}
	*/

	$scope.onFileSelect = function($files) {
	    for (var i = 0; i < $files.length; i++) {
	      var file = $files[i];
	      $scope.upload = $upload.upload({
	        url: '/restcomm-rvd/services/ras/apps' + "/testname", // upload.php
	        file: file,
	      }).progress(function(evt) {
	        console.log('percent: ' + parseInt(100.0 * evt.loaded / evt.total));
	      }).success(function(data, status, headers, config) {
			  if (status == 409) {
	    		  console.log(data.exception.message);
	    		  Notifications.warn("This application is already installed");
	    	  } else {
				  //console.log('Application uploaded successfully');
				  //$location.path("/ras/apps/" + data[0].projectName + "/config");
				  //$location.path("/ras/config/" + data[0].projectName);
	    		  Notifications.success("Application installed");
				  $route.reload();
			  }
	      })
	      .error( function (data, status, headers) {
	    	  if (status == 409)
	    		  Notifications.warn("This application is already installed");
	    	  else
	    		  Notifications.error("Cannot import application package");
	      });
	    }
	};
	
	function filterApplications (apps, filter) {
		return $filter("appsFilter")(apps,filter);
	}

	$scope.setFilter = function (newFilter) {
		$scope.filter = newFilter;
		$scope.filteredApps = filterApplications($scope.appList, newFilter);
	}

	$scope.formatHtml = function (markup) {
		return $sce.trustAsHtml(markup);
	}

	
	$scope.appList = populateAppList(products,localApps);
	$scope.filter = "all";
	$scope.setFilter($scope.filter);
		
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

rappManagerCtrl.getLocalApps = function ($q, $http) {
	var deferred = $q.defer();
	$http({
		method:"GET",
		url:"/restcomm-rvd/services/ras/apps"
	}).success(function (data) {
		//console.log("successfully received local apps");
		deferred.resolve(data.payload);
	}).error(function () {
		console.log("Error receiving local apps");
		deferred.reject("error");
	});
	
	return deferred.promise;
}

rcMod.filter('appsFilter', function() {
  return function(appsList, filterType) {
	  filterType = filterType || "all";
	  var filteredList = [];
	  for (var i=0; i<appsList.length; i++) {
		  var app = appsList[i];
		  if (filterType == "all") {
		      filteredList.push(app);
		  }
		  else
		  if (filterType == "local") {
			  if (app.isLocal)
				filteredList.push(app);
		  }
		  else
		  if (filterType == "remote") {
			  if (app.isOnline )
				filteredList.push(app);
		  }
		 
	  }
	  return filteredList;
  };
})


