'use strict';

/* Controllers */

// defining controllers module. Each module is defined at it's own file at resources/js/controllers/*.js
angular.module('rcApp.controllers', ['ui.bootstrap','angular-datepicker']);

// Shorter controllers are also defined here to avoid creating too many files.

// Parent 'restcomm' state controller. This state assumes the availability of an authenticated user
angular.module('rcApp.controllers').controller('RestcommCtrl', function ($scope,AuthService, PublicConfig, $rootScope) {
    $scope.loggedAccount = AuthService.getAccount();
    $scope.sid = AuthService.getAccountSid();
    $rootScope.PublicConfig = PublicConfig;
});

angular.module('rcApp.controllers').controller('IdentityRegistrationCtrl', function ($scope, RCommIdentityInstances) {
    $scope.info = {
        InitialAccessToken: "",
        RedirectUrl: "",
        KeycloakBaseUrl: ""
    };
    $scope.accountInfo = {
        username: "",
        password: ""
    };

    $scope.registerInstance = function(info, accountInfo) {
        var authHeader = "Basic " + btoa(accountInfo.username + ":" + accountInfo.password);
        RCommIdentityInstances.register(info, authHeader).success(function (data, status) {
            console.log("successfully registered instance");
        }).error(function (data, status) {
            console.log("error registering instance");
        });

    }

});