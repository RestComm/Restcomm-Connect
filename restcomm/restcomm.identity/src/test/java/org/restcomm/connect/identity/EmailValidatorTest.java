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

import junit.framework.Assert;
import org.junit.Test;

/**
 * @author ddh.huy@gmail.com (Huy Dang)
 */
public class EmailValidatorTest {
    @Test
    public void emailValidationTest() {
        Assert.assertFalse(EmailValidator.isValidEmailFormat("1234"));
        Assert.assertFalse(EmailValidator.isValidEmailFormat("asdf123"));
        Assert.assertFalse(EmailValidator.isValidEmailFormat("asd@123"));
        Assert.assertFalse(EmailValidator.isValidEmailFormat("γιωργος123#!@"));
        Assert.assertFalse(EmailValidator.isValidEmailFormat("\"Test email\"@meail.com"));
        Assert.assertFalse(EmailValidator.isValidEmailFormat("user@.email.com"));
        Assert.assertFalse(EmailValidator.isValidEmailFormat("Test email <test-user@email.com>"));
        Assert.assertFalse(EmailValidator.isValidEmailFormat("123.test.email@email.test.com"));

        Assert.assertTrue(EmailValidator.isValidEmailFormat("testemail@127.0.0.1"));
        Assert.assertTrue(EmailValidator.isValidEmailFormat("test.email@email.test.com"));
        Assert.assertTrue(EmailValidator.isValidEmailFormat("test-email@email-test.com"));
        Assert.assertTrue(EmailValidator.isValidEmailFormat("testemail@email.com"));
    }
}
