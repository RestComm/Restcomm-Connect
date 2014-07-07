var App = angular.module('Rvd', ['angularFileUpload','ngRoute','ngDragDrop','ui.bootstrap','ui.bootstrap.collapse','ui.bootstrap.popover','ui.sortable' ,'angularSpinner','basicDragdrop']);

App.config([ '$routeProvider', function($routeProvider) {
	
	$routeProvider.when('/project-manager/:projectKind', {
		templateUrl : 'templates/projectManager.html',
		controller : 'projectManagerCtrl'
	})
	.when('/home', {
		templateUrl : 'templates/home.html',
		controller : 'homeCtrl'
	})
	.when('/designer/:projectName', {
		templateUrl : 'templates/designer.html',
		controller : 'designerCtrl'
	})
	.when('/upgrade/:projectName', {
		templateUrl : 'templates/upgrade.html',
		controller : 'upgradeCtrl'
	})	
	.otherwise({
		redirectTo : '/home'
	});

} ]);



App.factory('stepService', ['protos', function(protos) {
	var stepService = {
		serviceName: 'stepService',
		lastStepId: 0,
			 
		newStepName: function () {
			return 'step' + (++this.lastStepId);
		}		 
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
		nodes: {
				voice: {kind:'voice', name:'module', label:'Untitled module', steps:[], iface:{edited:false,editLabel:false}},
				ussd: {kind:'ussd', name:'module', label:'Untitled module', steps:[], iface:{edited:false,editLabel:false}},		
				sms: {kind:'sms', name:'module', label:'Untitled module', steps:[], iface:{edited:false,editLabel:false}},
		},
		accessOperationProtos: accessOperationProtos,
		stepProto: {
			// Voice
			say: {kind:'say', label:'say', title:'say', phrase:'', voice:undefined, language:undefined, loop:undefined, iface:{}},
			play: {kind:'play', label:'play', title:'play',loop:undefined,playType:'local',local:{wavLocalFilename:''}, remote:{wavUrl:''}, iface:{}},
			gather: {kind:'gather', label:'gather', title:'collect', action:undefined, method:'GET', timeout:undefined, finishOnKey:undefined, numDigits:undefined, steps:[], gatherType:"menu", menu:{mappings:[] /*{digits:1, next:"welcome.step1"}*/,}, collectdigits:{collectVariable:'',next:'', scope:"module"}, iface:{}},
			dial: {dialNouns:[], nextModule:undefined, kind:'dial',kind:'dial', label:'dial', title:'dial',action:undefined, method:undefined, timeout:undefined, timeLimit:undefined, callerId:undefined, iface:{}, record:undefined},
			number: {kind:'number', label:'number', title:'Number', numberToCall:'', sendDigits:'', numberUrl:'', iface:{}},
			redirect: {kind:'redirect', label:'redirect', title:'redirect', url:null,method:null,iface:{}},
			hungup: {kind:'hungup', label:'hang up', title:'hang up',iface:{}},
			externalService: {kind:'externalService', label:'externalService', title:'external service', url:'', urlParams:[], assignments:[], next:'', doRouting:false, nextType:'fixed', nextValueExtractor:{accessOperations:[], lastOperation: angular.copy(accessOperationProtos.object) }, iface:{}},
			reject: {kind:'reject', label:'reject', title:'reject', reason:undefined,iface:{}},
			pause: {kind:'pause', label:'pause', title:'pause', length:undefined, iface:{}},
			sms: {kind:'sms', label:'sms', title:'sms', text:'', to:undefined, from:undefined, statusCallback:undefined,next:null,iface:{}},
			record: {kind:'record', label:'record', title:'record', next:null, method:'GET', timeout:undefined, finishOnKey:undefined, maxLength:undefined, transcribe:undefined, transcribeCallback:undefined, playBeep:undefined, iface:{}},
			fax: {kind:'fax', label:'fax', title:'fax', to:undefined, from:undefined, text:'', next:null,statusCallback:undefined,iface:{}},
			// USSD
			ussdSay: {kind:'ussdSay', label:'USSD Message', title:'USSD Message', text:'', language:null,iface:{}},
			ussdCollect: {kind:'ussdCollect', label:'USSD Collect', title:'USSD Collect', gatherType:"menu", menu: {mappings:[]}, collectdigits:{collectVariable:null,next:'',scope:"module"}, text:'', language:null, messages:[], iface:{}},
			ussdSayNested: {text:''},
			ussdLanguage: {kind:'ussdLanguage', label:'Language', title:'Language', language:null, iface:{}},
			
			
		},
		dialNounProto: {
			number: {dialType: 'number', destination:'', sendDigits:undefined, beforeConnectModule:undefined},
			client: {dialType: 'client', destination:''},
			conference: {dialType: 'conference', destination:'', nextModule:undefined, muted:undefined, beep:undefined, startConferenceOnEnter:undefined, endConferenceOnExit:undefined, waitUrl:undefined, waitModule:undefined, waitMethod:undefined, maxParticipants:undefined},
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

/*
 * Newer version of syncModel that reset model to undefined instead of null.
 */
App.directive("syncModules", function(){

        return {
            restrict: 'A',
            link: function(scope, element, attrs, controller) {
            	scope.$on("refreshTargetDropdowns", function () {
            		//console.log( 'element ' + element + ' received refreshTargetDropdowns');
            		//console.log( 'selected value: ' + $(element).val() )
            		if ( $(element).val() ==="" )
            			scope.$eval(attrs.ngModel + " = undefined");
            	});            	
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

// Make field undefined if it is empty string or null
App.directive('autoClear', [function() {
    return {
      require: 'ngModel',
      link: function(scope, elm, attr, ctrl) {
        ctrl.$parsers.unshift(function(value) {
          return (value === '' || value === null) ? undefined : value;
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
				//console.log("adding operation");
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
			
			scope.resetOperation = function( operation ) {
				angular.copy(protos.accessOperationProtos[operation.kind], operation);
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

App.directive('ussdModule', [function () {
	return {
		restrict: 'A',
		link: function (scope,el,attrs) {
			scope.node.iface.remainingChars = scope.remainingUssdChars(scope.node);
			//console.log("(start) remaining chars: " + scope.node.iface.remainingChars);			
			/*var counterWatch = */scope.$watch('remainingUssdChars(node)', function (newCount) {
				scope.node.iface.remainingChars = newCount;
			});
			
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


