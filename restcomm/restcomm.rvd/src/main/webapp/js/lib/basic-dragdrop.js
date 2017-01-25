angular.module('basicDragdrop', [])
.directive('basicSortable', function() {
    return {
        restrict: 'A',
        scope: true,
        /*{
            listModel: "=",
            itemAdded: "&",
        },*/

        link: function (scope,element,attrs) {
        	scope.listModel = scope.$eval(attrs.listModel);
            scope.dragIndex = -1;
            scope.internalDragging = null; // is it an already existing list item we are dragging or an external element
            scope.swapItems = function(fromPos, toPos) {
                if (fromPos != toPos) {
                    var temp1 = scope.listModel[fromPos];
                    scope.listModel.splice(fromPos, 1);
                    scope.listModel.splice(toPos, 0, temp1);
                    //console.log('swapped items ' + fromPos + ", " + toPos);
                }
            }
			/*scope.removeItem(item) {
				console.log('removing item: ');
				console.log(item);
			}*/
			
			scope.removeItem = function (item) {
				//console.log('closing panel: ' + scope.listModel.indexOf(item) );
				//console.log(item);
				scope.listModel.splice(scope.listModel.indexOf(item),1);
			}

            element.sortable({
                revert:true,
                handle:'.rvd-handle',
                placeholder: "ui-state-highlight"
            });
            
            element.bind("sortstart", function (event,ui) {
                scope.internalDragging = true; // assume this is an internal dragging (reordering)
                scope.dragIndex = element.children().index(ui.item);
                //console.log("on sortstart. dragIndex: " + scope.dragIndex );
                //console.log("children size: " + element.children().length );
                scope.draggedControl = null;
            });
            
            
            element.bind( "sortbeforestop", function( event, ui ) {                 
                //event.stopPropagation();
            });
            
            
            element.bind("sortstop", function (event,ui) {
                //console.log("on sortstop");
                //console.log("internalDragging: " + scope.internalDragging );
                var drop_index = element.children().index(ui.item);
                if ( scope.internalDragging )
                    scope.$apply( function () { scope.swapItems(scope.dragIndex, drop_index); } );                
                else {
                    // External dragging
                    ui.item.remove();
                    //scope.itemAdded({item:ui.item,pos:drop_index,listmodel:scope.listModel});
                    var expression = attrs.itemAdded + "('" + ui.item.attr("class") + "'," + drop_index + ", listModel" + ")";
                    //console.log("addItem expression: " + expression);
                    scope.$eval( expression );
                }
            });
            
            element.bind("sortreceive", function (event,ui) {
               //console.log("on sortreceive"); 
               scope.internalDragging = false;
               event.stopImmediatePropagation();
            });
            
            /*element.bind("sortactivate", function (event,ui) {
                console.log("on sortactivate");
            });
            */
        }
    };
})

.directive('basicDraggable', ['dragService', function (dragService) {
    return {
        restrict: 'A',
        //scope: {
        //},
        
        link: function (scope,element,attrs) {			
            element.draggable({
                helper: 'clone',
                connectToSortable: attrs.dropTarget,
            });
            
            element.bind('dragstart', function (event, ui)  {
				//console.log("started dragging" );
				var dragModel = attrs.class;
				if ( attrs.dragModel ) {
					//console.log("setting dragModel to " + attrs.dragModel);
					dragModel = scope.$eval(attrs.dragModel);
					console.log( dragModel );
				}
				var dragId = dragService.newDrag(dragModel);
				//console.log( "created new drag: " + dragId );
			});
        }
    };
}])

.directive('basicDroppable', ['dragService', function(dragService) {
	return {
		restrict: 'A',
		//scope: ,
		link: function (scope,element,attrs) {
			element.droppable({accept: attrs.dropAccept, greedy:true});
		
			element.bind('drop', function (event,ui) {
				//event.stopImmediatePropagation();
				//console.log("basicDroppable.drop: event.target = " );
				//console.log( event.target );
				
				if (dragService.dragActive()) {
					
					
					var dragInfo = dragService.popDrag();
					scope.$apply( function () {
						scope.$eval(attrs.dropModel+"=aaa", {aaa:dragInfo.model});
					} );
				}
			});
		}
	}
}]);
