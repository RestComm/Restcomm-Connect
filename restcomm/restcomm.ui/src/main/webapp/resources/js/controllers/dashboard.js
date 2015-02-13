'use strict';

var rcMod = angular.module('rcApp');

rcMod.controller('DashboardCtrl', function ($scope, $resource, $route, $rootScope, SessionService, RCommStatistics) {
  $scope.sid = SessionService.get("sid");

  // TEMPORARY... FIXME!
  var Account = $resource('/restcomm/2012-04-24/Accounts.:format/:accountSid',
    { accountSid: $scope.sid, format: 'json' },
    {
      // charge: {method:'POST', params:{charge:true}}
    });

  $scope.accountData = Account.get();

  var startDate = new Date(new Date().setDate(new Date().getDate()-30)).toJSON().slice(0,10);
  var endDate = new Date().toJSON().slice(0,10);
  RCommStatistics.query({accountSid: $scope.sid, statName: 'Daily', StartDate: startDate, EndDate: endDate}, function(data) {
    $scope.callsData = [
      {
        "key" : "Calls" ,
        "bar": true,
        "values" : []
      },
      {
        "key" : "Minutes" ,
        "color": '#EBAA77',
        "stroke-width": '5px',
        "values" : []
      }
    ];
    var maxDuration = 0;
    var maxCalls = 0;
    angular.forEach(data, function(value,key){
      //console.log(value.start_date, value.count, value.usage);
      $scope.callsData[0].values.push([value.start_date, value.count || 0]);
      $scope.callsData[1].values.push([value.start_date, value.usage || 0]);
      maxDuration = Math.max(maxDuration, value.usage);
      maxCalls = Math.max(maxCalls, value.count);
    });
    $scope.options.chart.lines.yDomain = [0, maxDuration];
    $scope.options.chart.bars.yDomain = [0, maxCalls];
    $scope.options.chart.xAxis.ticks = $scope.callsData[1].values.length;
    $scope.callsAPI.updateWithOptions($scope.options);
  });

  RCommStatistics.query({accountSid: $scope.sid, statName: 'Daily', Category: 'SMS', StartDate: startDate, EndDate: endDate}, function(data) {
    $scope.smsData = [
      {
        "key" : "Messages" ,
        "bar": true,
        "values" : []
      }
    ];
    var maxSMS = 0;
    angular.forEach(data, function(value,key){
      //console.log(value.start_date, value.count);
      $scope.smsData[0].values.push([value.start_date, value.count || 0]);
      maxSMS = Math.max(maxSMS, value.count);
    });
    $scope.smsOptions.chart.yDomain = [0, maxSMS];
    $scope.smsAPI.updateWithOptions($scope.smsOptions);
  });

  //$scope.chartTypes = ['bulletChart','cumulativeLineChart','discreteBarChart','donutChart','historicalBarChart','lineChart','linePlusBarChart','linePlusBarWithFocusChart','lineWithFisheyeChart','lineWithFocusChart','multiBarChart','multiBarHorizontalChart','multiChart','parallelCoordinates','pieChart','scatterChart','scatterPlusLineChart','sparklinePlus','stackedAreaChart'];

  $scope.config = {
    visible: true, // default: true
    extended: false, // default: false
    disabled: false, // default: false
    autorefresh: true, // default: true
    refreshDataOnly: false // default: false
  };

  $scope.options = {
    chart: {
      type: 'linePlusBarChart',
      height: 350,
      margin: {
        top: 15,
        right: 35,
        bottom: 35,
        left: 35
      },
      x: function(d, i){return i;},
      y: function(d){return d[1];},
      showValues: true,
      valueFormat: function (d) {
        return d3.format(',')(d);
      },
      transitionDuration: 500,
      xAxis: {
        axisLabel: '',
        tickFormat: function(d) {
          var dx = $scope.callsData[0].values[d] && $scope.callsData[0].values[d][0] || 0;
          return dx ? d3.time.format('%d-%m-%Y')(new Date(dx)) : '';
        }
      },
      y1Axis: {
        axisLabel: 'Number of Calls'
      },
      y2Axis: {
        axisLabel: 'Duration'
      },
      lines: { // for line chart
        forceY: [1],
        yDomain: [0, 1000]
      },
      bars : {
        forceY: [1],
        yDomain: [0, 100]
      },
      tooltipContent: function (key, x, y, e, graph) {
        return '<h3 class="text-center" style="font-size: 1.2em;"><strong>' + e.point[0] + '</strong></h3>' +
          '<p>' +  y + ' ' + key.substr(0, key.indexOf('(')) + '</p>';
      }
    }
  };//end options});

  $scope.smsOptions = {
    "chart": {
      "type": "historicalBarChart",
      "height": 350,
      "margin": {
        "top": 15,
        "right": 10,
        "bottom": 35,
        "left": 20
      },
      x: function(d, i){ return i;},
      y: function(d){ return d[1];},
      "showValues": true,
      "transitionDuration": 500,
      xAxis: {
        axisLabel: '',
        tickFormat: function(d) {
          var dx = $scope.smsData[0].values[d] && $scope.smsData[0].values[d][0] || 0;
          return dx ? d3.time.format('%d-%m-%Y')(new Date(dx)).substr(0,2) : '';
        },
        ticks: 11,
        showMaxMin: false
      },
      "yAxis": {
        "axisLabel": "Y Axis",
        "axisLabelDistance": 35
      },
      tooltipContent: function (key, x, y, e, graph) {
        return '<h3 class="text-center" style="font-size: 1.2em;"><strong>' + e.point[0] + '</strong></h3>' +
          '<p>' +  y + ' ' + key + '</p>';
      }
    }
  }

});