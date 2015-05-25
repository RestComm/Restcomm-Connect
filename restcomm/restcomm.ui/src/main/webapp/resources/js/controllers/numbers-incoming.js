'use strict';

var rcMod = angular.module('rcApp');

// Numbers : Incoming : List ---------------------------------------------------

rcMod.controller('NumbersCtrl', function ($scope, $resource, $modal, $dialog, $rootScope, $anchorScroll, AuthService, Auth, RCommNumbers, Notifications) {
  $anchorScroll(); // scroll to top
  $scope.sid = AuthService.getLoggedSid();

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
  };

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
  };

  $scope.numbersList = RCommNumbers.query({accountSid: $scope.sid});
});

// Numbers : Incoming : Details (also used for Modal) --------------------------

var NumberDetailsCtrl = function ($scope, $routeParams, $location, $dialog, $modalInstance, AuthService, RCommNumbers, RCommApps, RCommAvailableNumbers, Notifications, allCountries, providerCountries, localApps, $rootScope) {

  // are we editing details...
  //if($scope.phoneSid === $routeParams.phoneSid) {

    $scope.sid = AuthService.getLoggedSid();
    $scope.phoneSid = $routeParams.phoneSid;

    $scope.numberDetails = RCommNumbers.get({accountSid:$scope.sid, phoneSid: $scope.phoneSid});
    
  //} // or registering a new one ?
  //else {
  //  // start optional items collapsed
  //  $scope.isCollapsed = true;
  //
  //  $scope.closeRegisterIncomingNumber = function () {
  //    $modalInstance.dismiss('cancel');
  //  };
  //}

  // query for available apps
  $scope.availableApps = RCommApps.query();
  $scope.localApps = localApps;

  //$scope.countries = countries;
  $scope.countries = allCountries;
  $scope.providerCountries = providerCountries;

  $scope.areaCodes = RCommAvailableNumbers.getAreaCodes();
  $scope.selected = undefined;

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
        $rootScope.$broadcast("incoming-number-updated", {phoneSid:$scope.phoneSid, params: params});
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
      $scope.availableNumbers = RCommAvailableNumbers.query({accountSid: $scope.sid, countryCode: countryCode.code});
    } else {
      $scope.availableNumbers = RCommAvailableNumbers.query({accountSid: $scope.sid, countryCode: countryCode.code, areaCode: areaCode});
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

var NumberRegisterCtrl = function ($scope, $routeParams, $location, $http, $dialog, $modalInstance, AuthService, RCommNumbers, RCommApps, RCommAvailableNumbers, Notifications, allCountries, providerCountries) {

  $scope.sid = AuthService.getLoggedSid();

  //$scope.countries = countries;
  $scope.countries = allCountries;
  $scope.providerCountries = providerCountries;

  $scope.areaCodes = RCommAvailableNumbers.getAreaCodes();
  $scope.selected = undefined;

  $scope.setProvider = function(isProvider) {
    $scope.isProvider = isProvider;
  };

  $scope.registerIncomingNumber = function(number, isSIP) {
    confirmNumberRegister(number, isSIP, $dialog, $scope, RCommNumbers, Notifications, $location, $http);
  };

  $scope.configureNewNumber = function(number) {
  };

  $scope.searching = false;

  $scope.findNumbers = function(areaCode, countryCode) {
    $scope.searching = true;
    $scope.availableNumbers = null;
    $scope.availableNumbers = RCommAvailableNumbers.query({accountSid: $scope.sid, countryCode: countryCode.code, areaCode: areaCode});
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

var confirmNumberRegister = function(phone, isSIP, $dialog, $scope, RCommNumbers, Notifications, $location, $http) {
  var title = 'Register Number ' + (phone.phone_number || phone.phoneNumber);
  var msg = 'Are you sure you want to register incoming number ' + (phone.phone_number || phone.phoneNumber) + ' (' + (phone.friendly_name || phone.friendlyName) +  ') ? ' + (isSIP ? '' : 'It will cost you ' + phone.cost + '.');
  var btns = [{result:'cancel', label: 'Cancel', cssClass: 'btn-default'}, {result:'confirm', label: 'Register', cssClass: 'btn-primary'}];

  $dialog.messageBox(title, msg, btns)
    .open()
    .then(function(result) {
      if (result == "confirm") {
        var params = createNumberParams(phone, isSIP);
        RCommNumbers.register({accountSid: $scope.sid}, $.param(params),
         function(phone, headers) { // success
           phone.registered = true;
           // TODO: add assigned id to number so it can be used with configure
           Notifications.success('Number "' + (phone.phone_number || phone.phoneNumber) + '" created successfully!');
           $location.path('/numbers/incoming/' + phone.sid);
         },
         function(httpResponse) { // error
           Notifications.error('Failed to register number "' + (phone.phone_number || phone.phoneNumber) + '".');
         }
       );
        /*
        // FIXME: for some reason $resource error callback is not called on error..
        $http({
          method: 'POST',
          url: '/restcomm/2012-04-24/Accounts/' + $scope.sid + '/IncomingPhoneNumbers.json',
          data: $.param(params),
          headers: {'Content-Type': 'application/x-www-form-urlencoded'}
        }).
          success(
          function(a, b) { // success
            console.log(a);
            console.log(b);
            phone.registered = true;
            // TODO: add assigned id to number so it can be used with configure
            Notifications.success('Number "' + (phone.phone_number || phone.phoneNumber) + '" created successfully!');
          }).error(
          function(e) { // error
            console.log(e);
            Notifications.error('Failed to register number "' + (phone.phone_number || phone.phoneNumber) + '".');
          }
        )*/
      }
    });
};

var createNumberParams = function(number) {
	createNumberParams(number, false)
}

var createNumberParams = function(number, isSIP) {
  var params = {};

  // Mandatory fields
  params["PhoneNumber"] = number.phone_number || number.phoneNumber;
  params["AreaCode"] = number.area_code || number.areaCode;

  if (!params["PhoneNumber"] && !params["AreaCode"]) {
    alert("You must provide either Number or Area Code.");
    return params;
  }

  // Optional fields
  params["FriendlyName"] = number.friendly_name || number.friendlyName;
  params["VoiceUrl"] = number.voice_url || number.voiceUrl;
  params["VoiceMethod"] = number.voice_method || number.voiceMethod;
  params["VoiceFallbackUrl"] = number.voice_fallback_url || number.voiceFallbackUrl;
  params["VoiceFallbackMethod"] = number.voice_fallback_method || number.voiceFallbackMethod;
  params["StatusCallback"] = number.status_callback_url || number.statusCallback;
  params["StatusCallbackMethod"] = number.status_callback_method || number.statusCallbackMethod;
  params["SmsUrl"] = number.sms_url || number.smsUrl;
  params["SmsMethod"] = number.sms_method || number.smsMethod;
  params["SmsFallbackUrl"] = number.sms_fallback_url || number.smsFallbackUrl;
  params["SmsFallbackMethod"] = number.sms_fallback_method || number.smsFallbackMethod;
  params["VoiceCallerIdLookup"] = number.voice_caller_id_lookup || number.voiceCallerIdLookup;
  if(isSIP) {
	  params["isSIP"] = "true";
  }

  for (var prop in params) {
    if (params.hasOwnProperty(prop) && params[prop] === undefined) {
      delete params[prop];
    }
  }

  return params;
};
