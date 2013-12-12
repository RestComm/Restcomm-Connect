function LoginCtrl($scope, $location, $rootScope, AuthService) {

    $scope.alerts = [];

    $scope.credentials = {
        host: window.location.host,
        sid: "administrator@company.com",
        token: "RestComm"
    };

    $scope.login = function() {
        AuthService.login($scope.credentials).success(function() {
            $location.path('/dashboard');
        });
    };

    $scope.closeAlert = function(index) {
        if($scope.closeAlertTimer) {
            clearTimeout($scope.closeAlertTimer);
            $scope.closeAlertTimer = null;
        }
        $scope.alerts.splice(index, 1);
    };

}

function MainCtrl($rootScope, $scope, $http, SessionService) {
}

function DashboardCtrl($scope, $resource, SessionService) {
    $scope.sid = SessionService.get("sid");

    // TEMPORARY... FIXME!
    var Account = $resource('/restcomm/2012-04-24/Accounts.:format/:accountSid',
        { accountSid:$scope.sid, format:'json' },
        {
            // charge: {method:'POST', params:{charge:true}}
        });

    $scope.accountData = Account.get();
}

// Profile ---------------------------------------------------------------------

function ProfileCtrl($scope, $resource, $routeParams, SessionService, md5) {
    $scope.sid = SessionService.get("sid");

    // TEMPORARY... FIXME!
    var Accounts = $resource('/restcomm/2012-04-24/Accounts.:format',
        { format:'json' },
        {
            // TEMPORARY.. FIXME!
            view: {method: 'GET', url: '/restcomm/2012-04-24/Accounts.:format/:accountSid'},
            update: {method:'PUT', url: '/restcomm/2012-04-24/Accounts/:accountSid.:format', headers : {'Content-Type': 'application/x-www-form-urlencoded'}}
        }
    );

    var accountBackup;

    $scope.$watch('account', function() {
        if (!angular.equals($scope.account, accountBackup)) {
            $scope.accountChanged = true;
            // console.log("CHANGED: " + $scope.accountChanged + " => VALID:" + $scope.profileForm.$valid);
        }
    }, true);

    $scope.newPassword = $scope.newPassword2 = "";

    $scope.$watchCollection('[newPassword, newPassword2]', function() {
        if($scope.newPassword == "" && $scope.newPassword2 == "") {
            $scope.profileForm.newPassword.$valid = $scope.profileForm.newPassword2.$valid = true;
            $scope.accountValid = $scope.profileForm.$valid;
            if($scope.account) {
                $scope.account.auth_token = accountBackup.auth_token;
            }
            return;
        }
        var valid = angular.equals($scope.newPassword, $scope.newPassword2);
        $scope.profileForm.newPassword.$valid = $scope.profileForm.newPassword2.$valid = valid;
        $scope.accountValid = $scope.profileForm.$valid && valid;
        $scope.account.auth_token = "<modified></modified>";
        // console.log("NP [" + $scope.profileForm.newPassword.$valid + "] NP2 [" + $scope.profileForm.newPassword2.$valid + "] FORM [" + $scope.profileForm.$valid + "]");
    }, true);

    $scope.resetChanges = function() {
        $scope.newPassword = $scope.newPassword2 = "";
        $scope.account = angular.copy(accountBackup);
        $scope.accountChanged = false;
    }

    $scope.updateProfile = function() {
        var params = {FriendlyName: $scope.account.friendly_name, Type: $scope.account.type, Status: $scope.account.status};

        if($scope.newPassword != "" && $scope.profileForm.newPassword.$valid) {
            params["Auth_Token"] = md5.createHash($scope.newPassword);
        }

        Accounts.update({accountSid:$scope.account.sid}, $.param(params), function() { // success
            if($scope.account.sid = SessionService.get("sid")) {
                SessionService.set('logged_user', $scope.account.friendly_name);
            }
            $scope.showAlert("success", "Profile Updated Successfully.");
            $scope.getAccounts();
        }, function() { // error
            // TODO: Show alert
            $scope.showAlert("error", "Failure Updating Profile. Please check data and try again.");
        });
        ;
    }

    $scope.alert = {}

    $scope.showAlert = function(type, msg) {
        $scope.alert.type = type;
        $scope.alert.msg = msg;
        $scope.alert.show = true;
    }

    $scope.closeAlert = function() {
        $scope.alert.type = "";
        $scope.alert.msg = "";
        $scope.alert.show = false;
    }

    // Start with querying for accounts...
    $scope.getAccounts = function() {
        $scope.accounts = Accounts.query(function(data){
            angular.forEach(data, function(value){
                if(value.sid == $routeParams.accountSid) {
                    $scope.account = angular.copy(value);
                    accountBackup = angular.copy(value);
                }
            });
            $scope.resetChanges();
        });
    }

    $scope.getAccounts();

}


// Numbers : Incoming ----------------------------------------------------------

function NumbersCtrl($scope, $resource, $dialog, SessionService) {
    $scope.sid = SessionService.get("sid");

    var Numbers = $resource('/restcomm/2012-04-24/Accounts/:accountSid/IncomingPhoneNumbers.:format',
        { accountSid:$scope.sid, format:'json' },
        {
            delete: {method:'DELETE', url: '/restcomm/2012-04-24/Accounts/:accountSid/IncomingPhoneNumbers/:phoneSid.:format'},
            register: {method:'POST', headers : {'Content-Type': 'application/x-www-form-urlencoded'}},
            update: {method:'POST', url: '/restcomm/2012-04-24/Accounts/:accountSid/IncomingPhoneNumbers/:phoneSid.:format', headers : {'Content-Type': 'application/x-www-form-urlencoded'}}
        }
    );

    var Apps = $resource('/restcomm-rvd/services/manager/projects/list');

    // edit incoming number friendly name --------------------------------------
    $scope.editingFriendlyName = "";

    $scope.setFriendlyName = function(pn) {
        var params = {PhoneNumber: pn.phone_number, FriendlyName: pn.friendly_name};

        Numbers.update({phoneSid:pn.sid}, $.param(params), function() { // success
            $scope.editingFriendlyName = "";
        }, function() { // error
            // TODO: Show alert
        });
    }

    // add incoming number -----------------------------------------------------

    $scope.modalOpts = {
        backdropFade: true,
        dialogFade: true
    };

    $scope.showRegisterIncomingNumber = function () {
        $scope.newNumber = [];
        $scope.registeringNewNumber = true;
        $scope.isCollapsed = true;
        $scope.availableApps = Apps.query();
    };

    $scope.closeRegisterIncomingNumber = function () {
        $scope.registeringNewNumber = false;
    };

    var createNumberParams = function(number) {
        var params = {};

        // Mandatory fields
        if(number.number) {
            params["PhoneNumber"] = number.number;
        }
        else if(number.areaCode) {
            params["AreaCode"] = number.areaCode;
        }
        else {
            alert("You must provide either Number or Area Code.");
        }

        // Optional fields
        if (number.friendlyName) {
            params["FriendlyName"] = number.friendlyName;
        }
        if (number.voiceURL) {
            params["VoiceUrl"] = number.voiceURL;
            params["VoiceMethod"] = number.voiceMethod;
        }
        if (number.voiceFallbackURL) {
            params["VoiceFallbackUrl"] = number.voiceFallbackURL;
            params["VoiceFallbackMethod"] = number.voiceFallbackMethod;
        }
        if (number.statusCallbackURL) {
            params["StatusCallback"] = number.statusCallbackURL;
            params["StatusCallbackMethod"] = number.statusCallbackMethod;
        }
        if (number.smsURL) {
            params["SmsUrl"] = number.smsURL;
            params["SmsMethod"] = number.smsMethod;
        }
        if (number.smsFallbackURL) {
            params["SmsFallbackUrl"] = number.smsFallbackURL;
            params["SmsFallbackMethod"] = number.smsFallbackMethod;
        }

        return params;
    }

    $scope.registerIncomingNumber = function(number) {
        var params = createNumberParams(number);
        Numbers.register($.param(params), function() { // success
            // TODO: Show alert
            $scope.registeringNewNumber = false;
            $scope.numbersList = Numbers.query({accountSid:$scope.sid});
        }, function() { // error
            // TODO: Show alert
        });
    };


    // delete incoming number --------------------------------------------------

    $scope.confirmNumberDelete = function(phone) {
        var title = 'Delete Number ' + phone.phone_number;
        var msg = 'Are you sure you want to delete incoming number ' + phone.phone_number + ' (' + phone.friendly_name +  ') ? This action cannot be undone.';
        var btns = [{result:'cancel', label: 'Cancel'}, {result:'confirm', label: 'Delete!', cssClass: 'btn-danger'}];

        $dialog.messageBox(title, msg, btns)
            .open()
            .then(function(result) {
                if (result == "confirm") {
                    Numbers.delete({accountSid:$scope.sid, phoneSid:phone.sid}, {}, function() {
                        // TODO: Show alert on delete success...
                        $scope.numbersList = Numbers.query({accountSid:$scope.sid});
                    }, function() {
                        // TODO: Show alert on delete failure...
                    });
                }
            });
    };

    $scope.numbersList = Numbers.query({accountSid:$scope.sid});
}

// Numbers : Incoming : Details ------------------------------------------------

function NumberDetailsCtrl($scope, $routeParams, $resource, SessionService) {
    $scope.sid = SessionService.get("sid");
    $scope.phoneSid = $routeParams.phoneSid;

    // FIXME: we (may) have data from previous controller..
    var Number = $resource('/restcomm/2012-04-24/Accounts/:accountSid/IncomingPhoneNumbers/:phoneSid.:format',
        { accountSid:$scope.sid, phoneSid:$scope.phoneSid, format:'json' },
        {
            delete: {method:'DELETE', url: '/restcomm/2012-04-24/Accounts/:accountSid/IncomingPhoneNumbers/:phoneSid.:format'},
            register: {method:'POST', headers : {'Content-Type': 'application/x-www-form-urlencoded'}}
        }
    );

    // FIXME: DUPLICATE CODE.. BLERGH

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
        if (number.status_callback) {
            params["StatusCallback"] = number.status_callback;
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
        params["VoiceCallerIdLookup"] = number.voice_caller_id_lookup;

        return params;
    }

    $scope.registerIncomingNumber = function(number) {
        var params = createNumberParams(number);
        Number.register($.param(params), function() { // success
            // TODO: Show alert
        }, function() { // error
            // TODO: Show alert
        });
    };

    // END OF FIXME: DUPLICATE CODE.. BLERGH

    $scope.numberDetails = Number.get();
}

// Numbers : SIP Clients -------------------------------------------------------

function ClientsCtrl($scope, $resource, $dialog, SessionService) {
    $scope.sid = SessionService.get("sid");

    var Clients = $resource('/restcomm/2012-04-24/Accounts/:accountSid/Clients.:format',
        { accountSid:$scope.sid, format:'json' },
        {
            delete: {method:'DELETE', url: '/restcomm/2012-04-24/Accounts/:accountSid/Clients/:clientSid.:format'},
            register: {method:'POST', headers : {'Content-Type': 'application/x-www-form-urlencoded'}},
            update: {method:'POST', url: '/restcomm/2012-04-24/Accounts/:accountSid/Clients/:clientSid.:format', headers : {'Content-Type': 'application/x-www-form-urlencoded'}}
        }
    );

    var Apps = $resource('/restcomm-rvd/services/manager/projects/list');

    // edit incoming client friendly name --------------------------------------
    $scope.editingFriendlyName = "";

    $scope.setFriendlyName = function(client) {
        var params = {Login: client.login, FriendlyName: client.friendly_name};

        Clients.update({clientSid:client.sid}, $.param(params), function() { // success
            $scope.editingFriendlyName = "";
        }, function() { // error
            // TODO: Show alert
        });
    }

    // add incoming client -----------------------------------------------------

    $scope.modalOpts = {
        backdropFade: true,
        dialogFade: true
    };

    $scope.showRegisterSIPClient = function () {
        $scope.newClient = [];
        $scope.registeringNewClient = true;
        $scope.isCollapsed = true;
        $scope.availableApps = Apps.query();
    };

    $scope.closeRegisterSIPClient = function () {
        $scope.registeringNewClient = false;
    };

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
        if (client.friendlyName) {
            params["FriendlyName"] = client.friendlyName;
        }
        if (client.voiceURL) {
            params["VoiceUrl"] = client.voiceURL;
            params["VoiceMethod"] = client.voiceMethod;
        }
        if (client.voiceFallbackURL) {
            params["VoiceFallbackUrl"] = client.voiceFallbackURL;
            params["VoiceFallbackMethod"] = client.voiceFallbackMethod;
        }
        if (client.statusCallbackURL) {
            params["StatusCallback"] = client.statusCallbackURL;
            params["StatusCallbackMethod"] = client.statusCallbackMethod;
        }
        if (client.smsURL) {
            params["SmsUrl"] = client.smsURL;
            params["SmsMethod"] = client.smsMethod;
        }
        if (client.smsFallbackURL) {
            params["SmsFallbackUrl"] = client.smsFallbackURL;
            params["SmsFallbackMethod"] = client.smsFallbackMethod;
        }

        return params;
    }

    $scope.registerSIPClient = function(client) {
        var params = createSIPClientParams(client);
        Clients.register($.param(params), function() { // success
            // TODO: Show alert
            $scope.registeringNewClient = false;
            $scope.clientsList = Clients.query({accountSid:$scope.sid});
        }, function() { // error
            // TODO: Show alert
        });
    };


    // delete sip client -------------------------------------------------------

    $scope.confirmClientDelete = function(client) {
        var title = 'Delete SIP Client \'' + client.login + '\'';
        var msg = 'Are you sure you want to delete SIP Client ' + client.login + ' (' + client.friendly_name +  ') ? This action cannot be undone.';
        var btns = [{result:'cancel', label: 'Cancel'}, {result:'confirm', label: 'Delete!', cssClass: 'btn-danger'}];

        $dialog.messageBox(title, msg, btns)
            .open()
            .then(function(result) {
                if (result == "confirm") {
                    Clients.delete({accountSid:$scope.sid, clientSid:client.sid}, {}, function() {
                        // TODO: Show alert on delete success...
                        $scope.clientsList = Clients.query({accountSid:$scope.sid});
                    }, function() {
                        // TODO: Show alert on delete failure...
                    });
                }
            });
    };

    $scope.clientsList = Clients.query({accountSid:$scope.sid});
}

// Numbers : SIP Clients : Details ------------------------------------------------

function ClientsDetailsCtrl($scope, $routeParams, $resource, $dialog, $location, SessionService) {
    $scope.sid = SessionService.get("sid");
    $scope.clientSid = $routeParams.clientSid;

    // FIXME: we (may) have data from previous controller..
    var Client = $resource('/restcomm/2012-04-24/Accounts/:accountSid/Clients/:clientSid.:format',
        { accountSid:$scope.sid, clientSid:$scope.clientSid, format:'json' },
        {
            delete: {method:'DELETE', url: '/restcomm/2012-04-24/Accounts/:accountSid/Clients/:clientSid.:format'},
            register: {method:'POST', headers : {'Content-Type': 'application/x-www-form-urlencoded'}}
        }
    );

    // FIXME: DUPLICATE CODE.. BLERGH

    var createClientParams = function(client) {
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
        if (client.status_callback) {
            params["StatusCallback"] = client.status_callback;
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
        params["VoiceCallerIdLookup"] = client.voice_caller_id_lookup;

        return params;
    }

    $scope.registerSIPClient = function(client) {
        var params = createClientParams(client);
        Client.register($.param(params), function() { // success
            // TODO: Show alert
        }, function() { // error
            // TODO: Show alert
        });
    };

    $scope.confirmClientDelete = function(client) {
        var title = 'Delete SIP Client \'' + client.login + '\'';
        var msg = 'Are you sure you want to delete SIP Client ' + client.login + ' (' + client.friendly_name +  ') ? This action cannot be undone.';
        var btns = [{result:'cancel', label: 'Cancel'}, {result:'confirm', label: 'Delete!', cssClass: 'btn-danger'}];

        $dialog.messageBox(title, msg, btns)
            .open()
            .then(function(result) {
                if (result == "confirm") {
                    Client.delete({accountSid:$scope.sid, clientSid:client.sid}, {}, function() {
                        // TODO: Show alert on delete success...
                        $location.path( "/numbers/clients/" );
                    }, function() {
                        // TODO: Show alert on delete failure...
                    });
                }
            });
    };

    // END OF FIXME: DUPLICATE CODE.. BLERGH

    $scope.clientDetails = Client.get();
}

// Numbers : Outgoing ----------------------------------------------------------

function OutgoingCtrl($scope, $resource, $dialog, SessionService) {
    $scope.sid = SessionService.get("sid");

    var OutgoingCallerIDs = $resource('/restcomm/2012-04-24/Accounts/:accountSid/OutgoingCallerIds.:format',
        { accountSid:$scope.sid, format:'json' },
        {
            delete: {method:'DELETE', url: '/restcomm/2012-04-24/Accounts/:accountSid/OutgoingCallerIds/:phoneSid.:format'},
            register: {method:'POST', headers : {'Content-Type': 'application/x-www-form-urlencoded'}}
        }
    );

    $scope.modalOpts = {
        backdropFade: true,
        dialogFade: true
    };

    $scope.showRegisterOutgoingNumber = function () {
        $scope.newNumber = [];
        $scope.registeringNewNumber = true;
        $scope.isCollapsed = true;
    };

    $scope.closeRegisterOutgoingNumber = function () {
        $scope.registeringNewNumber = false;
    };

    $scope.registerOutgoingNumber = function(number) {
        if(number.number) {
            // Numbers.register({PhoneNumber:number.number});
            OutgoingCallerIDs.register($.param({
                PhoneNumber : number.number
            }), function() { // success
                // TODO: Show alert
                $scope.registeringNewNumber = false;
                $scope.outgoingList = OutgoingCallerIDs.query({accountSid:$scope.sid});
            }, function() { // error
                // TODO: Show alert
            });
        }
        else {
            alert("You must provide a Phone Number.");
        }
    };


    // delete incoming number --------------------------------------------------

    $scope.confirmOutgoingNumberDelete = function(phone) {
        var title = 'Delete Outgoing Caller ID ' + phone.phone_number;
        var msg = 'Are you sure you want to delete outgoing number ' + phone.phone_number + ' (' + phone.friendly_name +  ') ? This action cannot be undone.';
        var btns = [{result:'cancel', label: 'Cancel'}, {result:'confirm', label: 'Delete!', cssClass: 'btn-danger'}];

        $dialog.messageBox(title, msg, btns)
            .open()
            .then(function(result) {
                if (result == "confirm") {
                    OutgoingCallerIDs.delete({accountSid:$scope.sid, phoneSid:phone.sid}, {}, function() {
                        // TODO: Show alert on delete success...
                        $scope.outgoingList = OutgoingCallerIDs.query({accountSid:$scope.sid});
                    }, function() {
                        // TODO: Show alert on delete failure...
                    });
                }
            });
    };

    $scope.outgoingList = OutgoingCallerIDs.query({accountSid:$scope.sid});
}

function MenuCtrl($scope, $http, $resource, AuthService, SessionService) {
    $scope.auth = AuthService;
    $scope.sid = SessionService.get("sid");

    $scope.logout = function() {
        AuthService.logout();
        $http.get('/restcomm/2012-04-24/Logout')/*.
            success(function() {console.log("Logged out from API.");}).
            error(function() {console.log("Failed to logout from API.");})*/;
    };

    //$scope.$watch(AuthService.isLoggedIn, function() { $scope.currentAccount.friendly_name = AuthService.getFriendlyName(); });

    var Accounts = $resource('/restcomm/2012-04-24/Accounts.:format',
        { accountSid:$scope.sid, format:'json' },
        {
            // TEMPORARY.. FIXME!
            view: {method: 'GET', url: '/restcomm/2012-04-24/Accounts.:format/:accountSid'},
            register: {method:'POST', headers : {'Content-Type': 'application/x-www-form-urlencoded'}}
        }
    );

    if(AuthService.isLoggedIn()) {
        var accountsList = Accounts.query(function() {
            $scope.accountsList = accountsList;
            for (x in accountsList){
                if(accountsList[x].sid == $scope.sid) {
                    $scope.currentAccount = accountsList[x];
                }
            }
        });
    }

    // add account -------------------------------------------------------------

    $scope.modalOpts = {
        backdropFade: true,
        dialogFade: true
    };

    $scope.showRegisterAccount = function () {
        $scope.newAccount = [];
        $scope.registeringNewAccount = true;
    };

    $scope.closeRegisterAccount = function () {
        $scope.registeringNewAccount = false;
    };

    $scope.createAccount = function(account) {
        if(account.email && account.password) {
            // Numbers.register({PhoneNumber:number.number});
            Accounts.register($.param({
                EmailAddress : account.email,
                Password: account.password,
                //Role: account.role,
                //Status: account.status,
                FriendlyName: account.friendlyName ? account.friendlyName : account.email
            }), function() { // success
                // TODO: Show alert
                $scope.registeringNewAccount = false;
                //$scope.outgoingList = OutgoingCallerIDs.query({accountSid:$scope.sid});
            }, function() { // error
                // TODO: Show alert
            });
        }
        else {
            alert("You must provide a Phone Number.");
        }
    };
}

function LogsCallsCtrl($scope, $resource, $timeout, SessionService) {

    $scope.Math = window.Math;

    $scope.sid = SessionService.get("sid");

    var Calls = $resource('/restcomm/2012-04-24/Accounts/:accountSid/Calls.:format',
        { accountSid:$scope.sid, format:'json'},
        {
            view: {method: 'GET', url: '/restcomm/2012-04-24/Accounts/:accountSid/Calls/:callSid.:format'},
            call: {method:'POST', headers : {'Content-Type': 'application/x-www-form-urlencoded'}},
            search: {method:'GET', headers : {'Content-Type': 'application/x-www-form-urlencoded'}}
        }
    );

    // pagination support ----------------------------------------------------------------------------------------------

    $scope.currentPage = 1; //current page
    $scope.maxSize = 5; //pagination max size
    $scope.entryLimit = 10; //max rows for data table

    $scope.setEntryLimit = function(limit) {
        $scope.entryLimit = limit;
        $scope.currentPage = 1;
        $scope.getCallsList($scope.currentPage-1);
    };

    $scope.pageChanged = function(page) {
        $scope.getCallsList(page-1);
    };

    $scope.getCallsList = function(page) {
        var params = $scope.search ? createSearchParams($scope.search) : undefined;
        Calls.search($.extend(params, {Page:page, PageSize:$scope.entryLimit}), function(data) {
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
    $scope.showingCallDetails = false;

    $scope.showCallDetails = function(message) {
        $scope.callDetails = message;
        $scope.showingCallDetails = true;
    }

    $scope.closeCallDetails = function() {
        $scope.callDetails = null;
        $scope.showingCallDetails = false;
    }

    // initialize with a query
    $scope.getCallsList(0);
}

function LogsCallsDetailsCtrl($scope, $routeParams, $resource, SessionService) {
    $scope.sid = SessionService.get("sid");
    $scope.callSid = $routeParams.callSid;

    // FIXME Don't duplicate this from LogsCallsCtrl...
    var Calls = $resource('/restcomm/2012-04-24/Accounts/:accountSid/Calls.:format',
        { accountSid:$scope.sid, format:'json' },
        {
            view: {method: 'GET', url: '/restcomm/2012-04-24/Accounts/:accountSid/Calls/:callSid.:format'},
            call: {method:'POST', headers : {'Content-Type': 'application/x-www-form-urlencoded'}}
        }
    );

    $scope.callDetails = Calls.view({callSid:$scope.callSid});
}

// Logs / Messages -------------------------------------------------------------

function LogsMessagesCtrl($scope, $resource, $timeout, SessionService) {

    $scope.Math = window.Math;

    $scope.sid = SessionService.get("sid");

    var Messages = $resource('/restcomm/2012-04-24/Accounts/:accountSid/SMS/Messages.:format',
        { accountSid:$scope.sid, format:'json' },
        {
            view: {method: 'GET', url: '/restcomm/2012-04-24/Accounts/:accountSid/SMS/Messages/:smsMessageSid.:format'},
            send: {method:'POST', headers : {'Content-Type': 'application/x-www-form-urlencoded'}}
        }
    );

    // pagination support ----------------------------------------------------------------------------------------------

    $scope.currentPage = 1; //current page
    $scope.maxSize = 5; //pagination max size
    $scope.entryLimit = 10; //max rows for data table

    $scope.setEntryLimit = function(limit) {
        $scope.entryLimit = limit;
        $scope.noOfPages = Math.ceil($scope.filtered.length / $scope.entryLimit);
    };

    $scope.setPage = function(pageNo) {
        $scope.currentPage = pageNo;
    };

    $scope.filter = function() {
        $timeout(function() { //wait for 'filtered' to be changed
            /* change pagination with $scope.filtered */
            $scope.noOfPages = Math.ceil($scope.filtered.length / $scope.entryLimit);
        }, 10);
    };

    // Modal : Message Details
    $scope.showingMessageDetails = false;

    $scope.showMessageDetails = function(message) {
        $scope.messageDetails = message;
        $scope.showingMessageDetails = true;
    }

    $scope.closeMessageDetails = function() {
        $scope.messageDetails = null;
        $scope.showingMessageDetails = false;
    }

    // initialize with a query
    $scope.messagesLogsList = Messages.query(function() {
        $scope.noOfPages = Math.ceil($scope.messagesLogsList.length / $scope.entryLimit);
    });

}

function LogsRecordingsCtrl($scope, $resource, $timeout, SessionService) {

    $scope.Math = window.Math;

    $scope.sid = SessionService.get("sid");

    var Recordings = $resource('/restcomm/2012-04-24/Accounts/:accountSid/Recordings.:format',
        { accountSid:$scope.sid, format:'json' },
        {
            view: {method: 'GET', url: '/restcomm/2012-04-24/Accounts/:accountSid/Recordings/:recordingSid.:format'},
            delete: {method:'DELETE',  url: '/restcomm/2012-04-24/Accounts/:accountSid/Recordings/:recordingSid.:format'}
        }
    );

    // pagination support ----------------------------------------------------------------------------------------------

    $scope.currentPage = 1; //current page
    $scope.maxSize = 5; //pagination max size
    $scope.entryLimit = 10; //max rows for data table

    $scope.setEntryLimit = function(limit) {
        $scope.entryLimit = limit;
        $scope.noOfPages = Math.ceil($scope.filtered.length / $scope.entryLimit);
    };

    $scope.setPage = function(pageNo) {
        $scope.currentPage = pageNo;
    };

    $scope.filter = function() {
        $timeout(function() { //wait for 'filtered' to be changed
            /* change pagination with $scope.filtered */
            $scope.noOfPages = Math.ceil($scope.filtered.length / $scope.entryLimit);
        }, 10);
    };

    // Modal : Recording Details
    $scope.showingRecordingDetails = false;

    $scope.showRecordingDetails = function(message) {
        $scope.recordingDetails = message;
        $scope.showingRecordingDetails = true;
    }

    $scope.closeRecordingDetails = function() {
        $scope.recordingDetails = null;
        $scope.showingRecordingDetails = false;
    }

    // initialize with a query
    $scope.recordingsLogsList = Recordings.query(function() {
        $scope.noOfPages = Math.ceil($scope.recordingsLogsList.length / $scope.entryLimit);
    });

}

function LogsTranscriptionsCtrl($scope, $resource, $timeout, SessionService) {

    $scope.Math = window.Math;

    $scope.sid = SessionService.get("sid");

    var Transcriptions = $resource('/restcomm/2012-04-24/Accounts/:accountSid/Transcriptions.:format',
        { accountSid:$scope.sid, format:'json' },
        {
            view: {method: 'GET', url: '/restcomm/2012-04-24/Accounts/:accountSid/Transcriptions/:transcriptionSid.:format'},
            delete: {method:'DELETE',  url: '/restcomm/2012-04-24/Accounts/:accountSid/Transcriptions/:transcriptionSid.:format'}
        }
    );

    // pagination support ----------------------------------------------------------------------------------------------

    $scope.currentPage = 1; //current page
    $scope.maxSize = 5; //pagination max size
    $scope.entryLimit = 10; //max rows for data table

    $scope.setEntryLimit = function(limit) {
        $scope.entryLimit = limit;
        $scope.noOfPages = Math.ceil($scope.filtered.length / $scope.entryLimit);
    };

    $scope.setPage = function(pageNo) {
        $scope.currentPage = pageNo;
    };

    $scope.filter = function() {
        $timeout(function() { //wait for 'filtered' to be changed
            /* change pagination with $scope.filtered */
            $scope.noOfPages = Math.ceil($scope.filtered.length / $scope.entryLimit);
        }, 10);
    };

    // Modal : Transcription Details
    $scope.showingTranscriptionDetails = false;

    $scope.showTranscriptionDetails = function(message) {
        $scope.transcriptionDetails = message;
        $scope.showingTranscriptionDetails = true;
    }

    $scope.closeTranscriptionDetails = function() {
        $scope.transcriptionDetails = null;
        $scope.showingTranscriptionDetails = false;
    }

    // initialize with a query
    $scope.transcriptionsLogsList = Transcriptions.query(function() {
        $scope.noOfPages = Math.ceil($scope.transcriptionsLogsList.length / $scope.entryLimit);
    });

}

function LogsNotificationsCtrl($scope, $resource, $timeout, SessionService) {

    $scope.Math = window.Math;

    $scope.sid = SessionService.get("sid");

    var Notifications = $resource('/restcomm/2012-04-24/Accounts/:accountSid/Notifications.:format',
        { accountSid:$scope.sid, format:'json' },
        {
            view: {method: 'GET', url: '/restcomm/2012-04-24/Accounts/:accountSid/Notifications/:notificationSid.:format'},
            delete: {method:'DELETE',  url: '/restcomm/2012-04-24/Accounts/:accountSid/Notifications/:notificationSid.:format'}
        }
    );

    // pagination support ----------------------------------------------------------------------------------------------

    $scope.currentPage = 1; //current page
    $scope.maxSize = 5; //pagination max size
    $scope.entryLimit = 10; //max rows for data table

    $scope.setEntryLimit = function(limit) {
        $scope.entryLimit = limit;
        $scope.noOfPages = Math.ceil($scope.filtered.length / $scope.entryLimit);
    };

    $scope.setPage = function(pageNo) {
        $scope.currentPage = pageNo;
    };

    $scope.filter = function() {
        $timeout(function() { //wait for 'filtered' to be changed
            /* change pagination with $scope.filtered */
            $scope.noOfPages = Math.ceil($scope.filtered.length / $scope.entryLimit);
        }, 10);
    };

    // Modal : Notification Details
    $scope.modalOpts = {
        backdropFade: true,
        dialogFade: true,
        dialogClass: 'modal modal-large'
    };

    $scope.showingNotificationDetails = false;

    $scope.showNotificationDetails = function(message) {
        $scope.notificationDetails = message;
        $scope.showingNotificationDetails = true;
    }

    $scope.closeNotificationDetails = function() {
        $scope.notificationDetails = null;
        $scope.showingNotificationDetails = false;
    }

    // initialize with a query
    $scope.notificationsLogsList = Notifications.query(function() {
        $scope.noOfPages = Math.ceil($scope.notificationsLogsList.length / $scope.entryLimit);
    });

}

