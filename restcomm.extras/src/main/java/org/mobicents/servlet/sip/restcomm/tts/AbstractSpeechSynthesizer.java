package org.mobicents.servlet.sip.restcomm.tts;

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.sip.restcomm.util.HexadecimalUtils;

@NotThreadSafe public abstract class AbstractSpeechSynthesizer implements SpeechSynthesizer {
  protected Configuration configuration;

  public AbstractSpeechSynthesizer() {
    super();
  }
  
  public String buildKey(final String text, final String gender, final String language) {
    final StringBuilder key = new StringBuilder();
    key.append(language).append(":").append(gender).append(":").append(text);
    return hash(key.toString());
  }

  @Override public void configure(final Configuration configuration) {
    this.configuration = configuration;
  }
  
  private String hash(final String key) {
    MessageDigest messageDigest = null;
	try {
	  messageDigest = MessageDigest.getInstance("SHA-256");
	} catch(final NoSuchAlgorithmException ignored) { }
	messageDigest.update(key.getBytes());
	final byte[] hash = messageDigest.digest();
	return new String(HexadecimalUtils.toHex(hash));
  }

  @Override public void shutdown() { }

  @Override public void start() throws RuntimeException { }

  @Override	public abstract boolean isSupported(String language)
      throws IllegalArgumentException;

  @Override public abstract URI synthesize(String text, String gender, String language)
      throws SpeechSynthesizerException;
}
