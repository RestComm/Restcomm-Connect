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

package org.restcomm.connect.mgcp;

import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Dmitriy Nadolenko
 */
public class AsrSignalTest {

    public static final String DEFAULT_LANG = "en-US";

    private String driver;
    private List<URI> initialPrompts;
    private String endInputKey;
    private long maximumRecTimer;
    private long waitingInputTimer;
    private long timeAfterSpeech;
    private String hotWords;
    private String input;
    private int numberOfDigits;

    @Before
    public void init() {
        driver = "no_name_driver";
        initialPrompts = Collections.singletonList(URI.create("hello.wav"));
        endInputKey = "#";
        maximumRecTimer = 10L;
        waitingInputTimer = 10L;
        timeAfterSpeech = 5L;
        hotWords = "Wait";
        input = "dtmf_speech";
        numberOfDigits = 1;
    }

    @Test
    public void testFormatting() {
        String expectedResult = "ip=hello.wav dr=no_name_driver ln=en-US eik=# mrt=100 wit=100 pst=50 hw=57616974 in=dtmf_speech mn=1 mx=1";
        AsrSignal asrSignal = new AsrSignal(driver, DEFAULT_LANG, initialPrompts, endInputKey, maximumRecTimer, waitingInputTimer,
                timeAfterSpeech, hotWords, input, numberOfDigits);
        String actualResult = asrSignal.toString();

        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testFormattingWithMultiplePrompts() {
        initialPrompts = new ArrayList<URI>() {{
           add(URI.create("hello.wav"));
           add(URI.create("world.wav"));
        }};
        String expectedResult = "ip=hello.wav,world.wav dr=no_name_driver ln=en-US eik=# mrt=100 wit=100 pst=50 hw=57616974 in=dtmf_speech mn=1 mx=1";
        AsrSignal asrSignal = new AsrSignal(driver, DEFAULT_LANG, initialPrompts, endInputKey, maximumRecTimer, waitingInputTimer,
                timeAfterSpeech, hotWords, input, numberOfDigits);
        String actualResult = asrSignal.toString();

        assertEquals(expectedResult, actualResult);
    }
}
