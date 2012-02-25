package org.mobicents.servlet.sip.restcomm;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.text.StrLookup;

public class ConfigurationStringLookup extends StrLookup {
  private final Map<String, String> dictionary;
  
  public ConfigurationStringLookup() {
    super();
    dictionary = new HashMap<String, String>();
  }
  
  public void addProperty(final String name, final String value) {
    dictionary.put(name, value);
  }
  
  @Override public String lookup(final String key) {
	final String result = dictionary.get(key);
    if(result != null) {
      return result;
    } else {
      return key;
    }
  }
}
