App.controller('projectManagerCtrl', function ( $scope, $http, $location, $routeParams, $timeout, $upload, notifications, authentication) {
	
	$scope.authInfo = authentication.getAuthInfo();
	$scope.projectNameValidator = /^[^:;@#!$%^&*()+|~=`{}\\\[\]"<>?,\/]+$/;
	$scope.projectKind = $routeParams.projectKind;
	if ( $scope.projectKind != 'voice' && $scope.projectKind != 'ussd' && $scope.projectKind != 'sms')
		$scope.projectKind = 'voice';
	$scope.error = undefined; 
	//$scope.notifications = [];

	
	$scope.refreshProjectList = function() {

		$http({
			url: '/restcomm/2012-04-24/Accounts/' + $scope.authInfo.username + '/Applications.json',
			method: 'GET'
		}).success(function (data, status, headers, config) {
			var projectList = [];
			for ( var i=0; i < data.length; i ++){
				if(data[i].project_sid){
					var project = {};
					project.name = data[i].friendly_name;
					project.startUrl = data[i].rcml_url;
					project.kind = data[i].kind;
					project.projectSid = data[i].project_sid;
					project.viewMode = 'view';
					projectList.push(project);	
				}
				
			}
			$scope.projectList = projectList;
		}).error(function (data, status, headers, config) {
			if (status == 500)
				notifications.put({type:'danger',message:"Internal server error"});
		});
	}
	
	$scope.createNewProject = function(name, kind, ticket) {
		$http({url: 'services/projects/' + name + "/?kind=" + kind + "&ticket=" + ticket,
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
			}
		 });
	}
	
	
	$scope.editProjectName = function(projectItem) {
		projectItem.viewMode = 'edit';
		projectItem.newProjectName = projectItem.name;
		projectItem.errorMessage = "";
	}
	
	$scope.applyNewProjectName = function(projectItem, ticket) {
		if ( projectItem.name == projectItem.newProjectName ) {
			projectItem.viewMode = 'view';
			return;
		}
		$http({ method: "PUT", url: 'services/projects/' + projectItem.projectSid + '/rename?newName=' + projectItem.newProjectName + "&ticket=" + ticket})
			.success(function (data, status, headers, config) { 
				console.log( "project " + projectItem.name + " renamed to " + projectItem.newProjectName );
				projectItem.name = projectItem.newProjectName;
				projectItem.viewMode = 'view';
				
			})
			.error(function (data, status, headers, config) {
				if (status == 409)
					projectItem.errorMessage = "Project already exists!";
				else
					projectItem.errorMessage = "Cannot rename project";
			});
	}
	
	$scope.deleteProject = function(projectItem, ticket) {
		$http({ method: "DELETE", url: 'services/projects/' + projectItem.projectSid + "?ticket=" + ticket})
		.success(function (data, status, headers, config) { 
			console.log( "project " + projectItem.name + " deleted " );
			$scope.refreshProjectList();
			projectItem.showConfirmation = false;
		})
		.error(function (data, status, headers, config) { console.log("cannot delete project"); });		
	}
	
	$scope.onFileSelect_ImportProject = function($files, ticket) {
	    for (var i = 0; i < $files.length; i++) {
	      var file = $files[i];
	      $scope.upload = $upload.upload({
	        url: 'services/projects?ticket=' + ticket,
	        file: file,
	      }).progress(function(evt) {
	        console.log('percent: ' + parseInt(100.0 * evt.loaded / evt.total));
	      }).success(function(data, status, headers, config) {
	    	  console.log('Project imported successfully');
	    	  $scope.refreshProjectList();
	    	  notifications.put({message:"Project imported successfully", type:"success"});
	      }).error(function(data, status, headers, config) {
	    	  if (status == 400) {// BAD REQUEST
	    		  console.log(data.exception.message);
	    		  notifications.put({message:"Error importing project", type:"danger"});
	    	  }
	      });
	    }
	};
	
    $scope.refreshProjectList();	
	
});

