/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

package org.mobicents.servlet.restcomm.identity;

import org.apache.commons.codec.digest.DigestUtils;
import org.mobicents.servlet.restcomm.entities.Account;

/**
 * Authenticates credentials againt a restcomm Account.
 *
 * @author orestis.tsakiridis@telestdax.com - Orestis Tsakiridis
 */
public class RestcommAuthenticator {

    /**
     * Verifies the passwordorAuthToken is correct for granting access to matchedAccount. It is important that
     * the account matches also the username/sid of the credentials. See AccountsDao.getAccountToAuthenticate().
     *
     * @param matchedAccount
     * @param passwordOrAuthToken
     * @return
     */
    public static boolean verifyPassword(Account matchedAccount, String passwordOrAuthToken ) {
        if (matchedAccount != null && passwordOrAuthToken != null) {
            // Compare both the plaintext version of the token and md5'ed version of it
            if ( passwordOrAuthToken.equals(matchedAccount.getAuthToken()) || DigestUtils.md5Hex(passwordOrAuthToken).equals(matchedAccount.getAuthToken())  ) {
                return true;
            }

        }
        return false;
    }


}
