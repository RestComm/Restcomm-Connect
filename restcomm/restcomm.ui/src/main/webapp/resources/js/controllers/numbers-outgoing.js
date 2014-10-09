'use strict';

var rcMod = angular.module('rcApp');

// Numbers : Outgoing ----------------------------------------------------------

rcMod.controller('OutgoingCtrl', function($scope, $resource, $modal, $dialog, SessionService, RCommOutgoingCallerIDs) {
  $scope.sid = SessionService.get("sid");

  $scope.showRegisterOutgoingCallerIDModal = function () {
    var registerOutgoingCallerIDModal = $modal.open({
      controller: OutgoingDetailsCtrl,
      scope: $scope,
      templateUrl: 'modules/modals/modal-register-outgoing.html'
    });

    registerOutgoingCallerIDModal.result.then(
      function () {
        // what to do on modal completion...
        $scope.outgoingList = RCommOutgoingCallerIDs.query({accountSid:$scope.sid});
      },
      function () {
        // what to do on modal dismiss...
      }
    );
  };

  // delete incoming number --------------------------------------------------

  $scope.confirmOutgoingNumberDelete = function(phone) {
    var title = 'Delete Outgoing Caller ID ' + phone.phone_number;
    var msg = 'Are you sure you want to delete outgoing number ' + phone.phone_number + ' (' + phone.friendly_name +  ') ? This action cannot be undone.';
    var btns = [{result:'cancel', label: 'Cancel', cssClass: 'btn-default'}, {result:'confirm', label: 'Delete!', cssClass: 'btn-danger'}];

    $dialog.messageBox(title, msg, btns)
      .open()
      .then(function(result) {
        if (result == "confirm") {
          RCommOutgoingCallerIDs.delete({accountSid:$scope.sid, phoneSid:phone.sid}, {}, function() {
            // TODO: Show alert on delete success...
            $scope.outgoingList = RCommOutgoingCallerIDs.query({accountSid:$scope.sid});
          }, function() {
            // TODO: Show alert on delete failure...
          });
        }
      });
  };

  $scope.outgoingList = RCommOutgoingCallerIDs.query({accountSid:$scope.sid});
});

var OutgoingDetailsCtrl = function($scope, $routeParams, $resource, $location, $dialog, $modalInstance, SessionService, RCommOutgoingCallerIDs, Notifications) {

  if($scope.outgoingSid = $routeParams.outgoingSid) {
    $scope.sid = SessionService.get("sid");

  } // or registering a new one ?
  else {
    // start optional items collapsed
    $scope.isCollapsed = true;

    $scope.closeRegisterOutgoingNumber = function () {
      $modalInstance.dismiss('cancel');
    };
  }

  $scope.registerOutgoingNumber = function(number) {
    if(number.phone_number) {
      RCommOutgoingCallerIDs.register({accountSid: $scope.sid}, $.param({
        PhoneNumber : number.phone_number
      }),
        function() { // success
          Notifications.success('Number "' + number.number + '" created successfully!');
          $modalInstance.close();
        },
        function() { // error
          Notifications.error('Failed to register Outgoing Caller ID "' + number.number + '".');
        }
      );
    }
    else {
      alert("You must provide a Phone Number.");
    }
  };

};