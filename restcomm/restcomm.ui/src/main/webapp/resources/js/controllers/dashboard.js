'use strict';

var rcMod = angular.module('rcApp');

rcMod.controller('DashboardCtrl', function ($scope, $resource, $rootScope, RCommStatistics) {

  $scope.ctrl = {
    slides: [
    '<p>Telestax is a proud sponsor of 2017 TADHack Global and TADSummit</p><p><img src="http://tadsummit.com/2017/wp-content/uploads/2017/06/TADS-2017-logo.png" width="90%"></p>',
    '<img src="https://secure.meetupstatic.com/photos/event/a/c/d/8/600_456464248.jpeg" align="left" width="120" hspace="10"> <h3>WEBINAR</h3><h5>Moving from Voice to Text increases Call Center Closure Rates by over 200%</h5><p class="pull-right">Learn more</p>',
    ]
  };

  var startDate = new Date(new Date().setDate(new Date().getDate() - 30));
  var startDateJSON = startDate.toJSON().slice(0,10);
  var endDate = new Date();
  var endDateJSON = endDate.toJSON().slice(0,10);
  RCommStatistics.query({accountSid: $scope.sid, statName: 'Daily', StartDate: startDateJSON, EndDate: endDateJSON}, function(data) {
    $scope.callsData = [
      {
        key : 'Calls' ,
        bar: true,
        color: '#44A5AB',
        values : [],
      },
      {
        key : 'Minutes' ,
        color: '#DD4730',
        'stroke-width': '5px',
        values : []
      }
    ];
    var maxDuration = 0;
    var maxCalls = 0;

    var lastDate = new Date(startDate);
    lastDate.setUTCHours(0,0,0,0);
    angular.forEach(data, function(value, key) {
      // fill with missing dates
      while (lastDate < new Date(value.start_date)) {
        console.log(lastDate, new Date(value.start_date));
        $scope.callsData[0].values.push([lastDate.getTime(), parseInt(Math.random()*30)]);
        $scope.callsData[1].values.push([lastDate.getTime(), parseInt(Math.random()*100)]);
        lastDate.setDate(lastDate.getDate() + 1);
      }
      lastDate.setDate(lastDate.getDate() + 1);

      //console.log(value.start_date, value.count, value.usage);
      $scope.callsData[0].values.push([new Date(value.start_date).getTime(), value.count || 0]);
      $scope.callsData[1].values.push([new Date(value.start_date).getTime(), value.usage || 0]);
      maxDuration = Math.max(maxDuration, value.usage);
      maxCalls = Math.max(maxCalls, value.count);
    });

    while (lastDate <= endDate) {
      $scope.callsData[0].values.push([lastDate.getTime(), parseInt(Math.random()*30)]);
      $scope.callsData[1].values.push([lastDate.getTime(), parseInt(Math.random()*100)]);
      lastDate.setDate(lastDate.getDate() + 1);
    }

    console.log('callsData', $scope.callsData);

    $scope.callsOptions.chart.bars.yDomain = [0, Math.max(parseInt(maxCalls * 1.1), 100)];;
    $scope.callsOptions.chart.lines.yDomain = [0, Math.max(parseInt(maxDuration * 1.1), 100)];;
    $scope.callsAPI.updateWithOptions($scope.callsOptions);
  });

  RCommStatistics.query({accountSid: $scope.sid, statName: 'Daily', Category: 'SMS', StartDate: startDateJSON, EndDate: endDateJSON}, function(data) {
    $scope.smsData = [
      {
        key : 'Messages' ,
        bar: true,
        values : []
      }
    ];
    var maxSMS = 0;

    var smsLastDate = new Date(startDate);
    angular.forEach(data, function(value,key){
      // fill with missing dates
      while (smsLastDate < new Date(value.start_date)) {
        $scope.smsData[0].values.push([smsLastDate.getTime(), parseInt(Math.random()*100)]);
        smsLastDate.setDate(smsLastDate.getDate() + 1);
      }
      smsLastDate.setDate(smsLastDate.getDate() + 1);

      //console.log(value.start_date, value.count);
      $scope.smsData[0].values.push([new Date(value.start_date).getTime(), value.count || 0, value.start_date]);
      maxSMS = Math.max(maxSMS, value.count);
    });

    while (smsLastDate <= endDate) {
      $scope.smsData[0].values.push([smsLastDate.getTime(), parseInt(Math.random()*100)]);
      smsLastDate.setDate(smsLastDate.getDate() + 1);
    }

    $scope.smsOptions.chart.yDomain = [0, Math.max(parseInt(maxSMS * 1.1), 100)];
    $scope.smsAPI.updateWithOptions($scope.smsOptions);
  });

  $scope.callsOptions = {
    chart: {
      legendLeftAxisHint: '',
      legendRightAxisHint: '',
      type: 'linePlusBarChart',
      focusEnable: false,
      useInteractiveGuideline: true,
      height: 400,
      margin: {
        top: 15,
        right: 50,
        bottom: 100,
        left: 35
      },
      x: function(d, i){return d[0];},
      y: function(d){return d[1];},
      showValues: true,
      valueFormat: function (d) {
        return d3.format(',')(d);
      },
      transitionDuration: 500,
      xAxis: {
        axisLabel: '',
        showMaxMin: false,
        rotateLabels: 45,
        tickFormat: function(d, v) {
          return d3.time.format('%d-%m-%Y')(new Date(d));
        },
        tickValues: function(values) {
          return _.map(values[0].values, function(val, idx, arr) {
            return idx % 2 === 0 ? val[0] : (arr.length <= 16 ? val[0] : '');
          });
        }
      },
      y1Axis: {
        axisLabel: 'Number of Calls',
      },
      y2Axis: {
        axisLabel: 'Duration'
      },
      lines: { // for line chart
        yDomain: [0, 1000]
      },
      bars : {
        yDomain: [0, 100]
      },
      tooltipContent: function (key, x, y, e, graph) {
        return '<h3 class="text-center" style="font-size: 1.2em;"><strong>' + e.point[0] + '</strong></h3>' +
          '<p>' +  y + ' ' + key.substr(0, key.indexOf('(')) + '</p>';
      },
    }
  };//end options});

  $scope.smsOptions = {
    chart: {
      type: "historicalBarChart",
      height: 400,
      margin: {
        top: 15,
        right: 50,
        bottom: 100,
        left: 35
      },
      x: function(d, i){ return d[0];},
      y: function(d){ return d[1];},
      useInteractiveGuideline: true,
      showValues: true,
      transitionDuration: 500,
      xAxis: {
        axisLabel: '',
        showMaxMin: false,
        rotateLabels: 45,
        tickFormat: function(d, v) {
          return d3.time.format('%d-%m-%Y')(new Date(d));
        },
        tickValues: function(values) {
          return _.map(values[0].values, function(val, idx, arr) {
            return idx % 2 === 0 ? val[0] : (arr.length <= 16 ? val[0] : '');
          });
        }
      },
      tooltipContent: function (key, x, y, e, graph) {
        return '<h3 class="text-center" style="font-size: 1.2em;"><strong>' + e.point[0] + '</strong></h3>' +
          '<p>' +  y + ' ' + key + '</p>';
      }
    }
  };

});
