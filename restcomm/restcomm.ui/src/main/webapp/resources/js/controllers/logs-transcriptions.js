'use strict';

var rcMod = angular.module('rcApp');

rcMod.controller('LogsTranscriptionsCtrl', function ($scope, $resource, $timeout, $modal, Identity, RCommLogsTranscriptions) {

  $scope.Math = window.Math;

  $scope.sid = Identity.getAccountSid();

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

rcMod.controller('LogsTranscriptionsDetailsCtrl', function($scope, $routeParams, $resource, $modalInstance, Identity, RCommLogsTranscriptions, transcriptionSid) {
  $scope.sid = Identity.getAccountSid();
  $scope.transcriptionSid = $routeParams.transcriptionSid || transcriptionSid;

  $scope.closeTranscriptionDetails = function () {
    $modalInstance.dismiss('cancel');
  };

  $scope.transcriptionDetails = RCommLogsTranscriptions.view({accountSid: $scope.sid, transcriptionSid:$scope.transcriptionSid});
});