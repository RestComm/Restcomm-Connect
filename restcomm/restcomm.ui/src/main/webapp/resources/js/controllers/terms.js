'use strict';

var rcMod = angular.module('rcApp');

rcMod.controller('TermsCtrl', function($scope) {
  $scope.modalShown = false;
  $scope.toggleModal = function() {
    $scope.modalShown = !$scope.modalShown;
  };
});

