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

    @Before
    public void init() {
        driver = "no_name_driver";
        initialPrompts = Collections.singletonList(URI.create("hello.wav"));
        endInputKey = "#";
        maximumRecTimer = 10L;
        waitingInputTimer = 10L;
        timeAfterSpeech = 5L;
        hotWords = "Wait";
    }

    @Test
    public void testFormatting() {
        String expectedResult = "ip=hello.wav dr=no_name_driver ln=en-US eik=# mrt=100 wit=100 pst=50 hw=57:61:69:74";
        AsrSignal asrSignal = new AsrSignal(driver, DEFAULT_LANG, initialPrompts, endInputKey, maximumRecTimer, waitingInputTimer,
                timeAfterSpeech, hotWords);
        String actualResult = asrSignal.toString();

        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testFormattingWithMultiplePrompts() {
        initialPrompts = new ArrayList<URI>() {{
           add(URI.create("hello.wav"));
           add(URI.create("world.wav"));
        }};
        String expectedResult = "ip=hello.wav,world.wav dr=no_name_driver ln=en-US eik=# mrt=100 wit=100 pst=50 hw=57:61:69:74";
        AsrSignal asrSignal = new AsrSignal(driver, DEFAULT_LANG, initialPrompts, endInputKey, maximumRecTimer, waitingInputTimer,
                timeAfterSpeech, hotWords);
        String actualResult = asrSignal.toString();

        assertEquals(expectedResult, actualResult);
    }
}
