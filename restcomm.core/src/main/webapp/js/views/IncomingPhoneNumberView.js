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
define(['jquery', 'underscore', 'backbone', 'text!templates/IncomingPhoneNumberTemplate.html',
    'jqueryui', 'jquerycookie'], function($, _, Backbone, IncomingPhoneNumberTemplate) {
  var IncomingPhoneNumberView = Backbone.View.extend( {
    render: function(sid) {
      var path = "/restcomm/2012-04-24/Accounts/" + $.cookie('sid') + "/IncomingPhoneNumbers/" +
      sid + ".json";
      var url = "http://" + $.cookie('sid') + ":" + $.cookie('auth-token') + "@" + $.cookie('host')
      	+ path;
      $('#dashboard-busy-indicator').show();
      $.getJSON(url, function(data, status) {
    	  var template = _.template(IncomingPhoneNumberTemplate, data);
    	  $('#content').empty();
    	  $('#content').append(template);
    	  $('#save-button').button();
    	  $('#save-button').click(function(event) {
    		var voiceUrl = $('#voice-url').val();
    		var voiceMethod = $('.voice-method').filter(':selected').val();
    		$('#dashboard-busy-indicator').show();
	        $.post(url, "VoiceUrl=" + voiceUrl + "&VoiceMethod=" + voiceMethod, new function() {
	        	window.location.hash = '#/user/account/phone-numbers/incoming';
	        }).fail(function() {
	        	window.location.hash = '#/';
	        });
	      });
    	  // event handlers for updating data.
      }).error(function() {
    	  window.location.hash = '#/';
      }).complete(function() {
    	  $('#dashboard-busy-indicator').hide();
      });
    }
  });
  return IncomingPhoneNumberView;
});