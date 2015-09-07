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
package org.mobicents.servlet.restcomm.mgcp;

import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * @author thomas.quintana@telestax.com (Thomas Quintana)
 */
public final class PlayTest {
    public PlayTest() {
        super();
    }

    @Test
    public void testFormatting() {
        final List<URI> announcements = new ArrayList<URI>();
        announcements.add(URI.create("hello.wav"));
        final Play play = new Play(announcements, 1);
        final String result = play.toString();
        assertTrue("an=hello.wav it=1".equals(result));
        System.out.println("Signal Parameters: " + result + "\n");
    }

    @Test
    public void testFormattingWithMultipleAnnouncements() {
        final List<URI> announcements = new ArrayList<URI>();
        announcements.add(URI.create("hello.wav"));
        announcements.add(URI.create("world.wav"));
        final Play play = new Play(announcements, 1);
        final String result = play.toString();
        assertTrue("an=hello.wav;world.wav it=1".equals(result));
        System.out.println("Signal Parameters: " + result + "\n");
    }

    @Test
    public void testFormattingWithNoAnnouncements() {
        final List<URI> announcements = new ArrayList<URI>();
        final Play play = new Play(announcements, 1);
        final String result = play.toString();
        assertTrue(result.isEmpty());
    }
}
