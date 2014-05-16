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

App.factory( 'dragService', [function () {
	var dragInfo;
	var dragId = 0;
	var pDragActive = false;
	var serviceInstance = {
		newDrag: function (model) {
			dragId ++;
			pDragActive = true;
			if ( typeof(model) === 'object' )
				dragInfo = { id : dragId, model : model };
			else
				dragInfo = { id : dragId, class : model };
				
			return dragId;
		},
		popDrag:  function () {
			if ( pDragActive ) {
				var dragInfoCopy = angular.copy(dragInfo);
				pDragActive = false;
				return dragInfoCopy;
			}
		},
		dragActive: function () {
			return pDragActive; 
		}
		
	};
	return serviceInstance;
}]);

App.factory('protos', function () {
	var protoInstance = { 
		nodes: {
				voice: {kind:'voice', name:'module', label:'Untitled module', steps:[], iface:{edited:false,editLabel:false}},
				ussd: {kind:'ussd', name:'module', label:'Untitled module', steps:[], iface:{edited:false,editLabel:false}},		
		},
	};
	return protoInstance;
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

App.directive('valueExtractor', ['protos','accessOperationKinds','objectActions','arrayActions', function (protos,accessOperationKinds,objectActions,arrayActions) {
	return {
		restrict: 'E',
		templateUrl: 'templates/directive/valueExtractor.html',
		scope: {
			extractorModel: '='
		},
		link: function(scope,el,attrs) {
			scope.accessOperationKinds = accessOperationKinds; //['object', 'array', 'value'];
			scope.objectActions = objectActions; //['propertyNamed'];
			scope.arrayActions = arrayActions; //['itemAtPosition'];
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

/*
 * Adds to scope: buttonOptions, selectedOption, addedClasses
 */
App.directive('multibutton', function () {
	return  {
		restrict: 'E',
		scope:true,
		templateUrl: 'templates/directive/multibutton.html',
		link: function (scope,element,attrs) {
			scope.buttonOptions = scope.$eval(attrs.options);
			if (scope.buttonOptions.length > 0 )
				scope.selectedOption = scope.buttonOptions[0];
			else
				scope.selectedOption = "";
			
			scope.addedClasses = attrs.buttonClass;
		},
		controller: function($scope) {
			$scope.selectOption = function(option) {
				$scope.selectedOption = option;
			}
		}
	}
});

App.directive('inputGroupSelect', function () {
	return  {
		restrict: 'E',
		replace: true,
		scope:true,
		templateUrl: 'templates/directive/inputGroupSelect.html',
		require: 'ngModel',
		link: function (scope,element,attrs,ctrl) {
			
			scope.selectOption = function(option) {
				scope.selectedOption = option;
				ctrl.$setViewValue(option);
			}
			
			ctrl.$render = function() {
				scope.selectedOption = ctrl.$viewValue;
			};
			
			scope.buttonOptions = scope.$eval(attrs.options);
			if (scope.buttonOptions.length > 0 )
				scope.selectedOption = scope.buttonOptions[0];
			else
				scope.selectedOption = "";
			scope.addedClasses = attrs.buttonClass;
			scope.menuClasses = attrs.menuClass;
		}
	}
});

