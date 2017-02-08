
angular.module('Rvd').directive('stepHeading', function () {
	return {
		restrict: 'E',
		transclude:true,
		templateUrl: 'templates/steps/stepHeading.html'
	}
});

angular.module('Rvd').directive('directiveStepHeading', function () {
	return {
		restrict: 'E',
		transclude:true,
		templateUrl: 'templates/steps/directiveStepHeading.html'
	}
});

/*
 * Usage:
 * 
 * <module-picker ng-model='step.exceptionNext'></module-picker>
 * 
 * Notes
 * 
 *  - A new inherited scope is created. The moduleVar variable is set in this scope.
 *  - The 'required' attribute is also propagated to the internal select
 */
angular.module('Rvd').directive('modulePicker', [function () {
	return {
		restrict: 'E',
		require: '?ngModel',
		templateUrl: 'templates/directive/modulePicker.html',
		scope: true,
		
		compile: function compile(element, attrs) {
			if ( attrs.hasOwnProperty('required' ) )
				$(element).find("select").attr("required","required")
			
			// this is the link function
			return function (scope,el,attrs,ngModel) {			
				ngModel.$render = function () {
					scope.moduleVar = ngModel.$viewValue;
					//console.log('internal model updated');
				}
				
				scope.$watch('moduleVar', function (newValue, oldValue) {
					ngModel.$setViewValue(newValue);
					//console.log('internal model has changed. Change propagates to external model');
				});
			}
		}
	};
}]);

angular.module('Rvd').directive('rvdVariable', ['variableRegistry', function (variableRegistry) {
	return {
		restrict: 'A',
		link: function (scope, element, attrs) {
			var varInfo = {};
			varInfo.id = variableRegistry.newId();
			//var varNameExpression = scope.$eval(attrs.rvdVariable);
			scope.$watch(attrs.rvdVariable, function (newValue, oldValue) {
				varInfo.name = newValue;
			});
			element.on('$destroy', function () {
				variableRegistry.removeVariable(varInfo);
			});
			variableRegistry.addVariable(varInfo);
		}
	}
}]);

angular.module('Rvd').directive('lookupContext', [function () {
	return {
		restrict: 'A',
		scope: true,
		link: function (scope) {
			scope.$on("variable-name-clicked", function (event,args) {
				event.stopPropagation();
				scope.$broadcast("inject-variable", args);
			})
		},
		controller: function ($scope) {}
	}
}]);

angular.module('Rvd').directive('lookupTarget', [function () {
	return {
		restrict: 'A',
		//scope:true,
		require: "^lookupContext",
		link: function (scope, element, attrs, ctrls) {
			scope.$on("inject-variable", function (event, args) {
				if (args.replace) {
                    scope.$eval(attrs.ngModel + "='"+"$"+args.value+"'");
				} else {
                    if ( element[0].selectionStart >= 0 ) {
                        var selStart = element[0].selectionStart;
                        var value = scope.$eval(attrs.ngModel) || "";
                        value = value.substring(0,selStart) + "$"+args.value+ (value.substring(selStart) == "" ? "" : ("" + value.substring(selStart)));
                        // escape single quotes
                        value = value.replace(/'/g,"\\\'");
                        scope.$eval(attrs.ngModel + "='"+value+"'");
                    }
                    var selStart = element[0].selectionStart;
                    var selEnd = element[0].selectionEnd;
				}
			});
		} 
	}
}]);


angular.module('Rvd').directive('variableLookup', ['variableRegistry', function (variableRegistry) {
	return {
		restrict: 'E',
		scope: true,
		replace: true,
		require: "^lookupContext",
		templateUrl: 'templates/directive/variableLookup.html',
		link: function (scope, element, attrs, lookupContextCtrl) {
			scope.view = attrs.view;
			scope.variables = variableRegistry.listAll();
			scope.selectVariable = function (variable) {
				scope.$emit("variable-name-clicked", {value: variable.name, replace: scope.$eval(attrs.replace)});
			}
		} 
	}
}]);


//use it this way: <input type="text" ng-focus="isFocused" ng-focus-lost="loseFocus()">
//for more information: http://stackoverflow.com/questions/14859266/input-autofocus-attribute/14859639#14859639 

angular.module('Rvd').directive('ngFocus', function($timeout) {
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


angular.module('Rvd').directive('inputGroupSelect', function () {
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

angular.module('Rvd').directive('rvdPanel', function () {
	return {
		transclude: true,
		restrict: 'E',
		scope: {
			title:'=panelTitle',
			//title:'=title',
			closePanel:'&onClose',
		},
		templateUrl: 'templates/directive/rvdPanel.html',
		link: function (scope,element,attrs) {
			console.log("create a new panel");
			//scope.panel = {title: 'Untitled'};
		}
	}
});


/*
 * Used in <select/> elements. Clears the select element model when the selection 
 * option has been removed. It works together with a separate mechanism that broadcasts 
 * the appropriate event (refreshTargetDropdowns) when the option is removed.   
 */
angular.module('Rvd').directive("syncModel", function(){

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
angular.module('Rvd').directive("syncModules", function(){

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


angular.module('Rvd').directive('nullIfEmpty', [function() {
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
angular.module('Rvd').directive('autoClear', [function() {
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

angular.module('Rvd').directive('valueExtractor', ['accessOperationKinds','objectActions','arrayActions', function (accessOperationKinds,objectActions,arrayActions) {
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


angular.module('Rvd').directive('ussdModule', [function () {
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

angular.module('Rvd').directive('rvdDropdown', function() {
	return {
		scope:{
		    model:'='
		},
		replace:true,
		restrict: 'E',
		templateUrl: 'templates/directive/rvdDropdown.html',
		link: function (scope,element,attrs) {
		    function getOptionByValue(list,value) {
		        for (var i=0; i<list.length; i++) {
		            if (list[i].value == value)
		                return list[i];
		        }
		        return null; // not found
		    }
		    scope.setActiveOption = function(option) {
		        scope.selectedOption = option;
		        scope.model = option.value;
		    }

		    scope.allOptions = []; //{"key":"null label","value":null}];
            scope.options = scope.$eval(attrs.options);
            if (!!scope.options && scope.options.length > 0)
                scope.allOptions = scope.allOptions.concat(scope.options);
            else
                scope.allOptions = [{"key":"null label","value":null}];
            console.log(scope.allOptions);
		    var selectedOption = getOptionByValue(scope.allOptions, scope.model);
		    if (selectedOption == null)
		        selectedOption = scope.allOptions[0];
		    scope.setActiveOption(selectedOption);
		}
	}
});

angular.module('Rvd').directive('vardef', function () {
    return {
        restrict: "E",
        scope: {
            varExpression: '=variable',
            varScope: '=variableScope'
        },
        templateUrl: "templates/directive/vardef.html",
        link: function (scope) {
            scope.setVarScope = function(varscope) {
                scope.varScope = varscope;
            }
        }
    }
})
.directive('typeChooser', function () {
    return {
        restroct: "E",
        scope: {
            type: '=model'
        },
        templateUrl: "templates/directive/typeChooser.html",
        link: function (scope) {
            scope.setType = function (newtype) {
                scope.type = newtype;
            }
        }
    }
})
;


