'use strict';

var rcMod = angular.module('rcApp');

// Numbers : SIP Clients : List ------------------------------------------------

rcMod.controller('ClientsCtrl', function($scope, $resource, $modal, $dialog, SessionService, RCommClients, RCommApps) {

  $scope.sid = SessionService.get("sid");

  // edit incoming client friendly name --------------------------------------
  $scope.editingFriendlyName = "";

  $scope.setFriendlyName = function(client) {
    var params = {Login: client.login, FriendlyName: client.friendly_name};

    RCommClients.update({accountSid:$scope.sid, clientSid:client.sid}, $.param(params),
      function() { // success
        $scope.editingFriendlyName = "";
      },
      function() { // error
      // TODO: Show alert
      }
    );
  }

  // add incoming client -----------------------------------------------------

  $scope.showRegisterSIPClientModal = function () {
    var registerSIPClientModal = $modal.open({
      controller: ClientDetailsCtrl,
      scope: $scope,
      templateUrl: 'modules/modals/modal-register-sip-client.html'
    });

    registerSIPClientModal.result.then(
      function () {
        // what to do on modal completion...
        $scope.clientsList = RCommClients.query({accountSid:$scope.sid});
      },
      function () {
        // what to do on modal dismiss...
      }
    );
  };

  // delete sip client -------------------------------------------------------

  $scope.confirmClientDelete = function(client) {
    confirmClientDelete(client, $dialog, $scope, RCommClients);
  }

  $scope.clientsList = RCommClients.query({accountSid:$scope.sid});
});

// Numbers : SIP Clients : Details (also used for Modal) -----------------------

var ClientDetailsCtrl = function ($scope, $routeParams, $location, $dialog, $modalInstance, SessionService, RCommClients, RCommApps, Notifications) {

  // are we editing details...
  if($scope.clientSid = $routeParams.clientSid) {
    $scope.sid = SessionService.get("sid");

    $scope.clientDetails = RCommClients.get({accountSid:$scope.sid, clientSid: $scope.clientSid});
  } // or registering a new one ?
  else {
    // start optional items collapsed
    $scope.isCollapsed = true;

    $scope.closeRegisterSIPClient = function () {
      $modalInstance.dismiss('cancel');
    };
  }

  // query for available apps
  $scope.availableApps = RCommApps.query();

  var createSIPClientParams = function(client) {
    var params = {};

    // Mandatory fields
    if(client.login && client.password) {
      params["Login"] = client.login;
      params["Password"] = client.password;
    }
    else {
      alert("You must provide Login and Password.");
    }

    // Optional fields
    if (client.friendly_name) {
      params["FriendlyName"] = client.friendly_name;
    }
    if (client.voice_url) {
      params["VoiceUrl"] = client.voice_url;
      params["VoiceMethod"] = client.voice_method;
    }
    if (client.voice_fallback_url) {
      params["VoiceFallbackUrl"] = client.voice_fallback_url;
      params["VoiceFallbackMethod"] = client.voice_fallback_method;
    }
    if (client.status_callback_url) {
      params["StatusCallback"] = client.status_callback_url;
      params["StatusCallbackMethod"] = client.status_callback_method;
    }
    if (client.sms_url) {
      params["SmsUrl"] = client.sms_url;
      params["SmsMethod"] = client.sms_method;
    }
    if (client.sms_fallback_url) {
      params["SmsFallbackUrl"] = client.sms_fallback_url;
      params["SmsFallbackMethod"] = client.sms_fallback_method;
    }

    return params;
  }

  $scope.registerSIPClient = function(client) {
    var params = createSIPClientParams(client);
    RCommClients.register({accountSid: $scope.sid}, $.param(params),
      function() { // success
        Notifications.success('SIP Client "' + client.login + '" created successfully!');
        $modalInstance.close();
      },
      function() { // error
        // TODO: Show alert
      }
    );
  };

  $scope.updateSIPClient = function(client) {
    var params = createSIPClientParams(client);
    RCommClients.update({accountSid: $scope.sid, clientSid: $scope.clientSid}, $.param(params),
      function() { // success
        Notifications.success('Client "' + client.login + '" updated successfully!');
      },
      function() { // error
        Notifications.error('Failed to update client "' + client.login + '".');
      }
    );
  };

  $scope.confirmClientDelete = function(client) {
    confirmClientDelete(client, $dialog, $scope, RCommClients, $location);
  }
}

var confirmClientDelete = function(client, $dialog, $scope, RCommClients, $location) {
  var title = 'Delete SIP Client \'' + client.login + '\'';
  var msg = 'Are you sure you want to delete SIP Client ' + client.login + ' (' + client.friendly_name +  ') ? This action cannot be undone.';
  var btns = [{result:'cancel', label: 'Cancel', cssClass: 'btn-default'}, {result:'confirm', label: 'Delete!', cssClass: 'btn-danger'}];

  $dialog.messageBox(title, msg, btns)
    .open()
    .then(function(result) {
      if (result == "confirm") {
        RCommClients.delete({accountSid:$scope.sid, clientSid:client.sid}, {},
          function() {
            if($location) {
              $location.path( "/numbers/clients/" );
            }
            else {
              $scope.clientsList = RCommClients.query({accountSid:$scope.sid});
            }
          },
          function() {
            // TODO: Show alert on delete failure...
          }
        );
      }
    });
};