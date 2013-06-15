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
define(['jquery', 'underscore', 'backbone', 'views/CallsView', 'views/LoginView',
    'views/DashboardView', 'views/NotificationsView', 'views/RecordingsView',
    'views/SmsMessagesView', 'views/TranscriptionsView', 'views/IncomingPhoneNumberView',
    'views/IncomingPhoneNumbersView'],
	function($, _, Backbone, CallsView, LoginView, DashboardView, NotificationsView,
		RecordingsView, SmsMessagesView, TranscriptionsView, IncomingPhoneNumberView,
		IncomingPhoneNumbersView) {
  var ApplicationRouter = Backbone.Router.extend({
    routes: {
      // URL routes
      '': 'showLogin',
      'user/account': 'showDashboard',
      'user/account/phone-numbers/PN:sid': 'showIncomingPhoneNumber',
      'user/account/phone-numbers/incoming': 'showIncomingPhoneNumberList',
      'user/account/log/calls': 'showCallList',
      'user/account/log/sms': 'showSmsMessageList',
      'user/account/log/recordings': 'showRecordingList',
      'user/account/log/transcriptions': 'showTranscriptionList',
      'user/account/log/notifications': 'showNotificationList',
      // Default route
      '*actions': 'defaultAction'
    }
  });

  var initialize = function() {
    var application_router = new ApplicationRouter();
    application_router.on('route:showLogin', function() { new LoginView().render(); });
    application_router.on('route:showDashboard', function() { new DashboardView().render(); });
    application_router.on('route:showIncomingPhoneNumber', function(sid) { new IncomingPhoneNumberView().render("PN" + sid); });
    application_router.on('route:showIncomingPhoneNumberList', function() { new IncomingPhoneNumbersView().render(); });
    application_router.on('route:showCallList', function() { new CallsView().render(); });
    application_router.on('route:showSmsMessageList', function() { new SmsMessagesView().render(); });
    application_router.on('route:showRecordingList', function() { new RecordingsView().render(); });
    application_router.on('route:showTranscriptionList', function() {  new TranscriptionsView().render(); });
    application_router.on('route:showNotificationList', function() {  new NotificationsView().render(); });
    application_router.on('route:defaultAction', function(actions) {
      // We have no matching route, lets just log the URL.
      console.log('No route:', actions);
    });
    Backbone.history.start();
  };
  
  return {
    initialize: initialize
  };
});