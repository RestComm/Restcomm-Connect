'use strict';

var rcMod = angular.module('rcApp');

rcMod.controller('DashboardCtrl', function ($scope, $resource, AuthService) {
  $scope.sid = AuthService.getLoggedSid(); //SessionService.get("sid");

  $scope.profile = AuthService.getProfile(); //Auth.authz.profile;
  
  // TEMPORARY... FIXME!
  
  var Account = $resource('/restcomm/keycloak/Accounts.:format/:accountSid',
    { accountSid: $scope.sid, format: 'json' },
    {
      // charge: {method:'POST', params:{charge:true}}
    });

  $scope.accountData = Account.get();
});