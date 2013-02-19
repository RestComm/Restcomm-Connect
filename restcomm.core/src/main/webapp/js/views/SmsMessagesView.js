/*
 * Telestax, Open Source Cloud Communications
 * Copyright 2013, Telestax, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
define(['jquery', 'underscore', 'backbone', 'collections/SmsMessageCollection',
    'text!templates/SmsMessageListTemplate.html', 'jqueryui', 'jquerycookie'],
    function($, _, Backbone, SmsMessageCollection, SmsMessageListTemplate) {
  var SmsMessagesView = Backbone.View.extend( {
    render: function() {
      var path = "/restcomm/2012-04-24/Accounts/" + $.cookie("sid") + "/SMS/Messages.json";
      var url = "http://" + $.cookie("sid") + ":" + $.cookie("auth-token") + "@" +
      	$.cookie('host') + path;
      $('#dashboard-busy-indicator').show();
      $.getJSON(url, function(data, status) {
    	  var collection = { smsMessages: new SmsMessageCollection(data).models };
    	  var template = _.template(SmsMessageListTemplate, collection);
    	  $('#content').empty();
    	  $('#content').append(template);
      }).error(function() {
    	  window.location.hash = '#/';
      }).complete(function() {
    	  $('#dashboard-busy-indicator').hide();
      });
    }
  });
  return SmsMessagesView;
});