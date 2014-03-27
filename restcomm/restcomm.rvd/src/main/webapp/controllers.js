App.controller('homeCtrl', function ($scope) {
	
});

App.controller('projectManagerCtrl', function ($scope, $http, $location, $routeParams) {
	
	$scope.projectNameValidator = /^[^:;@#!$%^&*()+|~=`{}\\\[\]"<>?,\/]+$/;
	$scope.projectKind = $routeParams.projectKind;
	if ( $scope.projectKind != 'voice' && $scope.projectKind != 'ussd')
		$scope.projectKind = 'voice';

	
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
	
	$scope.createNewProject = function(name, kind) {
		$http({url: 'services/manager/projects?name=' + name + "&kind=" + kind,
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


App.controller('designerCtrl', function($scope, $q, $routeParams, $location, stepService, protos, $http, $timeout, $upload, usSpinnerService) {
	
	$scope.logger = function(s) {
		console.log(s);
	};
		
	//console.log("routeParam:");
	//console.log( $routeParams );
	
	$scope.stepService = stepService;
	
	// Prototype and constant data structures
	$scope.nodesProto =	{name:'module', label:'Untitled module', steps:{}, stepnames:[], bootstrapSrc:'', iface:{edited:false,editLabel:false,bootstrapVisible:false}};
	$scope.languages = [
	                    {name:'bf',text:'Belgium-French'},
	                    {name:'bp',text: 'Brazilian-Portugues'},
	                    {name:'en-gb',text: 'British-English'},
	                    {name:'cf',text: 'Canadian-French'},
	                    {name:'cs',text: 'Czech'},
	                    {name:'dan',text: 'Dannish'},
	                    {name:'en',text:'English'},
	                    {name:'fi',text: 'Finnish'},
	                    {name:'es',text: 'Spanish'},
	                    {name:'fr',text: 'French'},
	                    {name:'de',text: 'German'},
	                    {name:'el',text: 'Greek'},
	                    {name:'it',text: 'Italian'},
	                    {name:'nl',text: 'Netherlands-Dutch'},
	                    {name:'no',text: 'Norwegian'},
	                    {name:'pl',text: 'Polish'},
	                    {name:'pt',text: 'Portuguese'},
	                    {name:'ru',text: 'Russian'},
	                    {name:'ar',text: 'Saudi-Arabia Arabic'},
	                    {name:'ca',text: 'Spain Catalan'}, 
	                    {name:'sv',text: 'Swedish'},
	                    {name:'tr',text: 'Turkish'}
	                    
	                   ];
	$scope.methods = ['POST', 'GET'];
	
		
	// State variables
	$scope.projectNotFound = false;
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
	
	$scope.spinnerSettings = {
		radius: 4,
		lines: 7,
		length: 5,
		width: 3,
	};
	
	// Some constants to be moved elsewhere = TODO
	$scope.yesNoBooleanOptions = [{caption:"Yes", value:true}, {caption:"No", value:false}];
	$scope.nullValue = null;

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
			/*
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
			*/
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
	
	
	$scope.saveProject = function() {
		var deferred = $q.defer();
		
		var state = {};
		state.lastStepId = stepService.lastStepId;
		state.nodes = $scope.nodes;
		state.activeNode = $scope.activeNode;
		state.lastNodeId = $scope.lastNodesId;
		state.visibleNodes = $scope.visibleNodes;
		state.startNodeName = $scope.nodeNamed( $scope.startNodeName ) == null ? null : $scope.nodeNamed( $scope.startNodeName ).name;
		state.projectKind = $scope.projectKind;
		
		
		// transmit state to the server
		//console.log( "saving " + $scope.projectKind + " project: " + $scope.projectName );
		$http({url: 'services/manager/projects?name=' + $scope.projectName,
				method: "POST",
				data: state,
				headers: {'Content-Type': 'application/data'}
		})
		.success(function (data, status, headers, config) {
			if ( data == "" || data.success ) {
				deferred.resolve('Project saved');
			} else {
				deferred.reject({type:'validationError', data:data});			
			}
		 }).error(function (data, status, headers, config) {
			 deferred.reject({type:'saveError'});
		 });	
		
		return deferred.promise;
	}
	

	
	//$scope.closeProject = function() {
	//	$location.path("#/project-manager/" + ($scope.projectKind ? $scope.projectKind : 'voice'));		
	//}
	
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
			$scope.projectKind = data.projectKind;
			
			if ( $scope.projectKind == 'voice' )
				$scope.refreshWavList(name);
			// maybe override .error() also to display a message?
		 }).error(function (data, status, headers, config) {
			//console.log("error opening project");
			$scope.projectNotFound = true;
		 });
	}
	
	$scope.refreshWavList = function(projectName) {
		$http({url: 'services/manager/projects/wavlist?name=' + projectName, method: "GET"})
		.success(function (data, status, headers, config) {
			console.log('getting wav list')
			//console.log( data );
			$scope.wavList = data;
		});
	}

	
	// First saves and then builds
	$scope.buildProject = function() {
		var deferred = $q.defer();
		
		$http({url: 'services/manager/projects/build?name=' + $scope.projectName, method: "POST"})
		.success(function (data, status, headers, config) {
			deferred.resolve('Build successfull');
		 }).error(function (data, status, headers, config) {
			 deferred.reject('buildError');
		 });
		
		return deferred.promise;
	}
	
	$scope.addAssignment = function(step) {
		console.log("adding assignment");
		step.assignments.push({moduleNameScope: null, destVariable:'', valueExtractor: {accessOperations:[], lastOperation: angular.copy(protos.accessOperationProtos.object)} });
	}
	$scope.removeAssignment = function(step,assignment) {
		step.assignments.splice( step.assignments.indexOf(assignment), 1 );
	}
    
    $scope.addUrlParam = function (step) {
        step.urlParams.push({name:'',value:''});
    }
	$scope.removeUrlParam = function(step,urlParam) {
		step.urlParams.splice( step.urlParams.indexOf(urlParam), 1 );
	}    

	// File upload stuff for play verbs
	$scope.onFileSelect = function($files) {
		    //$files: an array of files selected, each file has name, size, and type.
		    for (var i = 0; i < $files.length; i++) {
		      var file = $files[i];
		      $scope.upload = $upload.upload({

		        url: 'services/manager/projects/uploadwav?name=' + $scope.projectName , //upload.php script, node.js route, or servlet url
		        // method: POST or PUT,
		        // headers: {'headerKey': 'headerValue'},
		        // withCredential: true,
		        //data: {myObj: $scope.myModelObj},
		        file: file,
		        // file: $files, //upload multiple files, this feature only works in HTML5 FromData browsers
		        /* set file formData name for 'Content-Desposition' header. Default: 'file' */
		        //fileFormDataName: myFile, //OR for HTML5 multiple upload only a list: ['name1', 'name2', ...]
		        /* customize how data is added to formData. See #40#issuecomment-28612000 for example */
		        //formDataAppender: function(formData, key, val){} 
		      }).progress(function(evt) {
		        console.log('percent: ' + parseInt(100.0 * evt.loaded / evt.total));
		      }).success(function(data, status, headers, config) {
		        // file is uploaded successfully
		    	  console.log('file uploaded successfully');
		        //console.log(data);
		    	  $scope.$emit("fileupload");
		      });
		      //.error(...)
		      //.then(success, error, progress); 
		    }
	};
	
	$scope.$on('fileupload', function(event, data) {
		console.log("caught event fileupload");
		$scope.refreshWavList($scope.projectName);
	});
	
	$scope.deleteWav = function (wavItem) {
		$http({url: 'services/manager/projects/removewav?name=' + $scope.projectName + '&filename=' + wavItem.filename, method: "DELETE"})
		.success(function (data, status, headers, config) {
			console.log("Deleted " + wavItem.filename);
			$scope.$emit('wavfileDeleted', wavItem);
		}).error(function (data, status, headers, config) {
			console.log("Error deleting " + wavItem.filename);
		});
	}
	
	$scope.addDialNoun = function (item, pos, listmodel) {
		//console.log("adding dial noun");
		r = RegExp("dial-noun-([^ ]+)");
		m = r.exec( item.attr("class") );
		if ( m != null ) {
			//console.log("adding dial noun - " + m[1]);
			$scope.$apply( function ()  {
				listmodel.splice(pos,0, angular.copy(protos.dialNounProto[ m[1] ]));
			});
		}
	}
	
	$scope.removeDialNoun = function (dialstep,noun) {
		dialstep.dialNouns.splice( dialstep.dialNouns.indexOf(noun), 1 );
	}
	
	$scope.onSavePressed = function() {
		usSpinnerService.spin('spinner-save');
		$scope.clearStepWarnings();
		$scope.saveProject()
		.then( function () { return $scope.buildProject() } )
		.then(
			function () { 
				$scope.addAlert("Project saved", 'success');
				console.log("Project saved and built");
			}, 
			function (reason) { 
				if ( reason.type == 'saveError' ) {
					console.log("Error saving project");
					$scope.addAlert("Error saving project", 'danger');
				} else if ( reason.type == 'validationError') {
					console.log("Validation error");
					$scope.addAlert("Project saved with validation errors", 'warning');
					var r = /^\/nodes\/([0-9]+)\/steps\/([a-z]+[0-9]+)$/;
					for (var i=0; i < reason.data.errorItems.length; i++) {
						var failurePath = reason.data.errorItems[i].failurePath;
						m = r.exec( reason.data.errorItems[i].failurePath );
						if ( m != null ) {
							console.log("warning in module " + m[1] + " step " + m[2]);
							$scope.nodes[ m[1] ].steps[m[2]].iface.showWarning = true;
						}
					}
				} else { console.log("Unknown error");}
			} 
		)
		.finally(function () {
			usSpinnerService.stop('spinner-save');
			//console.log('save finished');
		});
		//.then( function () { console.log('project saved and built')});
	}
	
	$scope.$on('wavfileDeleted', function (event,data) {
		console.log("caught event wavfileDeleted");
		$scope.refreshWavList($scope.projectName);
	});
	
	$scope.alerts = [];
	$scope.addAlert = function(msg, type) {
	  var alert = null;
	  if (typeof type !== 'undefined')
		  alert = {type: type, msg: msg};
	  else
		  alert = {msg: msg};
	  
	  $scope.alerts.push(alert);
	  $timeout( function () { $scope.closeAlert(alert); }, 3000);
	};

	$scope.closeAlert = function(alert) {
	  $scope.alerts.splice($scope.alerts.indexOf(alert),1);
	};
	
	$scope.clearStepWarnings = function () {
		for ( var i=0; i<$scope.nodes.length; i++ ) {
			for (var stepname in $scope.nodes[i].steps)
				$scope.nodes[i].steps[stepname].iface.showWarning = false;
		}
	}
	
	
	/*    USSDSay / USSDCollect functions    */
	
	// cound how many characters are left for a ussd message. Make sure to disable trim on the bound input control
	$scope.countUssdChars = function(text) {
		return text.length;
	}
	
	// count total characters for the UssdCollect
	$scope.countUssdCollectChars = function(step) {
		var counter = 0;
		for (var i = 0; i <  step.messages.length; i ++) {
			counter += step.messages[i].text.length + 1; // +1 for the newline at the end of this message
		}
		return counter;
	}
	
	$scope.nestUssdMessage = function (item, pos, listmodel) {
		console.log("nesting ussd message");
		//r = RegExp("dial-noun-([^ ]+)");
		//m = r.exec( item.attr("class") );
		//if ( m != null ) {
			//console.log("adding dial noun - " + m[1]);
			$scope.$apply( function ()  {
				listmodel.splice(pos,0, angular.copy(protos.stepProto[ 'ussdSay' ]));
			});
		//}
	}
	
	$scope.removeNestedMessage = function (step,nested) {
		step.messages.splice( step.messages.indexOf(nested), 1 );
	}
		
	// Run the following after all initialization are complete
	
	console.log( "opening project " + $scope.projectName);
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

