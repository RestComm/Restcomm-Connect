angular.module('rcApp.controllers').controller('ApplicationsCtrl', function ($scope, RCommApplications, SessionService) {
    var accountSid = SessionService.get("sid");
    $scope.appsList = RCommApplications.query({accountSid: accountSid, includeNumbers: true});
});

angular.module('rcApp.controllers').controller('ApplicationDetailsCtrl', function ($scope, RCommApplications, RvdProjects, SessionService, $stateParams, $location, $dialog, Notifications, $filter, $httpParamSerializer, FileRetriever) {
    var accountSid = SessionService.get("sid");
    var applicationSid = $stateParams.applicationSid;
    $scope.app = RCommApplications.get({accountSid: accountSid, applicationSid: applicationSid}, function () {
        $scope.provider = $filter('appKind')($scope.app.rcml_url);
    });
    // TODO also retrieve IncomingNumbers list for specific application

    $scope.confirmApplicationDelete = function(app) {
        confirmApplicationDelete(app, $dialog, $scope, Notifications, RCommApplications, RvdProjects, $location)
    }

    $scope.editInDesigner = function(app) {
        window.open("/restcomm-rvd#/designer/" + app.sid + "=" + app.friendly_name); // TODO maybe escape friendly_name ???
    }

    $scope.saveExternalApp = function(app) {
        RCommApplications.save({accountSid: accountSid, applicationSid: applicationSid}, $httpParamSerializer({RcmlUrl: app.rcml_url}), function () {
            Notifications.success("Application '" + app.friendly_name + " ' saved");
            $location.path( "/applications" );
        });
    }

    $scope.downloadRvdApp = function(app) {
        var downloadUrl =  '/restcomm-rvd/services/projects/' + app.sid + '/archive?projectName=' + app.friendly_name; // TODO remove '/restcomm-rvd/' hardcoded value and use one from PublicConfig service
        FileRetriever.download(downloadUrl, app.friendly_name + ".zip").catch(function () {
            notifications.error("Error downloading project archive");
        });
    }
});

var confirmApplicationDelete = function(app, $dialog, $scope, Notifications, RCommApplications, RvdProjects, $location) {
  var title = 'Delete application \'' + app.friendly_name + '\'';
  var msg = 'Are you sure you want to delete application ' + app.sid + ' (' + app.friendly_name +  ') ? This action cannot be undone.';
  var btns = [{result:'cancel', label: 'Cancel', cssClass: 'btn-default'}, {result:'confirm', label: 'Delete!', cssClass: 'btn-danger'}];

  $dialog.messageBox(title, msg, btns)
    .open()
    .then(function(result) {
      console.log("Provider: " + $scope.provider);
      if (result == "confirm") {
        if ($scope.provider == 'rvd') {
            RvdProjects.delete({applicationSid:app.sid}, {},
                function() {
                    Notifications.success('RestComm Application "' + app.friendly_name + '" has been deleted.');
                    $location.path( "/applications" );
                },
                function() {
                    Notifications.error('Failed to delete application "' + app.friendly_name + '".');
                }
            );
        } else
        if ($scope.provider == 'external') {
            RCommApplications.delete({accountSid: app.account_sid, applicationSid:app.sid}, {},
               function() {
                    Notifications.success('RestComm Application "' + app.friendly_name + '" has been deleted.');
                    $location.path( "/applications" );
               },
               function() {
                   Notifications.error('Failed to delete application "' + app.friendly_name + '".');
               })
        }
      }
    });
};