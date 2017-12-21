angular.module('rcApp.controllers').controller('ApplicationTemplatesCtrl', function ($scope, SessionService, RvdProjectTemplates) {
    var accountSid = SessionService.get("sid");

    $scope.filter = {};
    $scope.templateList = RvdProjectTemplates.query();
    //$scope.appsList = RCommApplications.query({accountSid: accountSid, includeNumbers: true});

 });
