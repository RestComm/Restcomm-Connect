package org.mobicents.servlet.sip.restcomm;

import static org.junit.Assert.assertTrue;

import it.sauronsoftware.jave.EncoderException;
import it.sauronsoftware.jave.InputFormatException;

import java.io.File;
import java.net.URI;

import org.apache.commons.configuration.BaseConfiguration;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mobicents.servlet.sip.restcomm.tts.ttsapi.TtsApiSpeechSynthesizer;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 */

public class TtsApiSynthesizerTest {

	BaseConfiguration conf;
	
	@Before
	public void setUp(){
		conf = new BaseConfiguration();
		conf.addProperty("service-root", "http://tts-api.com/tts.mp3");
		conf.addProperty("cache-path",System.getProperty("java.io.tmpdir"));		
	}
	
	@Test @Ignore
	public void testSynthesizer() throws IllegalArgumentException, InputFormatException, EncoderException{
		TtsApiSpeechSynthesizer ttsapi = new TtsApiSpeechSynthesizer();
		ttsapi.configure(conf);
		ttsapi.start();
		URI ttsURI = ttsapi.synthesize("Hello World. How are you");

		File ttsFile = new File(ttsURI);
		assertTrue(ttsFile.exists());
		assertTrue(ttsFile.delete());
	}
}
