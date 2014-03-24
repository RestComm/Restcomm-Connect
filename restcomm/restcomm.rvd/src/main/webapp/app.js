var App = angular.module('Rvd', ['angularFileUpload','ngRoute','ngDragDrop','ui.bootstrap','ui.bootstrap.collapse','ui.bootstrap.popover','ui.sortable' ,'angularSpinner','basicDragdrop']);

App.config([ '$routeProvider', function($routeProvider) {
	
	$routeProvider.when('/project-manager', {
		templateUrl : 'templates/project-manager.html',
		controller : 'projectManagerCtrl'
	}).when('/designer/:projectName', {
		templateUrl : 'templates/designer.html',
		controller : 'designerCtrl'
	}).otherwise({
		redirectTo : '/project-manager'
	});

} ]);



App.factory('stepService', ['protos', function(protos) {
	//console.log("protos");
	//console.log( protos);
	var stepService = {
		serviceName: 'stepService',
		stepNames: ['say','gather','dial','redirect','hungup','externalService'],
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
			var newstep = angular.copy(protos.stepProto[kind])
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
}]);

App.factory('protos', function () {
	var accessOperationProtos = {
			object:{kind:'object',fixed:false, terminal:false},
			array:{kind:'array',fixed:false, terminal:false},
			value:{kind:'value',fixed:false, terminal:true},	
	}
	return { 
		accessOperationProtos: accessOperationProtos,
		stepProto: {
			// Voice
			say: {kind:'say', label:'say', title:'say', phrase:'', voice:null, language:null, loop:null, isCollapsed:false, iface:{optionsVisible:false}},
			play: {playType:'local', kind:'play', label:'play', title:'play', wavUrl:null, wavLocalFilename:null, loop:null, isCollapsed:false},
			gather: {kind:'gather', label:'gather', title:'collect', name:'', action:'', method:'GET', timeout:null, finishOnKey:'', numDigits:null, steps:{}, stepnames:[], isCollapsed:false, customHandlerSrc:'', next:'', mappings:[] /*{digits:1, next:"welcome.step1"}*/, collectVariable:'', gatherType:"menu", iface:{advancedView:false,optionsVisible:false}},
			dial: {dialNouns:[], nextModule:'', kind:'dial',kind:'dial', label:'dial', title:'dial',action:'', method:'GET', timeout:null, timeLimit:null, callerId:null, steps:[], isCollapsed:false},
			number: {kind:'number', label:'number', title:'Number', numberToCall:'', sendDigits:'', numberUrl:''},
			redirect: {kind:'redirect', label:'redirect', title:'redirect', url:'',method:''},
			hungup: {kind:'hungup', label:'hang up', title:'hang up', next:''},
			externalService: {kind:'externalService', label:'externalService', title:'external service', url:'', urlParams:[], assignments:[], next:'', doRouting:false, nextType:'fixed', nextVariable:'', nextValueExtractor:{accessOperations:[], lastOperation: angular.copy(accessOperationProtos.object) }, chosenAssignmentsModule:null},
			reject: {kind:'reject', label:'reject', title:'reject', reason:''},
			pause: {kind:'pause', label:'pause', title:'pause', length:null},
			sms: {kind:'sms', label:'sms', title:'sms', text:'', to:null, from:null, statusCallback:null,method:'GET', next:''},
			record: {kind:'record', label:'record', title:'record', next:'', method:'GET', timeout:null, finishOnKey:null, maxLength:null, transcribe:null, transcribeCallback:null, playBeep:true, iface:{optionsVisible:false}},
			fax: {kind:'fax', label:'fax', title:'fax', to:null, from:null, text:'', next:'', method:'GET', statusCallback:null},
			// USSD
			ussdSay: {kind:'ussdSay', label:'USSD Say', title:'USSD Say', text:'', language:null},
			ussdCollect: {kind:'ussdCollect', label:'USSD Collect', title:'USSD Collect', gatherType:"menu", text:'',mappings:[], collectVariable: null, next:null, language:null, messages:[]},
			ussdLanguage: {kind:'ussdLanguage', label:'Language', title:'Language', language:null},
			
			
		},
		dialNounProto: {
			number: {dialType: 'number', destination:'', sendDigits:null, beforeConnectUrl:'', beforeConnectModule:null},
			client: {dialType: 'client', destination:''},
			conference: {dialType: 'conference', destination:'', nextModule:null, muted:null, beep:null, startConferenceOnEnter:null, endConferenceOnExit:null, waitUrl:null, waitModule:null, waitMethod:'GET', maxParticipants:null},
			sipuri: {dialType: 'sipuri', destination:''},
		}
	};
});


/*
 * Used in <select/> elements. Clears the select element model when the selection 
 * option has been removed. It works together with a separate mechanism that broadcasts 
 * the appropriate event (refreshTargetDropdowns) when the option is removed.   
 */
App.directive("syncModel", function(){

        return {
            restrict: 'A',
            link: function(scope, element, attrs, controller) {
            	scope.$on("refreshTargetDropdowns", function () {
            		//console.log( 'element ' + element + ' received refreshTargetDropdowns');
            		//console.log( 'selected value: ' + $(element).val() )
            		if ( $(element).val() ==="" )
            			scope.$eval(attrs.ngModel + " = null");
            	});            	
            }
        }
});


App.directive('sortableSteps',function(stepService){
  return {
	  scope: true,	  
	  
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
			placeholder: 'sortable-placeholder'
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
			  //console.log( 'inserting element from position: ' + from_index );
			  //console.log( 'inserting element at position: ' + to_index );
			  var temp = scope.stepnames[to_index];
			  scope.$apply( function () {
				scope.stepnames[to_index] = scope.stepnames[from_index];
				scope.stepnames[from_index] = temp;
				scope.orderedSteps = getMapValuesByIndex(scope.steps, scope.stepnames);
			})
		  }
		  
		  if ( $(this).hasClass('nested') )
			event.stopImmediatePropagation();
      } );
    }
  }
});


App.directive('myDraggable',function(){
  
  return 	{
				link:function(scope,el,attrs){
					el.draggable({
						connectToSortable: attrs.myDraggable,
						helper: "clone",
						revert: "invalid"
					});
					/*el.disableSelection(); */
				}
			}
  
});


App.directive('nullIfEmpty', [function() {
    return {
      require: 'ngModel',
      link: function(scope, elm, attr, ctrl) {
        ctrl.$parsers.unshift(function(value) {
          return value === '' ? null : value;
        });
      }
    };
  }]
);

App.directive('valueExtractor', ['protos', function (protos) {
	return {
		restrict: 'E',
		templateUrl: 'templates/directive/valueExtractor.html',
		scope: {
			extractorModel: '='
		},
		link: function(scope,el,attrs) {
			//scope.extractorModel = {accessOperations:[], lastOperation: angular.copy(protos.accessOperationProtos.object) }
			scope.accessOperationKinds = ['object', 'array', 'value'];
			scope.objectActions = ['propertyNamed'];
			scope.arrayActions = ['itemAtPosition'];
			
			scope.addOperation = function (extractorModel) {
				console.log("adding operation");
				extractorModel.lastOperation.fixed = true;
				extractorModel.lastOperation.expression = scope.operationExpression( extractorModel.lastOperation );
				extractorModel.accessOperations.push(extractorModel.lastOperation);
				extractorModel.lastOperation = angular.copy(protos.accessOperationProtos.object)
			}
			scope.doneAddingOperations = function (extractorModel) {
				scope.addOperation(extractorModel);
				extractorModel.lastOperation = null;
			}
			
			scope.popOperation = function (extractorModel) { // removes last operation
				if ( extractorModel.accessOperations.length > 0 ) {
					extractorModel.lastOperation = extractorModel.accessOperations.pop();
					extractorModel.lastOperation.fixed = false;
				}
			}
			
			scope.operationExpression = function (operation) {
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
				case 'value':
					return " value";
				break;
				/*case 'float':
					return " get Float value";
				break;	
				case 'boolean':
					return " get Boolean value";
				break;*/		
				}
				return "UNKNOWN";
			}
			
			scope.extractorModelExpression = function (extractorModel) {
				var expr = '';
				for ( var i=0; i < extractorModel.accessOperations.length; i++ ) {
					expr += scope.operationExpression(extractorModel.accessOperations[i]);
				} 
				return expr;
			}
			
			scope.isTerminal = function (kind) {
				if (kind == null)
					return false;
				return protos.accessOperationProtos[kind].terminal;
			}

		}
	}
}]);

App.directive('modulePicker', [function () {
	return {
		restrict: 'E',
		templateUrl: 'templates/directive/modulePicker.html',
		scope: {
			options: '=',
		},
		link: function (scope,el,attrs) {
		},
	};
}]);

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


//use it this way: <input type="text" ng-focus="isFocused" ng-focus-lost="loseFocus()">
//for more information: http://stackoverflow.com/questions/14859266/input-autofocus-attribute/14859639#14859639 

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


