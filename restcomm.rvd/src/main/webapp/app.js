var App = angular.module('Rvd', ['ngRoute','ngDragDrop','ui.bootstrap','ui.bootstrap.collapse', 'ui.bootstrap.dialog','ui.sortable','angularSpinner']);

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


App.factory('stepService', function($rootScope) {
	var stepService = {
		serviceName: 'stepService',
		stepProto: {
					say: {kind:'say', label:'say', title:'say', phrase:'', voice:'man', language:'bf', loop:1, isCollapsed:false, iface:{optionsVisible:false}},
					play: {playType:'remote', kind:'play', label:'play', title:'play', wavUrl:'', wavLocalFilename:'', loop:null, isCollapsed:false},
					gather: {kind:'gather', label:'gather', title:'collect', name:'', action:'', method:'GET', timeout:null, finishOnKey:'', numDigits:null, steps:{}, stepnames:[], isCollapsed:false, customHandlerSrc:'', next:'', mappings:[] /*{digits:1, next:"welcome.step1"}*/, collectVariable:'', gatherType:"menu", iface:{advancedView:false,optionsVisible:false}},
					dial: {dialType:'number',number:'',client:'',conference:'',sipuri:'',kind:'dial',kind:'dial', label:'dial', title:'dial',action:'', method:'POST', timeout:30, timeLimit:14400, callerId:'', steps:[], isCollapsed:false},
					number: {kind:'number', label:'number', title:'Number', numberToCall:'', sendDigits:'', numberUrl:''},
					redirect: {kind:'redirect', label:'redirect', title:'redirect', next:''},
					hungup: {kind:'hungup', label:'hang up', title:'hang up', next:''},
		},
		stepNames: ['say','gather','dial','redirect','hungup'],
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
            		if ( $(element).val() =="" )
            			scope.$eval(attrs.ngModel + " = ''");
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
						//revert: "invalid"
					});
					/*el.disableSelection(); */
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


