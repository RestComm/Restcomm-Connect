'use strict';

var rcMod = angular.module('rcApp');


  rcMod.controller('RegisterCtrl', function ($scope, $rootScope,  $location, UserService, InstanceService, Notifications ) {
        // we will store our form data -user in this object
        $scope.user = {};
        $scope.dataLoading = false;
        $scope.user.wantNewsletter=true;
        
        $scope.instance = {};
        $scope.instance.authUrl = "https://identity.restcomm.com"; // TODO - this is hardcoded! Should we replace it with something read from configuration?
        $scope.instance.restcommBaseUrl = "https://192.168.2.3:8443";

        /*
        $scope.register = function() {
        $scope.dataLoading = true;
        UserService.Create($scope.user).then(function (response) {
                if (response.success) {
                    Notifications.success('Registration successful');
                    $location.path('/dashboard');
                } else {
                    Notifications.error(response.message)
                    $scope.dataLoading = false;
                    $location.path('/dashboard');
                }
            });
        }
        */
        
        $scope.register = function (instance) {
        	InstanceService.registerInstance(instance);
        }

});



