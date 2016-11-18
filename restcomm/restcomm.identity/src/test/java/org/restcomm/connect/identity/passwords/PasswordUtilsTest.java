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

import junit.framework.Assert;
import org.junit.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.restcomm.connect.commons.security.PasswordAlgorithm;

/**
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
public class PasswordUtilsTest {
    @Test
    public void foo() {
        Assert.assertEquals("RestComm",PasswordUtils.hashPassword("RestComm", PasswordAlgorithm.plain));
        Assert.assertEquals("77f8c12cc7b8f8423e5c38b035249166",PasswordUtils.hashPassword("RestComm", PasswordAlgorithm.md5));
        Assert.assertTrue("BCrypt hashed password verification failed",BCrypt.checkpw("RestComm",PasswordUtils.hashPassword("RestComm", PasswordAlgorithm.bcrypt_salted))); // "ACae6e420f425248d6a26948c17a9e2acf"
    }

}
