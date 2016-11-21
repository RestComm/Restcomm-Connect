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

package org.restcomm.connect.identity.passwords;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.NotImplementedException;
import org.mindrot.jbcrypt.BCrypt;
import org.restcomm.connect.commons.security.PasswordAlgorithm;

/**
 * Various password utility methods.
 */
public class PasswordUtils {

    public static String hashPassword(String password, PasswordAlgorithm algorithm) {
        switch (algorithm) {
            case plain:
                return password;
            case md5:
                return DigestUtils.md5Hex(password);
            case bcrypt_salted:
                return BCrypt.hashpw(password, BCrypt.gensalt());
        }
        throw new NotImplementedException("Uknown password hasing algorithm: " + algorithm);
    }

    public static boolean verifyPassword(String inputPass, String storedPass, PasswordAlgorithm storedAlgorithm) {
        if (storedAlgorithm == PasswordAlgorithm.plain) {
            return storedPass.equals(inputPass);
        } else
        if (storedAlgorithm == PasswordAlgorithm.md5) {
            return storedPass.equals(DigestUtils.md5Hex(inputPass));
        }
        if (storedAlgorithm == PasswordAlgorithm.bcrypt_salted) {
            return BCrypt.checkpw(inputPass,storedPass); // this does not need salt. It seems its already stored with the hashed password
        }
        else
            throw new NotImplementedException("Password algorithm not supported: " + storedAlgorithm);
    }
}
