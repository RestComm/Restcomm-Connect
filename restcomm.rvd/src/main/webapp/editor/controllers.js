
var App = angular.module('drag-and-drop', ['ngDragDrop','ui.bootstrap','ui.bootstrap.collapse', 'ui.bootstrap.dialog','ui.sortable','angularSpinner']);


App.factory('stepService', function($rootScope) {
	var stepService = {
		serviceName: 'stepService',
		stepProto: {
					say: {kind:'say', label:'say', title:'say', phrase:'', voice:'man', language:'bf', loop:1, isCollapsed:false, iface:{optionsVisible:false}},
					//play: {kind:'play', label:'play', title:'Play audio file', fileurl:'', loop:1, isCollapsed:false},
					gather: {kind:'gather', label:'gather', title:'collect', name:'', action:'', method:'GET', timeout:'5', finishOnKey:'', numDigits:'', steps:{}, stepnames:[], isCollapsed:false, customHandlerSrc:'', next:'', mappings:[] /*{digits:1, next:"welcome.step1"}*/, collectVariable:'', gatherType:"menu", iface:{advancedView:false,optionsVisible:false}},
					dial: {kind:'dial', label:'dial', title:'Dial', numberToCall:'', action:'', method:'POST', timeout:30, timeLimit:14400, callerId:'', steps:[], isCollapsed:false},
					number: {kind:'number', label:'number', title:'Number', numberToCall:'', sendDigits:'', numberUrl:''},
					redirect: {kind:'redirect', label:'redirect', title:'redirect', next:''},
		},
		stepNames: ['say','gather','dial','redirect'],
		lastStepId: 0,
			
		getMapValuesByIndex: function (map, index) {
			var values = [];
			for ( var i = 0; i < index.length; i ++ ) {
				if ( typeof (map[ index[i] ]) !== 'undefined' )
					values.push (map [index [i]]);
			}
			return values;
		}, 
		addStep: function ( steps, stepnames, kind, index ) {
			var newstep = angular.copy(this.stepProto[kind])
			newstep.name = 'step' + (++this.lastStepId);
			steps[newstep.name] = newstep;
			stepnames.splice(index, 0, newstep.name);
			//stepnames.push(newstep.name) ;
		},	
		removeStep: function (steps, stepnames, removed_step, orderedSteps ) {
			delete steps[removed_step.name];
			stepnames.splice( stepnames.indexOf(removed_step.name), 1 );
			orderedSteps.length = 0; //.splice(0, orderedSteps.length, this.getMapValuesByIndex(steps, stepnames) );
			orderedSteps.push.apply(orderedSteps, this.getMapValuesByIndex(steps, stepnames) );
			//console.log( orderedSteps );
		},
		 
	};
	
	return stepService;
});


App.directive('sortableSteps',function(stepService){
  return {
	  scope: true,
	  /*scope: {
		steps: '=',
		stepnames: '=',
		stepService: '=',
	  },*/ 
	  
	  
    link:function(scope,el,attrs){
		
		if ( typeof(scope.step) === 'undefined' ) {
			//console.log( 'PARENT SCOPE' );
			//console.log(scope);
			scope.steps = scope.node.steps;
			scope.stepnames = scope.node.stepnames;
		}
		else {
			//console.log( 'NESTED SCOPE' );
			//console.log(scope);
			scope.steps = scope.step.steps;
			scope.stepnames = scope.step.stepnames;
		}
		scope.orderedSteps = getMapValuesByIndex(scope.steps, scope.stepnames);
		
		el.sortable({
			revert: true,
			handle: '.panel-heading',
			//scrollSensitivity: 20,
			tolerance: 'pointer',
		});
		//el.disableSelection();
      
	  function getMapValuesByIndex(map, index) {
			var values = [];
			for ( var i = 0; i < index.length; i ++ ) {
				if ( typeof (map[ index[i] ]) !== 'undefined' )
					values.push (map [index [i]]);
			}
			return values;
	  }      
   
      el.on( "sortbeforestop", function( event, ui ) { 
		  
		  //if ( $(this).hasClass('nested') != ui.item.hasClass('nested') )
			//return;
		  
		  var to_index = el.children().index(ui.item);		  
		  if ( ui.item.hasClass('verb-button') ) {
			  // a new step should be created
			  var r = /kind-([^ ]+)/
			  var m = r.exec(ui.item.attr('class'));
			  ui.item.remove();
			  if ( m != null ) {
				  var kind = m[1];
				  scope.$apply( function () {
					stepService.addStep( scope.steps, scope.stepnames, kind, to_index );
			        scope.orderedSteps = getMapValuesByIndex(scope.steps, scope.stepnames);
				  });  
			  }
		  } else
		  if ( ui.item.hasClass('step') ) {
			  // just reordering steps
			  
			  var from_index = scope.stepnames.indexOf( ui.item.scope().step.name );
			  console.log( 'inserting element from position: ' + from_index );
			  console.log( 'inserting element at position: ' + to_index );
			  var temp = scope.stepnames[to_index];
			  scope.$apply( function () {
				scope.stepnames[to_index] = scope.stepnames[from_index];
				scope.stepnames[from_index] = temp;
				scope.orderedSteps = getMapValuesByIndex(scope.steps, scope.stepnames);
			})
		  }

//		  console.log( scope.stepnames );
//		  console.log( scope.steps );
		  
		  
		  if ( $(this).hasClass('nested') )
			event.stopImmediatePropagation();
      } );
    }
  }
})

App.directive('myDraggable',function(){
  
  return 	{
				link:function(scope,el,attrs){
					el.draggable({
						connectToSortable: attrs.myDraggable,
						helper: "clone",
						//revert: "invalid"
					});
					/*el.disableSelection(); */
				}
			}
  
})


App.controller('projectController', function($scope, stepService, $http, $dialog) {
	
	$scope.logger = function(s) {
		console.log(s);
	};
		
	
	$scope.stepService = stepService;
	
	// Prototype and constant data structures
	$scope.nodesProto = {
		voice:{name:'rcmlnode', kind:'voice', label:'Unititled node', steps:{}, stepnames:[], bootstrapSrc:'', iface:{edited:false,editLabel:false,bootstrapVisible:false}}, 
	};
	$scope.languages = [{name:'en',text:'English'},{name:'fr',text:'French'},{name:'it',text:'Italian'},{name:'sp',text:'Spanish'},{name:'el',text:'Greek'}];
	$scope.methods = ['POST', 'GET'];
	
		
	// State variables
	$scope.projectName = ''; // not stored in the state
	$scope.startNodeName = 'start';
	
	$scope.nodes = {voice: [] /*[angular.copy($scope.nodesProto.voice)]*/, control: []}		
	$scope.activeNodes = {voice:0, control:0} 	// contains the currently active node for all kinds of nodes
	$scope.lastNodesId = {voice:0, control:0}	// id generators for all kinds of nodes
	$scope.visibleNodes = "voice"; // or "control"	// view Voice Nodes or Control Nodes panel ?
	$scope.appView = 'spinner'; // spinner | projects | editor
	
	// Project management
	$scope.projectList = [];
	$scope.newProjectName = '';
	

	//console.log("projectController stepService: " + stepService.stepNames );


	// Functionality
	// ------------------

	$scope.setVisibleNodes = function(nodekind) { 
		$scope.visibleNodes = nodekind; 
	};
	$scope.loseFocus = function () {
		//console.log('lost focus');
	}
	
	
	
	// nodes
	$scope.nodeNamed = function (name) {
		for ( var i=0; i<$scope.nodes['voice'].length; i++ ) {
			var anynode = $scope.nodes['voice'][i];
			if (anynode.name == name)
				return anynode;
		}
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
		r = new RegExp("^(.+)/[^/]+\/$");
		m = r.exec(document.baseURI);
		if ( m != null )
			return m[1] + "/workspace/" + $scope.projectName + "/index.php?target=" + $scope.startNodeName;
		return '';
	}
	
	$scope.isActiveNodeByIndex = function (kind, index) { 
		return index == $scope.activeNodes[kind]; 
	};
	$scope.isActiveNode = function (kind,node) {
		return $scope.nodes[kind].indexOf(node) == $scope.activeNodes[kind];
	}
	$scope.setActiveNodeByIndex = function (kind,newindex) {
		$scope.activeNodes[kind] = (newindex != -1) ? newindex : 0 ;
	};
	$scope.setActiveNode = function (kind, node) {
		//console.log( "in setActiveNode" );
		$scope.setActiveNodeByIndex( kind, $scope.nodes[kind].indexOf(node) );
	};
	$scope.setActiveNodeByName = function (kind, nodename) {
		for ( node in $scope.nodes[kind] )
			if ( node.name == nodename ) {
				$scope.setActiveNode(kind, node); // TODO : focus too!
				break;
			}
	};
	$scope.addNode = function( kind, name ) {
		$newnode = angular.copy($scope.nodesProto[kind]);
		if ( typeof(name) === 'undefined' )
			$newnode.name += ++$scope.lastNodesId[kind];
		else
			$newnode.name = name;
		$scope.nodes[kind].push( $newnode );
		return $newnode;
	};
	$scope.addNodeAndFocus = function (kind,editLabel) {
		$scope.setVisibleNodes(kind);
		var node = $scope.addNode(kind);
		if (typeof editLabel !== undefined  && editLabel)
			node.iface.editLabel = true;
		$scope.setActiveNode(kind,node);
		return node;
	};
	$scope.removeNode = function(kind, index) {
		if ( index < $scope.nodes[kind].length ) {
			$scope.nodes[kind].splice(index,1);
			if ( $scope.activeNodes[kind] == index )
				$scope.setActiveNode(kind,0);
		}
	};
	

	
	$scope.getAllTargets = function() {
		var alltargets = [];
		for ( var i = 0; i < $scope.nodes.voice.length; i++ ) {
			var anynode = $scope.nodes.voice[i];
			alltargets.push( {label: anynode.label, name:anynode.name} );
			for ( var j=0; j < anynode.stepnames.length; j++ ) {
				var stepname = anynode.stepnames[j];
				if ( anynode.steps.hasOwnProperty(stepname) )
					var step = anynode.steps[stepname];
					var label = '';
					switch ( step.kind ) {
						case 'say': label = " - Say " + step.phrase.substring(0, Math.min(step.phrase.length,10)); break;
						case 'gather': label = " - Gather "; break;
					}
					var name = anynode.name + "." + step.name;
					label = anynode.label + "." + step.name + label;
					alltargets.push( {label: label, name: name} );
			}
		}
		//console.log( alltargets );
		return alltargets;	
	}
	
	
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
	$scope.expandAll = function() {
		$.each($scope.nodes['voice'][$scope.activeNodes['voice']].steps, function (index, step) {
			step.isCollapsed = false;
		});
	}
	$scope.collapseAll = function() {
		$.each($scope.nodes['voice'][$scope.activeNodes['voice']].steps, function (index, step) {
			step.isCollapsed = true;
		});		
	}
	
	/*
	$scope.getServerInfo = function () {
		$http({url: '../manager.php?action=getServerInfo',
				method: "GET",
		})
		.success(function (data, status, headers, config) {
			if ( data.success ) {
				console.log( data );
			} else
				console.log( data.message );
		 }).error(function (data, status, headers, config) {
				console.log('ERROR');
		 });
	}*/
	
	$scope.saveProject = function(onsuccess) {
		//console.log("saving");
		var state = {};
		state.lastStepId = stepService.lastStepId;
		state.nodes = $scope.nodes;
		state.activeNodes = $scope.activeNodes;
		state.lastNodeId = $scope.lastNodesId;
		state.visibleNodes = $scope.visibleNodes;
		state.startNodeName = $scope.startNodeName;
		
		
		// transmit state to the server
		$http({url: '../manager.php?action=updateState',
				method: "POST",
				data: state,
				headers: {'Content-Type': 'application/data'}
		})
		.success(function (data, status, headers, config) {
			//console.log( data );
			if ( data.success ) {
				console.log( data.message );
				if (typeof (onsuccess) === "function")
					onsuccess();
			} else
				console.log( data.message );
		 }).error(function (data, status, headers, config) {
				console.log('ERROR');
		 });	
	}
	
	$scope.refreshProjectList = function() {
		$http({url: '../rvdservices/manager/projects/list',
				method: "GET"
		})
		.success(function (data, status, headers, config) {
			//console.log( data );
			$scope.projectList = data;
			$scope.appView = "projects";
		});
	}
	
	$scope.closeProject = function() {
		$http({url: '../manager.php?action=close',
				method: "GET"
		})
		.success(function (data, status, headers, config) {
			console.log( data );
			$scope.projectName = '';
			//$scope.refreshProjectList();
			$scope.appView = "projects";
		 });	
	}
	
	$scope.openProject = function(name) {
		$http({url: '../manager.php?action=open&name=' + name,
				method: "GET"
		})
		.success(function (data, status, headers, config) {
			//console.log( data );
			$scope.projectName = name;
			
			stepService.lastStepId = data.lastStepId;
			$scope.nodes = data.nodes;
			$scope.activeNodes = data.activeNodes;
			$scope.lastNodesId = data.lastNodeId;
			$scope.visibleNodes = data.visibleNodes;
			$scope.startNodeName = data.startNodeName;	
					
			//$scope.refreshProjectList();
			$scope.appView = "editor";
		 });
	}
	
	$scope.createNewProject = function(name) {
		console.log( "creating new project " + name );
		$http({url: '../manager.php?action=create&name=' + name,
				method: "GET"
		})
		.success(function (data, status, headers, config) {
			console.log( data );
			$scope.refreshProjectList();
			if ( data.success ) {
				$scope.openProject(name);
				//$scope.projectName = name;
				$scope.newProjectName = '';
			}
		 });
	}
	
	// First saves and then builds
	$scope.buildProject = function() {
		$scope.saveProject(function() {
			$http({url: '../manager.php?action=build', method: "GET"})
			.success(function (data, status, headers, config) {
				console.log(data.message);
			 }).error(function (data, status, headers, config) {
				console.log(data.message);
			 });
		});
	}
	
	
	// Run the following after all initialization are complete
	
	//$scope.getServerInfo();
	
	// Open active project in client
	$http({url: '../manager.php?action=getActive',
			method: "GET",
	})
	.success(function (data, status, headers, config) {
		//console.log( data );
		if ( data.success ) {
			$scope.openProject(data.data.projectName);
		}
     }).error(function (data, status, headers, config) {
            console.log('ERROR');
     });
     
     // Initialize project list
     $scope.refreshProjectList();	
     
     // UNSORTED
     // -------------
     
     
	$scope.editLabelIfSelected = function (node) {
		if ( $scope.isActiveNode('voice', node) ) {
			node.iface.editLabel=!node.iface.editLabel;
		}
	}
			
});

App.filter('excludeNode', function() {
    return function(items, exclude_named) {
        var result = [];
        items.forEach(function (item) {
            if (item.name !== exclude_named) {
                result.push(item);
            }
        });                
        return result;
    }
});

// use it this way: <input type="text" ng-focus="isFocused" ng-focus-lost="loseFocus()">
// for more information: http://stackoverflow.com/questions/14859266/input-autofocus-attribute/14859639#14859639 
angular.module('ng').directive('ngFocus', function($timeout) {
    return {
        link: function ( scope, element, attrs ) {
            scope.$watch( attrs.ngFocus, function ( val ) {
                if ( angular.isDefined( val ) && val ) {
                    $timeout( function () { element[0].focus(); } );
                }
            }, true);

            element.bind('blur', function () {
                if ( angular.isDefined( attrs.ngFocusLost ) ) {
                    scope.$apply( attrs.ngFocusLost );

                }
            });
        }
    };
});


// add di

