package org.mobicents.servlet.sip.restcomm.util;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;

@ThreadSafe public final class UriUtils {
  private UriUtils() {
    super();
  }
  
  public static Map<String, String> parseEntity(final HttpEntity entity)
      throws IllegalStateException, IOException {
	final int length = (int)entity.getContentLength();
	final byte[] data = new byte[length];
	entity.getContent().read(data, 0, length);
    final String input = new String(data);
    final String[] tokens = input.split("&");
    final Map<String, String> map = new HashMap<String, String>();
    for(final String token : tokens) {
      final String[] parts = token.split("=");
      if(parts.length == 1) {
        map.put(parts[0], null);
      } else if(parts.length == 2) {
        map.put(parts[0], URLDecoder.decode(parts[1], "UTF-8"));
      }
    }
    return map;
  }
}
