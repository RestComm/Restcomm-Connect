'use strict';

var rcMod = angular.module('rcApp');

rcMod.controller('LogsNotificationsCtrl', function ($scope, $resource, $timeout, $modal, SessionService, RCommLogsNotifications) {

  $scope.Math = window.Math;

  $scope.sid = SessionService.get("sid");

  // pagination support ----------------------------------------------------------------------------------------------

  $scope.currentPage = 1; //current page
  $scope.maxSize = 5; //pagination max size
  $scope.entryLimit = 10; //max rows for data table

  $scope.setEntryLimit = function(limit) {
    $scope.entryLimit = limit;
    $scope.noOfPages = Math.ceil($scope.filtered.length / $scope.entryLimit);
  };

  $scope.setPage = function(pageNo) {
    $scope.currentPage = pageNo;
  };

  $scope.filter = function() {
    $timeout(function() { //wait for 'filtered' to be changed
      /* change pagination with $scope.filtered */
      $scope.noOfPages = Math.ceil($scope.filtered.length / $scope.entryLimit);
    }, 10);
  };

  // Modal : Notification Details
  $scope.showNotificationDetailsModal = function (notification) {
    $modal.open({
      controller: 'LogsNotificationsDetailsCtrl',
      scope: $scope,
      templateUrl: 'modules/modals/modal-logs-notifications.html',
      windowClass: 'temp-modal-lg',
      resolve: {
        notificationSid: function() {
          return notification.sid;
        }
      }
    });
  };

  // initialize with a query
  $scope.notificationsLogsList = RCommLogsNotifications.query({accountSid: $scope.sid}, function() {
    $scope.noOfPages = Math.ceil($scope.notificationsLogsList.length / $scope.entryLimit);
  });

});

rcMod.controller('LogsNotificationsDetailsCtrl', function($scope, $routeParams, $resource, $modalInstance, SessionService, RCommLogsNotifications, notificationSid) {
  $scope.sid = SessionService.get("sid");
  $scope.notificationSid = $routeParams.notificationSid || notificationSid;

  $scope.closeNotificationDetails = function () {
    $modalInstance.dismiss('cancel');
  };

  $scope.notificationDetails = RCommLogsNotifications.view({accountSid: $scope.sid, notificationSid:$scope.notificationSid});
});