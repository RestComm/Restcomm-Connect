'use strict';

var rcMod = angular.module('rcApp');

// Numbers : Incoming : List ---------------------------------------------------

rcMod.controller('NumbersCtrl', function ($scope, $resource, $modal, $dialog, $rootScope, $anchorScroll, SessionService, RCommNumbers, Notifications) {
  $anchorScroll(); // scroll to top
  $scope.sid = SessionService.get("sid");

  // edit incoming number friendly name --------------------------------------
  $scope.editingFriendlyName = "";

  $scope.setFriendlyName = function(pn) {
    var params = {PhoneNumber: pn.phone_number, FriendlyName: pn.friendly_name};

    RCommNumbers.update({accountSid:$scope.sid, phoneSid:pn.sid}, $.param(params),
      function() { // success
        $scope.editingFriendlyName = "";
      },
      function() { // error
        // TODO: Show alert
      }
    );
  }

  // add incoming number -----------------------------------------------------

  $scope.showRegisterIncomingNumberModal = function () {
    var registerIncomingNumberModal = $modal.open({
      controller: NumberDetailsCtrl,
      scope: $scope,
      templateUrl: 'modules/modals/modal-register-incoming-number.html'
    });

    registerIncomingNumberModal.result.then(
      function () {
        // what to do on modal completion...
        $scope.numbersList = RCommNumbers.query({accountSid:$scope.sid});
      },
      function () {
        // what to do on modal dismiss...
      }
    );
  };

  // delete incoming number --------------------------------------------------

  $scope.confirmNumberDelete = function(phone) {
    confirmNumberDelete(phone, $dialog, $scope, RCommNumbers, Notifications);
  }

  $scope.numbersList = RCommNumbers.query({accountSid: $scope.sid});
});

// Numbers : Incoming : Details (also used for Modal) --------------------------

var NumberDetailsCtrl = function ($scope, $routeParams, $location, $dialog, $modalInstance, SessionService, RCommNumbers, RCommApps, RCommAvailableNumbers, RCommAvailableNumbersNonUS, Notifications) {

  // are we editing details...
  if($scope.phoneSid = $routeParams.phoneSid) {
    $scope.sid = SessionService.get("sid");

    $scope.numberDetails = RCommNumbers.get({accountSid:$scope.sid, phoneSid: $scope.phoneSid});
  } // or registering a new one ?
  else {
    // start optional items collapsed
    $scope.isCollapsed = true;

    $scope.closeRegisterIncomingNumber = function () {
      $modalInstance.dismiss('cancel');
    };
  }

  // query for available apps
  $scope.availableApps = RCommApps.query();

  var createNumberParams = function(number) {
    var params = {};

    // Mandatory fields
    if(number.phone_number) {
      params["PhoneNumber"] = number.phone_number;
    }
    else if(number.area_code) {
      params["AreaCode"] = number.area_code;
    }
    else {
      alert("You must provide either Number or Area Code.");
    }

    // Optional fields
    if (number.friendly_name) {
      params["FriendlyName"] = number.friendly_name;
    }
    if (number.voice_url) {
      params["VoiceUrl"] = number.voice_url;
      params["VoiceMethod"] = number.voice_method;
    }
    if (number.voice_fallback_url) {
      params["VoiceFallbackUrl"] = number.voice_fallback_url;
      params["VoiceFallbackMethod"] = number.voice_fallback_method;
    }
    if (number.status_callback_url) {
      params["StatusCallback"] = number.status_callback_url;
      params["StatusCallbackMethod"] = number.status_callback_method;
    }
    if (number.sms_url) {
      params["SmsUrl"] = number.sms_url;
      params["SmsMethod"] = number.sms_method;
    }
    if (number.sms_fallback_url) {
      params["SmsFallbackUrl"] = number.sms_fallback_url;
      params["SmsFallbackMethod"] = number.sms_fallback_method;
    }
    if (number.voice_caller_id_lookup) {
      params["VoiceCallerIdLookup"] = number.voice_caller_id_lookup;
    }

    return params;
  };

  $scope.registerIncomingNumber = function(number) {
    var params = createNumberParams(number);
    RCommNumbers.register({accountSid: $scope.sid}, $.param(params),
      function() { // success
        Notifications.success('Number "' + number.phone_number + '" created successfully!');
        $modalInstance.close();
      },
      function() { // error
        Notifications.error('Failed to register number "' + number.phone_number + '".');
      }
    );
  };

  $scope.updateIncomingNumber = function(number) {
    var params = createNumberParams(number);
    RCommNumbers.update({accountSid: $scope.sid, phoneSid: $scope.phoneSid}, $.param(params),
      function() { // success
        Notifications.success('Number "' + number.phone_number + '" updated successfully!');
        $location.path( "/numbers/incoming/" );
      },
      function() { // error
        Notifications.error('Failed to update number "' + number.phone_number + '".');
      }
    );
  };

  $scope.confirmNumberDelete = function(phone) {
    confirmNumberDelete(phone, $dialog, $scope, RCommNumbers, Notifications, $location);
  };

  $scope.searching = false;

  $scope.findNumbers = function(areaCode, countryCode) {
    $scope.searching = true;
    $scope.availableNumbers = null;
    if(countryCode == null || countryCode === "" || countryCode.length == 0 || countryCode.length == 1) {
		document.getElementById("countryCode").value = "US";
		countryCode = "US";
	}
    if(countryCode !== "US") {
    	$scope.availableNumbers = RCommAvailableNumbersNonUS.query({accountSid: $scope.sid, countryCode: countryCode});
    } else {
    	$scope.availableNumbers = RCommAvailableNumbers.query({accountSid: $scope.sid, countryCode: countryCode, areaCode: areaCode});
    }
    $scope.availableNumbers.$promise.then(
      //success
      function(value){
        $scope.searching = false;
      },
      //error
      function(error){
        $scope.searching = false;
      }
    );
  }
};

var confirmNumberDelete = function(phone, $dialog, $scope, RCommNumbers, Notifications, $location) {
  var title = 'Delete Number ' + phone.phone_number;
  var msg = 'Are you sure you want to delete incoming number ' + phone.phone_number + ' (' + phone.friendly_name +  ') ? This action cannot be undone.';
  var btns = [{result:'cancel', label: 'Cancel', cssClass: 'btn-default'}, {result:'confirm', label: 'Delete!', cssClass: 'btn-danger'}];

  $dialog.messageBox(title, msg, btns)
    .open()
    .then(function(result) {
      if (result == "confirm") {
        RCommNumbers.delete({accountSid:$scope.sid, phoneSid:phone.sid}, {},
          function() {
            Notifications.success('The incoming number "' + phone.phone_number + '" has been deleted.');
            if($location) {
              $location.path( "/numbers/incoming/" );
            }
            else {
              $scope.numbersList = RCommNumbers.query({accountSid:$scope.sid});
            }
          },
          function() {
            // TODO: Show alert on delete failure...
            Notifications.error('Failed to delete the incoming number ' + phone.phone_number);
          }
        );
      }
    });
};

function countryCodeChange() {
	var countryCodeValue = document.getElementById("countryCode").value;
	if(countryCodeValue == null || countryCodeValue === "" || countryCodeValue.length == 0 || countryCodeValue.length == 1) {
		document.getElementById("countryCode").value = "US";
		countryCodeValue = "US";
	}
	if(countryCodeValue !== "US") {
		document.getElementById("areaCodeOptionsName").style.visibility = 'hidden';
		document.getElementById("areaCodeOptionsForm").style.visibility = 'hidden';
	} else {
		document.getElementById("areaCodeOptionsName").style.visibility = 'visible';
		document.getElementById("areaCodeOptionsForm").style.visibility = 'visible';
	}
}