'use strict';

var rcMod = angular.module('rcApp');

rcMod.controller('LogsRecordingsCtrl', function($scope, $resource, $timeout, $modal, SessionService, RCommLogsRecordings) {

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

});

rcMod.controller('LogsRecordingsDetailsCtrl', function($scope, $routeParams, $resource, $modalInstance, SessionService, RCommLogsRecordings, recordingSid) {
  $scope.sid = SessionService.get("sid");
  $scope.recordingSid = $routeParams.recordingSid || recordingSid;

  $scope.closeRecordingDetails = function () {
    $modalInstance.dismiss('cancel');
  };

  $scope.recordingDetails = RCommLogsRecordings.view({accountSid: $scope.sid, recordingSid: $scope.recordingSid});
});