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

package org.restcomm.connect.identity;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.log4j.Logger;

/**
 * @author ddh.huy@gmail.com (Huy Dang)
 */
public class EmailValidator {
    private static Logger logger = Logger.getLogger(EmailValidator.class);

    /**
     * Verify an email address has valid format or not
     *
     * @param email: the email address need to be verified
     * @return true if email format is valid, false if email format is invalid
     */
    public static boolean isValidEmailFormat ( String email ) {
        boolean isValid = false;
        try {
            InternetAddress emailAddress = new InternetAddress(email);
            emailAddress.validate();
            isValid = true;
        } catch (AddressException ex) {
            isValid = false;
            logger.error("Email " + email + " is invalid");
        }
        return isValid;
    }
}
