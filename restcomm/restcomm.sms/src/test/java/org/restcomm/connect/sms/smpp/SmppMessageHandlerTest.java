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
package org.restcomm.connect.sms.smpp;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import org.restcomm.connect.dao.entities.IncomingPhoneNumber;

public class SmppMessageHandlerTest {

    public SmppMessageHandlerTest() {
    }

    @Test
    public void testRemover() {
        List<IncomingPhoneNumber> numbers = new ArrayList();
        IncomingPhoneNumber.Builder builder =  IncomingPhoneNumber.builder();

        builder.setPhoneNumber("123.");
        numbers.add(builder.build());
        RegexRemover.removeRegexes(numbers);
        assertTrue(numbers.isEmpty());

        builder.setPhoneNumber("123*");
        numbers.add(builder.build());
        RegexRemover.removeRegexes(numbers);
        assertTrue(numbers.isEmpty());

        builder.setPhoneNumber("^123");
        numbers.add(builder.build());
        RegexRemover.removeRegexes(numbers);
        assertTrue(numbers.isEmpty());

        builder.setPhoneNumber(".");
        numbers.add(builder.build());
        RegexRemover.removeRegexes(numbers);
        assertTrue(numbers.isEmpty());

        builder.setPhoneNumber("[5]");
        numbers.add(builder.build());
        RegexRemover.removeRegexes(numbers);
        assertTrue(numbers.isEmpty());

        builder.setPhoneNumber("1|2");
        numbers.add(builder.build());
        RegexRemover.removeRegexes(numbers);
        assertTrue(numbers.isEmpty());

        builder.setPhoneNumber("\\d");
        numbers.add(builder.build());
        RegexRemover.removeRegexes(numbers);
        assertTrue(numbers.isEmpty());

        builder.setPhoneNumber("+1234");
        numbers.add(builder.build());
        builder.setPhoneNumber("+1234*");
        numbers.add(builder.build());
        builder.setPhoneNumber("+1234.*");
        numbers.add(builder.build());
        builder.setPhoneNumber("9887");
        numbers.add(builder.build());
        builder.setPhoneNumber("+");
        numbers.add(builder.build());
        RegexRemover.removeRegexes(numbers);
        assertEquals(3, numbers.size());
    }

}
