'use strict';

var rcMod = angular.module('rcApp');

rcMod.controller('LoginCtrl', function ($scope, $rootScope, $location, $timeout, AuthService, Notifications) {

  $scope.alerts = [];

  $scope.credentials = {
    host: window.location.host,
    sid: "administrator@company.com",
    token: "RestComm"
  };

  $scope.login = function() {
    AuthService.login($scope.credentials).
      success(function(data, status, headers, config) {
        // Success may come in many forms...
        if (status == 200) {
          if(AuthService.getWaitingReset(data)) {
            $scope.updatePassword = true;
          }
          else {
            $location.path('/dashboard');
          }
        }
        else {
          Notifications.error('Login failed. Please confirm your username and password.')
          // FIXME: Use ng-animate...
          $scope.loginFailed = true;
          $timeout(function() { $scope.loginFailed = false; }, 1000);
        }
      });
  };

  $scope.closeAlert = function(index) {
    if($scope.closeAlertTimer) {
      clearTimeout($scope.closeAlertTimer);
      $scope.closeAlertTimer = null;
    }
    $scope.alerts.splice(index, 1);
  };

  // For password reset
  $scope.update = function() {
    AuthService.updatePassword($scope.credentials, $scope.newPassword).success(function(data, status) {
      // Success may come in many forms...
      if (status == 200) {
        $location.path('/dashboard');
      }
      else {
        alert("Failed to update password. Please try again.");
        $location.path('/login');
      }
    });
  }
  /*
   $scope.newPassword = $scope.confPassword = "";

   $scope.$watchCollection('[newPassword, confPassword]', function() {
   var valid = angular.equals($scope.newPassword, $scope.confPassword);
   $scope.updatePassForm.newPassword.$valid = $scope.updatePassForm.confPassword.$valid = valid;
   $scope.accountValid = $scope.updatePassForm.$valid && valid;
   console.log("XXX");
   }, true);
   */

});