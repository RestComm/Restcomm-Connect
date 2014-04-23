'use strict';

var rcMod = angular.module('rcApp');

rcMod.controller('LogsCallsCtrl', function($scope, $resource, $timeout, $modal, SessionService, RCommLogsCalls) {

  $scope.Math = window.Math;

  $scope.sid = SessionService.get("sid");

  // pagination support ----------------------------------------------------------------------------------------------

  $scope.currentPage = 1; //current page
  $scope.maxSize = 5; //pagination max size
  $scope.entryLimit = 10; //max rows for data table
  $scope.noOfPages = 1; //max rows for data table

  $scope.setEntryLimit = function(limit) {
    $scope.entryLimit = limit;
    $scope.currentPage = 1;
    $scope.getCallsList($scope.currentPage-1);
  };

  $scope.pageChanged = function(page) {
    $scope.getCallsList(page-1);
  };

  $scope.getCallsList = function(page) {
    var params = $scope.search ? createSearchParams($scope.search) : {};
    RCommLogsCalls.search($.extend({accountSid: $scope.sid, Page: page, PageSize: $scope.entryLimit}, params), function(data) {
      $scope.callsLogsList = data.calls;
      $scope.totalCalls = data.total;
      $scope.noOfPages = data.num_pages;
    });
  }

  $scope.filter = function() {
    $timeout(function() { //wait for 'filtered' to be changed
      /* change pagination with $scope.filtered */
      $scope.noOfPages = Math.ceil($scope.filtered.length / $scope.entryLimit);
    }, 10);
  };

  var createSearchParams = function(search) {
    var params = {};

    // Mandatory fields
    if(search.start_time) {
      params["StartTime"] = search.start_time;
    }
    if(search.end_time) {
      params["EndTime"] = search.end_time;
    }
    if(search.from) {
      params["From"] = search.from;
    }
    if(search.to) {
      params["To"] = search.to;
    }
    if(search.status) {
      params["Status"] = search.status;
    }

    return params;
  }

  // Modal : Call Details
  $scope.showCallDetailsModal = function (call) {
    $modal.open({
      controller: 'LogsCallsDetailsCtrl',
      scope: $scope,
      templateUrl: 'modules/modals/modal-logs-calls.html',
      resolve: {
        callSid: function() {
          return call.sid;
        }
      }
    });
  };

  // initialize with a query
  $scope.getCallsList(0);
});

rcMod.controller('LogsCallsDetailsCtrl', function($scope, $routeParams, $resource, $modalInstance, SessionService, RCommLogsCalls, callSid) {
  $scope.sid = SessionService.get("sid");
  $scope.callSid = $routeParams.callSid || callSid;

  $scope.closeCallDetails = function () {
    $modalInstance.dismiss('cancel');
  };

  $scope.callDetails = RCommLogsCalls.view({accountSid: $scope.sid, callSid:$scope.callSid});
});