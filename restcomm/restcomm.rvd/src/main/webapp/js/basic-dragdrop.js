angular.module('basicDragdrop', [])
.directive('basicSortable', function() {
    return {
        restrict: 'A',
        scope: {
            listModel: "=",
            itemAdded: "&",
        },

        link: function (scope,element,attrs) {
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

            element.sortable({
                revert:true,
                //containment:element,
            });
            
            element.bind("sortstart", function (event,ui) {
                scope.internalDragging = true; // assume this is an internal dragging (reordering)
                scope.dragIndex = element.children().index(ui.item);
                //console.log("on sortstart. dragIndex: " + scope.dragIndex );
                //console.log("children size: " + element.children().length );
                scope.draggedControl = null;
            });
            
            /*
            element.bind( "sortbeforestop", function( event, ui ) { 
                console.log("sortbeforestop executed");

            });
            */
            
            element.bind("sortstop", function (event,ui) {
                //console.log("on sortstop");
                //console.log("internalDragging: " + scope.internalDragging );
                var drop_index = element.children().index(ui.item);
                if ( scope.internalDragging )
                    scope.$apply( function () { scope.swapItems(scope.dragIndex, drop_index); } );                
                else {
                    // External dragging
                    ui.item.remove();
                    scope.itemAdded({item:ui.item,pos:drop_index,listmodel:scope.listModel});
                }
            });
            
            element.bind("sortreceive", function (event,ui) {
               //console.log("on sortreceive"); 
               scope.internalDragging = false;
            });
            
            /*element.bind("sortactivate", function (event,ui) {
                console.log("on sortactivate");
            });
            */
        }
    };
})
.directive('basicDraggable', function () {
    return {
        restrict: 'A',
        scope: {
        },
        
        link: function (scope,element,attrs) {
            element.draggable({
                helper: 'clone',
                connectToSortable: attrs.dropTarget,
            });
        }
    };
});
