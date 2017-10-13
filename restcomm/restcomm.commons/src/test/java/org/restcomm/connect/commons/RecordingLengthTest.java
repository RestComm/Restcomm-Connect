package org.restcomm.connect.commons;

import org.junit.Test;
import org.restcomm.connect.commons.util.WavUtils;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

public class RecordingLengthTest {

    @Test
    public void testDuration() throws URISyntaxException, IOException, UnsupportedAudioFileException {
        URI recordingFileUri = this.getClass().getClassLoader().getResource("test_recording.wav").toURI();
        File recordingFile = new File(recordingFileUri);

        double duration = WavUtils.getAudioDuration(recordingFile);

        assertEquals(12, duration, 1.0);
    }

    @Test
    public void testDuration2() throws URISyntaxException, IOException, UnsupportedAudioFileException {
        URI recordingFileUri = this.getClass().getClassLoader().getResource("test_recording2.wav").toURI();
        File recordingFile = new File(recordingFileUri);

        double duration = WavUtils.getAudioDuration(recordingFile);

        assertEquals(19, duration, 1.0);
    }

}
