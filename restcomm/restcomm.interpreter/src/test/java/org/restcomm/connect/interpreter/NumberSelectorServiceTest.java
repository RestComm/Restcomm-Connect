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
package org.restcomm.connect.interpreter;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InOrder;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.IncomingPhoneNumbersDao;
import org.restcomm.connect.dao.entities.IncomingPhoneNumber;
import org.restcomm.connect.dao.entities.IncomingPhoneNumberFilter;

public class NumberSelectorServiceTest {

    public NumberSelectorServiceTest() {
    }

    @Test
    public void testNoMatch() {
        Sid srcSid = Sid.generate(Sid.Type.ORGANIZATION);
        Sid destSid = srcSid;
        String number = "1234";
        List<IncomingPhoneNumber> emptyNumbers = new ArrayList();

        IncomingPhoneNumbersDao numDao = Mockito.mock(IncomingPhoneNumbersDao.class);
        when(numDao.getIncomingPhoneNumbersByFilter((IncomingPhoneNumberFilter) any())).
                thenReturn(emptyNumbers, emptyNumbers, emptyNumbers);
        when(numDao.getIncomingPhoneNumbersRegex((IncomingPhoneNumberFilter) any())).
                thenReturn(emptyNumbers);
        InOrder inOrder = inOrder(numDao);
        NumberSelectorService service = new NumberSelectorService(numDao);

        IncomingPhoneNumber found = service.searchNumber(number, srcSid, destSid);

        Assert.assertNull(found);
        inOrder.verify(numDao, times(2)).getIncomingPhoneNumbersByFilter((IncomingPhoneNumberFilter) any());
        inOrder.verify(numDao, times(1)).getIncomingPhoneNumbersRegex((IncomingPhoneNumberFilter) any());
        inOrder.verify(numDao, times(1)).getIncomingPhoneNumbersByFilter((IncomingPhoneNumberFilter) any());

    }

    @Test
    public void testPerfectMatch() {
        Sid srcSid = Sid.generate(Sid.Type.ORGANIZATION);
        Sid destSid = Sid.generate(Sid.Type.ORGANIZATION);
        String number = "1234";
        List<IncomingPhoneNumber> numbers = new ArrayList();
        final IncomingPhoneNumber.Builder builder = IncomingPhoneNumber.builder();
        builder.setPhoneNumber(number);
        numbers.add(builder.build());
        IncomingPhoneNumbersDao numDao = Mockito.mock(IncomingPhoneNumbersDao.class);
        when(numDao.getIncomingPhoneNumbersByFilter((IncomingPhoneNumberFilter) any())).
                thenReturn(numbers);
        InOrder inOrder = inOrder(numDao);
        NumberSelectorService service = new NumberSelectorService(numDao);

        IncomingPhoneNumber found = service.searchNumber(number, srcSid, destSid);

        Assert.assertNotNull(found);
        Assert.assertEquals(number, found.getPhoneNumber());
        inOrder.verify(numDao, times(1)).getIncomingPhoneNumbersByFilter((IncomingPhoneNumberFilter) any());
        verify(numDao, never()).getIncomingPhoneNumbersRegex((IncomingPhoneNumberFilter) any());
    }

    @Test
    public void testUSMatch() {
        Sid srcSid = Sid.generate(Sid.Type.ORGANIZATION);
        Sid destSid = Sid.generate(Sid.Type.ORGANIZATION);
        String number = "+1234";
        List<IncomingPhoneNumber> emptyList = new ArrayList();
        List<IncomingPhoneNumber> numbers = new ArrayList();
        final IncomingPhoneNumber.Builder builder = IncomingPhoneNumber.builder();
        builder.setPhoneNumber(number);
        numbers.add(builder.build());
        IncomingPhoneNumbersDao numDao = Mockito.mock(IncomingPhoneNumbersDao.class);
        when(numDao.getIncomingPhoneNumbersByFilter((IncomingPhoneNumberFilter) any())).
                thenReturn(emptyList, numbers);
        InOrder inOrder = inOrder(numDao);
        NumberSelectorService service = new NumberSelectorService(numDao);

        IncomingPhoneNumber found = service.searchNumber("1234", srcSid, destSid);

        Assert.assertNotNull(found);
        Assert.assertEquals(number, found.getPhoneNumber());
        inOrder.verify(numDao, times(2)).getIncomingPhoneNumbersByFilter((IncomingPhoneNumberFilter) any());
        verify(numDao, never()).getIncomingPhoneNumbersRegex((IncomingPhoneNumberFilter) any());
    }

    @Test
    public void testNoPlusMatch() {
        Sid srcSid = Sid.generate(Sid.Type.ORGANIZATION);
        Sid destSid = Sid.generate(Sid.Type.ORGANIZATION);
        String number = "1234";
        List<IncomingPhoneNumber> emptyList = new ArrayList();
        List<IncomingPhoneNumber> numbers = new ArrayList();
        final IncomingPhoneNumber.Builder builder = IncomingPhoneNumber.builder();
        builder.setPhoneNumber(number);
        numbers.add(builder.build());
        IncomingPhoneNumbersDao numDao = Mockito.mock(IncomingPhoneNumbersDao.class);
        when(numDao.getIncomingPhoneNumbersByFilter((IncomingPhoneNumberFilter) any())).
                thenReturn(emptyList, numbers);
        InOrder inOrder = inOrder(numDao);
        NumberSelectorService service = new NumberSelectorService(numDao);

        IncomingPhoneNumber found = service.searchNumber("+1234", srcSid, destSid);

        Assert.assertNotNull(found);
        Assert.assertEquals(number, found.getPhoneNumber());
        inOrder.verify(numDao, times(2)).getIncomingPhoneNumbersByFilter((IncomingPhoneNumberFilter) any());
        verify(numDao, never()).getIncomingPhoneNumbersRegex((IncomingPhoneNumberFilter) any());
    }

    @Test
    public void testPerfectMatchNoOrg() {
        Sid srcSid = null;
        Sid destSid = null;
        String number = "1234";
        List<IncomingPhoneNumber> numbers = new ArrayList();
        final IncomingPhoneNumber.Builder builder = IncomingPhoneNumber.builder();
        builder.setPhoneNumber(number);
        numbers.add(builder.build());
        IncomingPhoneNumbersDao numDao = Mockito.mock(IncomingPhoneNumbersDao.class);
        when(numDao.getIncomingPhoneNumbersByFilter((IncomingPhoneNumberFilter) any())).
                thenReturn(numbers);
        InOrder inOrder = inOrder(numDao);
        NumberSelectorService service = new NumberSelectorService(numDao);

        IncomingPhoneNumber found = service.searchNumber(number, srcSid, destSid);

        Assert.assertNotNull(found);
        Assert.assertEquals(number, found.getPhoneNumber());
        inOrder.verify(numDao, times(1)).getIncomingPhoneNumbersByFilter((IncomingPhoneNumberFilter) any());
        verify(numDao, never()).getIncomingPhoneNumbersRegex((IncomingPhoneNumberFilter) any());
    }

    @Test
    public void testStarMatch() {
        Sid srcSid = Sid.generate(Sid.Type.ORGANIZATION);
        Sid destSid = srcSid;
        String number = "1234";
        String star = "*";
        List<IncomingPhoneNumber> emptyList = new ArrayList();
        List<IncomingPhoneNumber> numbers = new ArrayList();
        final IncomingPhoneNumber.Builder builder = IncomingPhoneNumber.builder();
        builder.setPhoneNumber(star);
        numbers.add(builder.build());
        IncomingPhoneNumbersDao numDao = Mockito.mock(IncomingPhoneNumbersDao.class);
        when(numDao.getIncomingPhoneNumbersByFilter((IncomingPhoneNumberFilter) any())).
                thenReturn(emptyList,emptyList, numbers);
        when(numDao.getIncomingPhoneNumbersRegex((IncomingPhoneNumberFilter) any())).
                thenReturn(emptyList);
        InOrder inOrder = inOrder(numDao);
        NumberSelectorService service = new NumberSelectorService(numDao);

        IncomingPhoneNumber found = service.searchNumber(number, srcSid, destSid);

        Assert.assertNotNull(found);
        Assert.assertEquals(star, found.getPhoneNumber());
        inOrder.verify(numDao, times(2)).getIncomingPhoneNumbersByFilter((IncomingPhoneNumberFilter) any());
        inOrder.verify(numDao, times(1)).getIncomingPhoneNumbersRegex((IncomingPhoneNumberFilter) any());
    }

    @Test
    public void testRegexNoOrg() {
        Sid srcSid = null;
        Sid destSid = null;
        String regex = "12.*";
        List<IncomingPhoneNumber> emptyNumbers = new ArrayList();
        List<IncomingPhoneNumber> numbers = new ArrayList();
        final IncomingPhoneNumber.Builder builder = IncomingPhoneNumber.builder();
        builder.setPhoneNumber(regex);
        numbers.add(builder.build());
        IncomingPhoneNumbersDao numDao = Mockito.mock(IncomingPhoneNumbersDao.class);
        when(numDao.getIncomingPhoneNumbersByFilter((IncomingPhoneNumberFilter) any())).
                thenReturn(emptyNumbers, emptyNumbers, emptyNumbers);
        when(numDao.getIncomingPhoneNumbersRegex((IncomingPhoneNumberFilter) any())).
                thenReturn(numbers);
        InOrder inOrder = inOrder(numDao);
        NumberSelectorService service = new NumberSelectorService(numDao);

        IncomingPhoneNumber found = service.searchNumber("1234", srcSid, destSid);

        Assert.assertNull(found);
        inOrder.verify(numDao, times(3)).getIncomingPhoneNumbersByFilter((IncomingPhoneNumberFilter) any());
        inOrder.verify(numDao, never()).getIncomingPhoneNumbersRegex((IncomingPhoneNumberFilter) any());

    }

    @Test
    public void testRegexMatch() {
        Sid srcSid = Sid.generate(Sid.Type.ORGANIZATION);
        Sid destSid = srcSid;
        String regex = "12.*";
        String longestRegex = "123.*";
        List<IncomingPhoneNumber> emptyNumbers = new ArrayList();
        List<IncomingPhoneNumber> numbers = new ArrayList();
        final IncomingPhoneNumber.Builder builder = IncomingPhoneNumber.builder();
        builder.setPhoneNumber(regex);
        numbers.add(builder.build());
        builder.setPhoneNumber(longestRegex);
        numbers.add(builder.build());
        IncomingPhoneNumbersDao numDao = Mockito.mock(IncomingPhoneNumbersDao.class);
        when(numDao.getIncomingPhoneNumbersByFilter((IncomingPhoneNumberFilter) any())).
                thenReturn(emptyNumbers, emptyNumbers);
        when(numDao.getIncomingPhoneNumbersRegex((IncomingPhoneNumberFilter) any())).
                thenReturn(numbers);
        InOrder inOrder = inOrder(numDao);
        NumberSelectorService service = new NumberSelectorService(numDao);

        IncomingPhoneNumber found = service.searchNumber("1234", srcSid, destSid);

        Assert.assertNotNull(found);
        Assert.assertEquals(longestRegex, found.getPhoneNumber());
        inOrder.verify(numDao, times(2)).getIncomingPhoneNumbersByFilter((IncomingPhoneNumberFilter) any());
        inOrder.verify(numDao, times(1)).getIncomingPhoneNumbersRegex((IncomingPhoneNumberFilter) any());

    }

}
