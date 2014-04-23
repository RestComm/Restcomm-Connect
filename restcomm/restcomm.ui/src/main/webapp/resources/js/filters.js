'use strict';

/* Filters */

var rcFilters = angular.module('rcApp.filters', []);

rcFilters.filter('startFrom', function() {
  return function(input, start) {
    if(input) {
      start = +start; //parse to int
      return input.slice(start);
    }
    return [];
  }
});

rcFilters.filter('newlines', function () {
  return function(text) {
    return text != null ? unescape(text).replace(/\\n/g, '\n').replace(/\\t/g, '\t').replace(/&/g, '\n').replace(/\+/g, ' ') : null;
  }
});

rcFilters.filter('noHTML', function () {
  return function(text) {
    return text != null ? text
      .replace(/&/g, '&amp;')
      .replace(/>/g, '&gt;')
      .replace(/</g, '&lt;') : null;
  }
});

