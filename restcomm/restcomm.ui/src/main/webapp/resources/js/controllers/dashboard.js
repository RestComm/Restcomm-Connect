'use strict';

var rcMod = angular.module('rcApp');

rcMod.controller('DashboardCtrl', function ($scope, $resource, SessionService, Auth, md5) {
  $scope.sid = SessionService.get("sid");

  $scope.profile = Auth.authz.profile;
  
  // calculate account sid from userame;
  var username = Auth.authz.profile.username;
  var accountSid = "AC" + md5.createHash(username);
  
  // TEMPORARY... FIXME!
  
  var Account = $resource('/restcomm/keycloak/Accounts.:format/:accountSid',
    { accountSid: accountSid, format: 'json' },
    {
      // charge: {method:'POST', params:{charge:true}}
    });

  $scope.accountData = Account.get();
});