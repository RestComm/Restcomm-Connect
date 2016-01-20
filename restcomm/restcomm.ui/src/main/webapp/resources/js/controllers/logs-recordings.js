'use strict';

var rcMod = angular.module('rcApp');

rcMod.controller('LogsRecordingsCtrl', function($scope, $resource, $timeout, $modal, AuthService, RCommLogsRecordings) {

  $scope.Math = window.Math;

  $scope.sid = AuthService.getLoggedSid(); // SessionService.get("sid");

  // pagination support ----------------------------------------------------------------------------------------------

  $scope.currentPage = 1; //current page
  $scope.maxSize = 5; //pagination max size
  $scope.entryLimit = 10; //max rows for data table
  $scope.reverse = false;
  $scope.predicate = "date_created";

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

  // Modal : Recording Details
  $scope.showRecordingDetailsModal = function (recording) {
    $modal.open({
      controller: 'LogsRecordingsDetailsCtrl',
      scope: $scope,
      templateUrl: 'modules/modals/modal-logs-recordings.html',
      resolve: {
        recordingSid: function() {
          return recording.sid;
        }
      }
    });
  };

  // initialize with a query
  $scope.recordingsLogsList = RCommLogsRecordings.query({accountSid: $scope.sid}, function() {
    $scope.noOfPages = Math.ceil($scope.recordingsLogsList.length / $scope.entryLimit);
  });

 $scope.sort = function(item) {
          if ($scope.predicate == 'date_created') {
              return new Date(item.date_created);
          }
             return  item[$scope.predicate];
      };

 $scope.sortBy = function(field) {
          if ($scope.predicate != field) {
              $scope.predicate = field;
              $scope.reverse = false;
          } else {
              $scope.reverse = !$scope.reverse;
          }
      };

});

rcMod.controller('LogsRecordingsDetailsCtrl', function($scope, $routeParams, $resource, $modalInstance, AuthService, RCommLogsRecordings, recordingSid) {
  $scope.sid = AuthService.getLoggedSid(); // SessionService.get("sid");
  $scope.recordingSid = $routeParams.recordingSid || recordingSid;

  $scope.closeRecordingDetails = function () {
    $modalInstance.dismiss('cancel');
  };

  $scope.recordingDetails = RCommLogsRecordings.view({accountSid: $scope.sid, recordingSid: $scope.recordingSid});
});