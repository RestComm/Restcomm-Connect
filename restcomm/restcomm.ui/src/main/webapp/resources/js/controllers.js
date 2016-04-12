'use strict';

/* Controllers */

// defining controllers module. Each module is defined at it's own file at resources/js/controllers/*.js
angular.module('rcApp.controllers', ['ui.bootstrap','angular-datepicker']);

// Shorter controllers are also defined here to creating too many files.

// Parent 'restcomm' state controller. This state assumes the availability of an authenticated user
angular.module('rcApp.controllers').controller('RestcommCtrl', function ($scope,AuthService) {
    $scope.loggedAccount = AuthService.getAccount();
    $scope.sid = AuthService.getAccountSid();
});