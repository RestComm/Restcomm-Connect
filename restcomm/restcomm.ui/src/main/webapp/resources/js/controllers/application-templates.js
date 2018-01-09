'use strict';

angular.module('rcApp.controllers').controller('ApplicationTemplatesCtrl', function ($scope, $state, SessionService, RvdProjectTemplates) {
  var accountSid = SessionService.get("sid");

  $scope.queryTags = {tagVoice:true, tagSms: true, tagUssd: true};
  $scope.query = {};
  $scope.templateList = RvdProjectTemplates.query();
  $scope.blankVoiceTemplate = {id: 'BLANK', name: 'Blank', description: 'Empty application', tags: ['voice','sms','ussd']};
  //$scope.appsList = RCommApplications.query({accountSid: accountSid, includeNumbers: true});

  $scope.templateClicked = function (template) {
    $state.go('restcomm.application-creation-from-template', {templateId: template.id});
  }

});

rcDirectives.directive('applicationTemplate', function () {
  return {
    restrict: 'E',
    templateUrl: 'modules/application-template-item.html',
    scope: {
      template: '='
    },
    link: function( scope, element, attrs) {
    }
  }
});

// takes a query object as a parameter like: {tagVoice:true, tagSms: true, tagUssd: true}
rcFilters.filter('tagFilter', function () {
  return function(input, filter) {
    if (!input || !Array.isArray(input))
      return input;
    var resultTemplates = [];
    for (var i=0; i < input.length; i++) {
      var template = input[i];
      // check tags
      if ( (filter.tagVoice && template.tags.indexOf('voice') != -1) || (filter.tagSms && template.tags.indexOf('sms') != -1) || (filter.tagUssd && template.tags.indexOf('ussd') != -1) ) {
        resultTemplates.push(template);
      }
    }
    return resultTemplates;
  }

});

