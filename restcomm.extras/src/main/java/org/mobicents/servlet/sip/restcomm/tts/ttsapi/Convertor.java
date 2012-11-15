package org.mobicents.servlet.sip.restcomm.tts.ttsapi;

import it.sauronsoftware.jave.AudioAttributes;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.EncoderException;
import it.sauronsoftware.jave.EncodingAttributes;
import it.sauronsoftware.jave.InputFormatException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.FileUtils;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 */

public class Convertor {

	public static URI convert(URI source) throws Exception{
//		File sourceFile = new File
		URL sourceURL = source.toURL();
		File sourceFile = File.createTempFile("source", ".mp3");
		sourceFile.deleteOnExit();
		FileUtils.copyURLToFile(sourceURL, sourceFile);
		String target = sourceFile.getPath().replaceAll("mp3", "wav");
		File targetFile = new File(target);
		targetFile.deleteOnExit();
		AudioAttributes audio = new AudioAttributes();
		audio.setCodec("pcm_s16le");
		EncodingAttributes attrs = new EncodingAttributes();
		attrs.setFormat("wav");
		attrs.setAudioAttributes(audio);
		Encoder encoder = new Encoder();
		encoder.encode(sourceFile, targetFile, attrs);
		return targetFile.toURI();
	}	
}
