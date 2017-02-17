var designerCtrl = App.controller('designerCtrl', function($scope, $q, $stateParams, $location, stepService, $http, $timeout, $injector, stepRegistry, stepPacker, $modal, notifications, ModelBuilder, projectSettingsService, webTriggerService, nodeRegistry, editedNodes, project, designerService, $filter, $anchorScroll, bundledWavs, fileRetriever) {

	$scope.project = project;
	$scope.visibleNodes = editedNodes.getEditedNodes();

	$scope.download = function (applicationSid,projectName) {
	    var downloadUrl =  '/restcomm-rvd/services/projects/' + applicationSid + '/archive?projectName=' + projectName;
	    fileRetriever.download(downloadUrl, projectName + ".zip").catch(function () {
	        notifications.put({type:"danger", message:"Error downloading project archive"});
	    });
	}

	$scope.getProjectSettings = function () {
		return projectSettingsService.getProjectSettings(); // returns a $resource that will be filled up automatically
	}
	$scope.getActiveNodeName = function () {
		var activeNode = editedNodes.getActiveNode();
		return activeNode;
	}
	$scope.setActiveNode = function (nodeName) {
		editedNodes.setActiveNode(nodeName);
	}
	$scope.setStartNode = function (name) {
		console.log( 'set start node to ' + name );
		$scope.project.startNodeName = name;
	}
	$scope.startNodeSet = function (project) {
		if ( typeof(nodeRegistry.getNode(project.startNodeName)) !== 'undefined' )
			return true;
		return false;
	}
	$scope.getEditedNodeNames = function () {
		return editedNodes.getEditedNodes();
	}
	$scope.isNodeActive = function (nodeName) {
		return editedNodes.isNodeActive(nodeName);
	}
	$scope.addNodeClicked = function(kind) {
		var newnode = ModelBuilder.build( kind+"NodeModel" );
		nodeRegistry.addNode(newnode);
		editedNodes.addEditedNode(newnode.name);
		editedNodes.setActiveNode(newnode.name);
	}
	$scope.removeNode = function(nodeName) {
		nodeRegistry.removeNode(nodeName);
	};
	$scope.hideNode = function(nodeName) {
		editedNodes.removeEditedNode(nodeName);
	};
	$scope.editNode = function(nodeName) {
		editedNodes.addEditedNode(nodeName);
		editedNodes.setActiveNode(nodeName);
	}
	$scope.getNodeLabel = function(nodeName) {
		return nodeRegistry.getNode(nodeName).label;
	};
	$scope.onEditNodeLabel = function (view, nodeName) {
		if ( editedNodes.isNodeActive(nodeName) )
			view.editLabel = !view.editLabel;
	}
	$scope.onEditLabelFinished = function (view,nodeName) {
		view.editLabel = false;
		nodeRegistry.getNode(nodeName).label = view.label;
		console.log("label editing finished");
	}
	$scope.searchNodesFieldLostFocus = function () {
		console.log("searchNodesFieldLostFocus");
	}
	$scope.getAllTargets = function() {
		return nodeRegistry.getNodes();
	}
	$scope.clearStepWarnings = function () {
		var nodes = nodeRegistry.getNodes();
		for ( var i=0; i<nodes.length; i++ ) {
			for (var j=0; j< nodes[i].steps.length; j++) {
			    if (nodes[i].steps[j].iface)
					nodes[i].steps[j].iface.showWarning = false;
			}
		}
		$scope.$broadcast("clear-step-warnings");
	}
	$scope.editFilteredNodes = function(nodes,token, moduleFilterEnabled) {
		var filteredNodes = $filter('filterNodesByLabel')(nodes,token, moduleFilterEnabled);
		for (var i=0; i<filteredNodes.length; i++) {
			$scope.editNode(filteredNodes[i].name);
		}
	}
	$scope.editAllNodes = function (nodes, token, moduleFilterEnabled) {
		if (moduleFilterEnabled)
			$scope.editFilteredNodes(nodes,token, moduleFilterEnabled )
		else {
			for (var i=0; i<nodes.length; i++) {
				$scope.editNode(nodes[i].name);
			}
		}
	}
	$scope.hideAllButStartNode = function(nodes,startNodeName) {
		for (var i=0; i<nodes.length; i++) {
			if (nodes[i].name != startNodeName) {
				$scope.hideNode(nodes[i].name);
			}
		}
	}
	$scope.hideAllNodes = function(nodes) {
		for (var i=0; i<nodes.length; i++) {
			$scope.hideNode(nodes[i].name);
		}
	}
	$scope.toggleModuleFilter = function () {
		if ( !$scope.moduleFilterEnabled )
			$scope.moduleFilterEnabled = true;
		else
			$scope.moduleFilterEnabled = false;
	}
	$scope.nodeNamed = function (name) {
		return nodeRegistry.getNode(name);
	}
	$scope.getStartUrl = function () {
		r = new RegExp("^([^#]+/)[^/#]*#");
		m = r.exec(document.baseURI);
		if ( m != null )
			return m[1] + "services/apps/" + $scope.applicationSid + "/controller";
		return '';
	}
	$scope.addGatherMapping = function( gatherStep ) {
		gatherStep.menu.mappings.push({digits:"", next:""});
	};
	$scope.removeGatherMapping = function (gatherStep, mapping) {
		gatherStep.menu.mappings.splice( gatherStep.menu.mappings.indexOf(mapping), 1 );
	}
	// ussd collect handles adding mappings a little differently
	$scope.addUssdCollectMapping = function (collectStep) {
		collectStep.menu.mappings.push({digits:"", next:""});
	}
	// returns the number of conference nounds contained in the dial
	$scope.dialContainsConference = function (dialstep) {
		var count = 0;
		for (var i=0; i<dialstep.dialNouns.length; i++) {
			var noun = dialstep.dialNouns[i];
			if (noun.dialType == "conference")
				count ++;
		}
		return count;
	}
	$scope.selectBundledWav = function(playstep, wavUrl) {
		playstep.remote.wavUrl = wavUrl;
	}


	$scope.bundledWavs = bundledWavs;

	$scope.selectedView = 'rcml';
	$scope.settings = {}; // REMOVE THIS!!! - populate this from some resolved
							// parameters

	// Prototype and constant data structures
	$scope.languages = [
											{name:'bf',text:'Belgium-French'},
											{name:'bp',text: 'Brazilian-Portugues'},
											{name:'en-gb',text: 'British-English'},
											{name:'cf',text: 'Canadian-French'},
											{name:'zh-cn',text: 'Chinese'},
											{name:'zh-hk',text: 'Chinese - Hong Kong'},
											{name:'zh-tw',text: 'Chinese - Taiwan'},
											{name:'cs',text: 'Czech'},
											{name:'dan',text: 'Dannish'},
											{name:'en',text:'English'},
											{name:'fi',text: 'Finnish'},
											{name:'es',text: 'Spanish'},
											{name:'fr',text: 'French'},
											{name:'de',text: 'German'},
											{name:'el',text: 'Greek'},
											{name:'it',text: 'Italian'},
											{name:'ja',text: 'Japanese'},
											{name:'nl',text: 'Netherlands-Dutch'},
											{name:'no',text: 'Norwegian'},
											{name:'pl',text: 'Polish'},
											{name:'pt',text: 'Portuguese'},
											{name:'ru',text: 'Russian'},
											{name:'ar',text: 'Saudi-Arabia Arabic'},
											{name:'ca',text: 'Spain Catalan'},
											{name:'sv',text: 'Swedish'},
											{name:'th',text: 'Thai'},
											{name:'tr',text: 'Turkish'}
										 ];
	$scope.methods = ['POST', 'GET'];

	$scope.ussdMaxEnglishChars = 182;
	$scope.ussdMaxForeignChars = 91;

	// State variables
	$scope.projectError = null; // SET when opening a project fails
	$scope.projectName = $stateParams.projectName;
	$scope.applicationSid = $stateParams.applicationSid;

	//$scope.nodes = [];
	//$scope.activeNode = 0 	// contains the currently active node for all kinds
							// of nodes
	//$scope.lastNodesId = 0	// id generators for all kinds of nodes

	$scope.wavList = [];

	// Some constants to be moved elsewhere = TODO
	$scope.yesNoBooleanOptions = [{caption:"Yes", value:true}, {caption:"No", value:false}];
	$scope.nullValue = null;
	$scope.rejectOptions = [{caption:"busy", value:"busy"}, {caption:"rejected", value:"rejected"}];

	projectSettingsService.refresh($scope.applicationSid);

	/*
	 * When targets change, broadcast an events so that all <select syncModel/>
	 * elements update appropriately. It is uses as a workaround for cases when
	 * a selected target is removed thus leaving the <select>'s model out of
	 * sync.
	 */
	$scope.$watch('getAllTargets().length', function(newValue, oldValue) {
		$timeout( function () {
			$scope.$broadcast("refreshTargetDropdowns");
		});
	});


	$scope.refreshWavList = function() {
		designerService.getWavList($scope.applicationSid).then(function (wavList) {
			$scope.project.wavList = wavList;
		});

	}

	$scope.addAssignment = function(step) {
		console.log("adding assignment");
		step.assignments.push({moduleNameScope: null, destVariable:'', scope:'module', valueExtractor: {accessOperations:[], lastOperation: angular.copy(protos.accessOperationProtos.object)} });
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

	$scope.$on('fileupload', function(event, data) {
		$scope.refreshWavList();
	});

	$scope.addDialNoun = function (classAttribute, pos, listmodel) {
		// console.log("adding dial noun");
		r = RegExp("dial-noun-([^ ]+)");
		m = r.exec( classAttribute );
		if ( m != null ) {
			// console.log("adding dial noun - " + m[1]);
			var noun = $injector.invoke([m[1]+'NounModel', function(model){
				return new model();
			}]);
			$scope.$apply( function ()	{
				listmodel.splice(pos,0, noun);
			});
		}
	}

	$scope.removeDialNoun = function (dialstep,noun) {
		dialstep.dialNouns.splice( dialstep.dialNouns.indexOf(noun), 1 );
	}

	$scope.addStep = function (classAttribute,pos,listmodel) {
		//console.log("Adding step ");
		r = RegExp("button-([^ ]+)");
		m = r.exec( classAttribute );
		if ( m != null ) {
			var step;
			var stepkind = m[1];
			if (stepkind == "control") {
                step = {kind: stepkind}
			} else {
                step = $injector.invoke([stepkind+'Model', function(model){
                    var stepname = stepRegistry.name();
                    return new model(stepname);
                }]);
			}

			//console.log("adding step - " + m[1]);
			$scope.$apply( function ()	{
				listmodel.splice(pos,0, step);
			});
		}
	}

	$scope.removeStep = function (step,node_steps,steps) {
		//console.log("Removing step");
		var container;
		if ( typeof steps != 'undefined')
			container = steps;
		else
			container = node_steps;

		container.splice( container.indexOf(step), 1);
	}



	$scope.onSavePressed = function() {
		var nodes = nodeRegistry.getNodes();
		$scope.saveSpinnerShown = true;
		$scope.clearStepWarnings();
		$scope.$broadcast("update-dtos"); // command all step directives to updated dto model objects.
		designerService.saveProject($scope.applicationSid, $scope.project)
		.then( function () { return designerService.buildProject($scope.applicationSid) } )
		.then(
			function () {
			    if ($scope.showGraph)
			        $scope.drawGraph();
				notifications.put({type:"success", message:"Project saved"});
				console.log("Project saved and built");
			},
			function (reason) {
			    if ($scope.showGraph)
			        $scope.drawGraph();
				if ( reason.exception.className == 'ValidationException' ) {
					console.log("Validation error");
					notifications.put({type:"warning", message:"Project saved with validation errors"});
					var r = /^\/nodes\/([0-9]+)\/steps\/([0-9]+)$/;
					var errorItems = reason.exception.jsonSchemaReport.errorItems;
					for (var i=0; i < errorItems.length; i++) {
						var failurePath = errorItems[i].failurePath;
						m = r.exec( errorItems[i].failurePath );
						if ( m != null ) {
							console.log("warning in module " + nodes[ m[1] ].name + " step " + nodes[ m[1] ].steps[m[2]].name);
							var step = nodes[ m[1] ].steps[m[2]];
							if (step.kind == 'control') {
							    $scope.$broadcast('notify-step', {target: step.name, type: 'validation-error', data: errorItems[i]}); // TODO at some point when all steps use directives use the messaging mechanism for them too
							} else
							    nodes[ m[1] ].steps[m[2]].iface.showWarning = true;
						}
					}
				} else
				if ( reason.exception.className == 'IncompatibleProjectVersion' ) {
					console.log("error saving project - Project version is incompatible with current RVD version");
					notifications.put({type:"danger", message:"Error saving project. Project version is incompatible with current RVD version"});
				} else {
					console.log("error saving project");
					notifications.put({type:"danger", message:"Error saving project"});
				}
			}
		)
		.finally(function () {
			$scope.saveSpinnerShown = false;
		});
		// .then( function () { console.log('project saved and built')});
	}

	$scope.$on('project-wav-removed', function (event,data) {
		$scope.refreshWavList();
	});



	/* USSDSay / USSDCollect functions */

	// cound how many characters are left for a ussd message. Make sure to
	// disable trim on the bound input control
	$scope.countUssdChars = function(text) {
		return text.length;
	}

	// count total characters for the UssdCollect
	$scope.countUssdCollectChars = function(step) {
		var counter = 0;
		for (var i = 0; i <	step.messages.length; i ++) {
			counter += step.messages[i].text.length + 1; // +1 for the
															// newline at the
															// end of this
															// message
		}
		return counter;
	}

	$scope.getUssdNodeLang = function (node) {
		var lang = "en";
		for ( var i=0; i>node.steps.length; i++ ) {
			var step = node.steps[i];
			if ( step.kind == "ussdLanguage")
				if (step.language != null	&&	step.language != 'en') {
					lang = step.language;
					break;
				}
		}
		return lang;
	}

	$scope.countNodeUssdChars = function (node) {
		var sum = 0;
		for ( var i=0; i<node.steps.length; i++ ) {
			var step = node.steps[i];
			if ( step.kind == "ussdSay" )
				sum += $scope.countUssdChars(step.text);
			else
			if ( step.kind == "ussdCollect" )
				sum += $scope.countUssdCollectChars(step)
		}
		return sum;
	}

	$scope.remainingUssdChars = function (node) {
		var total = $scope.countNodeUssdChars(node);
		var remaining = $scope.ussdMaxEnglishChars - total;
		if ( $scope.getUssdNodeLang(node) != 'en' )
			remaining = $scope.ussdMaxForeignChars - total;
		return remaining;
	}

	$scope.nestUssdMessage = function (classAttribute, pos, listmodel) {
		$scope.$apply( function ()	{
			var nestedMessage;
			nestedMessage = $injector.invoke(['ussdSayNestedModel', function(model){
				return new model();
			}]);
			listmodel.splice(pos,0, nestedMessage);
		});
	}

	$scope.removeNestedMessage = function (step,nested) {
		step.messages.splice( step.messages.indexOf(nested), 1 );
	}



	// Exception controller & functionality
	/*
	var exceptionConfigCtrl = function ($scope, $modalInstance, projectModules) {
		$scope.moduleSummary = projectModules.getModuleSummary();
		$scope.exceptionMappings = [];


		$scope.addExceptionMapping = function() {
			$scope.exceptionMappings.push({exceptionName:undefined, next:undefined});
		}
		$scope.removeExceptionMapping = function (mapping) {
			$scope.exceptionMappings.splice($scope.exceptionMappings.indexOf(mapping), 1);
		}

		$scope.ok = function () {
			$modalInstance.close($scope.exceptionMappings);
		};

		$scope.cancel = function () {
		$modalInstance.dismiss('cancel');
		};
	};
	$scope.showExceptionConfig = function () {
		var modalInstance = $modal.open({
			templateUrl: 'templates/exceptionConfigModal.html',
			controller: exceptionConfigCtrl,
			size: 'lg',
			// resolve: {
			// items: function () {
			// return $scope.items;
			// }
			// }
		});

		modalInstance.result.then(function (exceptionMappings) {
			console.log(exceptionMappings);
		}, function () {
			// $log.info('Modal dismissed at: ' + new Date());
		});
	}
	*/
	// Web Trigger
	$scope.showWebTrigger = function (applicationSid) {
		webTriggerService.showModal(applicationSid);
	}


	// Application logging
	$scope.showProjectSettings = function (applicationSid, projectName) {
		projectSettingsService.showModal(applicationSid, projectName);
	}

	// Run the following after all initialization are complete

	//console.log( "opening project " + $scope.projectName);
	//$scope.openProject( $scope.projectName );


		 // UNSORTED
		 // -------------

	$scope.makeFullScreen = function(elementId) {
		var elem = $('#' + elementId)[0];
		if (elem.requestFullscreen) {
		  elem.requestFullscreen();
		} else if (elem.msRequestFullscreen) {
		  elem.msRequestFullscreen();
		} else if (elem.mozRequestFullScreen) {
		  elem.mozRequestFullScreen();
		} else if (elem.webkitRequestFullscreen) {
		  elem.webkitRequestFullscreen();
		}
	};

	$scope.drawGraph = function() {
		$('#paper').empty();
		var graph = new joint.dia.Graph();

		var state = $scope.getAllTargets();

		var adjacencyList = {};

		var paper = new joint.dia.Paper({
			el: $('#paper'),
			//width: 750,
			//height: 200,
			gridSize: 1,
			model: graph,
			interactive: false,
		});

		getNexts = function (nm, module) {
			if(!adjacencyList[nm]) {
				adjacencyList[nm] = [];
			}
			getRealNexts(nm, module);
		};

		getRealNexts = function (nm, module) {
			if(!module) {
				return;
			}
			Object.keys(module).forEach(function (d) {
				if((d === 'next' && module.next !== '') || (d === 'nextModule' && module.nextModule !== '')) {
					var transit = {'name': labels[module.next || module.nextModule]};
					if(module.dialNouns) {
						transit.link = 'Dial ';
						for (var i = 0; i < module.dialNouns.length; i++) {
							transit.link += (module.dialNouns[i].destination + ',\n');
						}
						transit.link = transit.link.substr(0, transit.link.length-2);
					}
					if(module.digits) {
						transit.link = ("Press '" + module.digits + "'");
					}
					if(module.collectVariable) {
						transit.link = ("'" + module.collectVariable + "' = <collect>");
					}
					if(module.value) {
						transit.link = ("ES Result = '" + module.value + "'");
					}

					adjacencyList[nm].push(transit);
					if(labels[module[d]] && !adjacencyList[labels[module[d]]]) {
						adjacencyList[labels[module[d]]] = [];
					}
				}
				if(typeof module[d] === 'object')
					getRealNexts(nm, module[d]);
			});
		};

		// maps module name -> label
		var labels = {};

		// fills labels map
		state.forEach(function(link) {
			labels[link.name] = link.name;
		});

		// fills adjancencyList
		state.forEach(function(link) {
			link.steps.forEach(function(step) {
				getNexts(link.name, step);
			});
		});

		buildGraphFromAdjacencyList = function (adjacencyList) {

			var elements = [];
			var links = [];

			_.each(adjacencyList, function(edges, parentElementLabel) {
				elements.push(makeElement(parentElementLabel.hasOwnProperty("name") ? parentElementLabel.name : parentElementLabel));

				_.each(edges, function(childElementLabel) {
					if(childElementLabel.name)
						links.push(makeLink((parentElementLabel.hasOwnProperty("name") ? parentElementLabel.name : parentElementLabel), (childElementLabel.hasOwnProperty("name") ? childElementLabel.name : childElementLabel), childElementLabel.hasOwnProperty("link") ? childElementLabel.link : ""));
				});
			});

			// Links must be added after all the elements. This is because when the links
			// are added to the graph, link source/target elements must be in the graph already.
			return elements.concat(links);
		};

		makeLink = function (parentElementLabel, childElementLabel, linkLabel) {

			var lnk = new joint.dia.Link({
				source: { id: parentElementLabel },
				target: { id: childElementLabel },
				attrs: {
					'.connection': { stroke: '#428BCA', 'stroke-width': 2 },
					'.marker-target': { d: 'M 6 0 L 0 3 L 6 6 z' }
				},
				smooth: false,
			});
			lnk.label(0, {
				position: 0.5,
				attrs: {
					rect: { fill: '#FFF' },
					text: { fill: '#333', text: linkLabel, 'font-size': 10, 'font-family': 'Monaco', 'font-weight': 'bold' }
				}
			});
			return lnk;
		};

		makeElement = function (name) {
			// from the module name retrieve all the module (node) details
			var node = nodeRegistry.getNode(name);
			var label = node.label;

			var maxLineLength = _.max(label.split('\n'), function(l) { return l.length; }).length;

			// Compute width/height of the rectangle based on the number
			// of lines in the label and the letter size. 0.6 * letterSize is
			// an approximation of the monospace font letter width.
			var letterSize = 12;
			var width = 1.75 * (letterSize * (0.4 * maxLineLength + 1));
			var height = 1.4 * ((label.split('\n').length + 1) * letterSize);

			var rect = new joint.shapes.basic.Rect({
				id: name,
				size: { width: width, height: height },
				attrs: {
					text: { text: label.toUpperCase(), 'font-size': letterSize, 'font-family': 'Monaco', dy: 3 },
					rect: {
						fill: '#FFBD69',
						width: width, height: height,
						rx: 5, ry: 5,
						'stroke-width': 2, stroke: 'black'
					}
				}
			});

			return rect;
		};

		layout = function () {
			var cells = buildGraphFromAdjacencyList(adjacencyList);
			graph.resetCells(cells);
			joint.layout.DirectedGraph.layout(graph, { setLinkVertices: true });
			paper.fitToContent();
			paper.scale(0.97, 0.97);
			paper.setOrigin(1, 1);

			var oldWidth = $('#paper').children().width();
			var newWidth = Math.max(oldWidth, $('#paper').width());
			if (newWidth > oldWidth) {
				var translateX = (newWidth - oldWidth) / 2;
				paper.setOrigin(translateX, 1);
			}
			paper.setDimensions(newWidth);

			paper.on('cell:pointerdblclick', function(cell, evt, x, y) {
				$scope.$apply( function () {
					$scope.editFilteredNodes($scope.getAllTargets(), cell.model.id);
					// FIXME: For AngularJS 1.4, just use $anchorScroll('es');
					$("body").animate({scrollTop: $("#es").offset().top}, "slow");
				});
			});
		};

		layout();

	};


});

angular.module('Rvd').service('designerService', ['stepRegistry', '$q', '$http', 'stepPacker', 'ModelBuilder', 'nodeRegistry', 'editedNodes', function (stepRegistry, $q, $http, stepPacker, ModelBuilder, nodeRegistry, editedNodes) {
	var service = {};

	function openProject(name) {
		var deferred = $q.defer();

		editedNodes.clear();
		nodeRegistry.clear();


		$http({url: 'services/projects/' + name,
			method: "GET"
		})
		.success(function (data, status, headers, config) {
			var project = {};
			project.projectName = name;
			unpackState(project, data);
			if ( project.projectKind == 'voice' ) {
				getWavList(name).then(function (wavList) {
					project.wavList = wavList;
					deferred.resolve(project);
				}, function (error) {
					deferred.reject(error);
				});
			} else {
				deferred.resolve(project);
			}
			editedNodes.addEditedNode(project.startNodeName); // add a module tab
			editedNodes.setActiveNode(project.startNodeName); // give focus to this tab


			// maybe override .error() also to display a message?
		 }).error(function (data, status, headers, config) {
		    if (status == 404) {
		      deferred.reject("ProjectNotFound");
		    } else {
		      // TODO fix this generic handling
			  deferred.reject("IncompatibleProjectVersion");
			}
			 //if ( data.serverError && (data.serverError.className == 'IncompatibleProjectVersion') )
			//	 $location.path("/upgrade/" + name)
			// else
			//	 $scope.projectError = data.serverError;
		 });
		return deferred.promise;
	}

	function unpackState(project, packedState) {
		nodeRegistry.reset(packedState.lastNodeId);
		stepRegistry.reset(packedState.lastStepId);
		for ( var i=0; i < packedState.nodes.length; i++) {
			//var node = ModelBuilder.build( packedState.nodes[i].kind+"Node" ).init( packedState.nodes[i] );
			var node = ModelBuilder.build( "nodeModel" ).init( packedState.nodes[i] );
			nodeRegistry.addNode(node);
		}
		//project.nodes = packedState.nodes;
		//project.lastNodesId = packedState.lastNodeId;
		project.startNodeName = packedState.header.startNodeName;
		project.projectKind = packedState.header.projectKind;
		project.version = packedState.header.version;
		//project.exceptionHandlingInfo = ModelBuilder.build('ExceptionHandlingInfo').init(packedState.exceptionHadlingInfo);
	}

	function packState(project) {
		var state = {header:{}, iface:{}};
		// state.lastStepId = stepService.lastStepId;
		state.lastStepId = stepRegistry.current();
		var registry_nodes = nodeRegistry.getNodes();
		state.nodes = angular.copy(registry_nodes);
		for ( var i=0; i < state.nodes.length; i++) {
			var node = state.nodes[i];
			for (var j=0; j<node.steps.length; j++) {
				var step = registry_nodes[i].steps[j];
                var packedStep;
				if (step.kind == "control") {
				    packedStep = step; // no packing for 'control' elements. TODO remove packing alltogether after porting all elements to angular directives
				} else {
                    packedStep = step.pack();
				}
                node.steps[j] = packedStep;
			}
		}
		//state.iface.activeNode = $scope.activeNode;
		state.lastNodeId = nodeRegistry.lastNodeId;
		state.header.startNodeName = project.startNodeName; //$scope.nodeNamed( $scope.startNodeName ) == null ? null : $scope.nodeNamed( $scope.startNodeName ).name;
		state.header.projectKind = project.projectKind;
		state.header.version = project.version;
		//state.exceptionHandlingInfo = $scope.exceptionHandlingInfo.pack();

		return state;
	}

	function getWavList(applicationSid) {
		var deferred = $q.defer();
		$http({url: 'services/projects/'+ applicationSid + '/wavs' , method: "GET"})
		.success(function (data, status, headers, config) {
			//$scope.wavList = data;
			deferred.resolve(data);
		})
		.error(function (data, status, headers, config) {
			deferred.reject("wav-error");
		});
		return deferred.promise;
	}

	function saveProject(applicationSid,project) {
		var deferred = $q.defer();

		var state = packState(project);
		$http({url: 'services/projects/'+ applicationSid,
				method: "POST",
				data: state,
				headers: {'Content-Type': 'application/data'}
		})
		.success(function (data, status, headers, config) {
			if (data.rvdStatus == 'OK' )
				deferred.resolve('Project saved');
			else
				deferred.reject(data);
		 }).error(function (data, status, headers, config) {
			 deferred.reject(data);
		 });

		return deferred.promise;
	}

	function buildProject(applicationSid) {
		var deferred = $q.defer();

		$http({url: 'services/projects/' + applicationSid + '/build', method: "POST"})
		.success(function (data, status, headers, config) {
			deferred.resolve('Build successfull');
		 }).error(function (data, status, headers, config) {
			 deferred.reject('buildError');
		 });

		return deferred.promise;
	}

	function getBundledWavs() {
		var deferred = $q.defer();
		$http({url: 'services/designer/bundledWavs', method: "GET"})
		.success(function (data, status, headers, config) {
			deferred.resolve(data.payload);
		 }).error(function (data, status, headers, config) {
			 deferred.reject('Error fetching designer bundled wavs');
		 });
		return deferred.promise;
	}

	service.openProject = openProject;
	service.getWavList = getWavList;
	service.saveProject = saveProject;
	service.buildProject = buildProject;
	service.getBundledWavs = getBundledWavs;

	return service;

}]);

/*
    Contains step management operations for a specific module
*/
angular.module('Rvd').controller("nodeController",["$scope", "nodeRegistry", function ($scope, nodeRegistry) {
		$scope.node = nodeRegistry.getNode($scope.nodeSummary.name);
		// handle step removel notifications from individual steps (only control-step supports it for now)
		$scope.$on("step-removed", function (event, step) {
		    var steps = event.currentScope.node.steps;
            steps.splice( steps.indexOf(step), 1);
		});
}]);

angular.module('Rvd').controller("nodeTabController",["$scope", "nodeRegistry", function ($scope, nodeRegistry) {
		$scope.node = nodeRegistry.getNode($scope.nodeSummary.name);
}]);

angular.module('Rvd').filter('filterNodesByLabel', function () {
	return function (items, token, moduleFilterEnabled) {
		if ( !token || !moduleFilterEnabled )
			return items;

		var filtered = [];
		var r = new RegExp(token,"i")
		for (var i = 0; i < items.length; i++) {
			var item = items[i];
			if ( item.label.search(r) != -1 )
			filtered.push(item);
		}
		return filtered;
	};
});

