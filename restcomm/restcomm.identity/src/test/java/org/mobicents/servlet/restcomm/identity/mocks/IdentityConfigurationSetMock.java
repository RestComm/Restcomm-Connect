/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2016, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.mobicents.servlet.restcomm.identity.mocks;

import org.mobicents.servlet.restcomm.configuration.sets.IdentityConfigurationSet;

/**
 * @author Orestis Tsakiridis
 */
public class IdentityConfigurationSetMock implements IdentityConfigurationSet {

    Boolean headless;
    String authServerBaseUrl;
    String authServerUrl;
    String username;
    String password;
    Boolean inviteExistingUsers;
    String adminAccountSid;
    String[] redirectUris;
    MigrationMethod method;
    String realm = "restcomm";
    String realmKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv";

    public IdentityConfigurationSetMock() {
    }

    public void setHeadless(Boolean headless) {
        this.headless = headless;
    }

    public void setAuthServerBaseUrl(String authServerBaseUrl) {
        this.authServerBaseUrl = authServerBaseUrl;
    }

    public void setAuthServerUrl(String authServerUrl) {
        this.authServerUrl = authServerUrl;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setInviteExistingUsers(Boolean inviteExistingUsers) {
        this.inviteExistingUsers = inviteExistingUsers;
    }

    public void setAdminAccountSid(String adminAccountSid) {
        this.adminAccountSid = adminAccountSid;
    }

    public void setRedirectUris(String[] redirectUris) {
        this.redirectUris = redirectUris;
    }

    public void setMethod(MigrationMethod method) {
        this.method = method;
    }

    @Override
    public Boolean getHeadless() {
        return headless;
    }

    @Override
    public String getAuthServerBaseUrl() {
        return authServerBaseUrl;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Boolean getInviteExistingUsers() {
        return inviteExistingUsers;
    }

    @Override
    public String getAdminAccountSid() {
        return adminAccountSid;
    }

    @Override
    public String[] getRedirectUris() {
        return redirectUris;
    }

    @Override
    public String getRealm() {
        return realm;
    }

    @Override
    public String getRealmkey() {
        return realmKey;
    }

    @Override
    public MigrationMethod getMethod() {
        return method;
    }

    @Override
    public String getAuthServerUrl() {
        return authServerUrl;
    }
}
