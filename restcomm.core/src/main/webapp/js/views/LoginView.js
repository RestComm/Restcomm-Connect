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
define(['jquery', 'underscore', 'backbone', 'text!templates/LoginTemplate.html',
    'jqueryui', 'jquerycookie'], function($, _, Backbone, LoginTemplate) {
  var LoginView = Backbone.View.extend( {
    el: $('#content'),
    render: function() {
      this.$el.empty();
      this.$el.append(LoginTemplate);
      $('#login-button, #clear-button').button();
      $('#login-button').click(function(event) {
        $.cookie('sid', $('#sid').val());
        $.cookie('auth-token', $('#auth-token').val());
        $.cookie('host', $('#host').val());
        window.location.hash = '#/user/account';
      });
      $('#clear-button').click(function(event) {
        var fields = $('.login-dialog-form-input');
        for(item in fields) {
          fields[item].value = "";
        }
      });
    }
  });
  return LoginView;
});