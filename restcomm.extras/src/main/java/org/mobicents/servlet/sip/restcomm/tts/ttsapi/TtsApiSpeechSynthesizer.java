package org.mobicents.servlet.sip.restcomm.tts.ttsapi;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.mobicents.servlet.sip.restcomm.cache.DiskCache;
import org.mobicents.servlet.sip.restcomm.tts.AbstractSpeechSynthesizer;
import org.mobicents.servlet.sip.restcomm.tts.SpeechSynthesizerException;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 */

public class TtsApiSpeechSynthesizer extends AbstractSpeechSynthesizer {
	private static final String soundFileType = "WAV";
	private URI serviceRoot;
	private DiskCache cache;

	@Override 
	public void start() throws RuntimeException {
		serviceRoot = URI.create(configuration.getString("service-root"));
		cache = new DiskCache(configuration.getString("cache-path"));
	}

	public URI getSpeech(final String text, final String gender, final String language) throws SpeechSynthesizerException {		
		List<NameValuePair> qparams = new ArrayList<NameValuePair>();
		qparams.add(new BasicNameValuePair("q", text));
		qparams.add(new BasicNameValuePair("return_url", "1"));

		try {
			URI uri = URIUtils.createURI(serviceRoot.getScheme(), serviceRoot.getHost(), serviceRoot.getPort(), 
					serviceRoot.getPath(), URLEncodedUtils.format(qparams, "UTF-8"), null);
			HttpGet httpget = new HttpGet(uri);
			HttpClient client = new DefaultHttpClient();
			final HttpResponse response = client.execute(httpget);
			final int status = response.getStatusLine().getStatusCode();
			if(status == HttpStatus.SC_OK) {
				HttpEntity entity = response.getEntity();
				if (entity!=null){
					InputStream stream = entity.getContent();
			         BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			         URI source = URI.create(reader.readLine());
			         ArrayList<URI> result = (ArrayList<URI>) Convertor.convert(source);
			         URI mediaURI = result.get(1);
			         URI cacheURI = cache.put(buildKey(text, "men", "en"), mediaURI); 
			         for(URI fileURI: result){
			        	 (new File(fileURI)).delete();
			         }
			         return cacheURI;
				} else {
					final StringBuilder buffer = new StringBuilder();
					buffer.append("Error to access TTS-API service");
					throw new SpeechSynthesizerException(buffer.toString());
				}
			} else {
				final String reason = response.getStatusLine().getReasonPhrase();
				final StringBuilder buffer = new StringBuilder();
				buffer.append(status).append(" ").append(reason);
				throw new IOException(buffer.toString());
			}
		} catch(final Exception exception) {
			throw new SpeechSynthesizerException(exception);
		}
	}

	@Override
	public boolean isSupported(String language) throws IllegalArgumentException {
		return "en".equals(language);
	}

	public URI synthesize(String text){
		return synthesize(text,"men","en");
	}
	
	@Override
	public URI synthesize(String text, String gender, String language) throws SpeechSynthesizerException {
		String key = buildKey(text, gender, language);
		if(cache.contains(key, soundFileType)) {
			return cache.get(key, soundFileType);
		} else {
			return getSpeech(text, gender, language);
		}
	}

}
