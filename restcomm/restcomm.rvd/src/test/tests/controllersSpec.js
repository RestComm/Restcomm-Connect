'use strict';

/* jasmine specs for controllers go here */


describe('Project management', function () {
	
	var scope;
	
    beforeEach(angular.mock.module('Rvd')); 
    beforeEach(angular.mock.inject( function ($rootScope, $controller, $httpBackend) {
    	 
    	$httpBackend.when('GET', 'services/manager/projects?name=FooProject').respond({"startNodeName":"start","lastStepId":3,"nodes":[{"name":"start","label":"Welcome","steps":{"step1":{"kind":"say","label":"say","title":"say","phrase":"welcome to restcom Visual Service Designer","voice":null,"language":null,"loop":null,"isCollapsed":false,"iface":{"optionsVisible":false},"name":"step1"}},"stepnames":["step1"],"iface":{"edited":false,"editLabel":false}}],"activeNode":0,"lastNodeId":0});
    	scope = $rootScope.$new(); 
        $controller('designerCtrl', {$scope:scope, $routeParams:{projectName:'FooProject'}}); 
    })); 
    
    it('Start module is start', function () {
    	expect(scope.startNodeName).toBe('start');
    }); 
    it('Project name set', function () {
    	expect(scope.projectName).toBe('FooProject');
    });
    it('A single module exists', function () {
    	console.log(scope.nodes);
    	expect(scope.nodes.length).toEqual(1);
    });
    
    
});

/*
describe('controllers', function(){

    var scope; 

    beforeEach(angular.mock.module('Rvd'));
    beforeEach(angular.mock.inject( function ($rootScope, $controller) {
        scope = $rootScope.$new();
        $controller('projectManagerCtrl', {$scope:scope});
    })); 
    
    it('create new project', function () {
        scope.createNewProject('aaa');
    });
    
    it('get project list', function () {
        scope.refreshProjectList();
    });
});

*/
