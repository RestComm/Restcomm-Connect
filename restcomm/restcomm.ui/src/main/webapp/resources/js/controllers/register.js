'use strict';

var rcMod = angular.module('rcApp');


  rcMod.controller('RegisterCtrl', function ($scope, $rootScope,  $location, UserService, Notifications ) {
        // we will store our form data -user in this object
        $scope.user = {};
        $scope.dataLoading = false;
        $scope.user.wantNewsletter=true;

        $scope.register = function() {
         $scope.dataLoading = true;
        UserService.Create($scope.user).then(function (response) {
                if (response.success) {
                    Notifications.Success('Registration successful');
                    $location.path('/login');
                } else {
                    //Notifications.error(response.message) //TODO: When Service is ready.
                    Notifications.info('Registration system is under construction!');
                    $scope.dataLoading = false;
                    $location.path('/dashboard'); //TODO:Need to finish the service for the registration.
                }
            });
    }

});



