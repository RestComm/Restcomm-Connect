'use strict';

var rcMod = angular.module('rcApp');

rcMod.controller('LogsCallsCtrl', function($scope, $resource, $timeout, $uibModal, SessionService, RCommLogsCalls) {

  $scope.Math = window.Math;

  $scope.sid = SessionService.get("sid");

  // default search values
  $scope.search = {
    //local_only: true,
    sub_accounts: false
  }

  // pagination support ----------------------------------------------------------------------------------------------

  $scope.currentPage = 1; //current page
  $scope.maxSize = 5; //pagination max size
  $scope.entryLimit = 10; //max rows for data table
  $scope.noOfPages = 1; //max rows for data table
  $scope.reverse = false;
  $scope.predicate = "date_created";

  $scope.setEntryLimit = function(limit) {
    $scope.entryLimit = limit;
    $scope.currentPage = 1;
    $scope.getCallsList($scope.currentPage-1);
  };

  $scope.pageChanged = function() {
    $scope.getCallsList($scope.currentPage-1);
  };

  $scope.getCallsList = function(page) {
    var params = $scope.search ? createSearchParams($scope.search) : {LocalOnly: true};
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
    if(search.local_only) {
      params["LocalOnly"] = search.local_only;
    }
    if (search.sub_accounts) {
      params["SubAccounts"] = search.sub_accounts;
    }

    return params;
  }

  // Modal : Call Details
  $scope.showCallDetailsModal = function (call) {
    $uibModal.open({
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

//Activate click event for date buttons.
 $scope.openDate = function(elemDate) {
   if (elemDate === "startDate") {
        angular.element('#startpicker').trigger('click');
   }else{
        angular.element('#endpicker').trigger('click');
   }
};


$scope.sort = function(item) {
        if ($scope.predicate == 'date_created') {
            return new Date(item.date_created);
        }
       if ($scope.predicate == 'cost') {
          if (item[$scope.predicate])
            return parseFloat(item[$scope.predicate]);
          else
           return  item[$scope.predicate] = parseFloat('0.00');
        }
        if ($scope.predicate == 'duration') {
         if (item[$scope.predicate])
           return parseFloat(item[$scope.predicate]);
          else
           return  item[$scope.predicate] = parseFloat('0');
        }
    };

$scope.sortBy = function(field) {
        if ($scope.predicate != field) {
            $scope.predicate = field;
            $scope.reverse = false;
        } else {
            $scope.reverse = !$scope.reverse;
        }
    };


  // initialize with a query
  $scope.getCallsList(0);
});

rcMod.controller('LogsCallsDetailsCtrl', function($scope, $stateParams, $resource, $uibModalInstance, SessionService, RCommLogsCalls, callSid) {
  $scope.sid = SessionService.get("sid");
  $scope.callSid = $stateParams.callSid || callSid;

  $scope.closeCallDetails = function () {
    $uibModalInstance.dismiss('cancel');
  };

  $scope.callDetails = RCommLogsCalls.view({accountSid: $scope.sid, callSid:$scope.callSid});
});