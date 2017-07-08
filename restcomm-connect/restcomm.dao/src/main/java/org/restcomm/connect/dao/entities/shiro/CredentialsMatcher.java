/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
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
package org.restcomm.connect.dao.entities.shiro;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.credential.SimpleCredentialsMatcher;
import org.restcomm.connect.commons.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe
public final class CredentialsMatcher extends SimpleCredentialsMatcher {
    public CredentialsMatcher() {
        super();
    }

    @Override
    public boolean doCredentialsMatch(final AuthenticationToken token, final AuthenticationInfo info) {
        final String tokenCredentials = new String((char[]) token.getCredentials());
        final String accountCredentials = new String((char[]) info.getCredentials());

        if (accountCredentials.equals(tokenCredentials)){
            return true;
        } else {
            final String hashedToken = DigestUtils.md5Hex(tokenCredentials);
            return accountCredentials.equals(hashedToken);
        }
    }
}
