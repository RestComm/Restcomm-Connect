'use strict';

var rcMod = angular.module('rcApp');

rcMod.controller('MenuCtrl', function($scope, $http, $resource, $rootScope, $location, $modal, AuthService, SessionService, Notifications, RCommAccounts, authMode) {

  /* watch location change and update root scope variable for rc-*-pills */
  $rootScope.$on('$locationChangeStart', function(/*event, next, current*/) {
    $rootScope.location = $location.path();
  });
  
  $scope.authMode = authMode;
  //$scope.auth = AuthService;
  $scope.sid = SessionService.get('sid');

  $scope.testNotifications = function() {
    Notifications.info('This is an info message');
    Notifications.warn('This is an warning message');
    Notifications.error('This is an error message');
    Notifications.success('This is an success message');
  };

  $scope.logout = function() {
    AuthService.logout();
    //$http.get('/restcomm/2012-04-24/Logout')
    /*.
     success(function() {console.log('Logged out from API.');}).
     error(function() {console.log('Failed to logout from API.');})*/;
  };

  // otsakir - disable it for now. maybe it's not needed
  /*
  if(AuthService.isLoggedIn()) {
    var accountsList = RCommAccounts.query(function() {
      $scope.accountsList = accountsList;
      for (var x in accountsList){
        if(accountsList[x].sid == $scope.sid) {
          $scope.currentAccount = accountsList[x];
        }
      }
    });
  }
  */

  // add account -------------------------------------------------------------

  $scope.showRegisterAccountModal = function () {
    var registerAccountModal = $modal.open({
      controller: RegisterAccountModalCtrl,
      scope: $scope,
      templateUrl: 'modules/modals/modal-register-account.html'
    });

    registerAccountModal.result.then(
      function () {
        // what to do on modal completion...
      },
      function () {
        // what to do on modal dismiss...
      }
    );
  };

  $scope.showAboutModal = function () {
    $modal.open({
      controller: AboutModalCtrl,
      windowClass: 'temp-modal-lg',
      templateUrl: 'modules/modals/modal-about.html'
    });
  };

});

rcMod.controller('ProfileCtrl', function($scope, $resource, $routeParams, SessionService, RCommAccounts, RCommAccountOperations, md5, Auth, AuthService, $location, Notifications) {
	var accountBackup = {};
		
	/*
	$scope.$watchCollection('[account.friendly_name]', function() {
		console.log("fiendly_name changed!");
	});
	*/
	
	$scope.loggedSid = AuthService.getLoggedSid();
	$scope.userLink = {}; // holds all info regarding account-user linking

  function refreshAllAccounts () {
	// retrieve all sub-accounts for currently logged user
	$scope.accounts = RCommAccounts.all({format:'json'}, function (accounts) {});
  }
  
  function reloadAccount() {
	// if there is another account specified in the location bar, try to load this one
	if ( $routeParams.accountSid ) {
		$scope.account = RCommAccounts.view({format:'json', accountSid: $routeParams.accountSid}, function (account) {
			onAccountReload(account);
		});
		
	} else {  // retrieve currently logged account information
		$scope.account = RCommAccounts.view({format:'json', accountSid:AuthService.getLoggedSid()}, function (account) {
			onAccountReload(account);			
		});
	}
  }
    
  $scope.resetChanges = function() {
    $scope.account = angular.copy(accountBackup);
    //$scope.accountChanged = false;
  };
  
  $scope.updateProfile = function() {
    var params = {FriendlyName: $scope.account.friendly_name, Type: $scope.account.type, Status: $scope.account.status};

    RCommAccounts.update({accountSid:$scope.account.sid}, $.param(params), function() { // success
    	$scope.accounts = RCommAccounts.all({format:'json'}, function (accounts) {});
    }, function() { // error
      $scope.showAlert('error', 'Failure Updating Profile. Please check data and try again.');
    });
  };
  
  function onAccountReload(account) {
	  angular.copy(account, accountBackup);
	  $scope.userLink = {};
	  $scope.userLink.email_address = account.email_address;
  }
  
  $scope.assignApikey = function (account) {
	  RCommAccountOperations.assignKey({accountSid:account.sid},null, function () {
			// reload current account info
			$scope.account = RCommAccounts.view({format:'json', accountSid: account.sid}, function (account) {
				Notifications.success("API Key enabled.");
				onAccountReload(account);
			});
	  });
  }
  
  $scope.revokeApikey = function (account) {
	  RCommAccountOperations.revokeKey({accountSid:account.sid},null, function () {
		// reload current account info
		$scope.account = RCommAccounts.view({format:'json', accountSid: account.sid}, function (account) {
			Notifications.success("API Key disabled.");
			angular.copy(account, accountBackup);
		});
	  });
  };  

  $scope.linkUser = function (account, userLink) {
	  var params;
	  if ( userLink.create )
		params = {username: userLink.email_address, create:userLink.create, friendly_name: accountBackup.friendly_name, password:accountBackup.sid};
	  else
	    params = {username: userLink.email_address};
	  console.log(params);
	  RCommAccountOperations.linkUser({accountSid:account.sid}, $.param(params), function () {
		  Notifications.success("Linked to user '" + userLink.email_address + "'");
		  reloadAccount();
	  });
  }
  
  $scope.unlinkUser = function (account,userLink) {
	  RCommAccountOperations.unlinkUser({accountSid:account.sid}, function () {
		  Notifications.success("Broke link with user '" + userLink.email_address + "'");
		reloadAccount();
	  });
  }
  
  $scope.deleteAccount = function (account) {
	  RCommAccounts.remove({accountSid:account.sid}, function () {
		  Notifications.success('Account "' + account.friendly_name + '" removed.');
		  $location.path("/profile");
	  })
  }
  
  $scope.showAlert = function(type, msg) {
    $scope.alert.type = type;
    $scope.alert.msg = msg;
    $scope.alert.show = true;
  };

  $scope.closeAlert = function() {
    $scope.alert.type = '';
    $scope.alert.msg = '';
    $scope.alert.show = false;
  };
  
  // catch events
  $scope.$on("sub-account-created", function (params) {
	  console.log("sub-account-created received");
	  refreshAllAccounts();
  });
  
    $scope.alert = {};
	refreshAllAccounts();
	reloadAccount();

});

// Register Account Modal

var RegisterAccountModalCtrl = function ($scope, $rootScope, $modalInstance, RCommAccounts, Notifications) {

  $scope.statuses = ['ACTIVE','UNINITIALIZED','SUSPENDED','INACTIVE','CLOSED'];
  $scope.createAccount = function(account) {
      RCommAccounts.register($.param(
        {
          Status: account.status,
          FriendlyName: account.friendlyName
        }),
        function() { // success
          Notifications.success('Account "' + account.friendlyName + '" created successfully!');
          $modalInstance.close();
          $rootScope.$broadcast("sub-account-created", {/*nothing here yet*/});
        },
        function() { // error
          Notifications.error('Account creation failed.');
        }
      );
  };

  $scope.cancel = function () {
    $modalInstance.dismiss('cancel');
  };

};

var AboutModalCtrl = function ($scope, $modalInstance, RCommJMX) {

  $scope.Math = window.Math;

  $scope.getData = function() {
    $scope.info = RCommJMX.get({path: 'java.lang:type=*'},
      function(data){
        $scope.OS = data.value['java.lang:type=OperatingSystem'];
        $scope.JVM = data.value['java.lang:type=Runtime'];
        $scope.Memory = data.value['java.lang:type=Memory'];
        $scope.Threads = data.value['java.lang:type=Threading'];
      },
      function(){}
    );
  };

  $scope.cancel = function () {
    $modalInstance.dismiss('cancel');
  };

  $scope.getData();
};
