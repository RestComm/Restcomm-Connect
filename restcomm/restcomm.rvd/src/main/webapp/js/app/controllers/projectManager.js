App.controller('projectManagerCtrl', function ( $scope, $http, $location, $stateParams, $timeout, $upload, notifications, authentication, fileRetriever ) {

	var account = authentication.getAccount();

	$scope.authInfo = authentication.getAuthInfo();
	$scope.projectNameValidator = /^[^:;@#!$%^&*()+|~=`{}\\\[\]"<>?,\/]+$/;
	$scope.projectKind = $stateParams.projectKind;
	if ( $scope.projectKind != 'voice' && $scope.projectKind != 'ussd' && $scope.projectKind != 'sms')
		$scope.projectKind = 'voice';
	$scope.error = undefined; 
	//$scope.notifications = [];
	$scope.appItems = [];
	$scope.retrievingApps = false;

	
	$scope.refreshProjectList = function() {
		$scope.appItems = [];
		$scope.retrievingApps = true;
		$http({
			url: '/restcomm/2012-04-24/Accounts/' + account.sid + '/Applications.json',
			method: 'GET',
			headers: {Authorization: authentication.getAuthHeader()}
		}).success(function (data, status, headers, config) {
			for (var i=0; i<data.length; i++) {
			    var item = {
			        viewMode:'view',
			        app: data[i]
			    }
			    $scope.appItems.push(item);
			}
			$scope.retrievingApps = false;
			/*
			restcommApps = data;
			$http({url: 'services/projects',
				method: "GET"
			}).success(function (data, status, headers, config) {
				for (var i in restcommApps) {
					var currentApp = restcommApps[i];
					var applicationSid = currentApp.sid;
					for ( var i=0; i < data.length; i ++){
						if(data[i].name === applicationSid){
							var project = {};
							project.applicationSid = applicationSid;
							project.name = currentApp.friendly_name;
							project.startUrl = currentApp.rcml_url;
							project.kind = currentApp.kind;
							project.viewMode = 'view';
							project.status = data[i].status;
							projectList.push(project);
							break;
						}
					}
				}
				$scope.projectList = projectList;

			}).error(function (data, status, headers, config) {
				if (status == 500)
				notifications.put({type:'danger',message:"Internal server error"});
			});
			*/
		}).error(function (data, status, headers, config) {
			$scope.retrievingApps = false;
			if (status == 500)
				notifications.put({type:'danger',message:"Internal server error"});
			if (status == 401){
				notifications.put({type:'danger',message:"You have been logged out! Please, log-in using the same Account in RVD and Adminitration console.",timeout:0});
				authentication.doLogout();
			}
		});
	}
	
	$scope.createNewProject = function(name, kind, ticket) {
		$http({url: 'services/projects/' + name + "/?kind=" + kind,
				method: "PUT"
		})
		.success(function (data, status, headers, config) {
			console.log( "project created");
			$location.path("/designer/" + data.sid + "=" + name);
		 })
		 .error(function (data, status, headers, config) {
			if (status == 409) {
				console.log("project already exists");
				notifications.put({type:'danger',message:'A Voice, SMS or USSD project  with that name already exists in the workspace (maybe it belongs to another user).'});
			} else
			if (status >= 500) {
				console.log("internal server error: " + status);
				notifications.put({type:'danger',message:'Internal server error.'});
			} else {
				console.log("operation failed: " + status);
				notifications.put({type:'danger',message:'Could not create project.'});
			}
		 });
	}
	
	
	$scope.editProjectName = function(item) {
		item.viewMode = 'edit';
		item.newName = item.app.friendly_name;
		item.errorMessage = "";
	}
	
	$scope.applyNewProjectName = function(item, ticket) {
		if ( item.app.friendly_name == item.newName ) {
			item.viewMode = 'view';
			return;
		}
		$http({ method: "PUT", url: 'services/projects/' + item.app.sid + '/rename?newName=' + item.newName})
			.success(function (data, status, headers, config) { 
				console.log( "project " + item.app.friendly_name + " renamed to " + item.newName );
				item.app.friendly_name = item.newName;
				item.viewMode = 'view';
			})
			.error(function (data, status, headers, config) {
				if (status == 409)
					item.errorMessage = "Project already exists!";
				else
					item.errorMessage = "Cannot rename project";
			});
	}
	
	$scope.deleteProject = function(item, ticket) {
		$http({ method: "DELETE", url: 'services/projects/' + item.app.sid})
		.success(function (data, status, headers, config) { 
			console.log( "project " + item.app.friendly_name + " deleted " );
			$scope.refreshProjectList();
			item.viewMode = 'view';
		})
		.error(function (data, status, headers, config) {
            $scope.refreshProjectList();
            item.viewMode = 'view';
            if (status == 404) {
                // 404 should be treated with care in case the project does not exist but the application does
                // no error displayed here
            } else {
                console.log("cannot delete project");
                if (status >= 500) {
                    notifications.put({type:'danger',message:'Internal server error.'});
                } else {
                    notifications.put({type:'danger',message:'Could not delete project.'});
                }
		    }
		});
	}
	
	$scope.onFileSelect_ImportProject = function($files, ticket) {
	    for (var i = 0; i < $files.length; i++) {
	      var file = $files[i];
	      $scope.upload = $upload.upload({
	        url: 'services/projects',
	        file: file,
	      }).progress(function(evt) {
	        //console.log('percent: ' + parseInt(100.0 * evt.loaded / evt.total));
	      }).success(function(data, status, headers, config) {
	    	  console.log('Project imported successfully');
	    	  $scope.refreshProjectList();
	    	  notifications.put({message:"Project imported successfully", type:"success"});
	      }).error(function(data, status, headers, config) {
	    	  if (status == 400 && data && data.exception && data.exception.className == "UnsupportedProjectVersion") {
	    		  console.log(data.exception.message);
	    		  notifications.put({message:"Cannot import project. " + data.exception.message, type:"danger"});
	    	  } else {
                  console.log(data);
	    	      notifications.put({message:"Error importing project", type:"danger"});
	    	  }
	      });
	    }
	};

	$scope.download = function (applicationSid,projectName) {
	    var downloadUrl =  '/restcomm-rvd/services/projects/' + applicationSid + '/archive?projectName=' + projectName;
	    fileRetriever.download(downloadUrl, projectName + ".zip").catch(function () {
	        notifications.put({type:"danger", message:"Error downloading project archive"});
	    });
	}
	
    $scope.refreshProjectList();	
	
});

/**
 * filters a project list as populated by projectManager controler by project kind
 */
App.filter("projectFilter", function () {
    return function(input, kind) {
        if (!input)
            return input;
        var filtered = [];
        for (var i=0; i <input.length; i++) {
            if (input[i].app.kind == kind)
                filtered.push(input[i]);
        }
        return filtered;
    }
})

