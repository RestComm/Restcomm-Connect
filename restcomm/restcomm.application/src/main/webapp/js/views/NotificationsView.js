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
define(['jquery', 'underscore', 'backbone', 'collections/NotificationCollection',
    'text!templates/NotificationListTemplate.html', 'jqueryui', 'jquerycookie'],
    function($, _, Backbone, NotificationCollection, NotificationListTemplate) {
  var NotificationsView = Backbone.View.extend( {
    render: function() {
      var path = "/restcomm/2012-04-24/Accounts/" + $.cookie('sid') + "/Notifications.json";
      var url = "http://" + $.cookie('sid') + ":" + $.cookie('auth-token') + "@" +
      	$.cookie('host') + path;
      $('#dashboard-busy-indicator').show();
      $.getJSON(url, function(data, status) {
    	  var collection = { notifications: new NotificationCollection(data).models };
    	  var template = _.template(NotificationListTemplate, collection);
    	  $('#content').empty();
    	  $('#content').append(template);
      }).error(function() {
    	  window.location.hash = '#/';
      }).complete(function() {
    	  $('#dashboard-busy-indicator').hide();
      });
    }
  });
  return NotificationsView;
});