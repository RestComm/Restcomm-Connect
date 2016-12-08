'use strict';

var rcMod = angular.module('rcApp');

rcMod.controller('LoginCtrl', function ($scope, $rootScope, $location, $timeout, $dialog, AuthService, Notifications, $state) {

  $scope.alerts = [];

  $scope.credentials = {
    host: window.location.host/*,
    sid: "administrator@company.com",
    token: "RestComm"*/
  };

  $scope.login = function() {
    AuthService.login($scope.credentials.sid, $scope.credentials.token).then(function (loginStatus) {
        // SUCCESS
        if (loginStatus == 'UNINITIALIZED' ){
        	var params = { userName:$scope.credentials.sid};
            $state.go('public.uninitialized',params);
        }
        else
            $location.path('/dashboard');
    }, function (errorStatus) {
        // ERROR
        if (errorStatus == 'SUSPENDED')
            showAccountSuspended($dialog);
        else
        if (errorStatus == "AUTH_ERROR") {
            Notifications.error('Login failed. Please confirm your username and password.');
            // FIXME: Use ng-animate...
            $scope.loginFailed = true;
            $timeout(function() { $scope.loginFailed = false; }, 1000);
        }
        else
            Notifications.error('Unknown error');
    });
  };

  $scope.closeAlert = function(index) {
    if($scope.closeAlertTimer) {
      clearTimeout($scope.closeAlertTimer);
      $scope.closeAlertTimer = null;
    }
    $scope.alerts.splice(index, 1);
  };

  /*
   $scope.newPassword = $scope.confPassword = "";

   $scope.$watchCollection('[newPassword, confPassword]', function() {
   var valid = angular.equals($scope.newPassword, $scope.confPassword);
   $scope.updatePassForm.newPassword.$valid = $scope.updatePassForm.confPassword.$valid = valid;
   $scope.accountValid = $scope.updatePassForm.$valid && valid;
   console.log("XXX");
   }, true);
   */

  var showAccountSuspended = function($dialog) {
    var title = 'Account Suspended';
    var msg = 'Your account has been suspended. Please contact the support team for further information.';
    var btns = [{result:'cancel', label: 'Close', cssClass: 'btn-default'}];

    $dialog.messageBox(title, msg, btns).open();
  };
});

// assumes user has been authenticated but his account is not initialized
rcMod.controller('UninitializedCtrl', function ($scope,AuthService,$state,$stateParams) {
	$scope.userName = $stateParams.userName;
  // For password reset
  $scope.update = function() {
    AuthService.updatePassword($scope.newPassword).then(function () {
        $state.go('restcomm.dashboard');
    }, function (error) {
        alert("Failed to update password. Please try again.");
        $state.go('public.login');
    });
  }
});