'use strict';

var rcMod = angular.module('rcApp');

rcMod.controller('LogsTranscriptionsCtrl', function ($scope, $resource, $timeout, $modal, SessionService, RCommLogsTranscriptions) {

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

  // Modal : Transcription Details
  $scope.showTranscriptionDetailsModal = function (transcription) {
    $modal.open({
      controller: 'LogsTranscriptionsDetailsCtrl',
      scope: $scope,
      templateUrl: 'modules/modals/modal-logs-transcriptions.html',
      resolve: {
        transcriptionSid: function() {
          return transcription.sid;
        }
      }
    });
  };

  // initialize with a query
  $scope.transcriptionsLogsList = RCommLogsTranscriptions.query({accountSid: $scope.sid}, function() {
    $scope.noOfPages = Math.ceil($scope.transcriptionsLogsList.length / $scope.entryLimit);
  });

});

rcMod.controller('LogsTranscriptionsDetailsCtrl', function($scope, $routeParams, $resource, $modalInstance, SessionService, RCommLogsTranscriptions, transcriptionSid) {
  $scope.sid = SessionService.get("sid");
  $scope.transcriptionSid = $routeParams.transcriptionSid || transcriptionSid;

  $scope.closeTranscriptionDetails = function () {
    $modalInstance.dismiss('cancel');
  };

  $scope.transcriptionDetails = RCommLogsTranscriptions.view({accountSid: $scope.sid, transcriptionSid:$scope.transcriptionSid});
});