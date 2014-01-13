App.controller('projectManagerCtrl', function ($scope, $http, $location) {
	
	
	$scope.projectNameValidator = /^[^:;@#!$%^&*()+|~=`{}\\\[\]"<>?,\/]+$/;
	
	$scope.refreshProjectList = function() {
		$http({url: 'services/manager/projects/list',
				method: "GET"
		})
		.success(function (data, status, headers, config) {
			$scope.projectList = data;
			for ( var i=0; i < $scope.projectList.length; i ++)
				$scope.projectList[i].viewMode = 'view';
		});
	}
	
	$scope.createNewProject = function(name) {
		$http({url: 'services/manager/projects?name=' + name,
				method: "PUT"
		})
		.success(function (data, status, headers, config) {
			console.log( "project created");
			$location.path("/designer/" + name);
		 });
	}
	
	
	$scope.editProjectName = function(projectItem) {
		projectItem.viewMode = 'edit';
		projectItem.newProjectName = projectItem.name;
		projectItem.errorMessage = "";
	}
	
	$scope.applyNewProjectName = function(projectItem) {
		if ( projectItem.name == projectItem.newProjectName ) {
			projectItem.viewMode = 'view';
			return;
		}
		$http({ method: "PUT", url: 'services/manager/projects/rename?name=' + projectItem.name + "&newName=" + projectItem.newProjectName })
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
	
	$scope.deleteProject = function(projectItem) {
		$http({ method: "DELETE", url: 'services/manager/projects/delete?name=' + projectItem.name })
		.success(function (data, status, headers, config) { 
			console.log( "project " + projectItem.name + " deleted " );
			$scope.refreshProjectList();
			projectItem.showConfirmation = false;
		})
		.error(function (data, status, headers, config) { console.log("cannot delete project"); });		
	}
	
    $scope.refreshProjectList();	
	
});


App.controller('designerCtrl', function($scope, $routeParams, $location, stepService, $http, $dialog, $timeout) {
	
	$scope.logger = function(s) {
		console.log(s);
	};
		
	console.log("routeParam:");
	console.log( $routeParams );
	
	$scope.stepService = stepService;
	
	// Prototype and constant data structures
	$scope.nodesProto =	{name:'module', label:'Untitled module', steps:{}, stepnames:[], bootstrapSrc:'', iface:{edited:false,editLabel:false,bootstrapVisible:false}};
	$scope.languages = [{name:'en',text:'English'},{name:'fr',text:'French'},{name:'it',text:'Italian'},{name:'sp',text:'Spanish'},{name:'el',text:'Greek'}];
	$scope.methods = ['POST', 'GET'];
	
		
	// State variables
	$scope.projectName = $routeParams.projectName;
	$scope.startNodeName = 'start';
	
	$scope.nodes = [];		
	$scope.activeNode = 0 	// contains the currently active node for all kinds of nodes
	$scope.lastNodesId = 0	// id generators for all kinds of nodes
	//$scope.visibleNodes = "voice"; // or "control"	// view Voice Nodes or Control Nodes panel ?
	$scope.wavList = [];
	
	// Project management
	$scope.projectList = [];
	//$scope.newProjectName = $routeParams.projectName;
	

	//console.log("projectController stepService: " + stepService.stepNames );


	// Functionality
	// ------------------

	$scope.loseFocus = function () {
		//console.log('lost focus');
	}
	
	
	
	// nodes
	$scope.nodeNamed = function (name) {
		for ( var i=0; i<$scope.nodes.length; i++ ) {
			var anynode = $scope.nodes[i];
			if (anynode.name == name)
				return anynode;
		}
		return null;
	}
	$scope.setStartNode = function (name) {
		console.log( 'set start node to ' + name );
		$scope.startNodeName = name;
	}
	$scope.startNodeSet = function () {
		if ( typeof($scope.nodeNamed($scope.startNodeName)) !== 'undefined' )
			return true;
		return false;
	}
	$scope.getStartUrl = function () {
		r = new RegExp("^([^#]+/)[^/#]*#");
		m = r.exec(document.baseURI);
		if ( m != null )
			return m[1] + "services/apps/" + $scope.projectName + "/controller";
		return '';
	}

	
	$scope.isActiveNodeByIndex = function ( index) { 
		return index == $scope.activeNode; 
	};
	$scope.isActiveNode = function (node) {
		return $scope.nodes.indexOf(node) == $scope.activeNode;
	}
	$scope.setActiveNodeByIndex = function (newindex) {
		$scope.activeNode = (newindex != -1) ? newindex : 0 ;
	};
	$scope.setActiveNode = function ( node) {
		//console.log( "in setActiveNode" );
		$scope.setActiveNodeByIndex( $scope.nodes.indexOf(node) );
	};
	$scope.setActiveNodeByName = function ( nodename) {
		for ( node in $scope.nodes )
			if ( node.name == nodename ) {
				$scope.setActiveNode( node); // TODO : focus too!
				break;
			}
	};
	$scope.addNode = function( name ) {
		$newnode = angular.copy($scope.nodesProto);
		if ( typeof(name) === 'undefined' )
			$newnode.name += ++$scope.lastNodesId;
		else
			$newnode.name = name;
		$scope.nodes.push( $newnode );
		return $newnode;
	};
	$scope.addNodeAndFocus = function (editLabel) {
		//$scope.setVisibleNodes(kind);
		var node = $scope.addNode();
		if (typeof editLabel !== undefined  && editLabel)
			node.iface.editLabel = true;
		$scope.setActiveNode(node);
		return node;
	};
	$scope.removeNode = function( index) {
		if ( index < $scope.nodes.length ) {
			$scope.nodes.splice(index,1);
			if ( $scope.activeNode == index )
				$scope.setActiveNode(0);
		}
	};
	
	
	$scope.getAllTargets = function() {
		var alltargets = [];
		for ( var i = 0; i < $scope.nodes.length; i++ ) {
			var anynode = $scope.nodes[i];
			alltargets.push( {label: anynode.label, name:anynode.name} );
			for ( var j=0; j < anynode.stepnames.length; j++ ) {
				var stepname = anynode.stepnames[j];
				if ( anynode.steps.hasOwnProperty(stepname) )
					var step = anynode.steps[stepname];
					var label = '';
					switch ( step.kind ) {
						case 'say': 
							var max_phrase_length = 10;
							label = " - Say " + step.phrase.substring(0, Math.min(step.phrase.length,10));
							if ( step.phrase.length > max_phrase_length )
								label += "...";
						break;
						default: label = " - " + step.label + " "; break;
							
						//case 'gather': label = " - Gather "; break;
						//case 'dial': label = " - Dial "; break;
						//case 'hungup': label = " - Hungup "; break;
						//case 'dial': label = " - Dial "; break;
					}
					var name = anynode.name + "." + step.name;
					label = anynode.label + "." + step.name + label;
					alltargets.push( {label: label, name: name} );
			}
		}
		return alltargets;	
	}
	

	/*
	 * When targets change, broadcast an events so that all <select syncModel/> elements
	 * update appropriately. It is uses as a workaround for cases when a selected target is
	 * removed thus leaving the <select>'s model out of sync. 
	 */
	$scope.$watch('getAllTargets().length', function(newValue, oldValue) {
		$timeout( function () {
			$scope.$broadcast("refreshTargetDropdowns");
		});
	});
	
	
	// Utility functions
	$scope.getMapValuesByIndex = function (map, index) {
			var values = [];
			for ( var i = 0; i < index.length; i ++ ) {
				if ( typeof (map[ index[i] ]) !== 'undefined' )
					values.push (map [index [i]]);
			}
			return values;
	}

	
	// gather mappings
	$scope.addGatherMapping = function( gatherStep ) {
		// first find max inserted digit
		var max = 0;
		for (var i = 0; i < gatherStep.mappings.length; i ++ )
			if ( gatherStep.mappings[i].digits > max )
				max = gatherStep.mappings[i].digits;
				
		gatherStep.mappings.push({digits:max+1, node:"start"});
	};
	$scope.removeGatherMapping = function (gatherStep, mapping) {
		gatherStep.mappings.splice( gatherStep.mappings.indexOf(mapping), 1 );
	}
	
	
	// User interface
	$scope.toggleEditControls = function (node) {
		node.iface.edited = !node.iface.edited;
	};
	$scope.areSuccessiveSteps = function (node, index, kind1, kind2) {
		if ( node.steps.length - index >= 2 ) {
			if ( node.steps[index].kind == kind1  &&  node.steps[index+1].kind == kind2 )
				return true;
		} else
		if ( node.steps.length - index == 1 ) {
			if ( node.steps[index].kind == kind1  &&  kind2 == null )
				return true;
		} else
		if ( node.steps.length - index == 0 ) {
			if ( kind1 == null  &&  kind2 == null )
				return true;
		}

		return false;
	};
	
	
	$scope.saveProject = function(onsuccess) {
		
		var state = {};
		state.lastStepId = stepService.lastStepId;
		state.nodes = $scope.nodes;
		state.activeNode = $scope.activeNode;
		state.lastNodeId = $scope.lastNodesId;
		state.visibleNodes = $scope.visibleNodes;
		state.startNodeName = $scope.nodeNamed( $scope.startNodeName ) == null ? null : $scope.nodeNamed( $scope.startNodeName ).name;
		
		
		// transmit state to the server
		console.log( "saving project: " + $scope.projectName );
		$http({url: 'services/manager/projects?name=' + $scope.projectName,
				method: "POST",
				data: state,
				headers: {'Content-Type': 'application/data'}
		})
		.success(function (data, status, headers, config) {
			console.log( "project saved" );
			if (typeof (onsuccess) === "function")
				onsuccess();
		 }).error(function (data, status, headers, config) {
				console.log('ERROR');
		 });	
	}
	

	
	$scope.closeProject = function() {
		$location.path("/project-manager");		
	}
	
	$scope.openProject = function(name) {
		$http({url: 'services/manager/projects?name=' + name,
				method: "GET"
		})
		.success(function (data, status, headers, config) {
			//console.log( data );
			$scope.projectName = name;
			
			stepService.lastStepId = data.lastStepId;
			$scope.nodes = data.nodes;
			$scope.activeNode = data.activeNode;
			$scope.lastNodesId = data.lastNodeId;
			$scope.visibleNodes = data.visibleNodes;
			$scope.startNodeName = data.startNodeName;	
			
			$http({url: 'services/manager/projects/wavlist?name=' + name, method: "GET"})
			.success(function (data, status, headers, config) {
				console.log('getting wav list')
				console.log( data );
				$scope.wavList = data;
			});
			// maybe override .error() also to display a message?
		 });
	}

	
	// First saves and then builds
	$scope.buildProject = function() {
		$scope.saveProject(function() {
			$http({url: 'services/manager/projects/build', method: "POST"})
			.success(function (data, status, headers, config) {
				console.log("Build succesfull");
			 }).error(function (data, status, headers, config) {
				console.log("Error building");
			 });
		});
	}
	
	
	$scope.accessOperationKinds = ['object', 'array', 'string', 'float', 'boolean'];
	$scope.accessOperationProtos = {
			object:{kind:'object',fixed:false, terminal:false},
			array:{kind:'array',fixed:false, terminal:false},
			string:{kind:'string',fixed:false, terminal:true},
			float:{kind:'float',fixed:false, terminal:true},
			boolean:{kind:'boolean',fixed:false, terminal:true},	
	};
	$scope.objectActions = ['propertyNamed'];
	$scope.arrayActions = ['itemAtPosition'];
	
	
	$scope.addAssignment = function(step) {
		console.log("adding assignment");
		step.assignments.push({destVariable:'', accessOperations:[], lastOperation: angular.copy($scope.accessOperationProtos.object) });
	}
	$scope.removeAssignment = function(step,assignment) {
		step.assignments.splice( step.assignments.indexOf(assignment), 1 );
	}
	
	$scope.addOperation = function (assignment) {
		console.log("adding operation");
		assignment.lastOperation.fixed = true;
		assignment.lastOperation.expression = $scope.operationExpression( assignment.lastOperation );
		assignment.accessOperations.push(assignment.lastOperation);
		assignment.lastOperation = angular.copy($scope.accessOperationProtos.object)
	}
	$scope.doneAddingOperations = function (assignment) {
		$scope.addOperation(assignment);
		assignment.lastOperation = null;
	}
	
	$scope.popOperation = function (assignment) { // removes last operation
		if ( assignment.accessOperations.length > 0 ) {
			assignment.lastOperation = assignment.accessOperations.pop();
			assignment.lastOperation.fixed = false;
		}
	}
	
	$scope.operationExpression = function (operation) {
		switch (operation.kind) {
		case 'object':
			switch (operation.action) {
			case 'propertyNamed':
				return "."+operation.property;
			}
		break;
		case 'array':
			switch (operation.action) {
			case 'itemAtPosition':
				return "[" + operation.position + "]";
			}
		break;
		case 'string':
			return " get String value";
		break;
		case 'float':
			return " get Float value";
		break;	
		case 'boolean':
			return " get Boolean value";
		break;		
		}
		return "UNKNOWN";
	}
	
	$scope.assignmentExpression = function (assignment) {
		var expr = '';
		for ( var i=0; i < assignment.accessOperations.length; i++ ) {
			expr += $scope.operationExpression(assignment.accessOperations[i]);
		} 
		return expr;
	}

	
	
	
	// Run the following after all initialization are complete
	
	
	console.log( "opening project: " + $scope.projectName);
	$scope.openProject( $scope.projectName );
		
     
     // UNSORTED
     // -------------
     
     
	$scope.editLabelIfSelected = function (node) {
		if ( $scope.isActiveNode( node ) ) {
			node.iface.editLabel=!node.iface.editLabel;
		}
	}
	

			
});


// add di

