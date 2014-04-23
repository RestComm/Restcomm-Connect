'use strict';

var rcMod = angular.module('rcApp');

rcMod.controller('DashboardCtrl', function ($scope, $resource, SessionService) {
  $scope.sid = SessionService.get("sid");

  // TEMPORARY... FIXME!
  var Account = $resource('/restcomm/2012-04-24/Accounts.:format/:accountSid',
    { accountSid:$scope.sid, format:'json' },
    {
      // charge: {method:'POST', params:{charge:true}}
    });

  $scope.accountData = Account.get();
});